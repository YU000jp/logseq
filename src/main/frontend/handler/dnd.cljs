(ns frontend.handler.dnd
  "Provides fns for drag n drop"
  (:require [frontend.db.model :as db-model]
            [frontend.handler.editor :as editor-handler]
            [frontend.modules.outliner.core :as outliner-core]
            [frontend.modules.outliner.tree :as tree]
            [frontend.modules.outliner.transaction :as outliner-tx]
            [logseq.graph-parser.util.block-ref :as block-ref]
            [frontend.state :as state]))

(defn move-blocks
  [^js event blocks target-block move-to]
  (let [blocks' (map #(dissoc % :block/level :block/children) blocks)
        first-block (first blocks')
        top? (= move-to :top)
        nested? (= move-to :nested)
        alt-key? (and event (.-altKey event))
        current-format (:block/format first-block)
        target-format (:block/format target-block)
        first-block-page (:db/id (:block/page first-block))
        target-page (or (:db/id (:block/page target-block))
                        (:db/id target-block))]
    (cond
      ;; alt pressed, make a block-ref
      (and alt-key? (= (count blocks) 1))
      (do
        (editor-handler/set-block-property! (:block/uuid first-block)
                                            :id
                                            (str (:block/uuid first-block)))
        (editor-handler/api-insert-new-block!
         (block-ref/->block-ref (:block/uuid first-block))
         {:block-uuid (:block/uuid target-block)
          :sibling? (not nested?)
          :before? top?}))

      ;; format mismatch
      (and current-format target-format (not= current-format target-format))
      (state/pub-event! [:notification/show
                         {:content [:div "Those two pages have different formats."]
                          :status :warning
                          :clear? true}])


      (every? map? (conj blocks target-block))
      (if (= first-block-page target-page)
        (let [target-node (outliner-core/block target-block)]
          (outliner-tx/transact!
           {:outliner-op :move-blocks}
           (editor-handler/save-current-block!)
           (if top?
             (let [first-child?
                   (= (tree/-get-parent-id target-node)
                      (tree/-get-left-id target-node))]
               (if first-child?
                 (let [parent (tree/-get-parent target-node)]
                   (outliner-core/move-blocks! blocks (:data parent) false))
                 (let [before-node (tree/-get-left target-node)]
                   (outliner-core/move-blocks! blocks (:data before-node) true))))
             (outliner-core/move-blocks! blocks target-block (not nested?)))))
        (editor-handler/paste-blocks
         (mapcat #(db-model/get-block-and-children (state/get-current-repo) (:block/uuid %)) blocks)
         {:target-block target-block
          :sibling? (not nested?)}))

      :else
      nil)))
