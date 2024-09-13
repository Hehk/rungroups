(ns inline-css (:require [clojure.java.io :as io]
                         [clojure.string :as str]))

; TODO: This is a hack, using an html parser and then find all the css links and inline them

(defn read-file [file]
  (with-open [reader (io/reader file)]
    (slurp reader)))

(defn get-html [file]
  (with-open [reader (io/reader file)]
    (slurp reader)))

(defn fix-escape-chars [s]
  ; Tailwind uses \: for modifiers, when passed throught str the \ is removed
  ; so we need to replace it with \\\\: to make sure it is passed through
  (str/replace s #"\:" "\\\\:"))

(defn remove-comments [s]
  (str/replace s #"/\*[\s\S]*?\*/" ""))

(defn replace-css [a html]
  (str/replace html #"<link rel=\"stylesheet\" href=\"/styles.css\"></link>" (str "<style>" a "</style>")))

(defn is-used-var? [name s]
  (str/includes? s (str "var(" name ")")))
(defn remove-unused-vars [s]
  (let [vars (re-seq #"(--[^\)\:\;\s]*)" s)
        unused-vars (filter #(not (is-used-var? (second %) s)) vars)]
    (reduce (fn [val [_ name]]
              (str/replace val (re-pattern (str name ":[^\\;\\}]*;?")) ""))
            s unused-vars)))

(def css (-> "dist/styles.css"
             (read-file)
             (remove-unused-vars)
             (remove-comments)
             (fix-escape-chars)))

(defn run-replace [file css]
  (->> file
       (get-html)
       (replace-css css)
       (spit file)))

(run-replace "dist/index.html" css)
(run-replace "dist/groups.html" css)
(run-replace "dist/routes.html" css)
(run-replace "dist/about.html" css)

(println "DONE: Inlining css")
