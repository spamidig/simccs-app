#!/bin/bash

wget http://download.java.net/media/jai/builds/release/1_1_3/jai-1_1_3-lib.zip
unzip jai-1_1_3-lib.zip
mvn install:install-file -Dfile=./jai-1_1_3/lib/jai_codec.jar -DgroupId=javax.media -DartifactId=jai_codec -Dversion=1.1.3 -Dpackaging=jar
rm -r jai-1_1_3 jai-1_1_3-lib.zip
