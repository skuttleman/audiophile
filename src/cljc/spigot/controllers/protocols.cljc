(ns spigot.controllers.protocols)

(defprotocol ISpigotTaskHandler
  (on-error [this ctx ex])
  (on-complete [this ctx workflow])
  (process-task [this ctx task]))
