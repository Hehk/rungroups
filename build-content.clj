(ns create-html (:require [clojure.java.io :as io]
                          [babashka.fs :as fs]))

(def output-dir "dist")

(defn make-attrs [attrs]
  (->> attrs
       (map (fn [[k v]] (str " " (name k) "=\"" v "\"")))
       (apply str)))

(str "test" (apply str ["a" "b" "c"]))

(defn tag [tag-name attrs & content]
  (str "<" tag-name (make-attrs attrs) ">" (apply str content) "</" tag-name ">"))

(defn make-tag [tag-name]
  (fn [attrs & content]
    (apply tag tag-name attrs content)))

(def t-html (make-tag "html"))
(def t-meta (make-tag "meta"))
(def t-title (make-tag "title"))
(def t-div (make-tag "div"))
(def t-head (make-tag "head"))
(def t-body (make-tag "body"))
(def t-a (make-tag "a"))
(def t-h1 (make-tag "h1"))
(def t-p (make-tag "p"))
(def t-header (make-tag "header"))
(def t-nav (make-tag "nav"))
(def t-main (make-tag "main"))
(def t-link (make-tag "link"))

(defn header []
  (t-header {:class "container max-w-3xl mx-auto px-4 pt-16 sm:flex justify-between"}
            (t-h1 {:class "font-bold text-4xl mb-8 align-bottom"} "austin running")
            (t-nav {:class "flex flex-row gap-4"}
                   (t-a {:href "/"} "home")
                   (t-a {:href "/about"} "about")
                   (t-a {:href "/running"} "running")
                   (t-a {:href "/training"} "training")
                   (t-a {:href "/contact"} "contact"))))

(defn layout [& content]
  (str "<!DOCTYPE html>"
       (t-html {:lang "en"}
               (t-head {}
                       (t-meta {:charset "UTF-8"})
                       (t-meta {:name "viewport" :content "width=device-width, initial-scale=1.0"})
                       (t-title {} "austin running")
                       (t-link {:rel "icon" :type "image/svg+xml" :href "/favicon.svg"})
                       (t-link {:rel "stylesheet" :href "/styles.css"}))
               (t-body {}
                       (header)
                       (t-main {:class "container max-w-3xl mx-auto px-4 pt-8"}
                               content)))))

(defn write-html-file [data file]
  (let [file (str output-dir "/" file)]
    (with-open [writer (io/writer file)]
      (.write writer data))))

; Writing the files
(when-not (fs/exists? output-dir)
  (fs/create-dirs output-dir))

(fs/copy-tree "public" output-dir {:replace-existing true})

(write-html-file (layout "hello world") "index.html")



