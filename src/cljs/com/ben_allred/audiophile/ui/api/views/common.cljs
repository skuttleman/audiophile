(ns com.ben-allred.audiophile.ui.api.views.common
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]))

(defn re-fetch [{:keys [*resource]}]
  (fn [cb]
    (fn [result]
      (res/request! *resource)
      (when cb
        (cb result)))))
