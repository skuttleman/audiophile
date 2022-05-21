(ns audiophile.ui.components.notices
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.store.actions :as act]
    [reagent.core :as r]))

(defn level->class [level]
  (case level
    :success "is-success"
    :error "is-danger"
    :warning "is-warning"
    "is-info"))

(defn ^:private banner-message [{:keys [store]} banner]
  (let [{banner-id :id :keys [body level]} banner]
    [:li.banner-message.message
     {:class    [(level->class level)]
      :on-click (fn [_]
                  (store/dispatch! store (act/banner:remove! banner-id)))
      :style    {:cursor :pointer}}
     [:div.message-header
      [:button.delete {:aria-label "delete"}]]
     [:div.message-body
      [:div.body-text body]]]))

(defn ^:private toast-message [{:keys [store]} toast]
  (r/with-let [height (volatile! nil)]
    (let [{toast-id :id :keys [body level state]} toast
          adding? (= state :init)
          removing? (= state :removing)]
      [:li.toast-message.message
       (cond-> {:ref   (fn [node]
                         (some->> node
                                  .getBoundingClientRect
                                  .-height
                                  (vreset! height)))
                :class [(level->class level)
                        (when adding? "adding")
                        (when removing? "removing")]}
         (and removing? @height) (update :style assoc :margin-top (str "-" @height "px")))
       [:div.message-body
        {:on-click (fn [_]
                     (store/dispatch! store (act/toast:remove! toast-id)))
         :style    {:cursor :pointer}}
        [:div.body-text @body]]])))

(defn banners [sys banners]
  [:div.banner-container
   [:ul.banner-messages
    (for [[banner-id banner] (take 2 (sort-by key banners))]
      ^{:key banner-id}
      [banner-message sys banner])]])

(defn toasts [sys toasts]
  [:div.toast-container
   [:ul.toast-messages
    (for [[toast-id toast] (take 2 (sort-by key toasts))]
      ^{:key toast-id}
      [toast-message sys toast])]])
