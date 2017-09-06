# Clotty

Thin clojure wrapper around [jRxTx](https://github.com/openmuc/jrxtx) for doing
IO on serial ports.

Supports a (very) wide variety of platforms.

## Usage

First, install the native libs for your platform. Nothing to worry about, it is
fairly easy. For more information about the process or if something goes wrong,
	   go to [jRxTx](https://github.com/openmuc/jrxtx).

On a debian based distribution :

```
sudo apt install librxtx-java
```

When starting your application/repl, check if the "java.library.path" property
contains a path to the installed native libs. On debian, that should be
/usr/lib/jni :

```clj
(System/getProperty "java.library.path")
```

If this property is not properly set, you must do it yourself.

In your project file :
```clj
:jvm-opts ["-Djava.library.path=/PATH/TO/LIBS"]
```

When executing your uberjar :
```sh
java -Djava.library.path=/PATH/TO/LIBS -jar your_program.jar
```

What you can do :

```clj
(require '[clotty.core :as serial])


;; get a set of available ports on your machine
(serial/available-ports)


;; open a serial port
(def port (serial/open "/dev/ttyUSB0"
		   			   {:baud-rate 9600
					    :parity    :none}))


;; re-configure the port on the fly
(serial/configure port
 				  {:baud-rate 115200})


;; get a nice description of the port
(serial/describe port)

	
;; write something
(serial/write-bytes port
 					(.getBytes "Hello world!\n\r"))


;; read a single byte with a 2000 ms timeout
(serial/read-byte port
 				  2000)


;; check how many more bytes there is to read
(serial/available-bytes port)


;; serial ports are sequable into a byte stream
;; read 8 bytes as chars
(take 8
      (map char
	   	   port))

(first port)
(second port)


;; when you need a timeout on your sequence
(into []
 	  (comp (take 8)
	   	    (map char))
	  (serial/seq-timeout port
	   					  2000))


;; do not forget to close the port
(serial/close port)
```

Read the full [API](https://dvlopt.github.io/doc/clotty).


## License

Copyright © 2017 Adam Helinski

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
