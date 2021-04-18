(ns com.ben-allred.audiophile.api.services.auth.protocols)

(defprotocol IOAuthProvider
  (-redirect-uri [this opts])
  (-token [this opts])
  (-profile [this opts]))
