(ns com.ben-allred.audiophile.ui.api.views.projects
  (:refer-clojure :exclude [list])
  (:require
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.strings :as strings]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.components.input-fields.dropdown :as dd]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.vow.core :as v :include-macros true]
    [com.ben-allred.audiophile.ui.api.views.core :as views]))

(defn team-name [{:team/keys [name]}]
  [:em name])

(def ^:private personal?
  (comp #{:PERSONAL} :team/type))

(defn ^:private attrs->content [{:keys [options-by-id value]}]
  (if-let [team-id (first value)]
    (get-in options-by-id [team-id :team/name])
    "Select a team…"))

(defn ^:private project-href [nav project-id]
  (nav/path-for nav :ui/project {:params {:project/id project-id}}))

(defn ^:private file-href [nav file-id]
  (nav/path-for nav :ui/file {:params {:file/id file-id}}))

(defn ^:private team-view [team]
  [:h3 [:em (:team/name team)]])

(defn ^:private project-details [project *team]
  (let [opts {:nav/params {:params {:team/id (:project/team-id project)}}}]
    [:div {:style {:display :flex}}
     [:h2.subtitle (:project/name project)]
     [:div {:style {:width "16px"}}]
     [comp/with-resource [*team opts] team-view]]))

(defn ^:private create* [teams *int _cb]
  (let [options (->> teams
                     (colls/split-on personal?)
                     (apply concat)
                     (map (juxt :team/id identity)))
        *form (views/project-form *int options)
        options-by-id (into {} options)]
    (fn [_teams _*projects cb]
      [:div
       [comp/form {:*form        *form
                   :on-submitted (views/on-project-created *int cb)}
        (when (>= (count options-by-id) 2)
          [dd/dropdown (-> {:options        options
                            :options-by-id  options-by-id
                            :item-control   team-name
                            :force-value?   true
                            :label          "Team"
                            :attrs->content attrs->content}
                           (forms/with-attrs *form [:project/team-id])
                           dd/singleable)])
        [in/input (forms/with-attrs {:label       "Name"
                                     :auto-focus? true}
                                    *form
                                    [:project/name])]]])))

(defn version-form [{:keys [*artifacts *int]}]
  (fn [file cb]
    (let [project-id (:file/project-id file)
          *form (views/version-form *int project-id (:file/id file))
          on-submitted (views/on-version-created *int project-id cb)]
      (fn [_file _cb]
        [comp/form {:*form        *form
                    :disabled     (res/requesting? *artifacts)
                    :on-submitted on-submitted}
         [in/input (forms/with-attrs {:label "Version name"}
                                     *form
                                     [:version/name])]
         [in/uploader (-> {:label     "File"
                           :*resource *artifacts
                           :display   (if-let [filename (get-in @*form [:artifact/details :artifact/filename])]
                                        filename
                                        "Select file…")}
                          (forms/with-attrs *form [:artifact/details]))]]))))

(defn file-form [{:keys [*artifacts *int]}]
  (fn [project-id cb]
    (let [*form (views/file-form *int project-id)
          on-submitted (views/on-file-created *int project-id cb)]
      (fn [_project-id _cb]
        [comp/form {:*form        *form
                    :disabled     (res/requesting? *artifacts)
                    :on-submitted on-submitted}
         [in/input (forms/with-attrs {:label "Track name"}
                                     *form
                                     [:file/name])]
         [in/input (forms/with-attrs {:label "Version name"}
                                     *form
                                     [:version/name])]
         [in/uploader (-> {:label     "File"
                           :*resource *artifacts
                           :display   (if-let [filename (get-in @*form [:artifact/details :artifact/filename])]
                                        filename
                                        "Select file…")}
                          (forms/with-attrs *form [:artifact/details]))]]))))

(defn track-list [{:keys [file-form nav *modals version-form]}]
  (fn [files project-id]
    [:div
     [:div.buttons
      [in/plain-button
       {:class    ["is-primary"]
        :on-click (comp/modal-opener *modals
                                     "Upload new track"
                                     [file-form project-id])}
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
             [:a.link {:href (file-href nav (:file/id file))}
              [:span [:strong (:file/name file)] " - " (:version/name file)]]]
            [:td
             [in/plain-button
              {:class    ["is-outlined"]
               :on-click (comp/modal-opener *modals
                                            "Upload new version"
                                            [version-form file])}
              "Upload new version"]]])]]
       [:div "This projects doesn't have any tracks. You should upload one."])]))

(defn one [{:keys [*files *project *team track-list]}]
  (fn [state]
    (let [project-id (get-in state [:nav/route :params :project/id])
          opts {:nav/params (:nav/route state)}]
      [:div
       [comp/with-resource [*project opts] project-details *team]
       [comp/with-resource [*files opts] track-list project-id]])))

(defn list [{:keys [nav]}]
  (fn [projects _state]
    [:div
     [:p [:strong "Your projects"]]
     (if (seq projects)
       [:ul
        (for [{:project/keys [id name]} projects]
          ^{:key id}
          [:li.layout--space-between
           [:a.link {:href (project-href nav id)}
            [:span name]]])]
       [:p "You don't have any projects. Why not create one?"])]))

(defn create [{:keys [*int *teams]}]
  (fn [cb]
    [comp/with-resource *teams create* *int cb]))
