(ns com.ben-allred.audiophile.api.services.auth.core
  (:require
    [com.ben-allred.audiophile.api.services.auth.protocols :as pauth]))

(defn redirect-uri
  ([provider]
   (redirect-uri provider nil))
  ([provider opts]
   (pauth/-redirect-uri provider opts)))

(defn token
  ([provider]
   (token provider nil))
  ([provider opts]
   (pauth/-token provider opts)))

(defn profile
  ([provider]
   (profile provider nil))
  ([provider opts]
   (pauth/-profile provider opts)))
