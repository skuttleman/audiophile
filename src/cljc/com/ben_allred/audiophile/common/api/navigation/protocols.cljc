(ns com.ben-allred.audiophile.common.api.navigation.protocols)

(defprotocol IHistory
  "This can only be implemented in browser targeted cljs builds"
  (start! [this] "Start monitoring and reacting to the browser history state")
  (stop! [this] "Stop interacting with the browser history state")
  (navigate! [this path] "Push a new state to the browser history")
  (replace! [this path] "Replace the current browser history state"))
