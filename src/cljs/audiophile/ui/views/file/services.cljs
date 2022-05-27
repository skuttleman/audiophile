(ns audiophile.ui.views.file.services
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.infrastructure.protocols :as pcom]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.forms.watchable :as form.watch]
    [audiophile.ui.resources.impl :as ires]
    [audiophile.ui.services.pages :as pages]
    [audiophile.ui.views.file.player :as player]
    [audiophile.ui.views.file.protocols :as proto]
    [reagent.core :as r]))

(def ^:private comments#validator:new
  (val/validator {:spec specs/comment:create}))

(defn artifacts#res:fetch-one [{:keys [http-client nav store]}]
  (ires/base store
             (fn [{artifact-id :artifact/id}]
               (http/get http-client
                         (nav/path-for nav :routes.api/artifacts:id {:params {:artifact/id artifact-id}})
                         {:response-type :blob}))))

(defn comments#form:new [{:keys [store] :as sys} *comments version-id]
  (let [init-val {:comment/file-version-id version-id}
        *form (form.std/create store init-val comments#validator:new)]
    (pages/form:new sys
                    {:on-success    (fn [_]
                                      (res/request! *comments))
                     :remote->local (constantly init-val)}
                    *form
                    :routes.api/comments)))

(defn comments#res:fetch-all [sys file-id]
  (pages/res:fetch sys :routes.api/files:id.comments {:params {:file/id file-id}}))

(defn files#modal:version [sys body]
  (pages/modal:open sys [:h1.subtitle "Upload a new version"] body))

(defn files#nav:add-version! [{:keys [nav]} {:keys [handle] :as route} file]
  (doto (-> file :file/versions first :file-version/id)
    (->> (assoc-in route [:params :file-version-id]) (nav/replace! nav handle))))

(defn files#res:fetch-one [sys file-id]
  (pages/res:fetch sys :routes.api/files:id {:params {:file/id file-id}}))

(defn player#create [*artifact opts]
  (doto (player/->ArtifactPlayer (name (gensym)) (r/atom nil) *artifact)
    (proto/load! opts)))

(defn projects#nav:ui [{:keys [nav]} project-id]
  (nav/path-for nav :routes.ui/projects:id {:params {:project/id project-id}}))

(defn versions#nav:qp [{:keys [nav]}]
  (fn [version-id]
    (let [{:keys [handle params]} @nav]
      (nav/replace! nav handle {:params (assoc params :file-version-id version-id)}))))

(defn versions#form:selector [{:keys [store] :as sys} version-id]
  (let [handler (versions#nav:qp sys)]
    (doto (form.watch/create store {:file-version-id version-id})
      (add-watch ::qp (fn [_ _ _ val]
                        (handler (:file-version-id val)))))))

(defn id [player]
  (pcom/id player))

(defn set-region!
  ([player]
   (set-region! player nil))
  ([player opts]
   (proto/set-region! player opts)))

(defn region [player]
  (proto/region player))

(defn load!
  ([player]
   (load! player nil))
  ([player opts]
   (proto/load! player opts)))

(defn ready? [player]
  (proto/ready? player))

(defn error? [player]
  (proto/error? player))

(defn destroy! [player]
  (pcom/destroy! player))

(defn play-pause! [player]
  (proto/play-pause! player))

(defn playing? [player]
  (proto/playing? player))
