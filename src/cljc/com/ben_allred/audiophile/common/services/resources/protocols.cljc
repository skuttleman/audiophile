(ns com.ben-allred.audiophile.common.services.resources.protocols)

(defprotocol IResource
  "A component for handling asynchronous activity"
  (request! [this opts] "Initiate a request for this resource - should return a [[com.ben-allred.vow.protocols/IPromise]]")
  (status [this]
    "Get the status of the resource represented as a keyword.
     Return value should be one of the following:

     |   status    |               meaning                 |
     |:------------|:--------------------------------------|
     | :init       | initialized (or re-initialized) state |
     | :requesting | resource is processing                |
     | :success    | resource has processed successfully   |
     | :success    | resource has processed unsuccessfully |"))
