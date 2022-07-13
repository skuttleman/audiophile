(ns audiophile.ui.views.project.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.strings :as strings]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.views.common.core :as views]
    [audiophile.ui.views.common.services :as cserv]
    [audiophile.ui.views.project.services :as serv]
    [reagent.core :as r]))

(defn ^:private create* [sys attrs]
  (r/with-let [*artifacts (cserv/artifacts#res:new sys)
               *form (serv/files#form:new sys attrs (:project-id attrs))]
    (let [filename (get-in @*form [:artifact/details :artifact/filename])]
      [comp/form {:*form    *form
                  :disabled (res/requesting? *artifacts)}
       [in/uploader (-> {:style     {:width           "100%"
                                     :justify-content :flex-start}
                         :label     "File"
                         :*resource *artifacts
                         :display   (or filename "Select file…")}
                        (forms/with-attrs *form [:artifact/details]))]
       [in/input (forms/with-attrs {:label "Track name"}
                                   *form
                                   [:file/name])]
       [in/input (forms/with-attrs {:label "Version name"}
                                   *form
                                   [:version/name])]])
    (finally
      (forms/destroy! *form)
      (forms/destroy! *artifacts))))

(defmethod modals/body ::create
  [_ sys {:keys [*res close!] :as attrs}]
  (let [attrs (assoc attrs :on-success (fn [result]
                                         (when close!
                                           (close! result))
                                         (some-> *res res/request!)))]
    [create* sys attrs]))

(defn ^:private team-view [team]
  [:h3 [:em (:team/name team)]])

(defn ^:private project-details [sys project]
  (r/with-let [team-id (:project/team-id project)
               *team (serv/teams#res:fetch-one sys team-id)]
    [:div {:style {:display :flex}}
     [:h2.subtitle (:project/name project)]
     [:div {:style {:width "16px"}}]
     [comp/with-resource *team team-view]]
    (finally
      (res/destroy! *team))))

(defn ^:private track-row [sys *files idx file]
  (r/with-let [click (serv/files#modal:version sys [::views/version {:*res *files
                                                                     :file file}])]
    [:tr
     [:td {:style {:white-space :nowrap}}
      [:em (strings/format "%02d" (inc idx))]]
     [:td {:style {:width "99%"}}
      [:a.link {:href (serv/files#nav:one sys (:file/id file))}
       [:span [:strong (:file/name file)] " - " (:version/name file)]]]
     [:td
      [comp/plain-button
       {:class    ["is-outlined" "is-info"]
        :on-click click}
       "New version"]]]))

(defn track-list [sys {:keys [*files *project project-id]} files]
  (r/with-let [click (serv/files#modal:create sys [::create {:*res       *files
                                                             :project-id project-id}])]
    (when-not (res/error? *project)
      (if (res/success? *project)
        [:div
         [:div.buttons
          [comp/plain-button
           {:class    ["is-primary"]
            :on-click click}
           "New track"]]
         (if (seq files)
           [:table.table.is-striped.is-fullwidth
            [:tbody
             (for [[idx file] (map-indexed vector files)]
               ^{:key (:file/id file)}
               [track-row sys *files idx file])]]
           [:div "This projects doesn't have any tracks. You should upload one."])]
        [comp/spinner]))))

(defn ^:private page [{:keys [nav] :as sys}]
  (r/with-let [project-id (-> @nav :params :project/id)
               *files (serv/files#res:fetch-all sys project-id)
               *project (serv/projects#res:fetch-one sys project-id)
               *sub (serv/projects#sub:start! sys project-id *project)]
    [:div.layout--space-below.layout--xxl.gutters
     [:div {:style {:width "100%"}}
      [comp/with-resource *project [project-details sys]]
      [comp/with-resource *files [track-list sys (maps/->m *files *project project-id)]]]]
    (finally
      (serv/project#sub:stop! *sub)
      (run! res/destroy! [*project *files]))))

(defn root [sys]
  [page sys])
