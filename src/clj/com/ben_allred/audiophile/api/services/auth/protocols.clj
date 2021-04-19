(ns com.ben-allred.audiophile.api.services.auth.protocols)

(defprotocol IOAuthProvider
  (-redirect-uri [this opts] "generate a redirect url to access the providers login flow")
  (-profile [this opts] "retrieve profile information from the auth provider"))
