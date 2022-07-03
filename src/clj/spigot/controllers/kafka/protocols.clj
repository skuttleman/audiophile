(ns spigot.controllers.kafka.protocols)

(defprotocol ISpigotTaskHandler
  (on-error [this ctx ex])
  (on-complete [this ctx workflow])
  (process-task [this ctx task]))

(defprotocol ISpigotProducer
  (send! [this k v opts]))
