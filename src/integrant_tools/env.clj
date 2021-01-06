(ns integrant-tools.env
  "Functions for pulling values from environment variables."
  (:require [clojure.string :as string]))

;; See https://github.com/duct-framework/core/blob/master/src/duct/core/env.clj

(defmulti coerce
  "Coerce a value to the type referenced by a symbol. By default supports
  `Int` and `Str`."
  (fn [x type] type))

(defmethod coerce 'Int [x _]
  (Long/parseLong x))

(defmethod coerce 'Double [x _]
  (Double/parseDouble x))

(defmethod coerce 'Str [x _]
  (str x))

(def bool-m {"true" true
             "t" true
             "yes" true
             "y" true
             "false" false
             "f" false
             "no" false
             "n" false
             "" false})

(defmethod coerce 'Bool [x _]
  (let [x (some-> x string/lower-case)]
    (cond
      (nil? x) false
      (some? (bool-m x)) (bool-m x)
      :else (throw (ex-info (str "Could not coerce '" (pr-str x) "' into a boolean."
                                 "Must be one of: \"true\", \"t\", \"false\", \"f\","
                                 "\"yes\", \"y\", \"no\", \"n\", \"\" or nil.")
                            {:value x, :coercion 'Bool})))))

(def ^:dynamic *env*
  (into {} (System/getenv)))

(defn env
  "Resolve an environment variable by name. Optionally accepts a type for
  coercion, and a keyword option, `:or`, that provides a default value if the
  environment variable is missing.

  For example:
      {:port #env [\"PORT\" Int :or 3000]}"
  ([name]
   (if (vector? name)
     (apply env name)
     (*env* name)))
  ([name type & options]
   (if (keyword? type)
     (apply env name 'Str type options)
     (let [{default :or} options
           value (*env* name)]
       (if (nil? value)
         default
         (coerce value type))))))
