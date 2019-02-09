(ns ipfs-website-deployer.logger
  (:require [clojure.pprint :refer [pprint]]))
;; log should be a atom that has the logs for all the builds
;; happening in ipfs-website-deploy

;; The structure should be something like:
;; {:owner {:repo {:sha {:build-num [log-items]}}}}

;; Do you need rebuilds? No, should not need, but in case, there
;; should be. So a sha can have multiple builds

;; Lets just put all the logs from a owner+repo+sha in the same
;; list and the list will just continue to grow

(def logs (atom {}))

(defn str-to-key [opts]
  (keyword
    (format
      "%s/%s/%s"
      (:owner opts)
      (:repo opts)
      (:sha opts))))


;; (defn create-logger []
;;   (let [k (str-to-key *opts*)]
;;     (fn [line]
;;       (do
;;         (let  [l (str (.toString (new java.util.Date)) " " line)]
;;           (println l
;;           (swap!
;;             logs
;;             assoc-in
;;             [k]
;;             (vec (conj (k @logs) l)))))))))

(def ^:dynamic *opts* {})

(defn log [line]
  (let [k (str-to-key *opts*)]
    (let  [l (str (.toString (new java.util.Date)) " " line)]
      ;; (println l
      (swap!
        logs
        assoc-in
        [k]
        (vec (conj (k @logs) l))))))

(defn get-logs [owner repo sha]
  ((str-to-key {:owner owner
                :repo repo
                :sha sha}) @logs))

(defmacro with-logger [opts & body]
 `(binding [*opts* ~opts]
    ~@body))
