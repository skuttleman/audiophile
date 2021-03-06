(ns audiophile.ui.dev.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.infrastructure.protocols :as pcom]
    [audiophile.common.infrastructure.resources.protocols :as pres]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.app :as app]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.forms.submittable :as form.submit]
    [audiophile.ui.services.login :as login]
    [audiophile.ui.store.actions :as act]
    [audiophile.ui.system.core :as sys]
    [clojure.pprint :as pp]
    [com.ben-allred.vow.core :as v]
    [integrant.core :as ig]
    [reagent.core :as r]))

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
  (when-let [store (app/find-component @sys :services/store)]
    (store/dispatch! store act/modal#remove-all!))
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
    (let [opts {:params {:redirect-uri (:path route)
                         :email        email}}]
      (v/resolve (nav/goto! nav :routes.auth/login opts))))
  (status [_]
    :init)

  pcom/IDestroy
  (destroy! [_]))

(defmethod login/form :dev
  [_ {:keys [nav store]} route]
  (r/with-let [id (uuids/random)
               *form (form.submit/create id
                                         (form.std/create id store nil login-validator)
                                         (->LoginResource nav route))]
    [comp/form {:class       ["login-form"]
                :submit/text "Login"
                :*form       *form}
     [in/input (forms/with-attrs {:label       "email"
                                  :auto-focus? true}
                                 *form
                                 [:email])]]
    (finally
      (forms/destroy! *form))))

(defn print-store-state
  ([]
   (pp/pprint @(app/find-component @sys :services/store)))
  ([ks]
   (pp/pprint (select-keys @(app/find-component @sys :services/store) (map keyword ks)))))
