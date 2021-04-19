(ns com.ben-allred.audiophile.common.services.resources.protocols)

(defprotocol IResource
  (request! [this opts] "initiate a request for this resource - should return a com.ben-allred/vow")
  (status [this] "get the status of the resource represented as a keyword
                  sample values: #{:init :requesting :success :error}"))
