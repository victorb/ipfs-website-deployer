(ns ipfs-website-deployer.dnsimple-test)
;;  (:require [clojure.test :refer :all]
;;            [clojure.pprint :refer [pprint]]
;;            [ipfs-website-deployer.dnsimple :refer :all]))
;;
;; (defn load-from-file
;;   ""
;;   [path]
;;   (clojure.edn/read-string (slurp path)))
;; 
;; (defn fixture-path
;;   ""
;;   [path]
;;   (str "test/ipfs_website_deployer/fixtures/" path))
;; 
;; (def fixture-existing
;;   (load-from-file
;;     (fixture-path "existing-dnsimple-zone.clj")))
;; 
;; (def fixture-new
;;   (load-from-file
;;     (fixture-path "new-dnsimple-zone.clj")))
;; 
;; (deftest is-record-type
;;   (testing "is-record-type"
;;     (is (= (is-record-type? {:type "TXT"} "TXT") true))
;;     (is (= (is-record-type? {:type "ALIAS"} "ALIAS") true))))
;; 
;; (deftest has-alias-record
;;   (testing "has-alias-record?"
;;     (is (= (has-alias-record? fixture-existing) true))
;;     (is (= (has-alias-record? fixture-new) false))))
;; 
;; (deftest has-record)
;;   (testing "has-record?"
;;     (is (= (has-record? fixture-existing {:type "TXT" :content "cloudflare-ipfs.com"}))))
