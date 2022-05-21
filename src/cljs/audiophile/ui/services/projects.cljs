(ns audiophile.ui.services.projects
  (:require
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.services.pages :as pages]))

(def ^:private validator:new
  (val/validator {:spec specs/project:create}))

(defn res:fetch-all [sys]
  (pages/res:fetch-all sys :api/projects))

(defn form:new [sys attrs team-id]
  (let [*form (form.std/create {:project/team-id team-id} validator:new)]
    (pages/form:new sys attrs *form :api/projects)))
