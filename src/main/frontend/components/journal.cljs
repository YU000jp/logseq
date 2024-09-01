(ns frontend.components.journal
  (:require [clojure.string :as string]
            [frontend.components.page :as page]
            [frontend.components.reference :as reference]
            ;;[frontend.components.scheduled-deadlines :as scheduled]
            [frontend.date :as date]
            [frontend.db :as db]
            [frontend.db-mixins :as db-mixins]
            [frontend.db.model :as model]
            [logseq.graph-parser.util.db :as db-util]
            [frontend.handler.page :as page-handler]
            ;; [frontend.components.block :as block]
            [frontend.context.i18n :refer [t]]
            [frontend.state :as state]
            ;; [logseq.graph-parser.util :as gp-util]
            [frontend.ui :as ui]
            ;; [frontend.handler.ui :as ui-handler]
            [frontend.util :as util]
            [frontend.util.text :as text-util]
            ;; [logseq.graph-parser.text :as text]
            ;; [goog.object :as gobj]
            ;; [reitit.frontend.easy :as rfe]
            [frontend.handler.route :as route-handler]
            [frontend.handler.notification :as notification]
            [rum.core :as rum]))

(defn get-year [date]
  (.getFullYear date))

(defn get-month [date]
  (inc (.getMonth date))) ; 0から始まるため1を加える

(rum/defc blocks-cp < rum/reactive db-mixins/query
  {}
  [repo page-entity]
  ;; (when-let [page-e (db/pull [:block/name (util/page-name-sanity-lc page)])]
  (page/page-blocks-cp repo page-entity {}))

(defn color-weekends
  [ts]
  (str (case (date/ts->day-of-week-number ts)
         0 "2px solid orange"
         1 "unset"
         2 "unset"
         3 "unset"
         4 "unset"
         5 "unset"
         6 "1px solid skyblue")))

(defn localDateString
  [ts year month]
  (cond
    (true? year)
    (.toLocaleDateString (js/Date. ts) (or (state/sub :preferred-language) "default")
                         (clj->js {:year "numeric" :month "long" :day "numeric" :weekday "short"}))
    (true? month)
    (str (.toLocaleDateString (js/Date. ts) (or (state/sub :preferred-language) "default")
                              (clj->js {:month "long" :day "numeric"})) " " (.toLocaleDateString (js/Date. ts) (or (state/sub :preferred-language) "default")
                                                                                                 (clj->js {:weekday "short"})))
    (not (or (true? year) (true? month)))
    (str (.toLocaleDateString (js/Date. ts) (or (state/sub :preferred-language) "default")
                              (clj->js {:day "numeric"})) " " (.toLocaleDateString (js/Date. ts) (or (state/sub :preferred-language) "default")
                                                                                   (clj->js {:weekday "short"})))))

(defn copy-button-on-click
  [e title]
  (util/stop e)
  (util/copy-to-clipboard! (str "[[" title "]]"))
  (notification/show! (t :notification/copied-to-clipboard) :success))

(defn journal-title
  ([page-entity lower-case-page-name title year month]
   (let [ts (date/journalDay->ts (:block/journal-day page-entity))]
     [:div.page-title-bar.flex.flex-1.p-4
      {:style {:align-items "center"
               :justify-content "space-between"}
       :on-mouse-down (fn [e]
                        (when (util/right-click? e)
                          (state/set-state! :page-title/context {:page lower-case-page-name})))}

      [:h1.journal-title.my-2.mb-3.cursor-pointer
       {:title (t :journals/click-to)
        :on-click (fn [e]
                    (if (util/shift-key? e)
                      [(when page-entity
                         (state/sidebar-add-block!
                          (state/get-current-repo)
                          (:db/id page-entity)
                          :page))
                       (.preventDefault e)]
                      (route-handler/redirect-to-page! lower-case-page-name)))}
       [(ui/icon "calendar-time" {:size 26
                                  :style {:margin-right "10px"}})
        [:span
         {:style {:font-size "2.2em"
                  :border-bottom (color-weekends ts)}}
         (localDateString ts year month)]]]


      [:span.journal-title-right-area.text-sm
       {:style {:cursor "copy" :opacity "0.6"}
        :title (str (t :journals/user-date-format-desc-title) "\n" (t :journals/user-date-format-desc) " [[" (state/get-date-formatter) "]]")
        :on-click (fn [e]
                    (copy-button-on-click e ts))}
       (str " [[" title "]]")]]))

  ([ts year month]
   [:div.page-title-bar.flex.flex-1.p-4
    {:style {:align-items "center"
             :justify-content "space-between"}}

    [:h1.journal-title.my-2.mb-3.cursor-pointer
     {:title (t :journals/click-to)}
     [(ui/icon "calendar-time" {:size 26
                                :style {:margin-right "10px"}})
      [:span
       {:style {:font-size "2.2em"
                :border-bottom (color-weekends ts)}}
       (localDateString ts year month)]]]]))


(rum/defc journal-cp < rum/reactive

  ([repo title]
   (let [lower-case-page-name (string/lower-case title)
         page-entity (db/pull [:block/name lower-case-page-name])]
     (journal-cp repo title page-entity true true false)))

  ([repo title page-entity year month weeklyJournal?]
   (let [;; Don't edit the journal title
         lower-case-page-name (string/lower-case title)
        ;;  exists? (db/page-exists? lower-case-page-name)
         today? (= lower-case-page-name
                   (string/lower-case (date/journal-name))) ;; 今日のジャーナルかどうか

         data-page-tags (when (seq (:block/tags page-entity))
                          (let [page-names (model/get-page-names-by-ids (map :db/id (:block/tags lower-case-page-name)))]
                            (text-util/build-data-value page-names)))]
     [:div.flex-1.journal.page (cond-> {}
                                 data-page-tags
                                 (assoc :data-page-tags data-page-tags))

     ;;TODO: 日付ナビゲーション設置

      (ui/foldable
       (journal-title page-entity lower-case-page-name title year month)

       [(if today? ;; 今日のジャーナルの場合はlazy-visibleにしない
          (blocks-cp repo page-entity)
          (ui/lazy-visible
           (fn [] (blocks-cp repo page-entity))))
         ;;{:debug-id (str "journal-blocks " page)}
        (when-not (and (true? weeklyJournal?)
                       (false? (or (state/sub :journal/weeklyJournalHideLinkedReferences?) false)))
          [:div.mt-20]
          (rum/with-key
            (reference/references title)
            (str title "-refs")))]
       {:default-collapsed? false})])))



(rum/defc journals < rum/reactive
  [latest-journals]
  (let [repo (state/sub :git/current-repo)]
    [:div#journals
     (ui/infinite-list
      "main-content-container"
      [(for [page-entity latest-journals]
         (let [{:block/keys [name]} page-entity]
           [:div.journal-item.content {:key name}

            (journal-cp repo name page-entity true true false)]))]
      {:has-more (page-handler/has-more-journals?)
       :more-class "text-l"
       :on-load (fn []
                  (page-handler/load-more-journals!))})]))

(rum/defc all-journals < rum/reactive db-mixins/query
  []
  (let [journals-length (state/sub :journals-length)
        latest-journals (db/get-latest-journals (state/get-current-repo) journals-length)]
    (journals latest-journals)))


(rum/defc weeklyJournal < rum/reactive
  ([]
   (weeklyJournal (or (state/sub :journal/weeklyJournalView) "thisWeek")))
  ([start-of-week?]
   (let [repo (state/get-current-repo)
         start-of-week? (state/sub :journal/weeklyJournalView)
         today (js/Date.)
         day-of-week (+ (.getDay today) (or (state/get-start-of-week) 0))
         start-of-this-week (doto (js/Date. today) (.setDate (- (.getDate today) day-of-week)))
         start-of-prev-week (cond
                              (= start-of-week? "prev")
                              (doto (js/Date. start-of-this-week) (.setDate (- (.getDate start-of-this-week) 7)))
                              (= start-of-week? "next")
                              (doto (js/Date. start-of-this-week) (.setDate (+ (.getDate start-of-this-week) 7)))
                              :else
                              start-of-this-week)
         end-of-next-week (cond
                            (= start-of-week? "prev")
                            start-of-this-week
                            (= start-of-week? "next")
                            (doto (js/Date. start-of-this-week) (.setDate (+ (.getDate start-of-this-week) 14)))
                            :else
                            (doto (js/Date. start-of-this-week) (.setDate (+ (.getDate start-of-this-week) 7))))
         dates (for [d (range (.getTime start-of-prev-week) (.getTime end-of-next-week) (* 24 60 60 1000))]
                 (js/Date. d))]
     [:div
      [:div.flex
       [:span.mr-4
        (t :weeklyJournal)]
       [:select.form-select.mr-4
        {:style {:width "150px"}
         :on-change (fn [e]
                      (state/weeklyJournalView! (.. e -target -value)))}
        [:option {:value "thisWeek"} "This Week"]
        [:option {:value "prev"} "Previous Week"]
        [:option {:value "next"} "Next Week"]]
       [:details
        [:summary
         (ui/icon "settings")]
        [(str "Hide Linked References")
         [:input {:type "checkbox"
                  :on-change (fn [e]
                               (state/weeklyJournalHideLinkedReferences! (.. e -target -checked)))}]]]]

      [:div#weeklyJournal
       (let [prev-date (atom nil)]
         (for [date dates]
           (let [current-date date
                 prev @prev-date
                 same-year? (and prev
                                 (= (get-year current-date) (get-year prev)))
                 same-month? (and prev
                                  (= (get-month current-date) (get-month prev)))]
             (reset! prev-date current-date)
             (when-let [ts (date/journalDay->ts (db-util/date->int date))]
               (when-let [journal-page-name (date/journal-name ts)]
                 (if-let [page-entity (db/entity [:block/name journal-page-name])]

                   (when-let [{:block/keys [name]} page-entity]
                     [:div.weekly-journal-item.content
                      {:key name}
                      (journal-cp repo name page-entity (not same-year?) (not same-month?) true)])

                   [:div.weekly-journal-item.content
                    {:key name}
                    (journal-title ts (not same-year?) (not same-month?))
                    (page/page {:parameters {:path {:name journal-page-name}}
                                :sidebar?   true
                                :repo       repo})]))))))]])))

