(ns merkledag.viz
  "Utilities for visualizing merkledag data webs."
  (:require
    [merkledag.core :as merkle]
    [multihash.core :as multihash]
    [rhizome.viz :as rhizome]))


(defn visualize-nodes
  "Constructs a graph visualizing the given collection of nodes."
  [nodes]
  (let [node-map (->> nodes (filter :id) (map (juxt :id identity)) (into {}))]
    (rhizome/view-graph
      (vals node-map)
      (fn adjacent [node]
        (keep (comp node-map :target) (:links node)))
      :node->descriptor
        (fn [node]
          {:label (let [short-hash (some-> (multihash/base58 (:id node)) (subs 0 8))]
                    (if-let [data (:data node)]
                      (remove nil? [short-hash (:data/type data) (:title data)])
                      short-hash))})
      :edge->descriptor
        (fn [from to]
          (let [lname (some #(when (= (:id to) (:target %)) (:name %)) (:links from))]
            {:label lname})))))


; TODO: for showing an update, collect all new nodes to be added, then do a
; breadth-first search for all nodes within n links of the nodes for context.
; When rendering, any new node or link from a new node should be colored or
; dashed.
;
; (-> (make-graph graph)
;     (add-nodes root-1)
;     (add-nodes root-2 {:color :red})
;     (update-line root-1 root-2))