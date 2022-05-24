(ns audiophile.backend.infrastructure.resources.s3
  (:require
    [clojure.java.io :as io]
    [cognitect.aws.client.api :as aws]
    [cognitect.aws.credentials :as aws.creds]
    [audiophile.backend.api.repositories.protocols :as prepos]
    [audiophile.common.infrastructure.http.protocols :as phttp]
    [audiophile.common.core.serdes.protocols :as pserdes]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private ->invoke! [invoke]
  (fn [client request]
    (let [result (invoke client request)]
      (when-let [ex (some->> result
                             (filter (comp #{"throwable"} name key))
                             first
                             val)]
        (throw ex))
      result)))

(deftype S3Client [client bucket invoke!]
  prepos/IKVStore
  (uri [_ key _]
    (format "s3://%s/%s" bucket key))
  (get [_ key _]
    (log/info "retrieving s3 artifact" key)
    (invoke! client {:op      :GetObject
                     :request {:Bucket bucket
                               :Key    key}}))
  (put! [_ key content opts]
    (log/info "saving s3 artifact" key)
    (invoke! client {:op      :PutObject
                     :request {:Bucket        bucket
                               :Key           key
                               :ContentType   (:content-type opts)
                               :ContentLength (:content-length opts)
                               :Metadata      (:metadata opts)
                               :Body          content}}))

  phttp/ICheckHealth
  (display-name [_]
    ::S3Client)
  (healthy? [_]
    (invoke! client {:op      :HeadBucket
                     :request {:Bucket bucket}})
    true)
  (details [_]
    {:bucket bucket}))

(defn client
  "Constructor for [[S3Client]] used for storing and retrieving s3 objects."
  [{:keys [access-key bucket region secret-key]}]
  (let [client (aws/client {:api                  :s3
                            :region               region
                            :credentials-provider (aws.creds/basic-credentials-provider
                                                    {:access-key-id     access-key
                                                     :secret-access-key secret-key})})]
    (->S3Client client bucket (->invoke! aws/invoke))))

(defn stream-serde
  "Constructs an anonymous [[pserdes/ISerde]] that converts s3 input/output to be used within the system."
  [_]
  (reify pserdes/ISerde
    (serialize [_ file _]
      (io/input-stream file))
    (deserialize [_ response _]
      (:Body response))))
