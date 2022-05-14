(ns com.ben-allred.audiophile.ui.dev.core
  (:require
    [clojure.core.async :as async]
    [clojure.pprint :as pp]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.domain.validations.core :as val]
    [com.ben-allred.audiophile.ui.api.forms.standard :as form]
    [com.ben-allred.audiophile.ui.api.forms.submittable :as sres]
    [com.ben-allred.audiophile.ui.app :as app]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.audiophile.ui.infrastructure.system :as cfg]
    [com.ben-allred.vow.core :as v]
    [integrant.core :as ig]))

(defonce ^:private sys
  (atom nil))

(def ^:private config
  (cfg/load-config "ui-dev.edn" [:duct.profile/base :duct.profile/dev]))

(defn ^:export init []
  (pp/pprint config)
  (let [ms (if @sys 0 2500)]
    (reset! sys (try (ig/init config)
                     (catch :default ex
                       (log/error ex "ERROR!!!")
                       nil)))
    (async/go
      (async/<! (async/timeout ms))
      (app/init @sys))))

(defn ^:export halt []
  (some-> sys deref ig/halt!))

(def ^:private email-re
  #"[a-z][a-z0-9-_\.]*[a-z]@[a-z][a-z0-9-_]*\.[a-z0-9-_\.]*[a-z]")

(def ^:private login-validator
  (val/validator {:spec [:map
                         [:email
                          [:re {:error/message "invalid email"} email-re]]]}))

(defmethod ig/init-key :audiophile.dev/login-form [_ {:keys [login-resource]}]
  (fn [route]
    (let [*form (sres/create login-resource (form/create route login-validator))]
      (fn [route]
        [comp/form {:*form       *form
                    :route       route
                    :class       ["login-form"]
                    :submit/text "Login"}
         [in/input (forms/with-attrs {:label       "email"
                                      :auto-focus? true}
                                     *form
                                     [:email])]]))))

(defmethod ig/init-key :audiophile.dev/login-fn [_ {:keys [nav]}]
  (fn [{value :form/value}]
    (let [params {:email        (:email value)
                  :redirect-uri (get-in value [:params :redirect-uri] "/")}]
      (nav/goto! nav :auth/login {:params params})
      (v/resolve))))

(defn component [k]
  (second (colls/only! (ig/find-derived @sys k))))
