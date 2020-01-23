(ns guestbook.routes.services
  (:require
    [guestbook.messages :as msg]
    [guestbook.middleware :as middleware]
    [ring.util.http-response :as response]))

(defn service-routes "doc-string" []
  ["/api"
   {:middleware [middleware/wrap-formats]}
   ["/messages"
    {:get
     (fn [_] 
       (response/ok (msg/message-list)))}]
   ["/message"
    {:post
     (fn [{:keys [params]}] 
       (try
         (msg/save-message! params)
         (response/ok {:status :ok})
         (catch Exception e
           (let [{id :guestbook/error-id
                  errors :erros} (ex-data e)]
             (case id
               :validation
               (response/bad-request {:errors errors})
               (response/internal-server-error
                 {:errors {:server-error ["Failed to save message!"]}}))))))}]])
;



