(ns frontend.handler.recent
  "Fns related to recent pages feature"
  (:require [frontend.db :as db]
            [frontend.db.model :as model]
            [frontend.state :as state]
            [frontend.util :as util]
            ;; [frontend.date :as date]
            ))

(defn add-page-to-recent!
  [repo page-name-or-block-uuid click-from-recent?]
  (when-not click-from-recent?
    (let [pages (or (db/get-key-value repo :recent/pages) '())
          page-name (if (uuid? page-name-or-block-uuid)
                      (let [block (model/get-block-by-uuid page-name-or-block-uuid)]
                        (get-in block [:block/page :block/original-name]))
                      page-name-or-block-uuid)
          favorited? (contains? (set (map util/page-name-sanity-lc (:favorites (state/sub-config))))
                                page-name)
        ;; journal? (date/valid-journal-title? page-name) ;TODO: ジャーナルを追加するかどうかの設定項目を追加する
          ]
      (when (and
             (not ((set pages) page-name))
             (not favorited?)
          ;;  (not journal?)
);;fix: ブックマークに含まれるものは履歴に入れない
        (let [new-pages (take 30 (distinct (cons page-name pages)))]
          (db/set-key-value repo :recent/pages new-pages))))))

(defn update-or-add-renamed-page
  [repo old-page-name new-page-name]
  (let [pages (or (db/get-key-value repo :recent/pages)
                  '())
        updated-pages (replace {old-page-name new-page-name} pages)
        updated-pages* (if (contains? (set updated-pages) new-page-name)
                         updated-pages
                         (cons new-page-name updated-pages))]
    (db/set-key-value repo :recent/pages updated-pages*)))
