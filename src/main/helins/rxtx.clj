;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at https://mozilla.org/MPL/2.0/.


(ns helins.rxtx

  "IO on serial ports based on jRXTX.
   
   Any IO operation will throw an exception in case of failure."

  {:author "Adam Helinski"}

  (:import (org.openmuc.jrxtx DataBits
                              FlowControl
                              Parity
                              SerialPort
                              SerialPortBuilder
                              SerialPortException
                              SerialPortTimeoutException
                              StopBits)
           (java.io InputStream
                    OutputStream)
           java.lang.AutoCloseable)
  (:refer-clojure :exclude [flush
                            read]))


;;;;;;;;;; Default values


(def defaults

  "Defaut values for keys and options used throughout this namespace."

  {:rxtx/baud-rate    9600
   :rxtx/data-bits    8
   :rxtx/flow-control :none
   :rxtx/parity       :even
   :rxtx/stop-bits    :1})



(defn- -obtain

  ;; Retrieves a value or relies on default one.

  [k hmap]

  (or (get hmap
           k)
      (get defaults
           k)))


;;;;;;;;;; Conversions


(defn- -number->data-bits

  ;; Converts a number to a DataBits enum value.

  ^DataBits

  [^long data-bits]

  (condp identical?
         data-bits
    5 DataBits/DATABITS_5
    6 DataBits/DATABITS_6
    7 DataBits/DATABITS_7
    8 DataBits/DATABITS_8))




(defn- -data-bits->number

  ;; Converts a Databits enum value to an number.

  ^long

  [data-bits]

  (condp identical?
         data-bits
    DataBits/DATABITS_5 5
    DataBits/DATABITS_6 6
    DataBits/DATABITS_7 7
    DataBits/DATABITS_8 8))




(defn- -kw->stop-bits

  ;; Converts a number to a StopBits enum value.

  ^StopBits

  [stop-bits]

  (condp identical?
         stop-bits
    :1   StopBits/STOPBITS_1
    :1.5 StopBits/STOPBITS_1_5
    :2   StopBits/STOPBITS_2))




(defn- -stop-bits->kw

  ;; Converts a StopBits enum value to a number.

  [stop-bits]

  (condp identical?
         stop-bits
    StopBits/STOPBITS_1   :1
    StopBits/STOPBITS_1_5 :1.5
    StopBits/STOPBITS_2   :2))



(defn- -kw->parity

  ;; Converts a kw to a Parity enum value.

  ^Parity

  [kw]

  (condp identical?
         kw
    :even  Parity/EVEN
    :mark  Parity/MARK
    :none  Parity/NONE
    :odd   Parity/ODD
    :space Parity/SPACE))




(defn- -parity->kw

  ;; Converts a Parity enum value to a kw.

  [^Parity parity]

  (condp identical?
         parity
    Parity/EVEN  :even
    Parity/MARK  :mark
    Parity/NONE  :none
    Parity/ODD   :odd
    Parity/SPACE :space))




(defn- -kw->flow-control

  ;; Converts a kw to a FlowControl enum value.

  ^FlowControl

  [kw]

  (condp identical?
         kw
    :none     FlowControl/NONE
    :rts-cts  FlowControl/RTS_CTS
    :xon-xoff FlowControl/XON_XOFF))




(defn- -flow-control->kw

  "Converts a FlowControl enum value to a kw."

  [^FlowControl flow-control]

  (condp identical?
         flow-control
    FlowControl/NONE     :none
    FlowControl/RTS_CTS  :rts-cts
    FlowControl/XON_XOFF :xon-xoff))




;;;;;;;;;;


(defn available-ports

  "Gets a set of available serial ports.

   Prints an error if a port is already locked but does not throw."

  []

  (into #{}
        (map str)
        (SerialPortBuilder/getSerialPortNames)))




(defn serial-port

  "Opens a serial port at the given path.

   The configuration map may contain :

      :rxtx/baud-rate
        Preferably a standard baud rate.

      :rxtx/data-bits
        How many bits per data char one of #{5 6 7 8}.

      :rxtx/flow-control
        Handshaking for preventing the sender from sending too much too fast, one of

          :none      ;; No handshaking
          :rts-cts   ;; Hardware flow-control
          :xon-xoff  ;; Software flow-control


      :rxtx/parity
        Error detection, one of :

          :even   ;; An even parity bit will be sent for each data char
          :mark   ;; Mark parity bit
          :none   ;; No parity bit
          :odd    ;; Odd parity will be sent for each data char
          :space  ;; Space parity

      :rxtx/stop-bits
        Bits sent at the end of each data char, one of #{:1 :1.5 :2}.


   Ex. (serial-port \"/dev/ttyUSB0\"
                    {:rxtx/baud-rate 2400
                     :rxtx/parity    :none})
   

   Cf. `defaults` for defaut values relative to these options."

  (^AutoCloseable
    
   [path]

   (serial-port path
                nil))


  (^AutoCloseable
    
   [path port-options]

   (.build (doto (SerialPortBuilder/newBuilder path)
             (.setBaudRate (-obtain :rxtx/baud-rate
                                    port-options))
             (.setDataBits (-number->data-bits (-obtain :rxtx/data-bits
                                                        port-options)))
             (.setStopBits (-kw->stop-bits (-obtain :rxtx/stop-bits
                                                    port-options)))
             (.setParity (-kw->parity (-obtain :rxtx/parity
                                               port-options)))
             (.setFlowControl (-kw->flow-control (-obtain :rxtx/flow-control
                                                          port-options)))))))




(defn close

  "Closes a serial port."

  [^AutoCloseable port]

  (.close port))




(defn describe 

  "Describes the current status of the given serial port by providing a map containing
   the same keys as the option for opening a serial port as well as a :rxtx/name and :rxtx/closed?.

   Cf. `serial-port`"

  [^SerialPort port]

  {:rxtx/baud-rate    (.getBaudRate port)
   :rxtx/closed?      (.isClosed port)
   :rxtx/data-bits    (-data-bits->number (.getDataBits port))
   :rxtx/flow-control (-flow-control->kw (.getFlowControl port))
   :rxtx/name         (.getPortName port)
   :rxtx/parity       (-parity->kw (.getParity port))
   :rxtx/stop-bits    (-stop-bits->kw (.getStopBits port))})




(defn input-stream

  "Given an open serial port, retrieves the associated java.io.InputStream.
  
   Should not be needed unless really needed."

  ^InputStream

  [^SerialPort port]

  (.getInputStream port))




(defn output-stream

  "Given an open serial port, retrieves the associated java.io.OutputStream.

   Should not be needed unless really needed."

  ^OutputStream

  [^SerialPort port]

  (.getOutputStream port))




;;;;;;;;;; Doing IO


(defn available-bytes

  "Estimates how many bytes are available for reading right now."

  [^SerialPort port]

  (.available (input-stream port)))




(defn flush

  "Ensures written bytes are properly flushed."

  [^SerialPort port]

  (.flush (output-stream port)))




(defn reconfigure

  "Reconfigures the given serial port on the fly by providing a map containing the same options as for
   opening a new port.

   Missing options will not resolve to default values.

   Cf. `serial-port`"

  [^SerialPort port port-options]

  (doseq [[option
           value]  port-options]
    (case option
      :rxtx/baud-rate    (.setBaudRate    port
                                          value)
      :rxtx/data-bits    (.setDataBits    port
                                          (-number->data-bits value))
      :rxtx/flow-control (.setFlowControl port
                                          (-kw->flow-control value))
      :rxtx/parity       (.setParity      port
                                          (-kw->parity value))
      :rxtx/stop-bits    (.setStopBits    port
                                          (-kw->stop-bits value))))
  nil)




(defn read

  "Reads an arbitrary amount of unsigned bytes from the serial port and returns them as a sequence.

   If a timeout is provided, unblocks once it is up and returns what has been read up to this point.
   It not, blocks until `n` have been read, no less."

  ([port n]

   (read port
         n
         0))


  ([^SerialPort port n timeout-ms]

   (.setSerialPortTimeout port
                          timeout-ms)
   (try
     (if (= n
            1)
       (list (.read (input-stream port)))
       (let [ba (byte-array n)]
         (take (.read (input-stream port)
                 ba)
               (map (fn to-unsigned [b]
                      (bit-and b
                               0xff))
                    ba))))
      (catch SerialPortTimeoutException _
        '()))))




(defprotocol IWritable

  "Anything that can be converted to a byte-array and then be written using a serial port."

  (to-byte-array [this]

    "Converts the given argument to a byte array."))




(extend-protocol IWritable


  (class (byte-array 0))

    (to-byte-array [this]
      this)


  Character

    (to-byte-array [this]
      (int this))


  clojure.lang.Seqable

    (to-byte-array [this]
      (byte-array this))


  Number

    (to-byte-array [this]
      (int this))


  String

    (to-byte-array [this]
      (.getBytes this)))




(defn write

  "Writes to the serial port anything satisfying the IWritable protocol.
  
   Are already available :

     Byte arrays
     Characters
     Sequences of any sort (seqables)
     Number converted to a single unsigned byte
     Strings"

  [^SerialPort port writable]

  (.write (output-stream port)
          (to-byte-array writable)))
