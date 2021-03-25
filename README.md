# dvlopt.rxtx

[![Clojars
Project](https://img.shields.io/clojars/v/dvlopt/rxtx.svg)](https://clojars.org/dvlopt/rxtx)

Simple API for doing serial IO.

Based on [jRxTx](https://github.com/openmuc/jrxtx).

Supports a (very) wide variety of platforms.

## Installation

There is a bit of a setup but nothing to worry about, it is fairly easy.

For more information about the process or if something goes wrong, go to
[jRxTx](https://github.com/openmuc/jrxtx).

For instance, on a debian based distribution :

```
sudo apt install librxtx-java
```

When starting your application or a repl, check if the "java.library.path"
property contains a path to the installed native libs. On debian, that should be
'/usr/lib/jni' :

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

## Usage

Read the [API](https://dvlopt.github.io/doc/clojure/dvlopt/rxtx/dvlopt.rxtx.html).

In short, without error handling :

```clj
(require '[dvlopt.rxtx :as rxtx])


(with-open [port (rxtx/serial-port "/dev/ttyUSB0"
                                   {::rxtx/baud-rate 9600
                                    ::rxtx/parity    :even})]

  ;; Different things can be written to a serial port besides a byte array.
  ;;
  (rxtx/write port
              "Hello ")
  (rxtx/write port
              \w)
  (rxtx/write port
              111)
  (rxtx/write port
              [114 108 100])

  ;; Reads the answer, up to 16 unsigned bytes, but with a timeout of 2000 milliseconds.
  (println :answer
           (String. (byte-array (rxtx/read port
                                           16
                                           2000))))
                                        
  )
```


## License

Copyright © 2017 Adam Helinski

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.
