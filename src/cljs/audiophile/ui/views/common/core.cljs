(ns audiophile.ui.views.common.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.components.core :as comp]
    [audiophile.ui.components.input-fields :as in]
    [audiophile.ui.components.modals :as modals]
    [audiophile.ui.forms.core :as forms]
    [audiophile.ui.views.common.services :as cserv]
    [reagent.core :as r]))

(defn ^:private version* [sys {:keys [file] :as attrs}]
  (r/with-let [*artifacts (cserv/artifacts#res:new sys)
               *form (cserv/files#form:version sys attrs (:file/id file))]
    (let [filename (get-in @*form [:artifact/details :artifact/filename])]
      [comp/form {:*form    *form
                  :disabled (res/requesting? *artifacts)}
       [in/uploader (-> {:style     {:width           "100%"
                                     :justify-content :flex-start}
                         :label     "File"
                         :*resource *artifacts
                         :display   (or filename "Select file…")}
                        (forms/with-attrs *form [:artifact/details]))]
       [in/input (forms/with-attrs {:label "Version name"}
                                   *form
                                   [:version/name])]])
    (finally
      (forms/destroy! *form)
      (res/destroy! *artifacts))))

(defmethod modals/body ::version
  [_ sys attrs]
  (let [attrs (cserv/modals#with-on-success attrs)]
    [version* sys attrs]))
