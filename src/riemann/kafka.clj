(ns riemann.kafka
  "Receives events from and forwards events to Kafka."
  (:require [kinsky.client :as client]
            [charred.api :as json]
            [riemann.test :as test])
  (:use [riemann.common        :only [event]]
        [riemann.core          :only [stream!]]
        [riemann.service       :only [Service ServiceEquiv ServiceStatus]]
        [clojure.tools.logging :only [debug info error]])
  (:import (org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecord)
           (java.util Collection Map)
           (org.apache.kafka.common.serialization Deserializer)
           (java.time Duration)
           (clojure.lang ILookup)))

(defn kafka
  "Returns a function that is invoked with a topic name and an optional message key and returns a stream. That stream is a function which takes an event or a sequence of events and sends them to Kafka.

  ```clojure
  (def kafka-output (kafka))

  (changed :state
    (kafka-output \"mytopic\"))
  ```

  Options:

  For a complete list of producer configuration options see https://kafka.apache.org/documentation/#producerconfigs

  - :bootstrap.servers  Bootstrap configuration, default is \"localhost:9092\".
  - :value.serializer   Value serializer, default is json-serializer.

  Example with SSL enabled:

  ```clojure
  (def kafka-output (kafka {:bootstrap.servers \"kafka.example.com:9092\"
                            :security.protocol \"SSL\"
                            :ssl.truststore.location \"/path/to/my/truststore.jks\"
                            :ssl.truststore.password \"mypassword\"}))
  ```"

  ([] (kafka {}))
  ([opts]
   (let [opts (merge {:bootstrap.servers "localhost:9092"
                      :value.serializer client/json-serializer}
                     opts)
         producer (client/producer (dissoc opts :value.serializer)
                                   (:value.serializer opts))]
     (fn make-stream [& args]
       (fn stream [event]
         (let [[topic message-key] args]
           (client/send!
             producer topic message-key event)))))))

(defn json-deserializer
  "Deserialize JSON. Let bad payload not break the consumption."
  []
  (client/deserializer
    (fn [_ payload]
      (when payload
        (try
          (json/read-json (String. payload "UTF-8") :key-fn keyword)
          (catch Exception e
            (error e "Could not decode message")))))))

(defprotocol Runner
  (is-running?
    [this]))

(defn start-kafka-thread
  "Start a kafka thread which will pop messages off the queue as long
  as running? is true"
  [running? core opts]
  (let [opts (merge {:consumer.config    {:bootstrap.servers "localhost:9092"
                                          :group.id          "riemann"}
                     :topics             ["riemann"]
                     :key.deserializer   (client/keyword-deserializer)
                     :value.deserializer (json-deserializer)
                     :poll.timeout.ms    100}
                    opts)
        ^KafkaConsumer consumer (KafkaConsumer. ^Map (client/opts->props (dissoc (:consumer.config opts) :enable.auto.commit))
                                                ^Deserializer (:key.deserializer opts)
                                                ^Deserializer (:value.deserializer opts)
                                                )
        ^Collection topics (flatten (:topics opts))
        ^Duration timeoutMs (Duration/ofMillis (:poll.timeout.ms opts))]
    (future
      (try
        (info "Subscribing to " topics "...")
        (.subscribe consumer topics)
        (while @running?
          (let [consumerRecords (.poll consumer timeoutMs)]
            (doseq [^ConsumerRecord record (iterator-seq (.iterator consumerRecords))]
              (let [value (.value record)]
                (if (map? value)
                  (stream! @core value)
                  (doseq [v value] (stream! @core v)))
                )
              )
            )
          )
        (catch Exception e
          (do
            (reset! running? false)
            (error e "Interrupted consumption")))
        (finally
          (.close consumer))))))

(defn kafka-consumer
  "Yield a kafka consumption service"
  [opts]
  (let [running? (atom true)
        core (atom nil)]
    (reify
      ILookup
      (valAt [this k not-found]
        (or (.valAt this k) not-found))
      (valAt [this k]
        (info "Looking up: " k)
        (when (= (name k) "opts") opts))
      ServiceEquiv
      (equiv? [this other]
        (= opts (:opts other)))
      Service
      (conflict? [this other]
        (= opts (:opts other)))
      (start! [this]
        (when-not test/*testing*
          (info "Starting kafka consumer")
          (start-kafka-thread running? core opts)))
      (reload! [this new-core]
        (info "Reload called, setting new core value")
        (reset! core new-core))
      (stop! [this]
        (reset! running? false)
        (info "Stopping kafka consumer"))
      Runner
      (is-running? [this]
        @running?))))
