(ns frontend.components.header
  (:require [cljs-bean.core :as bean]
            [frontend.components.export :as export]
            [frontend.components.page-menu :as page-menu]
            [frontend.components.scheduled-deadlines :as scheduled]
            ;; [frontend.handler.page :as page-handler]
            [frontend.components.plugins :as plugins]
            [frontend.components.server :as server]
            [frontend.components.right-sidebar :as sidebar]
            [frontend.components.svg :as svg]
            [frontend.config :as config]
            [frontend.date :as date]
            [frontend.context.i18n :refer [t]]
            [frontend.handler :as handler]
            [frontend.handler.file-sync :as file-sync-handler]
            [frontend.components.file-sync :as fs-sync]
            [frontend.components.repo :as repo]
            ;;[frontend.handler.plugin :as plugin-handler]
            [frontend.handler.route :as route-handler]
            [frontend.handler.user :as user-handler]
            [frontend.handler.web.nfs :as nfs]
            [frontend.mobile.util :as mobile-util]
            [frontend.state :as state]
            [frontend.ui :as ui]
            [frontend.util :as util]
            [frontend.version :refer [version]]
            [reitit.frontend.easy :as rfe]
            [rum.core :as rum]
            [clojure.string :as string]
            ;; [frontend.components.journal :as journal]
            ))

(rum/defc home-button
  < {:key-fn #(identity "home-button")}
  []
  (ui/with-shortcut :go/home "left"
    [:button.button.icon.inline.mx-1
     {:title (t :home)
      :on-click (fn [e]
                  (when (mobile-util/native-iphone?)
                    (state/set-left-sidebar-open! false))
                  (cond
                    (util/shift-key? e) (route-handler/sidebar-journals!)
                    (util/alt-key? e) (route-handler/redirect-to-page! (date/today))
                    :else
                    (route-handler/redirect-to-home!)))}
     (ui/icon "home" {:size ui/icon-size})]))

(rum/defc login < rum/reactive
  < {:key-fn #(identity "login-button")}
  []
  (let [_ (state/sub :auth/id-token)
        loading? (state/sub [:ui/loading? :login])
        sync-enabled? (file-sync-handler/enable-sync?)
        logged? (user-handler/logged-in?)]
    (when-not (or config/publishing?
                  logged?
                  (not sync-enabled?))
      [:span.flex.space-x-2
       [:a.button.text-sm.font-medium.block.text-gray-11
        {:on-click #(state/pub-event! [:user/login])}
        [:span (t :login)]
        (when loading?
          [:span.ml-2 (ui/loading "")])]])))

(rum/defc left-menu-button < rum/reactive
  < {:key-fn #(identity "left-menu-toggle-button")}
  [{:keys [on-click]}]
  (ui/with-shortcut :ui/toggle-left-sidebar "bottom"
    [:button.#left-menu.cp__header-left-menu.button.icon
     {:style {:cursor "col-resize"}
      :title (t :header/toggle-left-sidebar)
      :on-click on-click}
     (ui/icon "align-left" {:size 20})]))

(defn bug-report-url []
  (let [ua (.-userAgent js/navigator)
        safe-ua (string/replace ua #"[^_/a-zA-Z0-9\.\(\)]+" " ")
        platform (str "App Version: " version "\n"
                      "Git Revision: " config/REVISION "\n"
                      "Platform: " safe-ua "\n"
                      "Language: " (.-language js/navigator) "\n"
                      "Plugins: " (string/join ", " (map (fn [[k v]]
                                                           (str (name k) " (" (:version v) ")"))
                                                         (:plugin/installed-plugins @state/state))))]
    (str "https://github.com/logseq/logseq/issues/new?"
         "title=&"
         "template=bug_report.yaml&"
         "labels=from:in-app&"
         "platform="
         (js/encodeURIComponent platform))))

(rum/defc dropdown-menu < rum/reactive
  < {:key-fn #(identity "repos-dropdown-menu")}
  [{:keys [current-repo t]}]
  (let [page-menu (page-menu/page-menu nil)
        page-menu-and-hr (when (seq page-menu)
                           (concat page-menu [{:hr true}]))
        login? (and (state/sub :auth/id-token) (user-handler/logged-in?))]
    (ui/dropdown-with-links
     (fn [{:keys [toggle-fn]}]
       [:button.button.icon.toolbar-dots-btn
        {:on-click toggle-fn
         :title (t :header/more)}
        (ui/icon "dots" {:size ui/icon-size})])
     (->>
      [(when (state/enable-editing?)
         {:title (t :settings)
          :options {:on-click state/open-settings!}
          :icon (ui/icon "settings")})

       (when current-repo
         {:title (t :export-graph)
          :options {:on-click #(state/set-modal! export/export)}
          :icon (ui/icon "database-export")})

       (when (and current-repo (state/enable-editing?))
         {:title (t :import)
          :options {:href (rfe/href :import)}
          :icon (ui/icon "file-upload")})

       {:class "graph-view-nav"
        :title (t :right-side-bar/graph-view)
        :options {:href (rfe/href :graph)}
        :icon (ui/icon "network")}

       {:class "all-pages"
        :title (t :right-side-bar/all-pages)
        :options {:href (rfe/href :all-pages)}
        :icon (ui/icon "book")}

       (when current-repo
         {:class "all--files"
          :title (t :right-side-bar/all-files)
          :options {:href (rfe/href :all-files)}
          :icon (ui/icon "files")})

       (when (and config/dev? (state/sub [:ui/developer-mode?]))
         {:class "ui"
          :title "Dev: UI"
          :options {:href (rfe/href :ui)}
          :icon (ui/icon "plug")})

      ;;  {:title (t :help/shortcuts)
      ;;     :options {:on-click #(state/sidebar-add-block! (state/get-current-repo) "shortcut-settings" :shortcut-settings)} ;; :on-click #(state/pub-event! [:modal/keymap])
      ;;     :icon (ui/icon "keyboard")}

      ;;  (when-not config/publishing?
      ;;    {:title [:div.flex-row.flex.justify-between.items-center
      ;;             [:span (t :join-community)]]
      ;;     :options {:href "https://discuss.logseq.com"
      ;;               :title (t :discourse-title)
      ;;               :target "_blank"}
      ;;     :icon (ui/icon "brand-discord")})

      ;;  (when-not config/publishing?
      ;;    {:title [:div.flex-row.flex.justify-between.items-center
      ;;             [:span (t :help/bug)]]
      ;;     :options {:href (rfe/href :bug-report)}
      ;;     :icon (ui/icon "bug")})     

      ;;  {:title (t :handbook/title)
      ;;   :options {:on-click #(state/toggle-help!)}
      ;;   :icon (ui/icon "bulb")}

       (when login? {:hr true})
       (when login?
         {:item [:span.flex.flex-col.relative.group.pt-1
                 [:b.leading-none (user-handler/username)]
                 [:small.opacity-70 (user-handler/email)]
                 [:i.absolute.opacity-0.group-hover:opacity-100.text-red-rx-09
                  {:class "right-1 top-3" :title (t :logout)}
                  (ui/icon "logout")]]
          :options {:on-click #(user-handler/logout)}})]
      (concat page-menu-and-hr)
      (remove nil?))
     {})))

(rum/defc back-and-forward
  < {:key-fn #(identity "nav-history-buttons")}
  []
  [:div.flex.flex-row.mr-4.ml-4
   (ui/with-shortcut :go/backward "bottom"
     [:button.it.navigation.nav-left.button.icon
      {:title (t :header/go-back) :on-click #(js/window.history.back)}
      (ui/icon "arrow-left" {:size ui/icon-size})])
   (ui/with-shortcut :go/forward "bottom"
     [:button.it.navigation.nav-right.button.icon
      {:title (t :header/go-forward) :on-click #(js/window.history.forward)}
      (ui/icon "arrow-right" {:size ui/icon-size})])])

(rum/defc updater-tips-new-version
  [t]
  (let [[downloaded, set-downloaded] (rum/use-state nil)
        _ (rum/use-effect!
           (fn []
             (when-let [channel (and (util/electron?) "auto-updater-downloaded")]
               (let [callback (fn [_ args]
                                (js/console.debug "[new-version downloaded] args:" args)
                                (let [args (bean/->clj args)]
                                  (set-downloaded args)
                                  (state/set-state! :electron/auto-updater-downloaded args))
                                nil)]
                 (js/apis.addListener channel callback)
                 #(js/apis.removeListener channel callback))))
           [])]

    (when downloaded
      [:div.cp__header-tips
       [:p (t :updater/new-version-install)
        [:a.restart.ml-2
         {:on-click #(handler/quit-and-install-new-version!)}
         (svg/reload 16) [:strong (t :updater/quit-and-install)]]]])))

(rum/defc ^:large-vars/cleanup-todo header < rum/reactive
  [{:keys [open-fn current-repo default-home new-block-mode]}]
  (let [repos (->> (state/sub [:me :repos])
                   (remove #(= (:url %) config/local-repo)))
        _ (state/sub [:user/info :UserGroups])
        electron-mac? (and util/mac? (util/electron?))
        show-open-folder? (and (nfs/supported?)
                               (or (empty? repos)
                                   (nil? (state/sub :git/current-repo)))
                               (not (mobile-util/native-platform?))
                               (not config/publishing?))
        left-menu (left-menu-button {:on-click (fn []
                                                 (open-fn)
                                                 (state/set-left-sidebar-open!
                                                  (not (:ui/left-sidebar-open? @state/state))))})
        custom-home-page? (and (state/custom-home-page?)
                               (= (state/sub-default-home-page) (state/get-current-page)))
        ;; sync-enabled? (file-sync-handler/enable-sync?)
        current-page (or (state/get-current-page) (state/get-current-whiteboard))]
    [:div.cp__header.drag-region#head
     {:class           (util/classnames [{:electron-mac   electron-mac?
                                          :native-ios     (mobile-util/native-ios?)
                                          :native-android (mobile-util/native-android?)}])
      :on-double-click (fn [^js e]
                         (when-let [target (.-target e)]
                           (cond
                             (and (util/electron?)
                                  (.. target -classList (contains "drag-region")))
                             (js/window.apis.toggleMaxOrMinActiveWindow)

                             (mobile-util/native-platform?)
                             (util/scroll-to-top true))))}
     [:div.l.flex.drag-region
      [left-menu
       [:div.text-md.ml-2
        (repo/repos-dropdown)]
       (when (mobile-util/native-platform?)
         ;; back button for mobile
         (when-not (or (state/home?) custom-home-page? (state/whiteboard-dashboard?))
           (ui/with-shortcut :go/backward "bottom"
             [:button.it.navigation.nav-left.button.icon.opacity-70
              {:title (t :header/go-back) :on-click #(js/window.history.back)}
              (ui/icon "chevron-left" {:size 26})])))]]


     [:div.r.flex.drag-region
      (when (and current-repo
                 (not (config/demo-graph? current-repo)) ;; デモグラフの場合を除く
                 (user-handler/alpha-or-beta-user?))
        (fs-sync/indicator)
        [:div.text-sm.mr-4
         [:button.button.icon
          {:title (t :right-side-bar/scheduled-and-deadline)
           :on-click (fn []
                       (state/sidebar-add-block! current-repo "scheduled-and-deadline" :scheduled-and-deadline))}
          (scheduled/scheduled-and-deadlines-for-toolbar-tip (date/today))]])


      (when (and current-page current-repo)
        [:div.flex.items-center.space-x-2.mr-4.rounded-md
         {:style {:background-color "var(--lx-gray-04, var(--color-level-3, var(--rx-gray-04)))"}}
         [;; ページ用メニュー

          ;; Linked Referencesを表示する
          [:div.text-sm
           [:button.button.icon
            {:on-click (fn []
                         (state/sidebar-add-block! current-repo current-page :reference))
             :title (t :linked-references/sidebar-open)}
            (ui/icon "layers-difference" {:class "icon" :size 24})]]

          ;; Unlinked Referencesを表示する
          [:div.text-sm
           [:button.button.icon
            {:on-click (fn []
                         (state/sidebar-add-block! current-repo current-page :unlinked-reference))
             :title (t :unlinked-references/sidebar-open)}
            (ui/icon "list" {:class "icon" :size 24})]]


          ;; Page headers list
          [:div.text-sm
           [:button.button.icon
            {:on-click (fn []
                         (state/sidebar-add-block! current-repo "headers-list" :headers-list))
             :title (t :right-side-bar/page-headers-list)}
            (ui/icon "pennant" {:class "icon" :size 24})]]


         ;; ページのグラフを表示する
          [:div.text-sm
           [:button.button.icon
            {:on-click (fn []
                         (state/sidebar-add-block!
                          current-repo
                          "page-graph"
                          :page-graph))
             :title (t :right-side-bar/page-graph)}
            (ui/icon "hierarchy" {:class "icon" :size 24})]]

        ;; 削除ボタン
          (when-not (or (= current-page "contents")
                        config/publishing?)
            [:div.text-sm
             [:button.button.icon
              {:on-click (fn []
                           (state/set-modal! (page-menu/delete-page-dialog current-page)))
               :title (t :page/delete)}
              (ui/icon "trash-x" {:class "icon" :size 20 :color "red"})]])]])

      ;; (when (and (not= (state/get-current-route) :home)
      ;;            (not custom-home-page?))
      ;;   (home-button))

      ;; (when sync-enabled?
      ;;   (login))


      ;; search button for non-mobile
      (when current-repo
        (ui/with-shortcut :go/search "right"
          [:button.button.icon#search-button
           {:title (t :header/search)
            :on-click (fn [e]
                        (when (or (mobile-util/native-android?)
                                  (mobile-util/native-iphone?))
                          (state/set-left-sidebar-open! false))
                        (if (util/shift-key? e)
                          (let [repo (state/get-current-repo)]
                            (state/close-modal!)
                            ;; サイドバーで検索を開く
                            (state/sidebar-add-block! repo "" :search))
                          (state/pub-event! [:go/search])))}
           (ui/icon "search" {:size ui/icon-size})]))

      (when (util/electron?)
        (back-and-forward))
      (when config/lsp-enabled?
        [:<>
         (plugins/hook-ui-items :toolbar)
         (plugins/updates-notifications)])

      (when (state/feature-http-server-enabled?)
        (server/server-indicator (state/sub :electron/server)))

      (when-not (mobile-util/native-platform?)
        (new-block-mode))

      (when show-open-folder?
        [:a.text-sm.font-medium.button.icon.add-graph-btn.flex.items-center
         {:on-click #(route-handler/redirect! {:to :repo-add})}
         (ui/icon "folder-plus")
         (when-not config/mobile?
           [:span.ml-1 {:style {:margin-top (if electron-mac? 0 2)}}
            (t :on-boarding/add-graph)])])

      (when config/publishing?
        [:a.text-sm.font-medium.button {:href (rfe/href :graph)}
         (t :graph)])

      (dropdown-menu {:t            t
                      :current-repo current-repo
                      :default-home default-home})

      (sidebar/toggle)

      (updater-tips-new-version t)]]))
