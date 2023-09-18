(ns riemann.transport-test
  (:require [riemann.client :as client]
            [riemann.codec :as codec]
            [riemann.common :refer :all]
            [riemann.core :refer :all]
            [riemann.index :as index]
            [riemann.logging :as logging]
            [riemann.pubsub :as pubsub]
            [riemann.transport.tcp :refer :all]
            [riemann.transport.udp :refer :all]
            [charred.api :as json]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.test :refer :all])
  (:import (java.net Socket
                     SocketException
                     InetAddress)
           (java.io IOException
                    DataOutputStream)))

(logging/init)

(deftest udp-test
  (logging/suppress ["riemann.transport"
                     "riemann.core"
                     "riemann.pubsub"]
    (let [port   15555
          server (udp-server {:port port})
          sink   (promise)
          core   (transition! (core) {:services [server]
                                      :streams  [(partial deliver sink)]})
          client (client/udp-client {:port port})
          event  (event {:service "hi" :state "ok" :metric 1.23})]
      (try
        (client/send-event client event)
        (is (= (update event :time double) (deref sink 1000 :timed-out)))
        (finally
          (client/close! client)
          (stop! core))))))

(defn test-tcp-client
  [client-opts server-opts]
  (logging/suppress ["riemann.transport"
                     "riemann.core"
                     "riemann.pubsub"]
    (let [server (tcp-server server-opts)
          index (wrap-index (index/index))
          core (transition! (core) {:index index
                                    :services [server]
                                    :streams [index]})]
      (try
        (let [client (apply client/tcp-client (mapcat identity client-opts))]
          (try
            @(client/send-event client {:service "laserkat"})
            (is (= "laserkat" (-> client
                                (client/query "service = \"laserkat\"")
                                deref
                                first
                                :service)))
            (finally
              (client/close! client))))
        (finally
          (stop! core))))))

(deftest tls-test
  (let [server {:tls? true
                :key "test/data/tls/server.pkcs8"
                :cert "test/data/tls/server.crt"
                :ca-cert "test/data/tls/demoCA/cacert.pem"
                :port 15555}
        client {:tls? true
                :key "test/data/tls/client.pkcs8"
                :cert "test/data/tls/client.crt"
                :ca-cert "test/data/tls/demoCA/cacert.pem"
                :port 15555}]
    ; Works with valid config
    (test-tcp-client client server)

    (logging/suppress ["io.riemann.riemann.client.TcpTransport"]
      ; Fails with mismatching client key/cert
      (is (thrown? IOException
                   (test-tcp-client (assoc client :key (:key server))
                                    server)))

      ; Fails with non-CA client CA cert
      (is (thrown? IOException
                   (test-tcp-client (assoc client :ca-cert (:cert client))
                                    server)))
      ; Fails with mismatching server key/cert
      (is (thrown? IOException
                   (test-tcp-client client
                                    (assoc server :key (:key client))))))))

(deftest ignores-garbage
  (logging/suppress ["riemann.core"
                     "riemann.transport"
                     "riemann.pubsub"]
    (let [port   15555
          server (tcp-server {:port port})
          core   (transition! (core) {:services [server]})
          sock   (Socket. "localhost" port)]
      (try
        ; Write garbage
        (doto (.getOutputStream sock)
          (.write (byte-array (map byte (range -128 127))))
          (.flush))

        ; lmao, (.isClosed sock) is meaningless
        (is (= -1 (.. sock getInputStream read)))

        (finally
          (.close sock)
          (stop! core))))))

(deftest tcp-server-testing-test
  (let [server (tcp-server {:port 15555})]
    (binding [riemann.test/*testing* true]
      (riemann.service/start! server)
      (is (= nil @(:killer server))))))

(deftest udp-server-testing-test
  (let [server (udp-server {:port 15555})]
    (binding [riemann.test/*testing* true]
      (riemann.service/start! server)
      (is (= nil @(:killer server))))))
