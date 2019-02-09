(ns ipfs-website-deployer.core
  (:use compojure.core
        ring.util.response
        tentacles.core
        ipfs-website-deployer.logger
        [hiccup.middleware :only (wrap-base-url)])
  (:require [ring.middleware.json :as middleware]
            [ipfs-website-deployer.build :refer [mkdirp sh-str build]]
            [ipfs-website-deployer.dnsimple :refer [set-dnslink]]
            [ipfs-website-deployer.config :refer [load-config]]
            [ipfs-website-deployer.ipfs :refer [add]]
            [ipfs-website-deployer.ipfs-cluster :refer [pin]]
            [ipfs-website-deployer.env :refer [env]]
            [compojure.route :as route]
            [clojure.pprint :refer [pprint]]
            [clj-jwt.core  :refer :all]
            [iapetos.core :as prometheus]
            [iapetos.collector.ring :as ring]
            [clojure.java.io :as io]
            [clj-http.client :as client]
            [clj-jwt.key :refer [private-key]]
            [clj-time.core :refer [now plus minutes]]
            [compojure.handler :as handler]
            [ring.util.response :as response]
            [ring.middleware.oauth2 :refer [wrap-oauth2]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

;; https://github.com/lispyclouds/clj-docker-client
;; Claims a valid bearer token for 10 minutes
(def claim
  {:iss (env "GITHUB_APP_ID")
   :exp (plus (now) (minutes 10))
   :iat (now)})

; Load private-key from disk
(def gh-app-key (private-key (env "GITHUB_PRIVATE_KEY_PATH")))

; Signs key together with claim
(defn get-auth
  ""
  []
  (-> claim jwt (sign :RS256 gh-app-key) to-str))

; Binds `defaults` in body with bearer auth
(defmacro with-bearer-auth [& body]
  `(with-defaults {:accept "application/vnd.github.machine-man-preview+json"
                  :bearer-token (get-auth)}
      ~@body))

; Binds `defaults` in body with token auth
(defmacro with-token-auth [token & body]
  `(with-defaults {:oauth-token ~token}
      ~@body))

; Returns a installation token
(defn get-installation-token
  "Gets a access token for an installation"
  [installation_id]
  (with-bearer-auth
    (:token
      (api-call
        :post
        "/app/installations/%s/access_tokens"
        [installation_id]
        nil))))

(defn copy [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

(defn download-with-token
  [token owner repo sha]
  (let [dir (format "/tmp/%s/%s" owner repo)
        tarball (format "%s/%s.tar" dir sha)
        src-dir (format "%s/%s" dir sha)]
    (mkdirp dir)
    (mkdirp (str dir "/" sha))
    (io/copy
        (:body
          (client/get
            (format "https://api.github.com/repos/%s/%s/tarball/%s"
              owner repo sha)
            {:as :stream
             :headers {"Authorization" (str "token " token)}}))
          (io/file tarball))
    (println "before extract")
    (let [sh-cmd (format "tar xfv %s --strip-components=1 -C %s" tarball src-dir)]
      (pprint (sh-str sh-cmd "/tmp"))
      (println "after extract")
      src-dir)))

(defn unpack-resources [in out]
  (clojure.java.shell/sh 
   "sh" "-c" 
  (str " unzip " in " -d " out)))

(defn get-installation-id
  "Gets the installation token from a request body"
  [body]
  (get-in body ["installation" "id"]))

(defn get-commit-status
  "Sets the status of a git commit on GitHub"
  [owner repo sha]
  (api-call
    :get
    "/repos/%s/%s/statuses/%s"
    [owner repo sha]
    nil))

(def default-commit-status {:context "Website Build"})

(defn commit-status
  ""
  [opts]
  (merge default-commit-status opts))

(def pending-commit-status (commit-status {:state "pending"}))

(def success-commit-status (commit-status {:state "success"}))

(defn set-commit-pending
  "Sets a commit as PENDING"
  [owner repo sha]
  (api-call
    :post
    "/repos/%s/%s/statuses/%s"
    [owner repo sha]
    (merge pending-commit-status
           {:description "Building website..."
            :target_url (format "https://website-deploy.app/%s/%s/%s" owner repo sha)})))

(defn set-commit-success
  "Sets a commit as SUCCESS"
  [owner repo sha cid]
  (api-call
    :post
    "/repos/%s/%s/statuses/%s"
    [owner repo sha]
    (merge
      success-commit-status
      {:description "See preview by clickin on 'Details' there -> "
       :target_url (format "https://ipfs.io/ipfs/%s" cid)})))

(defn get-sha "" [body] (get-in body ["pull_request" "head" "sha"]))
(defn get-repo-name "" [body] (get-in body ["repository" "name"]))
(defn get-owner "" [body] (get-in body ["repository" "owner" "login"]))

(defn do-build
  "Incoming request to be built"
  [body]
  (let [token (get-installation-token (get-installation-id body))]
    (with-token-auth token
      (let [owner (get-owner body)
            repo (get-repo-name body)
            sha (get-sha body)]
        (do
          (future
            (set-commit-pending owner repo sha)
            (let [src-dir (download-with-token token owner repo sha)]
              (let [config (load-config src-dir)]
                (set-dnslink config (pin (add (build src-dir config))))))))
          (response "ok")))))

(defn pr-build [body]
  (let [token (get-installation-token (get-installation-id body))]
    (with-token-auth token
      (let [owner (get-owner body)
            repo (get-repo-name body)
            sha (get-sha body)]
        (with-logger {:owner owner :repo repo :sha sha}
          (do
            (log "##### Starting new and fresh build")
            (future
              (log (format "Building %s/%s#%s" owner repo sha))
              (set-commit-pending owner repo sha)
              (log "Put commit to pending")
              (let [src-dir (download-with-token token owner repo sha)]
                (let [config (load-config src-dir)]
                  (log "Config loaded")
                  (log config)
                  (let [cid (pin (add (build src-dir config)))]
                    (log (str "Website built " cid))
                    (set-commit-success owner repo sha cid))))))
            (response "ok"))))))

(defn master-build [body]
  (do (println "master-build")
      (response "master-build")))

(defn is-pr? [body] (= (get body "action") "opened"))
(defn is-master? [body] (= (get body "ref") "refs/heads/master"))

;; (do-build body)
;; "action": "opened" => PR created
;; "ref": "refs/heads/master" => Master changed
(defn handle-incoming-webhook "" [body]
  (cond
    (is-pr? body) (pr-build body)
    (is-master? body) (master-build body)))

(defroutes api-routes
  (GET "/:owner/:repo/:sha" [owner repo sha]
       (response
         (clojure.string/join "\n" (get-logs owner repo sha))))
  (POST "/webhook" {body :body} (handle-incoming-webhook body)))

(defroutes main-routes
  (GET "/dashboard" request (do
                              (pprint (-> request :oauth2/access-tokens :github :token))
                              (content-type (response/resource-response "dashboard.html" {:root "public"}) "text/html")))
  (GET "/" [] (content-type (response/resource-response "index.html" {:root "public"}) "text/html"))
  (route/resources "/")
  )

;; Available under /metrics
(defonce registry
  (-> (prometheus/collector-registry)
      (ring/initialize)))

(def website
  (-> (handler/site main-routes)
      (wrap-params)
      (wrap-session)
      (wrap-oauth2
        {:github
         {:authorize-uri    "https://github.com/login/oauth/authorize"
          :access-token-uri "https://github.com/login/oauth/access_token"
          :client-id        (env "GITHUB_CLIENT_ID")
          :client-secret    (env "GITHUB_SECRET")
          :scopes           ["user:email"]
          :launch-uri       "/login"
          :redirect-uri     (env "GITHUB_REDIRECT_URI")
          :landing-uri      "/dashboard"}})
      (wrap-defaults (-> site-defaults (assoc-in [:session :cookie-attrs :same-site] :lax)))))

(def api
  (-> (handler/api api-routes)
      (ring/wrap-metrics registry {:path "/metrics"})
      (middleware/wrap-json-body)
      (middleware/wrap-json-response)))

(def app (routes api website))
