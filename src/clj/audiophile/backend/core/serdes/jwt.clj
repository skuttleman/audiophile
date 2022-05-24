(ns audiophile.backend.core.serdes.jwt
  (:require
    [audiophile.common.core.serdes.core :as serdes]))

(defn auth-token [serde user]
  (serdes/serialize serde user {:jwt/claims {:aud #{:token/auth}}}))

(defn signup-token [serde user]
  (serdes/serialize serde user {:jwt/claims     {:aud #{:token/signup}}
                                :jwt/unit       :minutes
                                :jwt/expiration 15}))

(defn login-token [serde user]
  (serdes/serialize serde user {:jwt/claims     {:aud #{:token/login}}
                                :jwt/unit       :minutes
                                :jwt/expiration 5}))
