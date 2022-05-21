(ns com.ben-allred.audiophile.ui.infrastructure.services.teams
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.navigation.core :as nav]
    [com.ben-allred.audiophile.ui.infrastructure.forms.standard :as form.std]
    [com.ben-allred.audiophile.ui.infrastructure.forms.submittable :as form.submit]
    [com.ben-allred.audiophile.ui.infrastructure.resources.impl :as ires]))

(defn res:list [{:keys [http-client nav]}]
  (ires/http http-client
             (constantly {:method :get
                          :url    (nav/path-for nav :api/teams)})))

(defn form:new [{:keys [http-client nav]} on-submitted]
  (form.submit/create (form.std/create {:team/type :COLLABORATIVE})
                      (ires/http http-client
                                 (fn [body]
                                   {:method :post
                                    :url    (nav/path-for nav :api/teams)
                                    :body   {:data body}
                                    :http/async? true})
                                 on-submitted)))
