(ns ^:figwheel-hooks demo-reloadable-code.calendar
  (:require
    [goog.events     :as events]
    [hiccups.runtime :as hiccupsrt])
  (:require-macros
    [hiccups.core :refer [html]]))

; ------------------------------------------------------------------------------
; Calendar Logic
; ------------------------------------------------------------------------------

;; Constants

(def fifteen-min 0.25)

(def eod 17)

;; Utils

(defn time-range
  "Generates a range of times incrementing in .25 (fifteen min) blocks.
  For example, `[9 9.25 9.5 ...]`"
  ([]
   (time-range 9))
  ([start-time]
   (range start-time eod fifteen-min)))


(defn time+hour+min
  "Capture the hours and minutes for the time provided - returns a vector."
  [time]
  (let [match (re-matches #"([0-9]{1,2})([.][0-9]{1,2})" time)]
    (if (nil? match)
      []
      match)))


(defn format-time
  "Format the time provided into a human friendly time format.
  For example, we store the time 9:15 as 9.25. This will convert 9.25 to 9:15."
  [time]
  (let [time-string (str time)
        [original-time hour minutes] (time+hour+min time-string)]
    (cond
      (nil? original-time)
      (str time-string ":00")

      (= ".25" minutes)
      (str hour ":15")

      (= ".5" minutes)
      (str hour ":30")

      :else
      (str hour ":45"))))


; ------------------------------------------------------------------------------
; Actions
; ------------------------------------------------------------------------------

(defn add-event
  "Return an `event`"
  [{:keys [id name start-time end-time] :or {id (random-uuid)}}]
  {:id         id
   :name       name
   :start-time start-time
   :end-time   end-time})

; ------------------------------------------------------------------------------
; Components
; ------------------------------------------------------------------------------

(defn event-card
  "Return HTML `event-card` component"
  [{:keys [name start-time end-time]}]
  (let [event-time (str (format-time start-time) " - " (format-time end-time))]
    (html
      [:div.calendar-event
       [:p.calendar-event__title name]
       [:p.calendar-event__time  event-time]])))


(defn event-card-list
  "Create a series of event HTML components - returns a string"
  [events]
  (apply str (map #(event-card %1) events)))


(defn time-option
  "Return HTML `time-option` component"
  [time]
  (html
    [:option {:value time} (format-time time)]))


(defn time-option-list
  "Create a series of time HTML option components - returns a string."
  [times]
  (apply str (map #(time-option %1) times)))

; ------------------------------------------------------------------------------
; App
; ------------------------------------------------------------------------------

; State
(def app-state (atom []))


; Selectors
(def event-container (.. js/document (querySelector ".calendar-events")))

(def start-time-dropdown (.. js/document (querySelector "#event_start")))

(def end-time-dropdown (.. js/document (querySelector "#event_end")))


; DOM Helpers

(defn update-event-container!
  [events]
  (set! (.. event-container -innerHTML) (event-card-list events)))


(defn update-event-end-dropdown!
  "Re-populate event end dropdown with updated list of time options."
  [evt]
  (let [start-time (+ (js/parseFloat (.. start-time-dropdown -value)) 0.25)]
    (js/console.log "CHANGING")
    (set! (.. evt -target -innerHTML)
          (time-option-list (time-range start-time)))))


; Event Handlers

(defn handle-add-event!
  [e]
  (.preventDefault e)

  (js/console.log "submitting")

  (this-as this
    (let [event-name (.. this (querySelector "[name=event_name]") -value)
          start-time (js/parseFloat (.-value start-time-dropdown))
          end-time   (js/parseFloat (.-value end-time-dropdown))
          new-event  (add-event
                       {:name       event-name
                        :start-time start-time
                        :end-time   end-time})]

      ;; add new event to app-state
      (swap! app-state conj new-event)

      ;; add new event to events list
      (update-event-container! @app-state)

      ;; reset add event form
      (.. this reset)

      (set! (.. end-time-dropdown -innerHTML) (time-option-list (time-range 9.25))))))

; ------------------------------------------------------------------------------
; App
; ------------------------------------------------------------------------------

; Register event listeners

(defn ^:before-load teardown []
  (events/removeAll
   (.querySelector js/document ".calendar-form")))


(defn ^:after-load setup []
  (events/listen
    (.. js/document (querySelector ".calendar-form"))
    "submit"
    handle-add-event!)

  (events/listen
    (.. js/document (querySelector "#event_start"))
    "change"
    update-event-end-dropdown!))


(defonce initial-load
  (do (setup)

      ; only need these to run once at the beginning
      (set! (.. start-time-dropdown -innerHTML) (time-option-list (time-range)))

      (set! (.. end-time-dropdown -innerHTML) (time-option-list (time-range 9.25)))))


;; Init

(update-event-container! @app-state)
