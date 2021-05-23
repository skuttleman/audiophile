(ns com.ben-allred.audiophile.common.views.roots.files
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [integrant.core :as ig]))

(defn one* [file player]
  (let [version (first (:file/versions file))]
    [:div.panel
     [:div.panel-heading
      [:p (:file/name file) " - (" (:file-version/name version) ")"]]
     [:div.panel-block
      [player (:file-version/artifact-id (first (:file/versions file)))]]]))

(defmethod ig/init-key ::one [_ {:keys [*file player]}]
  (fn [state]
    [comp/with-resource [*file {:nav/params (:nav/route state)}] one* player]))
