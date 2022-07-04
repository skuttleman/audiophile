(ns spigot.controllers.protocols)

(defprotocol ISpigotTaskHandler
  (process-task [this ctx task]))

(defprotocol ISpigotStatusHandler
  (on-error [this ctx ex])
  (on-complete [this ctx workflow]))
