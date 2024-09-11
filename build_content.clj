(ns build-content (:require [clojure.java.io :as io]
                            [clojure.edn :as edn]
                            [clojure.string :as str]
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
(def t-h2 (make-tag "h2"))
(def t-h3 (make-tag "h3"))
(def t-h4 (make-tag "h4"))
(def t-p (make-tag "p"))
(def t-header (make-tag "header"))
(def t-nav (make-tag "nav"))
(def t-main (make-tag "main"))
(def t-link (make-tag "link"))
(def t-li (make-tag "li"))
(def t-ol (make-tag "ol"))
(defn t-br [] "<br />")

(defn styled-a [attrs & content]
  (apply t-a (assoc attrs :class "underline decoration-green-400 hover:text-green-400 underline-offset-4 leading-8") content))
(defn styled-p [attrs & content]
  (apply t-p (assoc attrs :class "mb-2 leading-6") content))
(defn styled-h4 [attrs & content]
  (apply t-h4 (assoc attrs :class "text-xl mb-4 mt-4 align-bottom") content))
(defn styled-h3 [attrs & content]
  (apply t-h3 (assoc attrs :class "text-2xl mb-4 mt-4 align-bottom") content))
(defn styled-h2 [attrs & content]
  (apply t-h2 (assoc attrs :class "font-bold text-3xl mb-8 mt-8 align-bottom") content))
(defn styled-h1 [attrs & content]
  (apply t-h1 (assoc attrs :class "font-bold text-4xl mb-8 align-bottom") content))

(defn header []
  (t-header {:class "container max-w-3xl mx-auto px-4 pt-16 sm:flex justify-between"}
            (styled-h1 {} "Austin running")
            (t-nav {:class "flex flex-row gap-4"}
                   (styled-a {:href "/" :ref "prefetch"} "Home")
                   (styled-a {:href "/groups" :ref "prefetch"} "Groups")
                   (styled-a {:href "/routes" :ref "prefetch"} "Routes")
                   (styled-a {:href "/about" :ref "prefetch"} "About"))))

(defn layout [description & content]
  (str "<!DOCTYPE html>"
       (t-html {:lang "en"}
               (t-head {}
                       (t-meta {:charset "UTF-8"})
                       (t-meta {:name "viewport" :content "width=device-width, initial-scale=1.0"})
                       (t-meta {:name "description" :content description})
                       (t-title {} "austin running")
                       (t-link {:rel "icon" :type "image/svg+xml" :href "/favicon.svg"})
                       (t-link {:rel "stylesheet" :href "/styles.css"}))
               (t-body {}
                       (header)
                       (apply t-main {:class "container max-w-3xl mx-auto px-4 pb-8"}
                              content)))))

(defn write-file [file data]
  (let [file (str output-dir "/" file)]
    (with-open [writer (io/writer file)]
      (.write writer data))))

; Writing the files
(when-not (fs/exists? output-dir)
  (fs/create-dirs output-dir))

(fs/copy-tree "public" output-dir {:replace-existing true})

(defn meetup [m]
  (t-li {}
        (t-div {:class "grid grid-cols-3 sm:grid-cols-6 gap-4 mb-2 items-center"}
               (t-div {:class "font-bold"} (:time m))
               (t-div {:class "text-right sm:text-left col-span-2 sm:col-span-5"}
                      (styled-a {:href (str "/groups#" (:id m))} (:group m))))
        (t-div {:class "sm:grid sm:grid-cols-6 mb-4"}
               (t-div {:class "sm:col-start-2 sm:col-span-5"} (styled-p {} (:description m)))
               (t-div {:class "sm:col-start-2 sm:col-span-5"} (styled-p {} (str "Location: " (:location m)))))))

(defn day [meetups day]
  (str
   (styled-h2 {} (str/capitalize (name day)))
   (t-ol {}
         (apply str (map meetup (get meetups day))))))

(defn day-block [active c]
  (let [cls "rounded block h-6 w-6 text-center text-white "
        cls (if active (str cls "bg-green-400") (str cls "bg-gray-400"))]
    (t-div {:class cls} c)))

(defn run-group [g]
  (t-div {:class "mb-8 gap-4"}
         (styled-h3 {:id (:id g)} (:name g))
         (styled-p  {} (:description g))
         (t-div {:class "flex flex-row gap-4 mb-2"} (if (= "" (:instagram g))
                                                      ""
                                                      (styled-a {:href (:instagram g)} "Instagram"))
                (if (= "" (:facebook g))
                  ""
                  (styled-a {:href (:facebook g)} "Facebook"))
                (if (= "" (:twitter g))
                  ""
                  (styled-a {:href (:twitter g)} "Twitter"))
                (if (= "" (:website g))
                  ""
                  (styled-a {:href (:website g)} "Website"))
                (if (= "" (:strava g))
                  ""
                  (styled-a {:href (:strava g)} "Strava")))
         (t-div {:class "flex flex-row gap-2"}
                (day-block (:monday g) "M")
                (day-block (:tuesday g) "T")
                (day-block (:wednesday g) "W")
                (day-block (:thursday g) "T")
                (day-block (:friday g) "F")
                (day-block (:saturday g) "S")
                (day-block (:sunday g) "S"))))

(def meetups (edn/read-string (slurp "data/meetups.edn")))
(write-file "index.html"
            (layout "All of the weekly running events in Austin, Texas"
                    (day meetups :monday)
                    (day meetups :tuesday)
                    (day meetups :wednesday)
                    (day meetups :thursday)
                    (day meetups :friday)
                    (day meetups :saturday)
                    (day meetups :sunday)))

(def run-groups (edn/read-string (slurp "data/run_groups.edn")))
(write-file "groups.html"
            (apply layout "All the running groups in Austin, Texas"
                   (styled-h2 {} "Groups")
                   (map run-group run-groups)))

(def long-runs [{:name "The Town Lake Loop"
                 :link "https://www.strava.com/routes/18412760"
                 :description "The loop, is a mostly dirt route that goes along the Colorado river through downtown Austin. It is the most popular route by far in Austin, and most long run groups at least do a section of their run on it."}
                {:name "Mt Bonnell"
                 :link "https://onthegomap.com/?m=r&u=mi&w%5B%5D=Routes+may+not+be+suitable+for+public+use.&c%5B%5D=Route+data+%C2%A92023+On+The+Go+Map%2C+OpenStreetMap+Contributors&d=15375&f=9b6114eb90&n=1&dm=1&context=share&r2=ybvu5XXuqIh1a2M1IPKHW1Jo6~2M7U580A0O4MAg1UC1KAi1j2Qd1s2v3a2x2i2j2W1Pc1Vm1X1_1f1q4Z3k4Z3c3b2w1d1k4~2OFKFq1Z1m2v1w4n3i2t1o2~1eEtAY7f5c7n5g8f6SFk2d1W2RQ57Z15731J1H49H91l1K0F0Gm1JA2AII3K242688a1Y19Y13w5Nu15_13u4Bk13g11y37m36W1Ii1Qe5e3i2q3CSOm1Aa10Q3KHw1BUVm1DCd1UJMNe1DY1W2e1m2k1Q4k1FW15S6a4x8w1r3EJA0OEs6c4G9K3M2O8i1Oi2u1a5e5_1g2y1s2g7i7c3_2_2k2260A06p2c3g1W1KMW1o1ESISMIm2Y1Y10i2PG3I8GKY3o4o1Y1KKGSW1s2a1c3Mg1k3q3IQ4CAMMe2Mq1KQ_1w1EM6U4i2Fm68q21s20o13OZ1u3O4g4MY16W1Ao1OK6s1EW7Um4O1221k1p2e1d2Y1~1y1X48h10R5Nj1n3FZ1JZ1x1p2Z2t2l1p21324w1n1Y3Z3c3j2w1n1e2n1EDKVKd1W1f2a1r289G3W7y1g8i2q5o2m8s3Q8i2Ac13S7R8b14h29P7l8r3p5n2f8h2~6x1F47AZ1s2Vg2Je1JW1DEd2o1v1o1b3k2X3a3v1o148i1i2a2u2y1q2Ka1Ga1k1o36O0S7i1x1Y4X1W2d1e2j1q291b4L~6Tr1DJ5n1NV9X15X5Pa1t34N0n12r27p2Gl63h25TDLz1v1JPLp1Ld2DX1HPj3p3Lf1Z1b3Vr2FRJJn1X1X3n4FJH7F4h2QX10l2X1LHHRDRVn1JLf1VW2l2KL0F15z2j2b3z2f7h7x1r2z1f2Z5d5h2t1h1NN7L1J4FAt6b4LD90DKv1s3Z4y8R5V6j1GP3l2j1~1d1EX1Od1KLl1Vr3h2b5h3t4z2y3n82Lx38f12j14t4Cz14t16v5OX14X1AP6~1Sj2e1RGf8g6b7o5X7g5dEuAn2W2h2u1v4o3l2w1p1a1JGNGj4W3v1e1b3c2j4a3p4a3z1g1l1Y1b1W1VQh2k2Z2y2r2w3Pe1h1k2J9B2f1TL9N39070T6L8n5o2"
                 :description "You can run from downtown to the top of Mt Bonnell, and back. It is a great route if you are up for hills. Overall the route will take about 10 miles, and will end you back on the Town Lake Loop."}])

(def tracks [{:name "Austin High School"
              :location "1715 W Cesar Chavez St, Austin, TX 78703"
              :description "The High School track is open to the public and is a great place to train."}
             {:name "Yellow Jacket Stadium"
              :location "3101-3189 Hargrave St, Austin, TX 78702"
              :description "A High School track in the East side of Austin."}])

(def trails [{:name "Greenbelt"
              :location "There are multiple entry points, parking at Barton Springs Pool and Barton Creek Greenbelt Trailhead are the easiest."
              :description "The greenbelt is a long trail that starts the the south west corner of Austin, it goes for around 10 miles out and back. It is very well maintained and highly trafficed on the weekends."}])

(write-file "routes.html"
            (layout "Top running routes in Austin, Texas"
                    (styled-h2 {} "Long Runs")
                    (apply str (map (fn [r] (t-div {:class "mb-8"}
                                                   (styled-h3 {} (:name r))
                                                   (styled-p {} (:description r))
                                                   (styled-a {:href (:link r)} "Link"))) long-runs))
                    (styled-h2 {} "Tracks")
                    (apply str (map (fn [r] (t-div {:class "mb-8"}
                                                   (styled-h3 {} (:name r))
                                                   (styled-p {} (:description r))
                                                   (styled-p {} (str "Location: " (:location r))))) tracks))
                    (styled-h2 {} "Trails")
                    (apply str (map (fn [r] (t-div {:class "mb-8"}
                                                   (styled-h3 {} (:name r))
                                                   (styled-p {} (:description r))
                                                   (styled-p {} (str "Location: " (:location r))))) trails))))

(write-file "about.html"
            (layout "About the austinrungroups.com"
                    (styled-h2 {} "About")
                    (styled-p {} "This website was created by "
                              (styled-a {:href "https://www.strava.com/athletes/25975441"} "Kyle Henderson")
                              ". I am the guy with a border collie named Reg.")
                    (t-br)
                    (styled-h2 {} "How to Help?")
                    (styled-p {} "The main source of truth for this website is a "
                              (styled-a {:href ""} "spreadsheet")
                              ". If you see anything off feel free to reach out to me. If you REALLY want to help, I am open to giving out edit privileges.")
                    (styled-p {} "For people with some coding experience, the website is generated at this "
                              (styled-a {:href "https://github.com/hehk/rungroups"} "repo")
                              ". The code is currently open and I will look through any pull requests, although I will not give any guarantees on a fast response time.")))

