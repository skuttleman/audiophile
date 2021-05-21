(ns ^:unit com.ben-allred.audiophile.common.services.resources.toaster-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.resources.core :as res]
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.services.resources.toaster :as toaster]
    [com.ben-allred.audiophile.common.services.ui-store.protocols :as pui-store]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]
    [test.utils :refer [async] :as tu]
    [test.utils.stubs :as stubs])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defn ^:private display! [vow store]
  (-> vow
      (v/peek (fn [_]
                (let [[_ {:keys [body]}] (ffirst (stubs/calls store :dispatch!))]
                  @body)))
      (v/then #(v/sleep % 150)
              #(v/sleep (v/reject %) 150))))

(deftest toast-resource-test
  (testing "ToastResource"
    (let [*resource (stubs/create (reify
                                    pres/IResource
                                    (request! [_ _]
                                      (v/create (fn [_ _])))
                                    (status [_] ::status)

                                    pv/IPromise
                                    (then [_ on-success on-error]
                                      (v/then (v/resolve 13) on-success on-error))

                                    IDeref
                                    (#?(:cljs -deref :default deref) [_]
                                      ::value)))
          store (stubs/create (reify
                                pui-store/IStore
                                (dispatch! [_ _])))]
      (async done
        (async/go
          (testing "#request!"
            (let [*redirect (toaster/->ToastResource *resource
                                                     store
                                                     (partial conj [:good])
                                                     (partial conj [:bad])
                                                     5
                                                     5)]
              (testing "when the underlying resource succeeds"
                (stubs/init! store)
                (stubs/set-stub! *resource :request! v/resolve)
                (let [result (tu/<p! (-> (res/request! *redirect ::opts)
                                         (display! store)))
                      actions (map first (stubs/calls store :dispatch!))]
                  (testing "updates the store"
                    (is (= [:toasts/add!
                            :toasts/display!
                            :toasts/hide!
                            :toasts/remove!]
                           (map first actions)))
                    (is (apply = (map (comp :id second) actions)))
                    (is (= {:level :success
                            :body  [:good ::opts]}
                           (-> actions
                               first
                               second
                               (select-keys #{:level :body})
                               (update :body deref)))))

                  (testing "returns the result"
                    (is (= [:success ::opts] result)))))

              (testing "when the underlying resource fails"
                (stubs/init! store)
                (stubs/set-stub! *resource :request! v/reject)
                (let [result (tu/<p! (-> (res/request! *redirect ::opts)
                                         (display! store)))
                      actions (map first (stubs/calls store :dispatch!))]
                  (testing "updates the store"
                    (is (= [:toasts/add!
                            :toasts/display!
                            :toasts/hide!
                            :toasts/remove!]
                           (map first actions)))
                    (is (apply = (map (comp :id second) actions)))
                    (is (= {:level :error
                            :body  [:bad ::opts]}
                           (-> actions (first) (second)
                               (select-keys #{:level :body})
                               (update :body deref)))))

                  (testing "returns the result"
                    (is (= [:error ::opts] result)))))))

          (testing "#status"
            (let [*redirect (toaster/->ToastResource *resource nil nil nil nil nil)]
              (testing "returns the status of the underlying resource"
                (is (= ::status (res/status *redirect))))))

          (testing "#then"
            (let [*redirect (toaster/->ToastResource *resource nil nil nil nil nil)]
              (testing "resolves the underlying resource"
                (is (= [:success 13] (tu/<p! *redirect))))))

          (testing "#deref"
            (let [*redirect (toaster/->ToastResource *resource nil nil nil nil nil)]
              (testing "returns the underlying resource's value"
                (is (= ::value @*redirect)))))

          (done))))))
