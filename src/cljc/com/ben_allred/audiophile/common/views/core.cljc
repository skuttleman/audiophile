(ns com.ben-allred.audiophile.common.views.core)

(defn app [state]
  [:div
   [:div.main.layout--inset
    {:class [(str "page-" (some-> state (get-in [:page :handler]) name))]}
    [:div.layout--inset
     (if-let [text (:text state)]
       [:div text]
       [:div.loader.large])]]])
