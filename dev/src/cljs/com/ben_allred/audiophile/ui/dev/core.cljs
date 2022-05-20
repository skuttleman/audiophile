(ns com.ben-allred.audiophile.ui.dev.core
  (:require
    [clojure.pprint :as pp]
    [com.ben-allred.audiophile.common.infrastructure.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.domain.validations.core :as val]
    [com.ben-allred.audiophile.common.infrastructure.navigation.core :as nav]
    [com.ben-allred.audiophile.ui.app :as app]
    [com.ben-allred.audiophile.ui.infrastructure.components.core :as comp]
    [com.ben-allred.audiophile.ui.infrastructure.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.infrastructure.forms.core :as forms]
    [com.ben-allred.audiophile.ui.infrastructure.forms.standard :as form.std]
    [com.ben-allred.audiophile.ui.infrastructure.forms.submittable :as form.submit]
    [com.ben-allred.audiophile.ui.infrastructure.system.core :as sys]
    [com.ben-allred.vow.core :as v]
    [integrant.core :as ig]))

(defonce ^:private sys
  (atom nil))

(def ^:private config
  (sys/load-config "ui-dev.edn" [:duct.profile/base :duct.profile/dev]))

(defn ^:export init []
  (reset! sys (try (ig/init config [:system.ui/components])
                   (catch :default ex
                     (log/error ex "ERROR!!!")
                     nil)))
  (doto @sys
    pp/pprint
    app/init))

(defn ^:export halt []
  (some-> sys deref ig/halt!))

(def ^:private email-re
  #"[a-z][a-z0-9-_\.]*[a-z]@[a-z][a-z0-9-_]*\.[a-z0-9-_\.]*[a-z]")

(def ^:private login-validator
  (val/validator {:spec [:map
                         [:email
                          [:re {:error/message "invalid email"} email-re]]]}))

(deftype LoginResource [nav route]
  pres/IResource
  (request! [_ {:keys [email]}]
    (nav/goto! nav :auth/login {:params {:redirect-uri (:path route)
                                         :email        email}})
    (v/resolve))
  (status [_]
    :init))

(defmethod ig/init-key :audiophile.dev.views/login-form [_ {:keys [nav]}]
  (fn [route]
    (let [*resource (->LoginResource nav route)
          *form (form.submit/create (form.std/create nil login-validator) *resource)]
      (fn [_route]
        [comp/form {:class       ["login-form"]
                    :submit/text "Login"
                    :*form       *form}
         [in/input (forms/with-attrs {:label       "email"
                                      :auto-focus? true}
                                     *form
                                     [:email])]]))))
