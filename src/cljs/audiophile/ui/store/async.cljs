(ns audiophile.ui.store.async
  (:require
    [audiophile.common.infrastructure.http.core :as http]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.store.actions :as act]
    [com.ben-allred.vow.core :as v :include-macros true]))

(defmethod store/async* ::act/modal:add!
  [[_ frame] {:keys [store]}]
  (store/dispatch! store [:modals/add! frame])
  (v/and (v/sleep 10)
         (store/dispatch! store [:modals/display! frame])))

(defmethod store/async* ::act/modal:remove!
  [[_ frame] {:keys [store]}]
  (store/dispatch! store [:modals/hide! frame])
  (v/and (v/sleep 500)
         (store/dispatch! store [:modals/remove! frame])))

(defmethod store/async* ::act/modal:remove-all!
  [_ {:keys [store]}]
  (store/dispatch! store [:modals/hide-all!])
  (v/and (v/sleep 500)
         (store/dispatch! store [:modals/remove-all!])))

(defmethod store/async* ::act/toast:add!
  [[_ {:keys [id body] :as toast}] {:keys [store]}]
  (let [body (delay
               (v/and (v/resolve (store/dispatch! store [:toasts/display! {:id id}]))
                      (v/sleep 6000)
                      (store/dispatch! store (act/toast:remove! id)))
               body)]
    (store/dispatch! store [:toasts/add! (assoc toast :body body)])))

(defmethod store/async* ::act/toast:remove!
  [[_ {:keys [id]}] {:keys [store]}]
  (v/and (v/resolve (store/dispatch! store [:toasts/hide! {:id id}]))
         (v/sleep 6000)
         (store/dispatch! store [:toasts/remove! {:id id}])))

(defmethod store/async* ::act/user:load-profile!
  [_ {:keys [http-client nav store]}]
  (-> (http/get http-client (nav/path-for nav :api/profile))
      (v/then (fn [{profile :data}]
                (store/dispatch! store [:user.profile/set! profile])))))
