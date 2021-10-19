(ns ^:unit com.ben-allred.audiophile.ui.infrastructure.resources.base-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.ui.infrastructure.resources.base :as bres]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.audiophile.test.utils :refer [async] :as tu]
    [com.ben-allred.audiophile.test.utils.spies :as spies]))

(deftest resource-test
  (testing "Resource"
    (async done
      (async/go
        (testing "#request!"
          (testing "when the request succeeds"
            (let [resource (bres/->Resource (atom {:status :init})
                                            #(v/resolve {:data %}))
                  result (tu/<p! (res/request! resource ::opts))]
              (is (= [:success ::opts] result))))

          (testing "when the request fails"
            (let [resource (bres/->Resource (atom {:status :init})
                                            #(v/reject {:error %}))
                  result (tu/<p! (res/request! resource ::opts))]
              (is (= [:error ::opts] result)))))

        (testing "#status"
          (testing "when the resource has not been requested"
            (let [opts->vow (spies/create (v/resolve))
                  resource (bres/->Resource (atom {:status :init}) opts->vow)]
              (testing "has the correct status"
                (is (= :init (res/status resource)))))

            (testing "when the resource has not finished the request"
              (let [opts->vow (spies/create (v/sleep 100))
                    resource (bres/->Resource (atom {:status :init}) opts->vow)]
                (res/request! resource ::opts)

                (testing "has the correct status"
                  (is (= :requesting (res/status resource))))))

            (testing "when the resource request succeeds"
              (let [opts->vow (spies/create (v/resolve {:data :data}))
                    resource (bres/->Resource (atom {:status :init}) opts->vow)]
                (tu/<p! (res/request! resource ::opts))

                (testing "has the correct status"
                  (is (= :success (res/status resource))))))

            (testing "when the resource request fails"
              (let [opts->vow (spies/create (v/reject {:error ::errors}))
                    resource (bres/->Resource (atom {:status :init}) opts->vow)]
                (tu/<p! (res/request! resource ::opts))

                (testing "has the correct status"
                  (is (= :error (res/status resource))))))))

        (testing "#deref"
          (let [opts->vow (spies/create (v/create (fn [_ _])))
                *resource (bres/->Resource (atom {:status :init}) opts->vow)]
            (testing "when the resource has not resolved"
              (testing "returns nil"
                (is (nil? @*resource))
                (res/request! *resource ::opts)
                (is (nil? @*resource))))

            (testing "when the resource succeeds"
              (spies/set-spy! opts->vow (v/resolve {:data ::data}))
              (tu/<p! (res/request! *resource ::opts))

              (testing "returns data"
                (is (= ::data @*resource))))

            (testing "when the resource succeeds"
              (spies/set-spy! opts->vow (v/reject {:error ::errors}))
              (tu/<p! (res/request! *resource ::opts))

              (testing "returns errors"
                (is (= ::errors @*resource))))))

        (done)))))
