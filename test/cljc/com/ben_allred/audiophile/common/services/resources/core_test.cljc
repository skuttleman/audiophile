(ns ^:unit com.ben-allred.audiophile.common.services.resources.core-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.resources.core :as res]
    [com.ben-allred.vow.core :as v]
    [test.utils :refer [async] :as tu]
    [test.utils.stubs :as stubs]))

(deftest resource-test
  (testing "Resource"
    (async done
      (async/go
        (testing "#request!"
          (testing "when the request succeeds"
            (let [resource (res/->Resource (atom {:status :init})
                                           #(v/resolve {:data %}))
                  result (tu/<p! (res/request! resource ::opts))]
              (is (= [:success ::opts] result))))

          (testing "when the request fails"
            (let [resource (res/->Resource (atom {:status :init})
                                           #(v/reject {:errors %}))
                  result (tu/<p! (res/request! resource ::opts))]
              (is (= [:error ::opts] result)))))

        (testing "#status"
          (let [opts->vow (stubs/create)
                resource (res/->Resource (atom {:status :init}) opts->vow)]
            (testing "when the resource has not been requested"
              (testing "has the correct status"
                (is (= :init (res/status resource)))))

            (testing "when the resource has not finished the request"
              (stubs/set-stub! opts->vow (v/create (fn [_ _])))
              (res/request! resource ::opts)

              (testing "has the correct status"
                (is (= :requesting (res/status resource)))))

            (testing "when the resource request succeeds"
              (stubs/set-stub! opts->vow (v/resolve {:data :data}))
              (tu/<p! (res/request! resource ::opts))

              (testing "has the correct status"
                (is (= :success (res/status resource)))))

            (testing "when the resource request fails"
              (stubs/set-stub! opts->vow (v/reject {:errors :errors}))
              (tu/<p! (res/request! resource ::opts))

              (testing "has the correct status"
                (is (= :error (res/status resource)))))))

        (testing "#then"
          (testing "when the request succeeds"
            (let [resource (res/->Resource (atom {:status :init})
                                           #(v/resolve {:data %}))
                  _ (res/request! resource ::opts)
                  result (tu/<p! (v/then resource (partial hash-map :value)))]
              (is (= [:success {:value ::opts}] result))))

          (testing "when the request fails"
            (let [resource (res/->Resource (atom {:status :init})
                                           #(v/reject {:errors %}))
                  _ (res/request! resource ::opts)
                  result (tu/<p! (v/catch resource (comp v/reject (partial hash-map :value))))]
              (is (= [:error {:value ::opts}] result))))

          (testing "when setting up a promise before the resource has been requested"
            (let [resource (res/->Resource (atom {:status :init})
                                           #(v/resolve {:data %}))
                  prom (v/then resource (partial hash-map :value))]
              (testing "and when requesting the resource"
                (res/request! resource ::opts)
                (testing "resolves"
                  (is (= [:success {:value ::opts}]
                         (tu/<p! prom))))))))

        (testing "#deref"
          (let [opts->vow (stubs/create (v/create (fn [_ _])))
                resource (res/->Resource (atom {:status :init}) opts->vow)]
            (testing "when the resource has not resolved"
              (testing "returns nil"
                (is (nil? @resource))
                (res/request! resource ::opts)
                (is (nil? @resource))))

            (testing "when the resource succeeds"
              (stubs/set-stub! opts->vow (v/resolve {:data ::data}))
              (tu/<p! (res/request! resource ::opts))

              (testing "returns data"
                (is (= ::data @resource))))

            (testing "when the resource succeeds"
              (stubs/set-stub! opts->vow (v/reject {:errors ::errors}))
              (tu/<p! (res/request! resource ::opts))

              (testing "returns errors"
                (is (= ::errors @resource))))))

        (done)))))
