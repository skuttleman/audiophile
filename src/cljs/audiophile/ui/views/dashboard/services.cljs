(ns audiophile.ui.views.dashboard.services
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.ui.services.pages :as pages]
    [audiophile.ui.forms.standard :as form.std]))

(def ^:private projects#validator:new
  (val/validator {:spec specs/project:create}))

(def ^:private teams#validator:new
  (val/validator {:spec specs/team:create}))

(defn projects#res:fetch-all [sys]
  (pages/res:fetch sys :api/projects))

(defn projects#form:new [sys attrs team-id]
  (let [*form (form.std/create {:project/team-id team-id} projects#validator:new)]
    (pages/form:new sys attrs *form :api/projects)))

(defn teams#res:fetch-all [sys]
  (pages/res:fetch sys :api/teams))

(defn teams#form:new [sys attrs]
  (let [*form (form.std/create {:team/type :COLLABORATIVE} teams#validator:new)]
    (pages/form:new sys attrs *form :api/teams)))
