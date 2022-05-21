(ns com.ben-allred.audiophile.ui.infrastructure.components.projects
  (:refer-clojure :exclude [list])
  (:require
    [clojure.set :as set]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.navigation.core :as nav]
    [com.ben-allred.audiophile.common.infrastructure.store.core :as store]
    [com.ben-allred.audiophile.ui.infrastructure.components.core :as comp]
    [com.ben-allred.audiophile.ui.infrastructure.components.input-fields :as in]
    [com.ben-allred.audiophile.ui.infrastructure.components.input-fields.dropdown :as dd]
    [com.ben-allred.audiophile.ui.infrastructure.components.modals :as modals]
    [com.ben-allred.audiophile.ui.infrastructure.forms.core :as forms]
    [com.ben-allred.audiophile.ui.infrastructure.services.projects :as sproj]
    [com.ben-allred.audiophile.ui.infrastructure.store.actions :as act]
    [com.ben-allred.vow.core :as v]
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
               *form (sproj/form:new sys
                                     (set/rename-keys attrs {:close! :on-success})
                                     (ffirst options))]
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
                                  [:project/name])]]]))

(defmethod modals/body ::create
  [_ sys attrs]
  [comp/with-resource (:*teams attrs) create* sys attrs])

(defn list [projects {:keys [nav]}]
  [:div
   [:p [:strong "Your projects"]]
   (if (seq projects)
     [:ul.project-list
      (for [{:project/keys [id name]} projects]
        ^{:key id}
        [:li.project-item.layout--space-between
         [:a.link {:href (nav/path-for nav :ui/project {:params {:project/id id}})}
          [:span name]]])]
     [:p "You don't have any projects. Why not create one?"])])

(defn tile [{:keys [store] :as sys} *teams]
  (r/with-let [*res (sproj/res:fetch-all sys)]
    [comp/tile
     [:h2.subtitle "Projects"]
     [comp/with-resource [*res {:spinner/size :small}] list sys]
     [in/plain-button
      {:class    ["is-primary"]
       :on-click (fn [_]
                   (store/dispatch! store (act/modal:add! [:h1.subtitle "Create a project"]
                                                          [::create {:*res   *res
                                                                     :*teams *teams}])))}
      "Create one"]]))
