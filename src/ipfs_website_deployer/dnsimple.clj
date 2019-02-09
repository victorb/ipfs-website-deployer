(ns ipfs-website-deployer.dnsimple
  (:require [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [ipfs-website-deployer.env :refer [env]]))

(def dnsimple-token (env "DNSIMPLE_TOKEN"))
(def base-url (env "DNSIMPLE_API"))

;; (def default-gateway "gateway-int.ipfs.io")
(def default-gateway (env "IPFS_GATEWAY"))

(def default-landing-page (env "DEFAULT_LANDING_PAGE"))
(def default-txt-record (str "dnslink=/ipfs/" default-landing-page))

(defn get-url [path]
  (str base-url path))

(def request-metadata
  {:headers {:authorization (str "Bearer " dnsimple-token)}
   :as :json})

(defn get-data
  [url]
  (get-in (client/get (get-url url) request-metadata) [:body :data]))

(defn post-data
  [url body]
  (get-in (client/post
            (get-url url)
            (merge request-metadata {:form-params body})) [:body :data]))

(defn patch-data
  [url body]
  (get-in (client/patch
            (get-url url)
            (merge request-metadata {:form-params body})) [:body :data]))

(defn get-account-id []
  (:id (first (get-data "/accounts"))))

(defn create-record
  [zone record]
  (post-data
    (format "/%s/zones/%s/records" (get-account-id) zone)
    {:name (:name record)
     :type (:type record)
     :content (:content record)
     :ttl 60}))

(defn update-record
  [zone old-record new-record]
  (patch-data
    (format "/%s/zones/%s/records/%s" (get-account-id) zone (:id old-record))
    new-record))

(defn get-records [account-id zone]
  (get-data (format "/%s/zones/%s/records" account-id zone)))

(defn is-record-type? [record record-type]
  (= (:type record) record-type))

(defn record-is-alias? [record] (is-record-type? record "ALIAS"))

(defn record-is-txt? [record] (is-record-type? record "TXT"))

(defn has-alias-record? [res]
  (> (count (filter record-is-alias? res)) 0))

(defn has-txt-record? [res]
  (> (count (filter record-is-txt? res)) 0))

(defn is-record [a b]
  (and
    (= (:type a) (:type b))
    (= (:content b) (:content a))))

(defn is-dnslink-record [record]
  (and
    (= (:type record) "TXT")
    (= (:name record) "_dnslink")
    (.contains (:content record) "dnslink=/ipfs/")))

(defn has-record? [records record]
  (> (count (filter #(is-record % record) records)) 0))

(defn has-dnslink-record? [records]
  (> (count (filter #(is-dnslink-record %) records)) 0))

(defn get-dnslink-record [zone]
  (first (filter #(is-dnslink-record %) (get-records (get-account-id) zone))))

(def default-alias-record {:type "ALIAS"
                           :content default-gateway
                           :ttl 60})

(def default-dnslink-record {:type "TXT"
                             :name "_dnslink"
                             :content "dnslink=/ipfs/"
                             :ttl 60})

(defn make-dnslink-record [hash]
  (merge default-dnslink-record {:content (str "dnslink=/ipfs/" hash)}))

(defn create-alias-record-if-missing
  [zone record]
  (if-not
    (has-record?
      (get-records
        (get-account-id)
        zone)
      default-alias-record)
    (create-record zone record)))

(defn create-or-update-dnslink-record
  [zone record]
  (if (has-dnslink-record? (get-records (get-account-id) zone))
    (update-record zone (get-dnslink-record zone) record)
    (create-record zone record)))

(defn format-record [record zone]
  (format "updated %s.%s value to be %s"
          (:name record)
          zone
          (:content record)))

(defn set-dnslink
  [config hash]
  (let [new-record (make-dnslink-record hash) zone (:website config)]
    (do
      (create-alias-record-if-missing zone default-alias-record)
      (create-or-update-dnslink-record zone new-record)
      (println (format-record new-record zone)))))
