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
            [frontend.db.model :as db]
            [frontend.db-mixins :as db-mixins]))


(defn check?
  [page-name]
  (true? (and (true? (date/valid-journal-title? (string/capitalize page-name)))
              (not (true? (state/scheduled-deadlines-disabled?))))))


(defn scheduled-and-deadlines-inner
  [page-name blocks]
  (if (seq blocks)
    [:div.scheduled-deadlines.references-blocks.mb-2.text-sm
     (let [ref-hiccup (block/->hiccup blocks
                                      {:id (str page-name "-agenda")
                                       :ref? true
                                       :group-by-page? true
                                       :editor-box editor/box}
                                      {})]
       (content/content page-name {:hiccup ref-hiccup}))]
    [:span (t :right-side-bar/scheduled-and-deadline-no-result)]))


(rum/defc scheduled-and-deadlines < rum/reactive db-mixins/query
  [page-name]
  (when (true? (check? page-name))
    (when-let [blocks (db/get-scheduled-and-deadlines (string/capitalize page-name))]
      (scheduled-and-deadlines-inner page-name blocks))))


(rum/defc scheduled-and-deadlines-for-left-menu < rum/reactive db-mixins/query
  [page-name on-contents-scroll]
  (when (true? (check? page-name))
    (when-let [blocks (db/get-scheduled-and-deadlines (string/capitalize page-name))]
      (when (seq blocks)
        [:div.nav-contents-container.gap-1.pt-1
         {:on-scroll on-contents-scroll}
         [:details
          {:open "true"}
          [:summary
           [(ui/icon "calendar-time" {:class "text-sm mr-1"})

            (let [count-number (count blocks)]
              [:span.overflow-hidden.text-ellipsis
               {:title (t :right-side-bar/scheduled-and-deadline-desc)}
               [(t :right-side-bar/scheduled-and-deadline) " (" count-number ")"]])]]
          (scheduled-and-deadlines-inner page-name blocks)]]))))


(rum/defc scheduled-and-deadlines-for-toolbar-tip < rum/reactive db-mixins/query
  [page-name]
  (when (true? (check? page-name))
    (when-let [blocks (db/get-scheduled-and-deadlines (string/capitalize page-name))]
      (when (seq blocks)
        [:div
         [(ui/icon "calendar-time" {:class "text-sm mr-1"})
          (let [count-number (count blocks)]
            [:span.overflow-hidden.text-ellipsis
             [" (" count-number ")"]])]]))))



(rum/defc repeat-tasks < rum/reactive db-mixins/query
  [page-name]
  (when (true? (check? page-name))
    (when-let [blocks (db/get-repeat-tasks (string/capitalize page-name))]
      (scheduled-and-deadlines-inner page-name blocks))))


(rum/defc repeat-tasks-for-left-menu < rum/reactive db-mixins/query
  [page-name on-contents-scroll]
  (when (true? (check? page-name))
    (when-let [blocks (db/get-repeat-tasks (string/capitalize page-name))]
      (when (seq blocks)
        [:div.nav-contents-container.gap-1.pt-1
         {:on-scroll on-contents-scroll}
         [:details
          [:summary
           [(ui/icon "repeat" {:class "text-sm mr-1"})

            (let [count-number (count blocks)]
              [:span.overflow-hidden.text-ellipsis
               {:title (t :right-side-bar/scheduled-and-deadline-desc)}
               [(t :right-side-bar/repeat-tasks) " (" count-number ")"]])]]
          (scheduled-and-deadlines-inner page-name blocks)]]))))


(rum/defc repeat-tasks-for-toolbar-tip < rum/reactive db-mixins/query
  [page-name]
  (when (true? (check? page-name))
    (when-let [blocks (db/get-repeat-tasks (string/capitalize page-name))]
      (when (seq blocks)
        [:div
         [(ui/icon "repeat" {:class "text-sm mr-1"})
          (let [count-number (count blocks)]
            [:span.overflow-hidden.text-ellipsis
             [" (" count-number ")"]])]]))))



(rum/defc scheduled-and-deadlines-for-date-history < rum/reactive db-mixins/query
  [page-name]
  (when (true? (check? page-name))
    (when-let [blocks (db/get-scheduled-and-deadlines-for-date-history (string/capitalize page-name))]
      (when (seq blocks)
        [:div.gap-1.pt-1
         [:details
          {:open "true"}
          [:summary
           [(ui/icon "calendar-time" {:class "text-sm mr-1"})

            (let [count-number (count blocks)]
              [:span.overflow-hidden.text-ellipsis
               {:title (t :right-side-bar/scheduled-and-deadline-desc)}
               [(t :right-side-bar/scheduled-and-deadline) " (" count-number ")"]])]]
          (scheduled-and-deadlines-inner page-name blocks)]]))))