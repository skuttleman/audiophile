(ns ^:unit com.ben-allred.audiophile.backend.infrastructure.resources.s3-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.infrastructure.resources.s3 :as s3]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]))

(deftest s3-client-test
  (testing "S3Client"
    (let [client (s3/->S3Client ::client "a-bucket" vector)]
      (testing "#uri"
        (testing "generates a resource uri"
          (is (= "s3://a-bucket/some-key" (repos/uri client "some-key")))))

      (testing "#get"
        (testing "invokes the client"
          (is (= [::client {:op      :GetObject
                            :request {:Bucket "a-bucket"
                                      :Key    "some-key"}}]
                 (repos/get client "some-key")))))

      (testing "#put!"
        (testing "invokes the client"
          (is (= [::client {:op      :PutObject
                            :request {:Bucket        "a-bucket"
                                      :Key           "some-key"
                                      :ContentType   "content/type"
                                      :ContentLength 12345
                                      :Metadata      {:foo :bar}
                                      :Body          ::content}}]
                 (repos/put! client
                             "some-key"
                             ::content
                             {:content-type   "content/type"
                              :content-length 12345
                              :metadata       {:foo :bar}}))))))))
