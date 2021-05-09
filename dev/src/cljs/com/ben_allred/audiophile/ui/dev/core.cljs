(ns com.ben-allred.audiophile.ui.dev.core
  (:require
    [clojure.core.async :as async]
    [clojure.pprint :as pp]
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.common.services.resources.validated :as vres]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [com.ben-allred.audiophile.common.views.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.app :as app]
    [com.ben-allred.audiophile.ui.services.config :as cfg]
    [com.ben-allred.audiophile.ui.services.forms.standard :as forms.std]
    [com.ben-allred.formation.core :as f]
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
                       config)))
    (async/go
      (async/<! (async/timeout ms))
      (app/init @sys))))

(defn ^:export halt []
  (some-> sys deref ig/halt!))

(def validator
  (f/validator {:email (f/required "email is required")}))

(defmethod ig/init-key ::login-form [_ {:keys [login-resource]}]
  (fn [_page]
    (let [form (vres/create login-resource (forms.std/create nil validator))]
      (fn [page]
        [comp/form {:form        form
                    :page        page
                    :submit/text "Login"}
         [in/input (forms/with-attrs {:label       "email"
                                      :auto-focus? true}
                                     form
                                     [:email])]]))))

(defn component [k]
  (second (colls/only! (ig/find-derived @sys k))))
