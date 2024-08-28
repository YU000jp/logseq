(ns frontend.extensions.handbooks.core
  (:require [clojure.string :as string]
            [rum.core :as rum]
            [cljs.core.async :as async :refer [<! >!]]
            [frontend.ui :as ui]
            [frontend.date :as date]
            [frontend.state :as state]
            [frontend.db :as db]
            [frontend.components.page :as page]
            [frontend.components.right-sidebar :as right-sidebar]
            [frontend.components.reference :as reference]
            ;; [frontend.modules.shortcut.config :as shortcut-config]
            [frontend.util :as util]
            [frontend.components.scheduled-deadlines :as scheduled]
            [frontend.context.i18n :refer [t]]))

(defonce *config (atom {}))

(defn parse-parent-key
  [s]
  (if (and (string? s) (string/includes? s "/"))
    (subs s 0 (string/last-index-of s "/"))
    s))

(defn button-with-count
  [title icon key count pane-state nav-to-pane!]
  [:button.category-card.text-left
   {:key      key
    :style    {:border-left-color "var(--ls-secondary-background-color)"
               :opacity (if (= count 0) "0.3" "1")}
    :on-click (fn []
                (nav-to-pane! [key nil [title " " count]] pane-state))}
   [:div.icon-wrap
    (ui/icon  icon {:size 22})]
   [:div.text-wrap
    [:strong title] count]])


(defn button
  [title icon key pane-state nav-to-pane!]
  [:button.category-card.text-left
   {:key      key
    :style    {:border-left-color "var(--ls-secondary-background-color)"}
    :on-click (fn []
                (nav-to-pane! [key nil [title]] pane-state))}
   [:div.icon-wrap
    (ui/icon  icon {:size 22})]
   [:div.text-wrap
    [:strong title]]])

(defn pane-dashboard
  [handbooks-nodes pane-state nav-to-pane! current-page-name-or-uuid repo]
  (let [current-page-name-or-uuid (state/get-current-page)]
    [:div.pane.dashboard-pane
     [:div.categories-list
    ;; button

      (let [title (t :right-side-bar/scheduled-and-deadline)
            key :scheduled
            count (scheduled/scheduled-and-deadlines-return-count (date/today))
            icon "calendar-time"]
        (button-with-count title icon key count pane-state nav-to-pane!))

      (let [title (t :right-side-bar/repeat-tasks)
            key :repeat-tasks
            count (scheduled/repeat-tasks-return-count (date/today))
            icon "repeat"]
        (button-with-count title icon key count pane-state nav-to-pane!))

      (let [title (t :right-side-bar/contents)
            key :contents
            icon "note"]
        (button title icon key pane-state nav-to-pane!))

      (let [title (t :right-side-bar/default-queries)
            key :default-queries
            icon "brand-4chan"]
        (button title icon key pane-state nav-to-pane!))

      (when current-page-name-or-uuid
        (let [title (t :right-side-bar/page-headers-list)
              key :page-headers-list
              icon "pennant"]
          (button title icon key pane-state nav-to-pane!)))

      ;; (when current-page-name-or-uuid
      ;;   (let [title (t :linked-references/sidebar-open)
      ;;         key :linked-references
      ;;         icon "layers-difference"]
      ;;     (button title icon key pane-state nav-to-pane!)))

    ;; (let [title (t :right-side-bar/page-graph)
    ;;       key :page-graph
    ;;       icon "hierarchy"]
    ;;   (button title icon key pane-state nav-to-pane!))

    ;; end button
      ]]))

(defn pane-settings
  []
  [:div.pane.pane-settings
   [:div.item
    [:p.flex.items-center.space-x-3.mb-0
     [:strong "Config"]
     ;; ここにトグル
     ]
    [:small.opacity-30 (str "desc")]]])

(defn scheduled-and-deadlines
  [handbooks-nodes pane-state nav-to-pane! current-page-name-or-uuid repo]
  [:div.pane.task-board
   (ui/lazy-visible
    (fn [] (scheduled/scheduled-and-deadlines (date/today)))
    {:debug-id "scheduled-and-deadlines"})])

(defn repeat-tasks
  [handbooks-nodes pane-state nav-to-pane! current-page-name-or-uuid repo]
  [:div.pane.task-board
   (ui/lazy-visible
    (fn [] (scheduled/repeat-tasks (date/today)))
    {:debug-id "repeat-tasks"})])

(defn contents-embed
  [handbooks-nodes pane-state nav-to-pane! current-page-name-or-uuid repo]
  [:div.pane.contents-embed
   (when-let [contents (db/entity [:block/name "contents"])]
     (page/contents-page contents))])

(defn default-queries
  [handbooks-nodes pane-state nav-to-pane! current-page-name-or-uuid repo]
  [:div.pane.default-queries
   (let [repo (state/sub :git/current-repo)]
     (page/today-queries repo))])

(defn page-headers-list
  [handbooks-nodes pane-state nav-to-pane! current-page-name-or-uuid repo]
  [:div.pane.page-headers-list
   (if current-page-name-or-uuid
     [:div.item-type-headers-list
      (right-sidebar/headers-list repo current-page-name-or-uuid)]
     [:div
      "No headers"])])

;; (defn linked-references
;;   [handbooks-nodes pane-state nav-to-pane! current-page-name-or-uuid repo]
;;   [:div.pane.linked-references
;;    (if current-page-name-or-uuid
;;      [:div {:key "page-references"}
;;       (reference/references (string/capitalize current-page-name-or-uuid) true true)]
;;      [:div
;;       "No references"])])

;; (defn page-graph
;;   [handbooks-nodes pane-state nav-to-pane! current-page-name-or-uuid repo]
;;   [:div.pane.page-graph
;;    {:title (t :right-side-bar/long-time)}
;;    (page/page-graph)])

(def panes-mapping
  {:dashboard    [pane-dashboard]
   :scheduled    [scheduled-and-deadlines]
   :repeat-tasks [repeat-tasks]
   :default-queries [default-queries]
   :page-headers-list [page-headers-list]
   :contents     [contents-embed]
  ;;  :linked-references  [linked-references]
  ;;  :page-graph   [page-graph]
   :settings     [pane-settings]})

(rum/defc ^:large-vars/data-var content
  []
  (let [[active-pane-state, set-active-pane-state!]
        (rum/use-state [:dashboard nil (t :handbook/title)])

        [handbooks-nodes]
        (rum/use-state nil)

        [history-state, set-history-state!]
        (rum/use-state ())

        repo (state/sub :git/current-repo)
        current-page-name-or-uuid (or (state/get-current-page) (state/get-current-whiteboard))
        active-pane-name (first active-pane-state)
        pane-render (first (get panes-mapping active-pane-name))
        pane-dashboard? (= :dashboard active-pane-name)
        nav-to-pane! (fn [next-state prev-state]
                       (let [next-key (:key (second next-state))
                             prev-key (:key (second prev-state))
                             in-chapters? (and prev-key next-key (string/includes? prev-key "/")
                                               (or (string/starts-with? next-key prev-key)
                                                   (apply = (map parse-parent-key [prev-key next-key]))))]
                         (when-not in-chapters?
                           (set-history-state!
                            (conj (sequence history-state) prev-state))))
                       (set-active-pane-state! next-state))

        [scrolled?, set-scrolled!] (rum/use-state false)
        on-scroll (rum/use-memo #(util/debounce 100 (fn [^js e] (set-scrolled! (not (< (.. e -target -scrollTop) 10))))) [])]

    ;; navigation sentry
    (rum/use-effect!
     (fn []
       (when (seq handbooks-nodes)
         (let [c (:handbook/route-chan @state/state)]
           (async/go-loop []
             (let [v (<! c)]
               (when (not= v :return)
                 (recur))))
           #(async/go (>! c :return)))))
     [handbooks-nodes])

    [:div.cp__handbooks-content
     {:class     (util/classnames [{:scrolled      scrolled?}])
      :on-scroll on-scroll}
     [:div.pane-wrap
      [:div.hd.flex.justify-between.select-none.draggable-handle
       [:h1.text-xl.flex.items-center.font-bold
        (if pane-dashboard?
          [:span "Handy Board"]
          [:button.active:opacity-80.flex.items-center.cursor-pointer
           {:on-click (fn [] (let [prev (first history-state)
                                   prev (cond-> prev
                                          (nil? (seq prev))
                                          [:dashboard])]
                               (set-active-pane-state! prev)
                               (set-history-state! (rest history-state))))}
           [:span.pr-2.flex.items-center (ui/icon "chevron-left")]
           (let [title (or (last active-pane-state) (t :handbook/title) "")]
             [:span.truncate.title {:title title} title])])]

       [:div.flex.items-center.space-x-3
        (when (state/developer-mode?)
          [:a.flex.items-center {:aria-label (t :handbook/settings)
                                 :tabIndex   "0"
                                 :on-click   #(nav-to-pane! [:settings nil "Settings"] active-pane-state)}
           (ui/icon "settings")])
        [:a.flex.items-center {:aria-label (t :handbook/close) :tabIndex "0" :on-click #(state/toggle! :ui/handbooks-open?)}
         (ui/icon "x")]]]


                 ;; entry pane
      (when pane-render
        (apply pane-render
               (case active-pane-name
                    ;; default inputs
                 [handbooks-nodes active-pane-state nav-to-pane! current-page-name-or-uuid repo])))]]))
