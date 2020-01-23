(ns guestbook.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [ajax.core :refer [GET POST]]
            [clojure.string :as string]
            [guestbook.validation :refer [validate-message]]))

(rf/reg-event-fx
  :app/initialize
  (fn [_ _] 
    {:db {:messages/loading? true}}))
;
(rf/reg-sub
  :messages/loading?
  (fn [db _]
	(:messages/loading? db)))
;
(rf/reg-event-db
  :messages/set
  (fn [db [_ messages]]
	(-> db
		(assoc :messages/loading? false
			   :messages/list messages))))
;
(rf/reg-sub
  :messages/list
  (fn [db _]
	(:messages/list db [])))
;
(rf/reg-event-db
  :message/add
  (fn [db [_ message]] 
    (update db :messages/list conj message)))
;
(defn send-message! "doc-string" [fields errors]
  (if-let [validation-errors (validate-message @fields)]
    (reset! errors validation-errors)
    (POST "/api/message"
        {:format :json
         :headers
         {"Accept" "application/transit+json"
          "x-csrf-token" (.-value (.getElementById js/document "token"))}
         :params @fields
         :handler #(do
                     (rf/dispatch [:message/add (assoc @fields :timestamp (js/Date.))])
                     (reset! fields nil)
                     (reset! errors nil))
         :error-handler #(do
                           (.log js/console (str %))
                           (reset! errors (get-in % [:response :errors])))})))
;
(defn get-messages [] 
  (GET "/api/messages"
	   {:headers {"Accept" "application/transit+json"}
		:handler #(do
                    (.log js/console %)
                   (rf/dispatch [:messages/set (:messages %)]))}))
;
(defn message-list "doc-string" [messages]
  (println messages)
  [:ul.messages
   (for [{:keys [timestamp message name]} @messages]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p " - " name]])])
;
(defn errors-component "doc-string" [errors id]
  (when-let [error (id @errors)]
    [:div.notification.is-danger (string/join error)]))
;
(defn message-form "doc-string" []
  (let [fields (r/atom {})
        errors (r/atom nil)]
    (fn [] 
      [:div
       [:p "Name: " (:name @fields)]
       [:p "Message: " (:message @fields)]
       [errors-component errors :server-error]
       [:div.field
        [:label.label {:for :name}  "Name"]
        [errors-component errors :name]
        [:input.input
         {:type :text
          :name :name
          :on-change #(swap! fields assoc :name (-> % .-target .-value))
          :value (:name @fields)}]]
       [:div.field
        [:label.label {:for :message} "Message"]
        [errors-component errors :message]
        [:textarea.textarea
         {:name :message
          :value (:message @fields)
          :on-change #(swap! fields assoc :message (-> % .-target .-value))}]]
       [:input.button.is-primary
        {:type :submit
         :on-click #(send-message! fields errors) 
         :value "comment"}]])))
;
(defn home "doc-string" []
  (let [messages (rf/subscribe [:messages/list])]
    (rf/dispatch [:app/initialize])
    (get-messages)
    (fn []
      [:div.content>div.columns.is-centered>div.column.is-two-thirds
       [:div.columns>div.column
        [:h3 "Messages"]
        [message-list messages]]
       [:div.columns>div.column
        [message-form]]])))
;
(r/render 
  [home]
  (.getElementById js/document "content"))
