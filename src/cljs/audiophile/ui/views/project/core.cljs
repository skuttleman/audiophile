(ns audiophile.ui.views.project.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.strings :as strings]
    [audiophile.common.infrastructure.navigation.core :as nav]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.store.actions :as act]
    [audiophile.ui.views.project.services :as serv]
    [clojure.set :as set]
    [reagent.core :as r]))

(defn ^:private create* [sys attrs]
  (r/with-let [*artifacts (serv/artifacts#res:new sys)
               *form (serv/files#form:new sys attrs (:project-id attrs))]
    (let [filename (get-in @*form [:artifact/details :artifact/filename])]
      [comp/form {:*form    *form
                  :disabled (res/requesting? *artifacts)}
       [in/uploader (-> {:label     "File"
                         :*resource *artifacts
                         :display   (or filename "Select file…")}
                        (forms/with-attrs *form [:artifact/details]))]
       [in/input (forms/with-attrs {:label "Track name"}
                                   *form
                                   [:file/name])]
       [in/input (forms/with-attrs {:label "Version name"}
                                   *form
                                   [:version/name])]])))

(defn ^:private version* [sys {:keys [file] :as attrs}]
  (r/with-let [*artifacts (serv/artifacts#res:new sys)
               *form (serv/files#form:version sys attrs (:file/id file))]
    (let [filename (get-in @*form [:artifact/details :artifact/filename])]
      [comp/form {:*form    *form
                  :disabled (res/requesting? *artifacts)}
       [in/uploader (-> {:label     "File"
                         :*resource *artifacts
                         :display   (or filename "Select file…")}
                        (forms/with-attrs *form [:artifact/details]))]
       [in/input (forms/with-attrs {:label "Version name"}
                                   *form
                                   [:version/name])]])))

(defmethod modals/body ::create
  [_ sys {:keys [*res close!] :as attrs}]
  (let [attrs (assoc attrs :on-success (fn [result]
                                         (when close!
                                           (close! result))
                                         (some-> *res res/request!)))]
    [create* sys attrs]))

(defmethod modals/body ::version
  [_ sys {:keys [*res close!] :as attrs}]
  (let [attrs (assoc attrs :on-success (fn [result]
                                         (when close!
                                           (close! result))
                                         (some-> *res res/request!)))]
    [version* sys attrs]))

(defn ^:private team-view [team]
  [:h3 [:em (:team/name team)]])

(defn ^:private project-details [project sys]
  (r/with-let [team-id (:project/team-id project)
               *team (serv/teams#res:fetch-one sys team-id)]
    [:div {:style {:display :flex}}
     [:h2.subtitle (:project/name project)]
     [:div {:style {:width "16px"}}]
     [comp/with-resource *team team-view]]))

(defn track-list [files {:keys [nav store]} *files *project project-id]
  (let [status (res/status *project)]
    (if-not (= :success status)
      [comp/spinner]
      [:div
       [:div.buttons
        [comp/plain-button
         {:class    ["is-primary"]
          :on-click (fn [_]
                      (store/dispatch! store (act/modal#add! [:h1.subtitle "Add a track"]
                                                             [::create {:*res       *files
                                                                        :project-id project-id}])))}
         "New track"]]
       (if (seq files)
         [:table.table.is-striped.is-fullwidth
          [:tbody
           (for [[idx file] (map-indexed vector files)]
             ^{:key (:file/id file)}
             [:tr
              [:td {:style {:white-space :nowrap}}
               [:em (strings/format "%02d" (inc idx))]]
              [:td {:style {:width "99%"}}
               [:a.link {:href (nav/path-for nav
                                             :ui/file
                                             {:params {:file/id (:file/id file)}})}
                [:span [:strong (:file/name file)] " - " (:version/name file)]]]
              [:td
               [comp/plain-button
                {:class    ["is-outlined" "is-info"]
                 :on-click (fn [_]
                             (store/dispatch! store (act/modal#add! [:h1.subtitle "Upload a new version"]
                                                                    [::version {:*res *files
                                                                                :file file}])))}
                "New version"]]])]]
         [:div "This projects doesn't have any tracks. You should upload one."])])))

(defn ^:private page [sys state]
  (r/with-let [project-id (get-in state [:nav/route :params :project/id])
               *files (serv/files#res:fetch-all sys project-id)
               *project (serv/projects#res:fetch-one sys project-id)]
    [:div
     [comp/with-resource *project project-details sys]
     [comp/with-resource *files track-list sys *files *project project-id]]))

(defn root [sys state]
  [page sys state])
