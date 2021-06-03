(ns com.ben-allred.audiophile.common.app.views.files
  (:require
    [com.ben-allred.audiophile.common.core.ui-components.core :as comp]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private file-artifact-id [file]
  (:file-version/artifact-id (first (:file/versions file))))

(defn ^:private one* [file player]
  (let [version (first (:file/versions file))]
    [:div.panel
     [:div.panel-heading
      [:p (:file/name file) " - (" (:file-version/name version) ")"]]
     [:div.panel-block
      [player (file-artifact-id file)]]]))

(defn one [{:keys [*file player]}]
  (fn [state]
    [comp/with-resource [*file {:nav/params (:nav/route state)}] one* player]))
