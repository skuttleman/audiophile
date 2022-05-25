(ns audiophile.ui.store.async
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.store.actions :as act]
    [com.ben-allred.vow.core :as v :include-macros true]))

(defmethod store/async* ::act/modal#add!
  [[_ frame] {:keys [store]}]
  (store/dispatch! store (act/modal:create frame))
  (v/and (v/sleep 10)
         (store/dispatch! store (act/modal:display frame))))

(defmethod store/async* ::act/modal#remove!
  [[_ frame] {:keys [store]}]
  (store/dispatch! store (act/modal:hide frame))
  (v/and (v/sleep 500)
         (store/dispatch! store (act/modal:clear frame))))

(defmethod store/async* ::act/modal#remove-all!
  [_ {:keys [store]}]
  (store/dispatch! store act/modal:hide-all)
  (v/and (v/sleep 500)
         (store/dispatch! store act/modal:clear-all)))

(defmethod store/async* ::act/toast#add!
  [[_ {:keys [id body] :as toast}] {:keys [store]}]
  (let [body (delay
               (v/and (v/resolve (store/dispatch! store (act/toast:display id)))
                      (v/sleep 5500)
                      (store/dispatch! store (act/toast#remove! id)))
               body)]
    (store/dispatch! store (act/toast:add (assoc toast :body body)))))

(defmethod store/async* ::act/toast#remove!
  [[_ {:keys [id]}] {:keys [store]}]
  (v/and (v/resolve (store/dispatch! store (act/toast:hide id)))
         (v/sleep 6000)
         (store/dispatch! store (act/toast:clear id))))

(defmethod store/async* ::act/profile#load!
  [_ {:keys [http-client nav store]}]
  (-> (http/get http-client (nav/path-for nav :routes.api/users.profile))
      (v/then (fn [{profile :data}]
                (store/dispatch! store (act/profile:set profile))))))
