
(ns voltron.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [bidi.bidi :as bidi]
            [accountant.core :as accountant]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType])
  (:import goog.History))

(enable-console-print!)

(println "This text is printed from src/voltron/core.cljs. Go ahead and edit it and see reloading in action.")

(def app-routes
  ["/"
   [["" :index]
    ["one" :a-one]
    ["two" :a-two]]])

(defmulti page-contents identity)

(defmethod page-contents :index []
  [:p "THE INDEX"])

(defmethod page-contents :a-one []
  [:p "THE ONE"])

(defmethod page-contents :a-two []
  [:p "THE TWO"])

(defmethod page-contents :default []
  [:p "THE DEFAULT"])

(defn app []
  (let [page (:current-page (session/get :route))]
    [:div
     [:p "HELLO"]
     [:p [:a {:href (bidi/path-for app-routes :index)} "Go home"]]
     [:p [:a {:href (bidi/path-for app-routes :a-one)} "Go to One"]]
     [:p [:a {:href (bidi/path-for app-routes :a-twoy)} "Go to Two"]]
     ^{:key page} [page-contents page]]))

(defn on-js-reload []
  (r/render-component [app] (js/document.getElementById "app")))

(defn ^:export init! []
  (accountant/configure-navigation!
   {:nav-handler (fn [path]
                   (print (str "In nav: " path))
                   (let [match (bidi/match-route app-routes path)
                         current-page (:handler match)
                         route-params (:route-params match)]
                     (session/put! :route {:current-page current-page
                                           :route-params route-params})))
    :path-exists? (fn [path]
                    (print (str "In exists: " path))
                    (boolean (bidi/match-route app-routes path)))})
  (accountant/dispatch-current!)
  (on-js-reload))
