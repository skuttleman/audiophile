(ns com.ben-allred.audiophile.ui.infrastructure.resources.users
  (:require
    [com.ben-allred.audiophile.common.app.navigation.core :as nav]
    [com.ben-allred.vow.core :as v :include-macros true]))

(defn login-fn [{:keys [nav]}]
  (fn [{value :form/value :keys [route]}]
    (let [params {:email        (:email value)
                  :redirect-uri (get-in route [:query-params :redirect-uri] "/")}]
      (nav/goto! nav :auth/login {:query-params params})
      (v/resolve))))
