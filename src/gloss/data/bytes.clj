;;   Copyright (c) Zachary Tellman. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns gloss.data.bytes
  (:use
    [potemkin]
    [gloss.core formats protocols])
  (:require
    [gloss.data.bytes.delimited :as delimited]
    [gloss.data.bytes.core :as core]))


(import-fn core/byte-count)
(import-fn core/take-bytes)
(import-fn core/drop-bytes)
(import-fn core/take-contiguous-bytes)
(import-fn core/rewind-bytes)
(import-fn core/dup-bytes)

(import-fn delimited/delimited-block)
(import-fn delimited/wrap-delimited-sequence)

(defn finite-byte-codec
  [len]
  (reify
    Reader
    (read-bytes [this b]
      (if (< (byte-count b) len)
	[false this b]
	[true (take-bytes len b) (drop-bytes len b)]))
    Writer
    (sizeof [_]
      len)
    (write-bytes [_ _ v]
      v)))

(defn finite-block
  [prefix-codec]
  (assert (sizeof prefix-codec))
  (let [read-codec (compose-readers
		     prefix-codec
		     (fn [len b]
		       (read-bytes (finite-byte-codec len) b)))]
    (reify
      Reader
      (read-bytes [_ b]
	(read-bytes read-codec b))
      Writer
      (sizeof [_]
	nil)
      (write-bytes [_ buf v]
	(concat
	  (with-buffer [buf (sizeof prefix-codec)]
	    (write-bytes prefix-codec buf v))
	  v)))))

 
