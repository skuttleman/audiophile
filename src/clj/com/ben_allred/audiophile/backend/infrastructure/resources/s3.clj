(ns com.ben-allred.audiophile.backend.infrastructure.resources.s3
  (:require
    [com.ben-allred.audiophile.backend.api.pubsub.core :as ps]
    [com.ben-allred.audiophile.backend.api.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.core.serdes.protocols :as pserdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.infrastructure.http.protocols :as phttp]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids])
  (:import
    (com.amazonaws.auth AWSCredentials AWSCredentialsProvider)
    (com.amazonaws.event ProgressEventType ProgressListener)
    (com.amazonaws.services.s3 AmazonS3 AmazonS3ClientBuilder)
    (com.amazonaws.services.s3.model GetObjectRequest HeadBucketRequest ObjectMetadata PutObjectRequest S3Object)
    (java.io File)))

(defn ^:private ->upload-listener [total on-progress]
  (let [transferred (atom 0)]
    (add-watch transferred ::watcher (fn [_ _ _ current]
                                       (on-progress current total)))
    (reify
      ProgressListener
      (progressChanged [_ val]
        (let [et (.getEventType val)
              bytes (.getBytes val)]
          (condp = et
            ProgressEventType/REQUEST_BYTE_TRANSFER_EVENT (swap! transferred + bytes)
            ProgressEventType/TRANSFER_COMPLETED_EVENT (remove-watch transferred ::watcher)
            nil))))))

(deftype S3Client [^AmazonS3 client ^String bucket]
  prepos/IKVStore
  (uri [_ key _]
    (format "s3://%s/%s" bucket key))
  (get [_ object-key _]
    (log/info "retrieving s3 artifact" object-key)
    (let [req (GetObjectRequest. bucket object-key)
          ^S3Object result (.getObject client req)
          md (.getObjectMetadata result)]
      {:content        (.getObjectContent result)
       :content-length (.getContentLength md)
       :content-type   (.getContentType md)
       :metadata       (into {}
                             (map (juxt (comp keyword key)
                                        val))
                             (.getUserMetadata md))}))
  (put! [_ object-key file {:keys [content-length content-type metadata on-progress]}]
    (log/info "saving s3 artifact" object-key)
    (let [meta (doto (ObjectMetadata.)
                 (.setContentType content-type)
                 (.setContentLength content-length)
                 (.setUserMetadata (maps/map-keys name metadata)))
          req (-> (PutObjectRequest. ^String bucket ^String object-key ^File file)
                  (.withMetadata meta)
                  (cond-> on-progress (.withGeneralProgressListener (->upload-listener (.length file) on-progress))))
          result (.putObject client req)]
      {:version-id (.getVersionId result)
       :e-tag      (.getETag result)}))

  phttp/ICheckHealth
  (display-name [_]
    ::S3Client)
  (healthy? [_]
    (let [req (HeadBucketRequest. bucket)]
      (.headBucket client req)))
  (details [_]
    {:bucket bucket}))

(defrecord CredentialProvider [access-key secret-key]
  AWSCredentialsProvider
  (refresh [_])
  (getCredentials [_]
    (reify
      AWSCredentials
      (getAWSAccessKeyId [_]
        access-key)
      (getAWSSecretKey [_]
        secret-key))))

(defn client
  "Constructor for [[S3Client]] used for storing and retrieving s3 objects."
  [{:keys [_access-key bucket region _secret-key] :as cfg}]
  (let [client (-> (AmazonS3ClientBuilder/standard)
                   (.withCredentials (map->CredentialProvider cfg))
                   (.withRegion ^String region)
                   .build)]
    (->S3Client client bucket)))

(defn stream-serde
  "Constructs an anonymous [[pserdes/ISerde]] that converts s3 input/output to be used within the system."
  [_]
  (reify pserdes/ISerde
    (serialize [_ file _]
      file)
    (deserialize [_ response _]
      (:content response))))
