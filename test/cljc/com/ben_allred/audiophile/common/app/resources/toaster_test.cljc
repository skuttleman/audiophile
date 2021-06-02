(ns ^:unit com.ben-allred.audiophile.common.app.resources.toaster-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.app.resources.toaster :as toaster]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.ui-components.protocols :as pcomp]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]
    [test.utils :refer [async] :as tu]
    [test.utils.stubs :as stubs])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

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
          toaster (stubs/create (reify
                                  pcomp/IAlert
                                  (create! [_ _ _])))]
      (async done
        (async/go
          (testing "#request!"
            (let [*redirect (toaster/->ToastResource *resource
                                                     toaster
                                                     (partial conj [:good])
                                                     (partial conj [:bad]))]
              (testing "when the underlying resource succeeds"
                (stubs/init! toaster)
                (stubs/set-stub! *resource :request! v/resolve)
                (let [result (tu/<p! (res/request! *redirect ::opts))
                      toast (colls/only! (stubs/calls toaster :create!))]
                  (testing "issues a toast message"
                    (is (= [:success [:good ::opts]]
                           toast)))

                  (testing "returns the result"
                    (is (= [:success ::opts] result)))))

              (testing "when the underlying resource fails"
                (stubs/init! toaster)
                (stubs/set-stub! *resource :request! v/reject)
                (let [result (tu/<p! (res/request! *redirect ::opts))
                      toast (colls/only! (stubs/calls toaster :create!))]
                  (testing "issues a toast message"
                    (is (= [:error [:bad ::opts]]
                           toast)))

                  (testing "returns the result"
                    (is (= [:error ::opts] result)))))))

          (testing "#status"
            (let [*redirect (toaster/->ToastResource *resource nil nil nil)]
              (testing "returns the status of the underlying resource"
                (is (= ::status (res/status *redirect))))))

          (testing "#then"
            (let [*redirect (toaster/->ToastResource *resource nil nil nil)]
              (testing "resolves the underlying resource"
                (is (= [:success 13] (tu/<p! *redirect))))))

          (testing "#deref"
            (let [*redirect (toaster/->ToastResource *resource nil nil nil)]
              (testing "returns the underlying resource's value"
                (is (= ::value @*redirect)))))

          (done))))))
