(ns audiophile.ui.views.file.services
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.common.infrastructure.navigation.core :as nav]
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

(defn artifacts#res:fetch-one [{:keys [http-client nav]}]
  (ires/base (fn [{artifact-id :artifact/id}]
               (http/get http-client
                         (nav/path-for nav :api/artifact {:params {:artifact/id artifact-id}})
                         {:response-type :blob}))))

(defn comments#form:new [sys *comments version-id]
  (let [init-val {:comment/file-version-id version-id}
        *form (form.std/create init-val comments#validator:new)]
    (pages/form:new sys
                    {:on-success    (fn [_]
                                      (res/request! *comments))
                     :remote->local (constantly init-val)}
                    *form
                    :api/comments)))

(defn comments#res:fetch-all [sys file-id]
  (pages/res:fetch sys :api/file.comments {:params {:file/id file-id}}))

(defn files#nav:add-version! [{:keys [nav]} {:keys [handle] :as route} file]
  (doto (-> file :file/versions first :file-version/id)
    (->> (assoc-in route [:params :file-version-id]) (nav/replace! nav handle))))

(defn files#res:fetch-one [sys file-id]
  (pages/res:fetch sys :api/file {:params {:file/id file-id}}))

(defn versions#form:selector [{:keys [nav store]} version-id]
  (doto (form.watch/create {:file-version-id version-id})
    (add-watch ::qp (fn [_ _ _ val]
                      (let [{:keys [handle params]} (:nav/route @store)]
                        (nav/replace! nav handle {:params (merge params val)}))))))

(defn player#create [*artifact]
  (player/->ArtifactPlayer (name (gensym)) (r/atom nil) *artifact))

(defn id [player]
  (proto/id player))

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
  (proto/destroy! player))

(defn play-pause! [player]
  (proto/play-pause! player))

(defn playing? [player]
  (proto/playing? player))
