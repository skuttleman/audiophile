(ns com.ben-allred.audiophile.ui.infrastructure.pages.login)

(defn root [sys]
  [:div
   [:button.button {:on-click (fn [_]
                                (.assign (.-location js/window)
                                         "http://localhost:8080/auth/login?email=skuttleman@gmail.com&redirect-uri=/"))}
    "LOGIN"]])
