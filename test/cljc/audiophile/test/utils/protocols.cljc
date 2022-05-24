(ns audiophile.test.utils.protocols)

(defprotocol IReport
  "Tracks invocations of a stub or spy"
  (calls [this] "Returns an ordered sequence of calls to the stub"))

(defprotocol IInit
  "A protocol for a spy or stub to initialize itself"
  (init! [this] "Initialize or reinitialize to initial state"))

(defprotocol IStub
  "A protocol for interacting with a reify stub"
  (set-stub! [this method f-or-val] "Update an individual stub")
  (get-stub [this method] "Return the current stub (if any) for a method"))

(defprotocol ISpy
  "A protocol for interacting with a function spy"
  (set-spy! [this f] "Changes the underlying function used for the spy"))
