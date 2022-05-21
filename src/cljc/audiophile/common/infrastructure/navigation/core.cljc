(ns audiophile.common.infrastructure.navigation.core
  (:require
    #?(:cljs [audiophile.ui.utils.dom :as dom])
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.navigation.protocols :as pnav]))

(defn ^:private path* [nav handle params]
  (if (keyword? handle)
    (serdes/serialize nav handle params)
    handle))

(defn path-for
  "return path for handle"
  ([nav handle]
   (path-for nav handle nil))
  ([nav handle params]
   (path* nav handle params)))

(defn navigate!
  "push a path + params to the browser's history"
  ([nav handle]
   (navigate! nav handle nil))
  ([nav handle params]
   (pnav/navigate! nav (path* nav handle params))))

(defn replace!
  "replace current path with new path"
  ([nav handle]
   (replace! nav handle nil))
  ([nav handle params]
   (pnav/replace! nav (path* nav handle params))))

(defn match-route [nav path]
  (serdes/deserialize nav path))

#?(:cljs
   (defn goto!
     ([nav handle]
      (goto! nav handle nil))
     ([nav handle params]
      (dom/assign! (path* nav handle params)))))
