(ns audiophile.ui.views.login.services
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.services.pages :as pages]
    [reagent.core :as r]))

(def ^:private users#validator:signup
  (val/validator {:spec specs/user:create}))

(defn users#form:signup [{:keys [nav store] :as sys} path]
  (let [{:user/keys [email handle] :as profile} (:user/profile @store)
        handle (or handle (second (re-find #"^([^@]+).*" email)))
        *conflicts (r/atom nil)
        *form (form.std/create (assoc profile :user/handle handle)
                               (pages/conflict-validator users#validator:signup *conflicts))
        attrs {:on-success (fn [{:login/keys [token]}]
                             (nav/goto! nav :routes.auth/login {:params {:redirect-uri path
                                                                         :login-token  token}}))
               :on-error   (fn [result]
                             (->> result
                                  (into {} (comp (map :error/details)
                                                 (mapcat :conflicts)
                                                 (map (fn [[k v]] [[k] v]))))
                                  (reset! *conflicts)))}]
    (pages/form:new sys attrs *form :routes.api/users)))

(defn users#nav:login! [{:keys [nav]} path]
  (fn [_]
    (nav/goto! nav :routes.auth/login {:params {:redirect-uri path}})))
