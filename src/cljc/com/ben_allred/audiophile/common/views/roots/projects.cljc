(ns com.ben-allred.audiophile.common.views.roots.projects
  (:require
    [#?(:cljs    com.ben-allred.audiophile.ui.services.forms.standard
        :default com.ben-allred.audiophile.common.services.forms.noop) :as form]
    [com.ben-allred.audiophile.common.services.forms.core :as forms]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.resources.core :as res]
    [com.ben-allred.audiophile.common.services.resources.validated :as vres]
    [com.ben-allred.audiophile.common.services.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [com.ben-allred.audiophile.common.views.components.input-fields :as in]
    [com.ben-allred.audiophile.common.views.components.input-fields.dropdown :as dd]
    [com.ben-allred.vow.core :as v #?@(:cljs [:include-macros true])]
    [integrant.core :as ig]))

(def ^:private validator
  (constantly nil))

(defmulti team-name :team/type)

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

(defmethod ig/init-key ::list [_ {:keys [nav]}]
  (fn [projects _state]
    [:div
     [:p [:strong "Your projects"]]
     (if (seq projects)
       [:ul
        (for [{:project/keys [id name]} projects]
          ^{:key id}
          [:li.layout--space-between
           [:span "• " name]
           [:a.link {:href (nav/path-for nav :ui/project {:route-params {:project-id id}})}
            "Details"]])]
       [:p "You don't have any projects. Why not create one?"])]))

(defn ^:private clicker [store title view]
  (fn [_]
    (ui-store/dispatch! store
                        (actions/modal! [:h2.subtitle title]
                                        view))))

(defmethod vres/internal->remote ::file
  [_ data]
  (merge data (:artifact/details data)))

(defmethod ig/init-key ::version-form [_ {:keys [artifacts file-version files]}]
  (fn [file-data _cb]
    (let [project-id (:file/project-id file-data)
          form (vres/create ::file
                            file-version
                            (form/create nil (constantly nil))
                            {:nav/params {:route-params {:file-id    (:file/id file-data)
                                                         :project-id project-id}}})
          file-opts {:nav/params {:route-params {:project-id project-id}}}]
      (fn [_file cb]
        [comp/form {:form         form
                    :disabled     (res/requesting? artifacts)
                    :on-submitted (fn [vow]
                                    (v/peek vow
                                            (fn [_]
                                              (res/request! files file-opts)
                                              (when cb (cb nil)))
                                            nil))}
         [in/input (forms/with-attrs {:label "Version name"}
                                     form
                                     [:version/name])]
         [in/uploader (-> {:label    "File"
                           :resource artifacts
                           :display  (if-let [filename (get-in @form [:artifact/details :artifact/filename])]
                                       filename
                                       "Select file…")}
                          (forms/with-attrs form [:artifact/details]))]]))))

(defmethod ig/init-key ::file-form [_ {:keys [artifacts file files]}]
  (fn [project-id _cb]
    (let [form (vres/create ::file
                            file
                            (form/create nil (constantly nil))
                            {:nav/params {:route-params {:project-id project-id}}})
          file-opts {:nav/params {:route-params {:project-id project-id}}}]
      (fn [_project-id cb]
        [comp/form {:form         form
                    :disabled     (res/requesting? artifacts)
                    :on-submitted (fn [vow]
                                    (v/peek vow
                                            (fn [e]
                                              (res/request! files file-opts)
                                              (when cb (cb e)))
                                            nil))}
         [in/input (forms/with-attrs {:label "Track name"}
                                     form
                                     [:file/name])]
         [in/input (forms/with-attrs {:label "Version name"}
                                     form
                                     [:version/name])]
         [in/uploader (-> {:label    "File"
                           :resource artifacts
                           :display  (if-let [filename (get-in @form [:artifact/details :artifact/filename])]
                                       filename
                                       "Select file…")}
                          (forms/with-attrs form [:artifact/details]))]]))))

(defmethod ig/init-key ::track-list [_ {:keys [file-form store version-form]}]
  (fn [files project-id]
    [:div
     [:button.button.is-white
      {:on-click (clicker store
                          "Upload new track"
                          [file-form project-id])}
      "New track"]
     [:p "Tracks"]
     [:ul
      (for [file files]
        ^{:key (:file/id file)}
        [:li
         [:span (:file/name file) " - " (:version/name file)]
         [:button.button.is-white
          {:on-click (clicker store
                              "Upload new version"
                              [version-form file])}
          "New version"]])]]))

(defn ^:private team-view [team]
  [:h3 [:em (:team/name team)]])

(defn ^:private project-details [project *team]
  (let [opts {:nav/params {:route-params {:team-id (:project/team-id project)}}}]
    [:div
     [:h2.subtitle (:project/name project)]
     [comp/with-resource [*team opts] team-view]]))

(defmethod ig/init-key ::one [_ {:keys [files project team track-list]}]
  (fn [state]
    (let [project-id (get-in state [:page :route-params :project-id])
          opts {:nav/params {:route-params {:project-id project-id}}}]
      [:div
       [comp/with-resource [project opts] project-details team]
       [comp/with-resource [files opts] track-list project-id]])))

(defn create* [teams *projects _cb]
  (let [options (->> teams
                     (colls/split-on personal?)
                     (apply concat)
                     (map (juxt :team/id identity)))
        form (vres/create *projects (form/create {:project/team-id (ffirst options)}
                                                 validator))
        options-by-id (into {} options)]
    (fn [_teams _*projects cb]
      [:div
       [comp/form {:form         form
                   :on-submitted (fn [vow]
                                   (v/peek vow cb nil))}
        (when (>= (count options-by-id) 2)
          [dd/dropdown (-> {:options        options
                            :options-by-id  options-by-id
                            :item-control   team-name
                            :force-value?   true
                            :label          "Team"
                            :attrs->content attrs->content}
                           (forms/with-attrs form [:project/team-id])
                           dd/singleable)])
        [in/input (forms/with-attrs {:label       "Name"
                                     :auto-focus? true}
                                    form
                                    [:project/name])]]])))

(defmethod ig/init-key ::create [_ {:keys [all-projects projects teams]}]
  (fn [cb]
    [comp/with-resource [teams] create* projects (fn [result]
                                                   (res/request! all-projects)
                                                   (when cb (cb result)))]))
