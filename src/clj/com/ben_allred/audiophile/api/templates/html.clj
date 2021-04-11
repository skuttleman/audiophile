(ns com.ben-allred.audiophile.api.templates.html
  (:require
    [hiccup.core :as hiccup]))

(defn layout [app]
  [:html {:lang "en"}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "ie=edge"}]
    [:link {:rel         "stylesheet"
            :href        "https://use.fontawesome.com/releases/v5.6.3/css/all.css"
            :integrity   "sha384-UHRtZLI+pbxtHCWp1t77Bi1L4ZtiqrqD80Kn4Z8NTSRyMA2Fd33n5dQ8lWUE00s/"
            :crossorigin "anonymous"
            :type        "text/css"}]
    [:link {:rel  "stylesheet"
            :href "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.1/css/bulma.min.css"
            :type "text/css"}]
    [:link {:rel  "stylesheet"
            :href "https://cdn.jsdelivr.net/npm/bulma-tooltip@2.0.2/dist/css/bulma-tooltip.min.css"
            :type "text/css"}]
    [:link {:rel "stylesheet" :href "/css/main.css"}]
    [:title "Audiophile"]]
   [:body
    [:div#root app]
    [:script {:src "/js/main.js"}]]])

(defn render [view]
  (str "<!doctype html>"
       (hiccup/html nil (layout view))))
