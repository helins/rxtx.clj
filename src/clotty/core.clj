(ns clotty.core

  "IO on serial ports by wrapping jrxtx"

  {:author "Adam Helinski"}

  (:import (java.io InputStream
                    OutputStream)
           (org.openmuc.jrxtx DataBits
                              StopBits
                              Parity
                              FlowControl
                              SerialPort
                              SerialPortBuilder
                              SerialPortException
                              SerialPortTimeoutException)))




;;;;;;;;;;


(defn- -to-data-bits

  "Convert a number to a DataBits enum value"

  ^DataBits

  [^long data-bits]

  (condp identical?
         data-bits
    5 DataBits/DATABITS_5
    6 DataBits/DATABITS_6
    7 DataBits/DATABITS_7
    8 DataBits/DATABITS_8))




(defn- -from-data-bits

  "Convert a Databits enum value to an number"

  ^long

  [^DataBits data-bits]

  (condp identical?
         data-bits
    DataBits/DATABITS_5 5
    DataBits/DATABITS_6 6
    DataBits/DATABITS_7 7
    DataBits/DATABITS_8 8))




(defn- -to-stop-bits

  "Convert a number to a StopBits enum value"

  ^StopBits

  [^long stop-bits]

  (condp identical?
         stop-bits
    1   StopBits/STOPBITS_1
    1.5 StopBits/STOPBITS_1_5
    2   StopBits/STOPBITS_2))




(defn- -from-stop-bits

  "Convert a StopBits enum value to a number"

  [^StopBits stop-bits]

  (condp identical?
         stop-bits
    StopBits/STOPBITS_1   1
    StopBits/STOPBITS_1_5 1.5
    StopBits/STOPBITS_2   2))




(defn- -to-parity

  "Convert a kw to a Parity enum value"

  ^Parity

  [^clojure.lang.Keyword parity]

  (condp identical?
         parity
    :even  Parity/EVEN
    :mark  Parity/MARK
    :none  Parity/NONE
    :odd   Parity/ODD
    :space Parity/SPACE))




(defn- -from-parity

  "Convert a Parity enum value to a kw"

  ^clojure.lang.Keyword

  [^Parity parity]

  (condp identical?
         parity
    Parity/EVEN  :even
    Parity/MARK  :mark
    Parity/NONE  :none
    Parity/ODD   :odd
    Parity/SPACE :space))




(defn- -to-flow-control

  "Convert a kw to a FlowControl enum value"

  ^FlowControl

  [^clojure.lang.Keyword flow-control]

  (condp identical?
         flow-control
    :none     FlowControl/NONE
    :rts-cts  FlowControl/RTS_CTS
    :xon-xoff FlowControl/XON_XOFF))




(defn- -from-flow-control

  "Convert a FlowControl enum value to a kw"

  ^clojure.lang.Keyword

  [^FlowControl flow-control]

  (condp identical?
         flow-control
    FlowControl/NONE     :none
    FlowControl/RTS_CTS  :rts-cts
    FlowControl/XON_XOFF :xon-xoff))




;;;;;;;;;;


(defmacro ^:private -read

  "Helper for `read-byte` and `read-bytes`."

  [port timeout & body]

  `(do
     (.setSerialPortTimeout ~port
                            ~timeout)
     (try
       ~@body
       (catch SerialPortTimeoutException _#
         nil)
       (catch SerialPortException e#
         (if (.isClosed ~port)
           -1
           (throw e#))))))




(defn- -lz-seq

  "Get a lazy sequence for reading a serial port byte per byte with a timeout"

  [^SerialPort port ^InputStream is timeout]

  (lazy-seq (let [b (-read port
                           timeout
                           (.read is))]
              (when (and (some? b)
                         (not= b
                               -1))
                (cons b
                      (-lz-seq port
                               is
                               timeout))))))




(defmacro ^:private -write

  "Helper for `write-byte` and `write-bytes`"

  [port & body]

  `(try
     ~@body
     true
     (catch SerialPortException e#
       (if (closed? ~port)
         false
         (throw e#)))))




;;;;;;;;;;


(defprotocol IWrapper

  "Wrap a SerialPort object"

  (raw [this]

    "Get the raw SerialPort object")


  (describe [this]

    "Describe the serial port as a map")


  (configure [this config]

    "Re-configure the serial port (eg. change baud-rate) on the fly.

     Throws an IOException if something goes wrong.

     Cf. `open`")


  (available-bytes [this]

    "Get the number of bytes that can be read from the buffer before blocking.

     Throws an IOException if something goes wrong")


  (read-byte [this]
             [this timeout]

    "Read a single byte.
    
     A timeout in milliseconds can be provided, 0 meaning forever.
    
     Returns nil if the timeout is elapsed
             -1  if the port is closed

     Throws an IOException if something goes wrong.")


  (read-bytes [this]
              [this ba]
              [this ba timeout]
              [this ba offset n]
              [this ba offset n timeout]

  "Read several bytes from the given serial port and write them a byte array.
  
   A timeout in milliseconds can be provided, 0 meaning forever.
  
   The offset for the byte array and the number of bytes to read are optionals as well.

   If nothing else than the port is provided, reads the number of availables bytes in the
   buffer into a newly created byte array.
  
   Returns nil          if the timeout is elapsed
           -1           if the port is closed
           a byte array if only the port is provided
  
   Throws an IOException if something goes wrong.
  
   Cf. `available-bytes`")


  (skip [this n]
        [this n timeout]

    "Skip over and discards `n` bytes of data. Just as reads, this function will
     block if `n` is greater than the number of bytes in the buffer.

     A timeout in milliseconds can be provided, 0 meaning forever.
    
     Returns the actual number of bytes skipped.
             nil if the timeout is elapsed

     Throws an IOException if something goes wrong.")


  (seq-timeout [this timeout]

    "Convert to a sequence for reading byte per byte with a timeout, 0 meaning forever.

     Cf. `read-byte`")


  (write-byte [this b]

    "Write a single byte to the given port.

     Returns true  if the write is successfull
             false if the port is closed

     Throws an IOException if something goes wrong.")


  (write-bytes [this ba]
               [this ba offset n]

    "Write a byte array to the given port.
    
     An offset and a number of bytes can be provided.

     Returns true  if the write is successfull
             false if the port is closed

     Throws an IOException if something goes wrong.")


  (closed? [this]

    "Is the serial port closed?")


  (close [this]

    "Close the serial port.
    
     Throws an IOException if something goes wrong."))




(deftype Wrapper [^SerialPort   port
                  ^InputStream  is
                  ^OutputStream os]

  IWrapper


    (raw [_]
      port)


    (describe [_]
      {:port         (.getPortName                        port)
       :closed?      (.isClosed                           port)
       :baud-rate    (.getBaudRate                        port)
       :data-bits    (-from-data-bits    (.getDataBits    port))
       :stop-bits    (-from-stop-bits    (.getStopBits    port))
       :parity       (-from-parity       (.getParity      port))
       :flow-control (-from-flow-control (.getFlowControl port))})


    (configure [_ config]
      (doseq [[k
               v] config]
        (case k
          :baud-rate    (.setBaudRate    port
                                         v)
          :data-bits    (.setDataBits    port
                                         (-to-data-bits v))
          :stop-bits    (.setStopBits    port
                                         (-to-stop-bits v))
          :parity       (.setParity      port
                                         (-to-parity v))
          :flow-control (.setFlowControl port
                                         (-to-flow-control v))))
      port)


    (available-bytes [_]
      (.available is))


    (read-byte [_ timeout]
      (-read port
             timeout
             (.read is)))


    (read-byte [this]
      (read-byte this
                 0))


    (read-bytes [_ ba timeout]

     (-read port
            timeout
            (.read is
                   ba)))


    (read-bytes [this ba]

     (read-bytes this
                 ba
                 0))


    (read-bytes [this]

     (let [n (available-bytes port)]
       (when (pos? n)
         (let [ba (byte-array n)]
           (read-bytes this
                       ba
                       0)
           ba))))


    (read-bytes [_ ba offset n timeout]

     (-read port
            timeout
            (.read is
                   ba
                   offset
                   n)))


    (read-bytes [this ba offset n]

     (read-bytes this
                 ba
                 offset
                 n
                 0))


    (skip [_ n timeout]
      (-read port
             timeout
             (.skip is
                    n)))


    (skip [this n]
      (skip this
            n
            0))


    (seq-timeout [_ timeout]
      (-lz-seq port
               is
               timeout))


    (write-byte [_ n]
      (-write port
              (.write os
                      ^long n)))


    (write-bytes [_ ba]
      (-write port
              (.write os
                      ^bytes ba)))


    (write-bytes [_ ba offset n]
      (-write port
              (.write os
                      ba
                      offset
                      n)))


    (closed? [_]
      (.isClosed port))


    (close [_]
      (.close port))




  clojure.lang.Seqable

    (seq [_]
      (-lz-seq port
               is
               0)))




;;;;;;;;;;


(defn available-ports

  "Get a set of available serial ports.

   Prints an error if a port is already locked but does not throw."

  []

  (into #{}
        (map str)
        (SerialPortBuilder/getSerialPortNames)))




(defn open

  "Given a path, open it at a serial port.

   The configuration map may contains

      :baud-rate
          Preferably a standard baud rate

      :data-bits
          How many bits per data char
          #{5 6 7 8}

      :stop-bits
          Bits sent at the end of each data char
          #{1 1.5 2}

      :parity
          Error detection
          #{:even   ;; An even parity bit will be sent for each data char
            :mark   ;; Mark parity bit
            :none   ;; No parity bit
            :odd    ;; Odd parity will be sent for each data char
            :space  ;; Space parity
            }

      :flow-control
          Handshaking for preventing the sender from sending too much too fast
          #{:none      ;; No handshaking
            :rts-cts   ;; Hardware flow-control
            :xon-xoff  ;; Software flow-control
            }
   

   Throws
      com.openmuc.jrxtx.PortNotFoundException if port is busy or doesn't exist
      IOException                             otherwise"

  ^Wrapper

  [path & [{:as   config
            :keys [baud-rate
                   data-bits
                   stop-bits
                   parity
                   flow-control]
            :or   {baud-rate    9600
                   data-bits    8
                   stop-bits    1
                   parity       :even
                   flow-control :none}}]]

  (let [raw ^SerialPort (-> (SerialPortBuilder/newBuilder path)
                            (.setBaudRate    baud-rate)
                            (.setDataBits    (-to-data-bits    data-bits))
                            (.setStopBits    (-to-stop-bits    stop-bits))
                            (.setParity      (-to-parity       parity))
                            (.setFlowControl (-to-flow-control flow-control))
                            .build)]
    (Wrapper. raw
              (.getInputStream raw)
              (.getOutputStream raw))))
