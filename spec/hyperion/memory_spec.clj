(ns hyperion.memory-spec
  (:use
    [speclj.core]
    [hyperion.core]
    [hyperion.memory]))

(describe "Memory Implementation"

  (with ds (reset! DS (new-memory-datastore)))
  (before @ds)

  (it "can be created"
    (should= {} @(.store @ds)))

  (it "assigns key to new records"
    (let [saved (save {:kind "widget"})]
      (should= 2 (count saved))
      (should= "widget" (:kind saved))
      (should-not= nil (:key saved))))

  (it "assigned keys are unique"
    (should= 10 (count (set (map #(:key (save {:kind %})) (range 10))))))

  (it "finds the items by key"
    (let [one (save {:kind "one"})
          two (save {:kind "two"})]
      (should= one (find-by-key (:key one)))
      (should= two (find-by-key (:key two)))))

  (it "deletes records"
    (let [one (save {:kind "one"})]
      (delete one)
      (should= nil (find-by-key (:key one)))))

  (it "finds records by kind"
    (let [one (save {:kind "one"})
          two (save {:kind "two"})
          tre (save {:kind "tre"})]
      (should= [] (find-by-kind "foo"))
      (should= [one] (find-by-kind "one"))
      (should= [two] (find-by-kind "two"))
      (should= [tre] (find-by-kind "tre"))))

  (it "applies filters to find-by-kind")
  (it "applies sorts to find-by-kind")
  (it "applies limit and offset to find-by-kind")
  (it "counts by kind")
  (it "counts by kind with filters")
  (it "finds by all kinds (find-all-kinds)")
  (it "countss by all kinds (count-all-kinds)")

  )

(run-specs :stacktrace true)