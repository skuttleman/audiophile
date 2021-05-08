(ns ^:unit com.ben-allred.audiophile.common.services.resources.redirect-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.resources.core :as res]
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.services.resources.redirect :as redirect]
    [com.ben-allred.audiophile.common.services.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]
    [test.utils :refer [async] :as tu]
    [test.utils.mocks :as mocks])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(deftest redirect-resource-test
  (testing "RedirectResource"
    (let [resource (mocks/->mock (reify
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
          nav (mocks/->mock (reify
                              nav/IHistory
                              (-navigate! [_ _])

                              pserdes/ISerde
                              (serialize [_ _ _]
                                "/path")))]
      (async done
        (async/go
          (let [routes {:success/page   :success/page
                        :success/params :success/params
                        :error/page     :error/page
                        :error/params   :error/params}]
            (testing "#request!"
              (let [redirect (redirect/->RedirectResource resource nav routes)]
                (testing "when the underlying resource succeeds"
                  (mocks/init! nav)
                  (mocks/set-mock! resource :request! v/resolve)
                  (let [result (tu/<p! (res/request! redirect ::opts))]
                    (testing "redirects"
                      (let [args (colls/only! (mocks/calls nav :serialize))]
                        (is (= [:success/page :success/params] args))))

                    (testing "returns the result"
                      (is (= [:success ::opts] result)))))

                (testing "when the underlying resource fails"
                  (mocks/init! nav)
                  (mocks/set-mock! resource :request! v/reject)
                  (let [result (tu/<p! (res/request! redirect ::opts))]
                    (testing "redirects"
                      (let [args (colls/only! (mocks/calls nav :serialize))]
                        (is (= [:error/page :error/params] args))))

                    (testing "returns the result"
                      (is (= [:error ::opts] result)))))))

            (testing "#status"
              (let [redirect (redirect/->RedirectResource resource nil routes)]
                (testing "returns the status of the underlying resource"
                  (is (= ::status (res/status redirect))))))

            (testing "#then"
              (let [redirect (redirect/->RedirectResource resource nil routes)]
                (testing "resolves the underlying resource"
                  (is (= [:success 13] (tu/<p! redirect))))))

            (testing "#deref"
              (let [redirect (redirect/->RedirectResource resource nil routes)]
                (testing "returns the underlying resource's value"
                  (is (= ::value @redirect))))))

          (done))))))