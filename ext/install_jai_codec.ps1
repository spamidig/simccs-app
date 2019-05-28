wget http://download.java.net/media/jai/builds/release/1_1_3/jai-1_1_3-lib.zip -OutFile '.\jai-1_1_3-lib.zip'
expand-archive -path '.\jai-1_1_3-lib.zip' -destinationpath '.\'
mvn install:install-file "-Dfile=.\jai-1_1_3\lib\jai_codec.jar" "-DgroupId=javax.media" "-DartifactId=jai_codec" "-Dversion=1.1.3" "-Dpackaging=jar"
rm -Recurse .\jai-1_1_3
rm .\jai-1_1_3-lib.zip