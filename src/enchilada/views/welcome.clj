(ns enchilada.views.welcome
  (:use [compojure.core :only [defroutes GET]]
        [hiccup.core :only [html]] 
        [hiccup.element :only [image link-to]] 
        [hiccup.page :only [include-js]]))

(defroutes routes 
  (GET "/" [] "Welcome - TODO"))

