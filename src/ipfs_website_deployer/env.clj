(ns ipfs-website-deployer.env
  (:require [dotenv :as dotenv]))

(defn getenv [n]
  (let [v (dotenv/env n)]
    v))
;; (println (format "Loading %s=%s" n v))

(def env (memoize getenv))
