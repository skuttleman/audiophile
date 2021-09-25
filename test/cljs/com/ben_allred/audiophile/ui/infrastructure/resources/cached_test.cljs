(ns ^:unit com.ben-allred.audiophile.ui.infrastructure.resources.cached-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.ui.infrastructure.resources.cached :as cached]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]
    [com.ben-allred.audiophile.test.utils :refer [async] :as tu]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]))

(deftest cached-resource-test
  (testing "CachedResource"
    (let [*resource (stubs/create (reify
                                    pres/IResource
                                    (request! [_ opts]
                                      [::vow opts])
                                    (status [_]
                                      ::status)

                                    pv/IPromise
                                    (then [_ on-success on-error]
                                      (v/then (v/resolve 13) on-success on-error))

                                    IDeref
                                    (-deref [_]
                                      ::value)))]
      (async done
        (async/go
          (testing "#request!"
            (let [*cached (cached/resource {:resource *resource})
                  result (res/request! *cached ::opts)]
              (testing "requests the underlying resources"
                (is (= ::opts (ffirst (stubs/calls *resource :request!)))))

              (testing "returns the results"
                (is (= [::vow ::opts] result)))

              (testing "returns cached results on subsequent calls"
                (stubs/init! *resource)
                (is (= [::vow ::opts] (res/request! *cached ::new-opts)))
                (is (empty? (stubs/calls *resource :request!))))))

          (testing "#status"
            (let [*cached (cached/->CachedResource (atom nil) *resource)]
              (testing "returns the status of the underlying resource"
                (is (= ::status (res/status *cached))))))

          (testing "#then"
            (let [*cached (cached/->CachedResource (atom nil) *resource)]
              (testing "resolves or rejects the underlying resource"
                (is (= [:success 13] (tu/<p! *cached))))))

          (testing "#deref"
            (let [*cached (cached/->CachedResource (atom nil) *resource)]
              (testing "returns the underlying resource's value"
                (is (= ::value @*cached)))))

          (done))))))
