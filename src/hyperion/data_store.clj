(ns hyperion.data-store)

(defprotocol DataStore
  (create [this collection-name record])
  (find-where [this collection-name attrs])
  (delete [this collection-name record])
  (update [this collection-name record]))
