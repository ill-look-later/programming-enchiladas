(ns enchilada.views.common
  (:require
    [hiccup.core :refer :all]
    [hiccup.page :refer :all]
    [clj-stacktrace.core :refer :all]
    [clj-stacktrace.repl :refer :all]
    [clj-time.core :refer [year now]]
    [enchilada.util.page :refer [include-async-js]]
    [enchilada.util.gist :refer :all]
    [enchilada.util.google-analytics :as ga]))

(defn url [gist & suffixes]
  (apply str "https://gist.github.com/" (login-id gist) suffixes))

(defn spinner [css-class]
  (html
    [:div#spinner {:class css-class }
      [:div {:class "spinner"}
       (for [x (range 1 13)]
          (html
            [:div {:class (str "bar" x)}]))]]))

(def home-url "/")

(def keywords "clojure,clojurescript,gist,programming,lisp,github,html5,canvas,svg,webgl,3d,javascript,algorithms,computer,science,noir,compojure,ring,middleware,lein,leiningen")

(def blurb "A ClojureScript-based HTML5 Canvas and SVG Graphics Playground, much like http://bl.ocks.org/ but specifically designed for showcasing small ClojuresScript code demos: The underlying agenda is to show how small simple functional programs can generate complex behaviour.")

(defn link [url sort-param num-items]
  (str
    url "?sort=" sort-param
    (if num-items
      (str "&n=" num-items))))

(def categories
  (array-map
    :random "Gists chosen at random"
    :latest "The most recently updated gists"
    :popular "The gists with most page views ☺"
    :favourites "Gists which have been ★'d the most"
    :unloved "Old, forgotten about, gists ☹" ))

(defn header-bar [sort-param search-param num-items home-page?]
  [:div.header
   [:section.container
    [:a.header-logo {:href home-url :title "Explore other ClojureScript Gists"} [:i.fa.fa-cutlery] " Programming Enchiladas"]
    [:span.command-bar
     [:ul.top-nav
      (for [[s title] categories
            :let [s (name s)]]
        [:li
         (if (not= s sort-param)
           [:a {:href (link home-url s num-items) :title title} s]
           s)])]
     [:form#search-form {:method "GET" :action "/"}
      (when-not (= sort-param "search")
        [:input {:type "hidden" :name "sort" :value sort-param}])
       [:span.search
        [:div [:i.fa.fa-search]]
        [:input {:type "search" :placeholder "Search..." :name "search" :value search-param}]]
       (when home-page?
         [:span.num-items
           "Display"
           [:select#num-items {:name "n"}
            (for [[k v] (array-map 10 10, 20 20, 50 50, -1 "All")]
              [:option
               (if (= k num-items)
                 {:selected true :value k}
                 {:value k})
               v])]
           "Items"
           [:script {:type "text/javascript"}
            "document.getElementById('num-items').onchange = function(){ document.getElementById('search-form').submit();};"]])]]]])

(defn footer-bar [home-page?]
  [:div.footer
   [:section.container
    [:p "Site: copyright © " (year (now)) " Richard Hull. License: "
     [:a {:href "http://creativecommons.org/licenses/by/3.0/legalcode"} "Creative Commons v3.0"]
     "&nbsp;&nbsp;Content: copyright as per respective owners else as otherwise specified."
     ]]])

(defn layout [& {:keys [title content refresh sort-param search-param count-param home-page? extra-js extra-metadata]}]
  (html5
    [:head
     [:title title]
     [:meta {:name "keywords" :content keywords}]
     [:meta {:name "description" :content blurb}]
     extra-metadata

     (when refresh
       [:meta {:http-equiv "refresh" :content refresh}])
     [:link {:rel "icon" :type "image/png" :href "/_assets/images/favicon.png"}]
     (include-css
       "//netdna.bootstrapcdn.com/font-awesome/4.0.0/css/font-awesome.css"
       "/_assets/css/default.css")
     (apply include-js
       "https://ajax.googleapis.com/ajax/libs/jquery/2.1.0/jquery.min.js"
       ;"/_assets/js/arbor.js"
       ;"/_assets/js/arbor-tween.js"
       extra-js)
     (include-async-js
       "//cdnjs.cloudflare.com/ajax/libs/react/0.8.0/react.min.js"
       "//cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"
       "/_assets/js/doc.shim.js"
       ;"http://www.senchalabs.org/philogl/PhiloGL/build/PhiloGL.js")
       "/_assets/js/PhiloGL.js")
     [:script {:type "text/x-mathjax-config"}
        "MathJax.Hub.Config({tex2jax: {inlineMath: [['$','$']]}});" ]
     (ga/google-maps-jscript-api
       (System/getenv "GOOGLEMAPS_API_KEY")
       false)
     (ga/generate-tracking-script
       (System/getenv "GA_TRACKING_ID")
       (System/getenv "SITE_URL"))]
    [:body
     [:div.wrapper (header-bar sort-param search-param count-param home-page?)]
     [:div.wrapper.content content]]
     [:div.wrapper (footer-bar home-page?)]))

(defn ribbon [text href]
  (html
    [:a.github-ribbon {:href href :title href :rel "me"} text]))

(defn- elem-partial [elem]
  (if (:clojure elem)
    [:tr
      [:td.source (h (source-str elem))]
      [:td.method (h (clojure-method-str elem))]]
    [:tr
      [:td.source (h (source-str elem))]
      [:td.method (h (java-method-str elem))]]))

(defn remove-newlines [s]
  (clojure.string/replace s "\n" " "))

(defn replace-apostrophes [s]
  (clojure.string/replace s "'" "\\'"))

(defn html-exception [ex]
  (let [ex-seq    (iterate :cause (parse-exception ex))
        exception (first ex-seq)
        causes    (rest ex-seq)]
    (html
      [:div#stacktrace
        [:div#exception
          [:h3.info (h (remove-newlines (str ex)))]
          [:table.trace
      :content
            [:tbody (map elem-partial (:trace-elems exception))]]]
        (for [cause causes :while cause]
          [:div#causes
           [:h3.info "Caused by: "
                    (h (.getName (:class cause))) " "
                    (h (remove-newlines (replace-apostrophes (:message cause))))]
           [:table.trace
             [:tbody (map elem-partial (:trimmed-elems cause))]]])])))

(defn first-filename [gist]
  (->>
    :files
    gist
    vals
    (map :filename)
    (filter #(.endsWith % ".cljs"))
    first))

(defn fork-info [gist]
  (when-let [fork (gist :fork_of)]
    (let [orig-owner (get-in fork [:owner :login])]
      [:span.text
       "— forked from "
       [:a {:href (str "/" orig-owner "/" (:id fork))} (str orig-owner "/" (first-filename gist))]])))

(defn find-next-space [text max-length]
  (let [idx (.indexOf text " " max-length)]
    (if (neg? idx)
      (count text)
      idx)))

(defn limit [text max-length]
  (if (> (count text) max-length)
    (str (subs text 0 (find-next-space text max-length)) "...")
    text))
