(ns frontend.components.journal
  (:require [clojure.string :as string]
            [frontend.components.page :as page]
            [frontend.components.reference :as reference]
            ;;[frontend.components.scheduled-deadlines :as scheduled]
            [frontend.date :as date]
            [frontend.db :as db]
            [frontend.db-mixins :as db-mixins]
            [frontend.db.model :as model]
            [frontend.handler.page :as page-handler]
            ;; [frontend.components.block :as block]
            [frontend.context.i18n :refer [t]]
            [frontend.state :as state]
            ;; [logseq.graph-parser.util :as gp-util]
            [frontend.ui :as ui]
            [frontend.util :as util]
            [frontend.util.text :as text-util]
            ;; [logseq.graph-parser.text :as text]
            ;; [goog.object :as gobj]
            ;; [reitit.frontend.easy :as rfe]
            [frontend.handler.route :as route-handler]
            [frontend.handler.notification :as notification]
            [rum.core :as rum]))

(rum/defc blocks-cp < rum/reactive db-mixins/query
  {}
  [repo page]
  (when-let [page-e (db/pull [:block/name (util/page-name-sanity-lc page)])]
    (page/page-blocks-cp repo page-e {})))

(rum/defc journal-cp < rum/reactive
  [title]
  (let [;; Don't edit the journal title
        lower-case-page-name (string/lower-case title)
        repo (state/sub :git/current-repo)
        today? (= lower-case-page-name
                  (string/lower-case (date/journal-name))) ;; ジャーナルかどうか
        page-entity (db/pull [:block/name lower-case-page-name])
        data-page-tags (when (seq (:block/tags page-entity))
                         (let [page-names (model/get-page-names-by-ids (map :db/id (:block/tags lower-case-page-name)))]
                           (text-util/build-data-value page-names)))]
    [:div.flex-1.journal.page (cond-> {}
                                data-page-tags
                                (assoc :data-page-tags data-page-tags))

     ;;TODO: 日付ナビゲーション設置

     (ui/foldable 
      (let [journal-day (:block/journal-day page-entity)
            ts (date/journalDay->ts journal-day)
            date (js/Date. ts)
            language (or (state/sub :preferred-language) "default")
            formatted-date (.toLocaleDateString date language
                                                (clj->js {:year "numeric" :month "long" :day "numeric" :weekday "short"}))]
        [:div.page-title-bar.flex.flex-1.p-4
         {:style {:align-items "center"
                  :justify-content "space-between"}
          :on-mouse-down (fn [e]
                           (when (util/right-click? e)
                             (state/set-state! :page-title/context {:page lower-case-page-name})))}

         (let [day-of-week-number (date/ts->day-of-week-number ts)]
         [:h1.journal-title.my-2.mb-3.cursor-pointer
          {:style {:font-size "2.2em"
         :border-bottom (str (case day-of-week-number
                               0 "2px solid orange"
                               1 "unset"
                               2 "unset"
                               3 "unset"
                               4 "unset"
                               5 "unset"
                               6 "1px solid skyblue"))}
           :title (t :journals/click-to)
           :on-click (fn [e]
                       (if (util/shift-key? e)
                         [(when page-entity
                            (state/sidebar-add-block!
                             (state/get-current-repo)
                             (:db/id page-entity)
                             :page))
                          (.preventDefault e)]
                         (route-handler/redirect-to-page! lower-case-page-name)))}
          formatted-date])


         [:span.journal-title-right-area.text-sm
          {:style {:cursor "copy"}
           :title (str (t :journals/user-date-format-desc-title) "\n" (t :journals/user-date-format-desc) " [[" (state/get-date-formatter) "]]")
           :on-click (fn [e]
                       (util/stop e)
                       (util/copy-to-clipboard! (str "[[" title "]]"))
                       (notification/show! (t :notification/copied-to-clipboard) :success))}
          (str " [[" title "]]")]])

      [(if today?
         (blocks-cp repo lower-case-page-name)
         (ui/lazy-visible
          (fn [] (blocks-cp repo lower-case-page-name))))
         ;;{:debug-id (str "journal-blocks " page)}
       [:div.mt-20]
       (rum/with-key
         (reference/references title)
         (str title "-refs"))]
      {:default-collapsed? false})])) ; ツールチップ用
       

     ;; TODO: 設定項目追加。ジャーナルで、今日以外は折りたたみ状態をデフォルトにする
     ;; (if today? false true)

    ;; サイドバーに移設したためコメントアウト
    ;; TODO: ジャーナルを開いたときに、サイドバーで呼び出すかどうかを選択できるようにする設定項目をつくる
    ;;  (page/today-queries repo today? false)
    ;;  (when today?
    ;;    (scheduled/scheduled-and-deadlines page))
     

(rum/defc journals < rum/reactive
  [latest-journals]
  [:div#journals
   (ui/infinite-list
    "main-content-container"
    [
    ;;  (when-let [repo (state/get-current-repo)]
    ;;    [(when-not (js/document.getElementById "open-sidebar-default-queries")
    ;;       (state/sidebar-add-block! repo "default-queries" :default-queries))
    ;;     (when-not (js/document.getElementById "open-sidebar-scheduled-and-deadline")
    ;;       (state/sidebar-add-block! repo "scheduled-and-deadline" :scheduled-and-deadline))])
     
     (for [{:block/keys [name]} latest-journals]
      [:div.journal-item.content {:key name}
       (journal-cp name)])]
    {:has-more (page-handler/has-more-journals?)
     :more-class "text-l"
     :on-load (fn []
                (page-handler/load-more-journals!))})])

(rum/defc all-journals < rum/reactive db-mixins/query
  []
  (let [journals-length (state/sub :journals-length)
        latest-journals (db/get-latest-journals (state/get-current-repo) journals-length)]
    (journals latest-journals)))
