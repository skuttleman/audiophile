(ns com.ben-allred.audiophile.common.views.components.core)

(defn spinner [{:keys [size]}]
  [(keyword (str "div.loader." (name (or size :large))))])

(defn with-resource [resource component & args]
  (let [[status data] @resource]
    (case status
      :success (into [component data] args)
      :error [:div.error "an error occurred"]
      [spinner nil])))

(defn not-found [_]
  [:div "not found"])
