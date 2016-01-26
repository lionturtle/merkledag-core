(ns merkledag.format
  "Serialization protocol for encoding the links and data of a node into a block
  and later decoding back into data."
  (:require
    [blocks.core :as block]
    (merkledag.codecs
      [bin :refer [bin-codec]]
      [node :refer [node-codec]])
    [merkledag.link :as link]
    [multicodec.core :as codec]
    (multicodec.codecs
      [filter :refer [filter-codec]]
      [mux :as mux :refer [mux-codec]]
      [text :refer [text-codec]])
    [multihash.core :as multihash])
  (:import
    java.io.PushbackInputStream
    merkledag.link.MerkleLink
    multihash.core.Multihash))


;; ## Type Handlers

; TODO: implement type plugin system
; Should load namespaces under merkledag.data:
; - merkledag.data.time
; - merkledag.data.bytes
; - merkledag.data.units
; ...


(def core-types
  ; TODO: is data/hash necessary? Multihashes shouldn't show up in data segments...
  {'data/hash
   {:description "Content-addressed multihash references"
    :reader multihash/decode
    :writers {Multihash multihash/base58}}

   'data/link
   {:description "Merkle links within an object"
    :reader link/read-link
    :writers {MerkleLink :name}}}) ; TODO: replace this with indexing?



;; ## Format Functions

(defn parse-block
  "Attempts to parse the contents of a block with the given codec. Returns an
  updated version of the block with additional keys set. At a minimum, this
  will add an `:encoding` key with the detected codec, or `nil` for raw blocks.

  The dispatched codec should return a map of attributes to merge into the
  block; typically including a `:data` field with the decoded block value. Node
  codecs should also return a `:links` vector."
  [codec block]
  (when block
    (->>
      (with-open [content (PushbackInputStream. (block/open block))]
        (let [first-byte (.read content)]
          (if (<= 0 first-byte 127)
            ; Possible multicodec header.
            (do (.unread content first-byte)
                (codec/decode! codec content))
            ; Unknown encoding.
            {:encoding nil})))
      (into block))))


(defn format-block
  "Serializes the given data value into a block using the given codec. Returns
  a block containing both the formatted content, an `:encoding` key for the
  actual codec used (if any), and additional data merged in if the value was a
  map."
  [codec data]
  (when data
    (let [content (codec/encode codec data)
          block (block/read! content)
          decoded (codec/decode codec content)]
      (when-not (if (map? data)
                  (and (= (:links data) (:links decoded))
                       (= (:data  data) (:data  decoded)))
                  (= (:data decoded) data))
        (throw (ex-info (str "Decoded data does not match input data " (class data))
                        {:input data
                         :decoded decoded})))
      (into block decoded))))



;; ## Codec Construction

(defn- lift-codec
  "Lifts a codec into a block format by wrapping the decoded value in a map with
  `:encoding` and `:data` entries."
  [codec]
  (filter-codec codec
    :decoding (fn wrap-data
                [data]
                {:encoding (:header codec)
                 :data data})))


(defn standard-format
  ([]
   (standard-format core-types))
  ([types]
   (mux-codec
     :bin  (lift-codec (bin-codec))
     :text (lift-codec (text-codec))
     :node (node-codec types))))
