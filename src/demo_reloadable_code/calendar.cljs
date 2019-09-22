(ns ^:figwheel-hooks demo-reloadable-code.calendar
  (:require
    [reagent.core :as r]))


(defn app []
  [:h1.site__title
    [:span.site__title-text "calendar"]])


(defn mount []
  (r/render [app] (js/document.getElementById "root")))


(defn ^:after-load re-render []
  (mount))


(defonce start-up (do (mount) true))
