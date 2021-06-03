(ns com.ben-allred.audiophile.common.core.navigation.core
  (:require
    [com.ben-allred.audiophile.common.core.navigation.protocols :as pnav]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.stubs.dom :as dom]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn path-for
  ([nav handle]
   (path-for nav handle nil))
  ([nav handle params]
   (serdes/serialize nav handle params)))

(defn navigate!
  "push a path + params to the browser's history"
  ([nav handle]
   (navigate! nav handle nil))
  ([nav handle params]
   (pnav/navigate! nav (path-for nav handle params))))

(defn replace!
  ([nav handle]
   (replace! nav handle nil))
  ([nav handle params]
   (pnav/replace! nav (path-for nav handle params))))

(defn goto!
  "send the browser to a location with a page rebuild"
  ([nav handle]
   (goto! nav handle nil))
  ([nav handle params]
   (dom/assign! (path-for nav handle params))))

(defn match-route [nav path]
  (serdes/deserialize nav path))
