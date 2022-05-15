(ns com.ben-allred.audiophile.common.api.navigation.core
  (:require
    [com.ben-allred.audiophile.common.api.navigation.protocols :as pnav]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

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
