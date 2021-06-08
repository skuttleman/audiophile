(ns com.ben-allred.audiophile.backend.api.protocols)

(defprotocol IOAuthProvider
  "Supplies the mechanism for authenticating a user with a third-party OAuth provider"
  (redirect-uri [this opts] "Generate a redirect url to access the providers login flow")
  (profile [this opts] "Retrieve profile information from the auth provider"))
