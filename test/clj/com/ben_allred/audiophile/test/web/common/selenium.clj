(ns com.ben-allred.audiophile.test.web.common.selenium
  (:require
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [clojure.string :as string])
  (:import
    (java.time Duration)
    (java.util.function Function)
    (org.openqa.selenium By SearchContext WebDriver WebElement)
    (org.openqa.selenium.firefox FirefoxDriver)
    (org.openqa.selenium.support.ui WebDriverWait)))

(defn create-driver [_cfg]
  (FirefoxDriver.))

(defn close! [^WebDriver driver]
  (.close driver))

(defn texts [elements]
  (map #(.getText ^WebElement %) elements))

(defn text [elements]
  (->> elements
       colls/force-seq
       texts
       (reduce (fn [builder text]
                 (.append builder text))
               (StringBuilder.))
       str))

(defn by-css [selector]
  (By/cssSelector selector))

(defn by-id [id]
  (By/id id))

(defn find-by [^SearchContext ctx ^By by]
  (.findElements ctx by))

(defn click! [^WebElement el]
  (.click el))

(defn enabled? [^WebElement el]
  (.isEnabled el))

(defn wait-for!
  ([^WebDriver driver pred]
   (wait-for! driver pred 10000))
  ([^WebDriver driver pred ms]
   (.until (WebDriverWait. driver (Duration/ofMillis ms))
           (reify Function
             (apply [_ driver]
               (when-some [result (pred driver)]
                 (Thread/sleep 10)
                 (if (instance? WebDriver result)
                   (when (enabled? result)
                     result)
                   result)))))))

(defn input! [^WebElement el text]
  (let [txt (string/join (map (constantly \backspace) (.getText el)))]
    (Thread/sleep 10)
    (.sendKeys el (into-array String [txt]))
    (Thread/sleep 10)
    (.sendKeys el (into-array String [text]))))

(defn visit! [^WebDriver driver url]
  (.get driver url)
  driver)
