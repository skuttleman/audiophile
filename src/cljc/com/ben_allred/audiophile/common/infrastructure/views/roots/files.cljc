(ns com.ben-allred.audiophile.common.infrastructure.views.roots.files
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.views.components.core :as comp]))

(defn ^:private one* [file player]
  (let [version (first (:file/versions file))]
    [:div.panel
     [:div.panel-heading
      [:p (:file/name file) " - (" (:file-version/name version) ")"]]
     [:div.panel-block
      [player (:file-version/artifact-id (first (:file/versions file)))]]]))

(defn one [{:keys [*file player]}]
  (fn [state]
    [comp/with-resource [*file {:nav/params (:nav/route state)}] one* player]))
