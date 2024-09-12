(ns inline-css (:require [clojure.java.io :as io]
                         [clojure.string :as str]))

; TODO: This is a hack, using an html parser and then find all the css links and inline them

(defn get-css [file]
  (with-open [reader (io/reader file)]
    (slurp reader)))

(defn get-html [file]
  (with-open [reader (io/reader file)]
    (slurp reader)))

(defn fix-escape-chars [s]
  ; Tailwind uses \: for modifiers, when passed throught str the \ is removed
  ; so we need to replace it with \\\\: to make sure it is passed through
  (str/replace s #"\:" "\\\\:"))

(defn replace-css [a html]
  (str/replace html #"<link rel=\"stylesheet\" href=\"/styles.css\"></link>" (str "<style>" a "</style>")))

(def css (fix-escape-chars (get-css "dist/styles.css")))

(defn run-replace [file css]
  (->> file
       (get-html)
       (replace-css css)
       (spit file)))

(run-replace "dist/index.html" css)
(run-replace "dist/groups.html" css)
(run-replace "dist/routes.html" css)
(run-replace "dist/about.html" css)
