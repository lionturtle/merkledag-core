(ns user
  (:require
    [blocks.core :as block]
    (blocks.store
      [file :refer [file-store]]
      [memory :refer [memory-store]])
    [byte-streams :as bytes]
    (clj-time
      [core :as time]
      [coerce :as coerce-time]
      [format :as format-time])
    [clojure.java.io :as io]
    [clojure.repl :refer :all]
    [clojure.string :as str]
    (merkledag
      [core :as merkle]
      [format :as format]
      [test-utils :refer [dprint-opts random-bytes]]
      [viz :as viz])
    [multicodec.core :as codec]
    [multihash.core :as multihash]
    [puget.printer :as puget]))


(try (require '[clojure.stacktrace :refer [print-cause-trace]]) (catch Exception e nil))
(try (require '[clojure.tools.namespace.repl :refer [refresh]]) (catch Exception e nil))


(defn dprint
  [value]
  (puget/pprint value dprint-opts))


#_
(defn init!
  []
  (graph/with-context graph
    (let [context-hash (multihash/decode "Qmb2TGZBNWDuWsJVxX7MBQjvtB3cUc4aFQqrST32iASnEh")
          node-1 (merkle/node
                   {:data/type :finance/posting
                    :uuid "foo-bar"})
          node-2 (merkle/node
                   {:data/type :finance/posting
                    :uuid "frobblenitz omnibus"})
          node-3 (merkle/node
                   [(merkle/link "@context" context-hash)]
                   {:data/type :finance/transaction
                    :uuid #uuid "31f7dd72-c7f7-4a15-a98b-0f9248d3aaa6"
                    :title "Gas Station"
                    :description "Bought a pack of gum."
                    :time (data/parse-inst "2013-10-08T00:00:00")
                    :entries [(merkle/link "posting-1" node-1)
                              (merkle/link "posting-2" node-2)]})]
      (def node-1 (merkle/put-node! graph node-1))
      (def node-2 (merkle/put-node! graph node-2))
      (def node-3 (merkle/put-node! graph node-3))))
  :init)
