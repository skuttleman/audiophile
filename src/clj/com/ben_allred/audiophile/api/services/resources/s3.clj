(ns com.ben-allred.audiophile.api.services.resources.s3
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [cognitect.aws.client.api :as aws]
    [cognitect.aws.credentials :as aws.creds]
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.services.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(deftype S3Client [client bucket invoke]
  prepos/IKVStore
  (uri [_ key _]
    (format "s3://%s/%s" bucket key))
  (get [_ key _]
    (invoke client {:op      :GetObject
                    :request {:Bucket bucket
                              :Key    key}}))
  (put! [_ key content opts]
    (invoke client {:op      :PutObject
                    :request {:Bucket        bucket
                              :Key           key
                              :ContentType   (:content-type opts)
                              :ContentLength (:content-length opts)
                              :Metadata      (:metadata opts)
                              :Body          content}})))

(defn client [{:keys [access-key bucket region secret-key]}]
  (let [client (aws/client {:api                  :s3
                            :region               region
                            :credentials-provider (aws.creds/basic-credentials-provider
                                                    {:access-key-id     access-key
                                                     :secret-access-key secret-key})})]
    (->S3Client client bucket aws/invoke)))

(defn stream-serde [_]
  (reify pserdes/ISerde
    (serialize [_ file _]
      (io/input-stream file))
    (deserialize [_ response _]
      (:Body response))))
