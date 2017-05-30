(ns voltron.core
  (:require [accountant.core :as accountant]
            [bidi.bidi :as bidi]
            [cljs.core.async :as async]
            [cljs-http.client :as http]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [reagent.core :as r]
            [reagent.session :as session]
            [clojure.string :as str]
            [cljsjs.marked])
  (:import goog.History)
  (:require-macros [cljs.core.async.macros :as asyncm :refer [go]]))

(enable-console-print!)

(def api-root "http://localhost:5000/api")
(def avatar-url "http://www.gravatar.com/avatar/a942cea13e537bb0ea754b6d216c3377?size=160")

;; Current world of displayable things
(def pages-state (r/atom {:titles [] :contents {}}))
;; Which page should be Now.
(def page-state (r/atom {}))

(defn hit-api
  ([endpoint param]
   (hit-api endpoint param (async/chan)))
  ([endpoint param chan]
   (let [uri (str/join "/" [api-root endpoint param])]
     (http/get uri {:with-credentials? false
                    :channel chan}))))

(defn header []
  [:div {:class "header"}
   [:div {:class "text"}
    [:h1 "Ross Donaldson's" [:p "Interblag Wobthing Delux!"]]]])

(declare load-page)
(defn tab [title]
  [:div {:class "post-tab" :name title}
   [:a {:class (if (and (not (empty? @page-state))
                        (= (:title @page-state) title))
                 "currentLink"
                 "inactiveLink")
        :href title}
    (str/capitalize title)]])

(defn side-bar []
  [:div {:class "side-bar"}
   [:img {:src avatar-url :href "www.gastove.com"}]
   [:hr]
   [:div {:class "icons"}
    [:a {:href "http://www.twitter.com/Gastove"}
     [:i {:class "fa fa-twitter fa-2x"}]]
    [:a {:href "http://www.github.com/Gastove"}
     [:i {:class "fa fa-github fa-2x"}]]]
   [:hr]
   (for [title (:titles @pages-state)]
     ^{:key title} [tab title])])

(defn page [title]
  [:div {:class "page"}
   (if-not (empty? @page-state)
     [:span {:dangerouslySetInnerHTML {:__html (js/marked (:text @page-state))}}])])

(defn app []
  (let [title (:current-page (session/get :route))]
    [:div {:class "app"}
     [header]
     [side-bar]
     [:div {:class "posts-view"}
      ^{:key title} [page]]]))

(def body-objects-loader
  (comp
   (map :body)
   (map #(.parse js/JSON %))
   (map #(js->clj % :keywordize-keys true))))

(def routes (atom ["/"]))

(defn load-routes []
  (let [route-chan (async/chan 1 body-objects-loader)]
    (hit-api "page" "-list" route-chan)
    (go
      (let [pages (async/<! route-chan)
            pairs (into [["" :home]] (map (fn [endpoint] [endpoint (keyword endpoint)]) pages))]
        (swap! routes (fn [_] ["/" pairs]))))))

(defn load-state []
  (let [page-chan (async/chan 1 body-objects-loader)
        endpoint "page"
        uri (str/join "/" [api-root endpoint])]
    (http/get uri {:with-credentials? false
                   :channel page-chan})
    (go
      (let [pages (async/<! page-chan)
            page-titles (map :title pages)
            page-contents (into {} (map (fn [{:keys [:title :body]}] [title, body]) pages))]
        (swap! pages-state assoc :titles page-titles)
        (swap! pages-state assoc :contents  page-contents)
        (if (empty? @page-state) ;; Empty map indicates no content has been loaded yet.
          (do
            (swap! page-state assoc :title "home")
            (swap! page-state assoc :text (page-contents "home"))))))))

(defn load-page [title]
  (.log js/console (str "Trying to load page: " title))
  (if-let [page ((:contents @pages-state) title)]
    (do
      (swap! page-state assoc :title title)
      (swap! page-state assoc :text page))))

(defn on-js-reload []
  (r/render-component [app] (js/document.getElementById "web-app"))
  (load-state)
  (load-routes))

(defn ^:export init! []
  (load-routes)
  (accountant/configure-navigation!
   {:nav-handler (fn [path]
                   (let [match (bidi/match-route @routes path)
                         current-page (or (:handler match) "/")
                         route-params (:route-params match)]
                     (session/put! :route {:current-page current-page
                                           :route-params route-params})
                     (.log js/console "Trying to resolve path: " path)
                     (.log js/console "Got match: " match)
                     (.log js/console "Name of current page is: " current-page)
                     (load-page (name current-page))))
    :path-exists? (fn [path]
                    (boolean (bidi/match-route @routes path)))})
  (accountant/dispatch-current!)
  (on-js-reload))
