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

(defmethod store/async* ::act/user:load-profile!
  [_ {:keys [http-client nav store]}]
  (-> (http/get http-client (nav/path-for nav :api/profile))
      (v/then-> :data act/profile:set! (->> (store/dispatch! store)))))
