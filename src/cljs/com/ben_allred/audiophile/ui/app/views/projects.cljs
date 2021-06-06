(ns com.ben-allred.audiophile.ui.app.views.projects
  (:refer-clojure :exclude [list])
  (:require
    [com.ben-allred.audiophile.common.app.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.strings :as strings]
    [com.ben-allred.audiophile.common.domain.validations.core :as val]
    [com.ben-allred.audiophile.common.domain.validations.specs :as specs]
    [com.ben-allred.audiophile.ui.app.forms.standard :as form]
    [com.ben-allred.audiophile.ui.app.forms.submittable :as form.sub]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.core.components.input-fields.dropdown :as dd]
    [com.ben-allred.audiophile.ui.core.forms.core :as forms]
    [com.ben-allred.vow.core :as v :include-macros true]))

(defn ^:private with-artifact [form]
  (cond-> form
    (and (vector? form)
         (= :artifact/id (first form)))
    (->> (conj [:map])
         (conj [:artifact/details]))))

(def ^:private validator
  (val/validator {:spec specs/project:create}))

(def ^:private file-validator
  (val/validator {:spec (with-meta (colls/postwalk with-artifact specs/file:create)
                                   {:missing-keys {:file/name        "track name is required"
                                                   :version/name     "version name is required"
                                                   :artifact/details "file is required"}})}))

(def ^:private version-validator
  (val/validator {:spec (with-meta (colls/postwalk with-artifact specs/version:create)
                                   {:missing-keys {:version/name     "version name is required"
                                                   :artifact/details "file is required"}})}))

(defmulti ^:private team-name :team/type)

(defmethod team-name :PERSONAL
  [_]
  [:em "MY PERSONAL STUFF"])

(defmethod team-name :COLLABORATIVE
  [{:team/keys [name]}]
  [:em name])

(def ^:private personal?
  (comp #{:PERSONAL} :team/type))

(defn ^:private attrs->content [{:keys [options-by-id value]}]
  (if-let [team-id (first value)]
    (get-in options-by-id [team-id :team/name])
    "Select a team…"))

(defn ^:private team-view [team]
  [:h3 [:em (:team/name team)]])

(defn ^:private project-details [project *team]
  (let [opts {:nav/params {:route-params {:team-id (:project/team-id project)}}}]
    [:div {:style {:display :flex}}
     [:h2.subtitle (:project/name project)]
     [:div {:style {:width "16px"}}]
     [comp/with-resource [*team opts] team-view]]))

(defn ^:private create* [teams *projects _cb]
  (let [options (->> teams
                     (colls/split-on personal?)
                     (apply concat)
                     (map (juxt :team/id identity)))
        *form (form.sub/create *projects (form/create {:project/team-id (ffirst options)}
                                                      validator))
        options-by-id (into {} options)]
    (fn [_teams _*projects cb]
      [:div
       [comp/form {:*form        *form
                   :on-submitted (fn [vow]
                                   (v/peek vow cb nil))}
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

(defmethod form.sub/internal->remote ::file
  [_ data]
  (merge data (:artifact/details data)))

(defn version-form [{:keys [*artifacts *file-version *files]}]
  (fn [file _cb]
    (let [project-id (:file/project-id file)
          *form (form.sub/create ::file
                                 *file-version
                                 (form/create nil version-validator)
                                 {:nav/params {:route-params {:file-id    (:file/id file)
                                                          :project-id project-id}}})
          file-opts {:nav/params {:route-params {:project-id project-id}}}]
      (fn [_file cb]
        [comp/form {:*form        *form
                    :disabled     (res/requesting? *artifacts)
                    :on-submitted (fn [vow]
                                    (v/peek vow
                                            (fn [_]
                                              (res/request! *files file-opts)
                                              (when cb (cb nil)))
                                            nil))}
         [in/input (forms/with-attrs {:label "Version name"}
                                     *form
                                     [:version/name])]
         [in/uploader (-> {:label     "File"
                           :*resource *artifacts
                           :display   (if-let [filename (get-in @*form [:artifact/details :artifact/filename])]
                                        filename
                                        "Select file…")}
                          (forms/with-attrs *form [:artifact/details]))]]))))

(defn file-form [{:keys [*artifacts *file *files]}]
  (fn [project-id _cb]
    (let [*form (form.sub/create ::file
                                 *file
                                 (form/create nil file-validator)
                                 {:nav/params {:route-params {:project-id project-id}}})
          file-opts {:nav/params {:route-params {:project-id project-id}}}]
      (fn [_project-id cb]
        [comp/form {:*form        *form
                    :disabled     (res/requesting? *artifacts)
                    :on-submitted (fn [vow]
                                    (v/peek vow
                                            (fn [e]
                                              (res/request! *files file-opts)
                                              (when cb (cb e)))
                                            nil))}
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
             [:a.link {:href (nav/path-for nav :ui/file {:route-params {:file-id (:file/id file)}})}
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
    (let [project-id (get-in state [:nav/route :route-params :project-id])
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
           [:a.link {:href (nav/path-for nav :ui/project {:route-params {:project-id id}})}
            [:span name]]])]
       [:p "You don't have any projects. Why not create one?"])]))

(defn create [{:keys [*all-projects projects *teams]}]
  (fn [cb]
    [comp/with-resource [*teams] create* projects (fn [result]
                                                    (res/request! *all-projects)
                                                    (when cb (cb result)))]))
