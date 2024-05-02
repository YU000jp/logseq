(ns frontend.handler.recent
  "Fns related to recent pages feature"
  (:require [frontend.db :as db]
            [frontend.db.model :as model]
            [frontend.state :as state]
            [frontend.util :as util]))

(defn add-page-to-recent!
  [repo page-name-or-block-uuid click-from-recent?]
  (let [pages (or (db/get-key-value repo :recent/pages)
                  '())
        page-name (if (uuid? page-name-or-block-uuid)
                    (when-let [block (model/get-block-by-uuid page-name-or-block-uuid)]
                      (get-in block [:block/page :block/original-name]))
                    page-name-or-block-uuid)
        favorites (:favorites (state/sub-config))
        favorited? (contains? (set (map util/page-name-sanity-lc favorites))
                              page-name)] 
    (when (and (not click-from-recent?) (not ((set pages) page-name)) (not favorited?));;fix: ブックマークに含まれるものは履歴に入れない
      (let [new-pages (take 20 (distinct (cons page-name pages)))]
        (db/set-key-value repo :recent/pages new-pages)))))

(defn update-or-add-renamed-page [repo old-page-name new-page-name]
  (let [pages (or (db/get-key-value repo :recent/pages)
                  '())
        updated-pages (replace {old-page-name new-page-name} pages)
        updated-pages* (if (contains? (set updated-pages) new-page-name)
                         updated-pages
                         (cons new-page-name updated-pages))]
    (db/set-key-value repo :recent/pages updated-pages*)))
