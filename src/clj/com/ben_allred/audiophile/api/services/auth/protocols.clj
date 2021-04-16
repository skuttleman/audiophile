(ns com.ben-allred.audiophile.api.services.auth.protocols)

(defprotocol IOAuthProvider
  (-redirect-uri [_ opts])
  (-token [_ opts])
  (-profile [_ opts]))
