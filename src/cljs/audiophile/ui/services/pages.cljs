(ns audiophile.ui.services.pages
  (:require
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.forms.submittable :as form.submit]
    [audiophile.ui.resources.impl :as ires]
    [com.ben-allred.vow.core :as v]))

(defn res:fetch-all [{:keys [http-client nav]} handle]
  (ires/http http-client
             (constantly {:method :get
                          :url    (nav/path-for nav handle)})))

(defn form:new [{:keys [http-client nav]} {:keys [on-success on-error *res]} *form handle]
  (form.submit/create *form
                      (ires/http http-client
                                 (fn [body]
                                   {:method      :post
                                    :url         (nav/path-for nav handle)
                                    :body        {:data body}
                                    :http/async? true})
                                 (fn [vow]
                                   (-> vow
                                       (v/peek on-success on-error)
                                       (v/peek (fn [_]
                                                 (some-> *res res/request!))))))))
