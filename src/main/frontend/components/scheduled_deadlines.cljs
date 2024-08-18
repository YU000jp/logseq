(ns frontend.components.scheduled-deadlines
  (:require [frontend.date :as date]
            [frontend.state :as state]
            [frontend.ui :as ui]
            [frontend.context.i18n :refer [t]]
            [frontend.components.content :as content]
            [frontend.components.block :as block]
            [clojure.string :as string]
            [frontend.components.editor :as editor]
            [rum.core :as rum]
            [frontend.db :as db]
            [frontend.db-mixins :as db-mixins]))


(defn- scheduled-or-deadlines?
  [page-name]
  (true? (and (date/valid-journal-title? (string/capitalize page-name))
              (not (true? (state/scheduled-deadlines-disabled?))))))


(defn- get-refs
  [page-name]
  (when (true? (scheduled-or-deadlines? page-name))
    (db/get-scheduled-or-deadlines (string/capitalize page-name))))


(rum/defc scheduled-and-deadlines-inner
  [page-name refs]
  (when refs
    (if (seq refs)
      [:div.scheduled-deadlines.references-blocks.mb-2.text-sm
       (let [ref-hiccup (block/->hiccup refs
                                        {:id (str page-name "-agenda")
                                         :ref? true
                                         :group-by-page? true
                                         :editor-box editor/box}
                                        {})]
         (content/content page-name {:hiccup ref-hiccup}))]
      [:span (t :right-side-bar/scheduled-and-deadline-no-result)])))

(rum/defc scheduled-and-deadlines
  [page-name]
  (when-let [refs (get-refs page-name)]
    (scheduled-and-deadlines-inner page-name refs)))


(rum/defc scheduled-and-deadlines-for-left-menu < rum/reactive db-mixins/query
  [page-name on-contents-scroll]
  (when-let [refs (get-refs page-name)]
    (when (seq refs)
      [:div.nav-contents-container.gap-1.pt-1
       {:on-scroll on-contents-scroll}
       [:details
        {:open "true"}
        [:summary
         [(ui/icon "calendar-time" {:class "text-sm mr-1"})

          (let [countNumber (count refs)]
            [:span.overflow-hidden.text-ellipsis
             {:title (t :right-side-bar/scheduled-and-deadline-desc)}
             [(t :right-side-bar/scheduled-and-deadline) " (" countNumber ")"]])]]
        (scheduled-and-deadlines-inner page-name refs)]])))


(defn- get-refs-for-repeat
  [page-name]
  (when (true? (scheduled-or-deadlines? page-name))
    (db/get-repeat-tasks (string/capitalize page-name))))

(rum/defc repeat-tasks
  [page-name]
  (let [refs (get-refs-for-repeat page-name)]
    (scheduled-and-deadlines-inner page-name refs)))

(rum/defc repeat-tasks-for-left-menu < rum/reactive db-mixins/query
  [page-name on-contents-scroll]
  (when-let [refs (get-refs-for-repeat page-name)]
    (when (seq refs)
      [:div.nav-contents-container.gap-1.pt-1
       {:on-scroll on-contents-scroll}
       [:details
        {:open "true"}
        [:summary
         [(ui/icon "calendar-time" {:class "text-sm mr-1"})

          (let [countNumber (count refs)]
            [:span.overflow-hidden.text-ellipsis
             {:title (t :right-side-bar/scheduled-and-deadline-desc)}
             [(t :right-side-bar/repeat-tasks) " (" countNumber ")"]])]]
        (scheduled-and-deadlines-inner page-name refs)]])))

(rum/defc repeat-tasks-for-toolbar-tip < rum/reactive db-mixins/query
  [page-name]
  (when-let [refs (get-refs-for-repeat page-name)]
    (when (seq refs)
      [:div
       [(ui/icon "calendar-time" {:class "text-sm mr-1"})
        (let [countNumber (count refs)]
          [:span.overflow-hidden.text-ellipsis
           {:title (t :right-side-bar/repeat-tasks)}
           [" (" countNumber ")"]])]])))


(rum/defc scheduled-and-deadlines-for-toolbar-tip < rum/reactive db-mixins/query
  [page-name]
  (when-let [refs (get-refs page-name)]
    (when (seq refs)
      [:div
       [(ui/icon "calendar-time" {:class "text-sm mr-1"})
        (let [countNumber (count refs)]
          [:span.overflow-hidden.text-ellipsis
           {:title (t :right-side-bar/scheduled-and-deadline)}
           [" (" countNumber ")"]])]])))


(defn- get-refs-for-date-history
  [page-name]
  (when (true? (scheduled-or-deadlines? page-name))
    (db/get-scheduled-or-deadlines-for-date-history (string/capitalize page-name))))


(rum/defc scheduled-and-deadlines-for-date-history < rum/reactive db-mixins/query
  [page-name]
  (when-let [refs (get-refs-for-date-history page-name)]
    (when (seq refs)
      [:div.gap-1.pt-1
       [:details
        {:open "true"}
        [:summary
         [(ui/icon "calendar-time" {:class "text-sm mr-1"})

          (let [countNumber (count refs)]
            [:span.overflow-hidden.text-ellipsis
             {:title (t :right-side-bar/scheduled-and-deadline-desc)}
             [(t :right-side-bar/scheduled-and-deadline) " (" countNumber ")"]])]]
        (scheduled-and-deadlines-inner page-name refs)]])))