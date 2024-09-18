(require
 '[babashka.http-client :as http]
 '[babashka.fs :as fs]
 '[clojure.data.csv :as csv]
 '[clojure.java.io :as io]
 '[clojure.string :as str])

(def base-url "https://docs.google.com/spreadsheets/d/1cy2U3JYRbCHj-KI-eszUZH2b5ZKNB5IJ2awiAny2Y8g/export?format=csv")
(def meetups-sheet-id "0")
(def run-groups-sheet-id "822867825")
(def events-sheet-id "1618285110")

(defn url [sheet-id] (str base-url "&gid=" sheet-id))

(defn read-csv-file [file-path]
  (with-open [reader (io/reader file-path)]
    (doall (csv/read-csv reader))))

(defn to-id [name]
  (-> name
      (str/replace #"\s+" "-")
      (str/lower-case)))

; Setting up the dist directory
(when-not (fs/exists? "data")
  (fs/create-dirs "data"))

; Save the first meetup content
(def meetups-file "data/meetups.csv")
(spit meetups-file (:body (http/get (url meetups-sheet-id))))

(defn meetup-header [header]
  (replace {"Running Group" :group
            "Day of the Week" :day
            "Time" :time
            "Description" :description
            "Location" :location} header))

(defn parse-time [time-str]
  (cond
    (= time-str "Morning") 600
    (= time-str "Afternoon") 1200
    (= time-str "Evening") 1800
    :else
    (let [[_ hour minute period] (re-matches #"(\d{1,2}):(\d{2}) ?(AM|PM)" time-str)
          hour-int (Integer/parseInt hour)
          minute-int (Integer/parseInt minute)
          hour-24 (cond
                    (= period "AM") (if (= hour-int 12) 0 hour-int)
                    (= period "PM") (if (= hour-int 12) 12 (+ hour-int 12)))]
      (+ (* hour-24 100) minute-int))))

(defn get-meetups [file]
  (let [data (read-csv-file file)
        header (meetup-header (first data))
        meetups (rest data)]
    (->> meetups
         (map #(zipmap header %))
         (map #(assoc % :id (to-id (:group %))))
         (map #(assoc % :parsed-time (parse-time (:time %))))
         (map #(assoc % :day (keyword (str/lower-case (:day %)))))
         (sort-by :parsed-time))))

(def meetups
  (->> meetups-file
       (get-meetups)
       (group-by :day)))

(spit "data/meetups.edn" (pr-str meetups))

; Generating the run-groups content
(def run-groups-file "data/run-groups.csv")
(spit run-groups-file (:body (http/get (url run-groups-sheet-id))))

(defn run-group-header [header]
  (replace {"Name" :name
            "Description" :description
            "Instagram" :instagram
            "Twitter" :twitter
            "Strava" :strava
            "Website" :website
            "Facebook" :facebook} header))

(defn day-has [meetups day id]
  (some #(= id (:id %)) (get meetups day [])))

(defn add-days [group]
  (let [id (:id group)]
    (assoc group
           :monday (day-has meetups :monday id)
           :tuesday (day-has meetups :tuesday id)
           :wednesday (day-has meetups :wednesday id)
           :thursday (day-has meetups :thursday id)
           :friday (day-has meetups :friday id)
           :saturday (day-has meetups :saturday id)
           :sunday (day-has meetups :sunday id))))

(defn get-run-groups [file]
  (let [data (read-csv-file file)
        header (run-group-header (first data))
        run-groups (rest data)]
    (->> run-groups
         (map #(zipmap header %))
         (map #(assoc % :id (to-id (:name %))))
         (sort-by :id)
         (map add-days))))

(def run-groups
  (->> run-groups-file
       (get-run-groups)))

(spit "data/run_groups.edn" (pr-str run-groups))

(spit "./data/events.csv" (:body (http/get (url events-sheet-id))))
(defn event-header [header]
  (replace {"Name" :name
            "Date" :date
            "Time" :time
            "Description" :description
            "Location" :location
            "Distances" :distances
            "Host" :host
            "Website" :website
            "Instagram" :instagram
            "Facebook" :facebook
            "Twitter" :twitter} header))

(defn parse-date [date]
  (let [formats ["M/dd/yy" "M/d/yy" "M/dd/yyyy" "M/d/yyyy" "MM/dd/yy" "MM/dd/yyyy"]
        formatters (map #(java.time.format.DateTimeFormatter/ofPattern %) formats)]
    (some #(try
             (java.time.LocalDate/parse date %)
             (catch Exception _ nil))
          formatters)))
(defn get-events [file]
  (let [data (read-csv-file file)
        header (event-header (first data))
        events (rest data)
        now (.toLocalDate (java.time.LocalDateTime/now))]
    (->> events
         (map #(zipmap header %))
         (map #(assoc % :date (parse-date (:date %))))
         (map #(assoc % :has-happened? (.isAfter now (:date %)))))))

(def events (get-events "./data/events.csv"))
(spit "data/events.edn" (pr-str events))

(println "DONE: downloading data")
