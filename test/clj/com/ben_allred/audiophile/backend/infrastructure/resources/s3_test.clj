(ns ^:unit com.ben-allred.audiophile.backend.infrastructure.resources.s3-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.infrastructure.resources.s3 :as s3]
    [com.ben-allred.audiophile.test.utils.services :as ts]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]
    [clojure.java.io :as io])
  (:import
    (com.amazonaws.services.s3 AmazonS3)
    (com.amazonaws.services.s3.model S3Object ObjectMetadata PutObjectResult)))

(defn ^:private ->s3-client []
  (stubs/create (reify
                  AmazonS3
                  (getObject [_ _]
                    (doto (S3Object.)
                      (.setObjectMetadata (doto (ObjectMetadata.)
                                            (.setContentLength 100)
                                            (.setContentType "plain/awesome")
                                            (.setUserMetadata {"foo" "bar"})))
                      (.setObjectContent (io/input-stream (.getBytes "foo")))))
                  (putObject [_ _]
                    (doto (PutObjectResult.)
                      (.setVersionId "version-id")
                      (.setETag "e-tag"))))))

(deftest s3-client-test
  (testing "S3Client"
    (let [s3-client (->s3-client)
          client (s3/->S3Client s3-client "a-bucket")]
      (testing "#uri"
        (testing "generates a resource uri"
          (is (= "s3://a-bucket/some-key" (repos/uri client "some-key")))))

      (testing "#get"
        (testing "invokes the client"
          (is (= {:content "foo"
                  :content-length 100
                  :content-type "plain/awesome"
                  :metadata {:foo "bar"}}
                 (-> client
                     (repos/get "some-key")
                     (update :content slurp))))))

      (testing "#put!"
        (testing "invokes the client"
          (is (= {:version-id "version-id"
                  :e-tag "e-tag"}
                 (repos/put! client
                             "some-key"
                             (io/file (io/resource "empty.mp3"))
                             {:content-type   "content/type"
                              :content-length 12345
                              :metadata       {:foo :bar}}))))))))
