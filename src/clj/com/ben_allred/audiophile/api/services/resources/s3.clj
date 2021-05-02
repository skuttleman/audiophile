(ns com.ben-allred.audiophile.api.services.resources.s3
  (:require
    [cognitect.aws.client.api :as aws]
    [cognitect.aws.credentials :as aws.creds]
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [integrant.core :as ig]))

(deftype S3Client [client bucket]
  prepos/IKVStore
  (uri [_ key _]
    (format "s3://%s/%s" bucket key))
  (get [_ key _]
    (aws/invoke client {:op      :GetObject
                        :request {:Bucket bucket
                                  :Key    key}}))
  (put! [_ key content opts]
    (aws/invoke client {:op      :PutObject
                        :request {:Bucket      bucket
                                  :Key         key
                                  :ContentType (:content-type opts)
                                  :Metadata    (:metadata opts)
                                  :Body        content}})))

(defmethod ig/init-key ::client [_ {:keys [access-key bucket region secret-key]}]
  (let [client (aws/client {:api                  :s3
                            :region               region
                            :credentials-provider (aws.creds/basic-credentials-provider
                                                    {:access-key-id     access-key
                                                     :secret-access-key secret-key})})]
    (->S3Client client bucket)))
