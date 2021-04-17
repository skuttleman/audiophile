(ns com.ben-allred.audiophile.common.services.resources.users
  (:require
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]
    [integrant.core :as ig]))

(defmethod ig/init-key ::login [_ {:keys [nav]}]
  (fn [{value :form/value :keys [page]}]
    (let [params {:email        (:email value)
                  :redirect-uri (get-in page [:query-params :redirect-uri] "/")}]
      (nav/goto! nav :auth/login {:query-params params})
      (v/resolve))))
