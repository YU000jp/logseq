(ns frontend.components.property.util
  "Property component utils"
  (:require [frontend.state :as state]
            [frontend.handler.db-based.property :as db-property-handler]))

(defn update-property!
  [property property-name property-schema]
  (when (or (not= (:block/original-name property) property-name)
            (not= (:block/schema property) property-schema))
    (db-property-handler/upsert-property!
     (state/get-current-repo)
     (:db/ident property)
     property-schema
     {:property-name property-name})))
