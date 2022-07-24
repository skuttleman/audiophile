(ns audiophile.ui.views.dashboard.projects
  (:require
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.components.input-fields.dropdown :as dd]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.views.common.services :as cserv]
    [audiophile.ui.views.dashboard.services :as serv]
    [reagent.core :as r]))

(def ^:private personal?
  (comp #{:PERSONAL} keyword :team/type))

(defn ^:private team-name [{:team/keys [name]}]
  [:em name])

(defn ^:private attrs->content [{:keys [options-by-id value]}]
  (if-let [team-id (first value)]
    (get-in options-by-id [team-id :team/name])
    "Select a team…"))

(defn ^:private create* [sys attrs teams]
  (r/with-let [options (->> teams
                            (colls/split-on personal?)
                            (apply concat)
                            (map (juxt :team/id identity)))
               options-by-id (into {} options)
               *form (cserv/projects#form:new sys attrs (ffirst options))]
    [:div
     [comp/form {:*form *form}
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
                                  [:project/name])]]]
    (finally
      (forms/destroy! *form))))

(defn ^:private update* [*form _]
  [:div
   [comp/form {:*form *form}
    [in/input (forms/with-attrs {:label       "Name"
                                 :auto-focus? true}
                                *form
                                [:project/name])]]])

(defmethod modals/body ::create
  [_ sys attrs]
  (let [attrs (cserv/modals#with-on-success attrs)]
    [comp/with-resource (:*teams attrs) [create* sys attrs]]))

(defmethod modals/body ::update
  [_ sys {:keys [*res project-id] :as attrs}]
  (r/with-let [attrs (cserv/modals#with-on-success attrs)
               *form (serv/projects#form:modify sys attrs (->> @*res
                                                               (filter (comp #{project-id} :project/id))
                                                               first))]
    [update* *form attrs]
    (finally
      (forms/destroy! *form))))

(defn project-item [{:keys [*res sys]} {:project/keys [id name]}]
  (r/with-let [click (when *res
                       (serv/projects#modal:update sys [::update {:*res *res :project-id id}]))]
    [:li.project-item.layout--space-between.layout--align-center
     [:a.link {:href (serv/projects#nav:ui sys id)}
      [:span name]]
     (when click
       [comp/plain-button {:class    ["is-text" "layout--space-between"]
                           :on-click click}
        [comp/icon :edit]
        [:span "edit"]])]))

(defn project-list [attrs projects]
  (if (seq projects)
    [:ul.project-list
     (for [{:project/keys [id] :as project} projects]
       ^{:key id}
       [project-item attrs project])]
    [:p "You don't have any projects. Why not create one?"]))

(defn tile-content [attrs projects]
  [:div
   [:p [:strong "Your projects"]]
   [project-list attrs projects]])

(defn tile [sys *projects *teams]
  (r/with-let [click (cserv/projects#modal:create sys [::create {:*res   *projects
                                                                 :*teams *teams}])]
    [comp/tile
     [:h2.subtitle "Projects"]
     [comp/with-resource [*projects {:spinner/size :small}] [tile-content {:*res *projects
                                                                           :sys  sys}]]
     [comp/plain-button
      {:class    ["is-primary"]
       :on-click click}
      "Create one"]]))
