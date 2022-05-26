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

(defn ^:private create* [teams sys attrs]
  (r/with-let [options (->> teams
                            (colls/split-on personal?)
                            (apply concat)
                            (map (juxt :team/id identity)))
               options-by-id (into {} options)
               *form (serv/projects#form:new sys attrs (ffirst options))]
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

(defmethod modals/body ::create
  [_ sys {:keys [*res close!] :as attrs}]
  (let [attrs (assoc attrs :on-success (fn [result]
                                         (when close!
                                           (close! result))
                                         (some-> *res res/request!)))]
    [comp/with-resource (:*teams attrs) create* sys attrs]))

(defn project-list [projects sys]
  [:div
   [:p [:strong "Your projects"]]
   (if (seq projects)
     [:ul.project-list
      (for [{:project/keys [id name]} projects]
        ^{:key id}
        [:li.project-item.layout--space-between
         [:a.link {:href (serv/projects#nav:ui sys id)}
          [:span name]]])]
     [:p "You don't have any projects. Why not create one?"])])

(defn tile [sys *teams]
  (r/with-let [*res (serv/projects#res:fetch-all sys)
               click (serv/projects#modal:create sys [::create {:*res   *res
                                                                :*teams *teams}])]
    [comp/tile
     [:h2.subtitle "Projects"]
     [comp/with-resource [*res {:spinner/size :small}] project-list sys]
     [comp/plain-button
      {:class    ["is-primary"]
       :on-click click}
      "Create one"]]))
