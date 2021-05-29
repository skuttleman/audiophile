(ns ^:unit com.ben-allred.audiophile.common.services.resources.multi-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.resources.core :as res]
    [com.ben-allred.audiophile.common.services.resources.multi :as multi]
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.vow.core :as v]
    [test.utils :refer [async] :as tu])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defn ->resource [status value result]
  (reify pres/IResource
    (request! [_ _]
      (case status
        :success (v/resolve result)
        :error (v/reject result)
        (v/create (fn [_ _]))))
    (status [_]
      status)

    IDeref
    (#?(:cljs -deref :default deref) [_]
      value)))

(deftest multi-resource-test
  (testing "MultiResource"
    (async done
      (async/go
        (testing "when all resources succeed"
          (let [*res-1 (->resource :success :value-1 :result-1)
                *res-2 (->resource :success :value-2 :result-2)
                *multi (multi/resource {:resources {:res-1 *res-1 :res-2 *res-2}})]
            (testing "returns expected request result"
              (is (= [:success {:res-1 :result-1
                                :res-2 :result-2}]
                     (tu/<p! (res/request! *multi)))))

            (testing "has expected value"
              (is (= {:res-1 :value-1
                      :res-2 :value-2}
                     @*multi)))

            (testing "is successful"
              (is (res/success? *multi))
              (is (= :success (res/status *multi))))))

        (testing "when one resource is requesting"
          (let [*res-1 (->resource :success :value-1 :result-1)
                *res-2 (->resource :requesting :req :req)
                *multi (multi/resource {:resources {:res-1 *res-1 :res-2 *res-2}})]
            (testing "does not return a result"
              (is (= [:success ::timeout]
                     (tu/<p! (-> [(res/request! *multi)
                                  (v/sleep ::timeout 100)]
                                 v/first)))))

            (testing "has expected value"
              (is (nil? @*multi)))

            (testing "is requesting"
              (is (res/requesting? *multi)))))

        (testing "when one resource has not been requested"
          (let [*res-1 (->resource :success :value-1 :result-1)
                *res-2 (->resource :init :init :init)
                *multi (multi/resource {:resources {:res-1 *res-1 :res-2 *res-2}})]
            (testing "does not return a result"
              (is (= [:success ::timeout]
                     (tu/<p! (-> [(res/request! *multi)
                                  (v/sleep ::timeout 100)]
                                 v/first)))))

            (testing "has expected value"
              (is (nil? @*multi)))

            (testing "has not been fully requested"
              (is (not (res/requested? *multi))))))

        (testing "when one resource has erred"
          (let [*res-1 (->resource :success :value-1 :result-1)
                *res-2 (->resource :error :value-2 :result-2)
                *multi (multi/resource {:resources {:res-1 *res-1 :res-2 *res-2}})]
            (testing "returns expected request result"
              (is (= [:error :result-2] (tu/<p! (res/request! *multi)))))

            (testing "has expected value"
              (is (= :value-2 @*multi)))

            (testing "is not success"
              (is (res/error? *multi)))))

        (done)))))
