(ns audiophile.ui.services.teams
  (:require
    [audiophile.common.domain.validations.core :as val]
    [audiophile.common.domain.validations.specs :as specs]
    [audiophile.ui.forms.standard :as form.std]
    [audiophile.ui.services.pages :as pages]))

(def ^:private validator:new
  (val/validator {:spec specs/team:create}))

(defn res:fetch-all [sys]
  (pages/res:fetch-all sys :api/teams))

(defn form:new [sys attrs]
  (let [*form (form.std/create {:team/type :COLLABORATIVE} validator:new)]
    (pages/form:new sys attrs *form :api/teams)))
