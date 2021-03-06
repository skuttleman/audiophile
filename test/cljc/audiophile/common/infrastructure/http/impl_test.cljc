(ns ^:unit audiophile.common.infrastructure.http.impl-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [are deftest is testing]]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.protocols :as pserdes]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.http.impl :as ihttp]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.common.infrastructure.resources.protocols :as pres]
    [audiophile.test.utils :refer [async] :as tu]
    [audiophile.test.utils.spies :as spies]
    [audiophile.test.utils.stubs :as stubs]
    [com.ben-allred.vow.core :as v])
  #?(:clj
     (:import
       (java.util Date)
       (org.apache.http.cookie Cookie))))

(defn ^:private add-cookies! [cs cookies]
  #?(:clj
     (when cs
       (doseq [[name value] cookies]
         (.addCookie cs
                     (reify
                       Cookie
                       (getComment [_] "comment")
                       (getCommentURL [_] "http://comment")
                       (getDomain [_] "domain")
                       (getExpiryDate [_] (Date. Long/MAX_VALUE))
                       (getName [_] name)
                       (getPath [_] "/")
                       (getPorts [_] (int-array 0))
                       (getValue [_] value)
                       (getVersion [_] 1)
                       (isExpired [_ _] false)
                       (isPersistent [_] true)
                       (isSecure [_] false)))))))

(deftest base-test
  (testing "base"
    (let [client (stubs/create
                   (reify
                     pres/IResource
                     (request! [_ _])))]
      (async done
        (async/go
          (testing "#request!"
            (let [resource (ihttp/->HttpBase client 1000)]
              (testing "when the request succeeds"
                (stubs/use! client :request!
                            (#?(:cljs async/go :default do)
                              {:status 200 :body "ok"}))
                (testing "handles the success"
                  (let [result (tu/<p! (res/request! resource {:some :opts}))]
                    (is (= [:success {:status 200 :body "ok"}] result))
                    (is (= {:some :opts} (ffirst (stubs/calls client :request!)))))))

              (testing "when the request fails"
                (stubs/use! client :request!
                            (#?(:cljs async/go :default do)
                              {:status 500 :body "not-ok"}))
                (testing "handles the failure"
                  (let [result (tu/<p! (res/request! resource {:some :opts}))]
                    (is (= [:error {:status 500 :body "not-ok"}] result))
                    (is (= {:some :opts} (ffirst (stubs/calls client :request!)))))))

              (testing "when the client throws"
                (stubs/use! client :request!
                            (ex-info "boom" {:status 500 :body "not-ok"}))
                (testing "handles the exception"
                  (let [result (tu/<p! (res/request! resource {:some :opts}))]
                    (is (= [:error {:status 500 :body "not-ok"}] result))
                    (is (= {:some :opts} (ffirst (stubs/calls client :request!)))))))))

          (done))))))

(defn ^:private cookie-responder [f m]
  (fn [request]
    (add-cookies! (:cookie-store request) {"cookie-name" "cookie-value"})
    (f (merge m {#?@(:clj [:cookie-store (:cookie-store request)])}))))

(deftest with-headers-test
  (testing "with-headers"
    (let [client (stubs/create
                   (reify
                     pres/IResource
                     (request! [_ _])))]
      (async done
        (async/go
          (testing "#request!"
            (let [resource (ihttp/->HttpHeaders client)]
              (testing "when the request succeeds"
                (stubs/use! client :request!
                            (cookie-responder v/resolve
                                              {:headers {"x-some-header"       "value"
                                                         "x-some-other-header" ["value-1"
                                                                                "value-2"]}}))
                (testing "handles the success"
                  (let [result (tu/<p! (res/request! resource {:headers {:foo "bar"
                                                                         :baz ["quux-1"
                                                                               "quux-2"]}}))]
                    (is (= [:success {:headers {:x-some-header       "value"
                                                :x-some-other-header ["value-1" "value-2"]}
                                      #?@(:clj [:cookies {"cookie-name" "cookie-value"}])}]
                           (-> result
                               (update 1 dissoc :cookie-store)
                               #?(:clj (update-in [1 :cookies "cookie-name"] :value)))))
                    (is (= {:headers {"foo" "bar"
                                      "baz" ["quux-1" "quux-2"]}}
                           (dissoc (ffirst (stubs/calls client :request!))
                                   :cookie-store))))))

              (testing "when the request fails"
                (stubs/use! client :request!
                            (cookie-responder v/reject
                                              {:headers {"x-some-header"       "value"
                                                         "x-some-other-header" ["value-1"
                                                                                "value-2"]}}))
                (testing "handles the failure"
                  (let [result (tu/<p! (res/request! resource {:headers {:foo "bar"
                                                                         :baz ["quux-1"
                                                                               "quux-2"]}}))]
                    (is (= [:error {:headers {:x-some-header       "value"
                                              :x-some-other-header ["value-1" "value-2"]}
                                    #?@(:clj [:cookies {"cookie-name" "cookie-value"}])}]
                           (-> result
                               (update 1 dissoc :cookie-store)
                               #?(:clj (update-in [1 :cookies "cookie-name"] :value)))))
                    (is (= {:headers {"foo" "bar"
                                      "baz" ["quux-1" "quux-2"]}}
                           (dissoc (ffirst (stubs/calls client :request!))
                                   :cookie-store))))))))

          (done))))))

(defn ^:private ->serde [k]
  (reify
    pserdes/ISerde
    (serialize [_ value _]
      [(keyword k "serialized") value])
    (deserialize [_ value _]
      [(keyword k "deserialized") value])

    pserdes/IMime
    (mime-type [_]
      (str "application/" k))))

(defn ^:private ->stub [serde body]
  {:headers {:content-type (some-> serde serdes/mime-type)}
   :body    body})

(deftest with-serde-test
  (testing "with-serde"
    (let [client (stubs/create
                   (reify
                     pres/IResource
                     (request! [_ _])))
          serdes {:foo (->serde "foo")
                  :bar (->serde "bar")}]
      (async done
        (async/go
          (testing "#request!"
            (let [resource (ihttp/->HttpSerde client serdes)]
              (testing "when the request succeeds"
                (testing "and when the request is serialized as application/foo"
                  (testing "and when the response is serialized as application/bar"
                    (stubs/init! client)
                    (stubs/use! client :request!
                                (v/resolve (->stub (:bar serdes) "result")))
                    (let [result (tu/<p! (res/request! resource (->stub (:foo serdes) "request")))]
                      (testing "makes the correct request"
                        (let [content-type (serdes/mime-type (:foo serdes))]
                          (is (= {:headers {:content-type content-type
                                            :accept       content-type}
                                  :body    (serdes/serialize (:foo serdes) "request")}
                                 (ffirst (stubs/calls client :request!))))))

                      (testing "has the expected response"
                        (is (= [:success (serdes/deserialize (:bar serdes) "result")]
                               result)))))))))

          (done))))))

(deftest with-progress-test
  (testing "with-async"
    (let [client (stubs/create
                   (reify
                     pres/IResource
                     (request! [_ _])))
          on-progress (spies/create)]
      (async done
        (async/go
          (testing "#request!"
            (let [resource (ihttp/->HttpProgress client)]
              (testing "when the request succeeds"
                (stubs/use! client :request!
                            (v/sleep (v/resolve :ok) 100))
                (let [prom (res/request! resource {:on-progress on-progress})
                      ch (:progress (first (colls/only! (stubs/calls client :request!))))]
                  (async/put! ch {:direction :upload :loaded 1 :total 2})
                  (async/put! ch {:direction :upload :loaded 2 :total 2})
                  (async/put! ch {:direction :download :loaded 1 :total 1})
                  (is (= [:success :ok] (tu/<p! prom)))
                  (is (= [{:progress/total 2, :progress/current 1}
                          {:progress/total 2, :progress/current 2}
                          {:progress/status :success}]
                         (map colls/only! (spies/calls on-progress))))))

              (testing "when the request fails"
                (spies/init! on-progress)
                (stubs/init! client)
                (stubs/use! client :request!
                            (v/sleep (v/reject :bad) 100))
                (let [prom (res/request! resource {:on-progress on-progress})
                      ch (:progress (first (colls/only! (stubs/calls client :request!))))]
                  (async/put! ch {:direction :upload :loaded 1 :total 2})
                  (async/put! ch {:direction :upload :loaded 2 :total 2})
                  (async/put! ch {:direction :download :loaded 1 :total 1})
                  (is (= [:error :bad] (tu/<p! prom)))
                  (is (= [{:progress/total 2, :progress/current 1}
                          {:progress/total 2, :progress/current 2}
                          {:progress/status :error}]
                         (map colls/only! (spies/calls on-progress))))))))

          (done))))))
