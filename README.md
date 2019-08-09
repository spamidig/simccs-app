# simccs-client
Before install:
jai_codec must be downloaded and installed manually to setup the openmap external library first with maven. Please run the following scripts:
<br></br>For Unix: ./ext/install_jai_codec.sh
<br></br>For Windows: ./ext/install_jai_codec.ps1 (Note the script from windows should be run with PowerShell.) 
<br></br>Alternatively, you could also manually add the ./ext/openmap.jar to your project as an external library. (This is not recommended and untested.)
<br></br>
<br></br>Deploy a single jar file: mvn -P pack-jar install
<br></br>Deploy a single jar file of SimCCS Editor: mvn -P pack-jar-without-gateway install

08 Nov 2017 - airavata v 0.16 does not include required classes, thus airavata must be installed from source: https://github.com/apache/airavata  This will be built as version 0.17-SNAPSHOT in your local repository.  When version 0.17 is released, this will no longer be required and the pom will be updated to reflect pulling it from the central repo.

# commons-math4 jar

To install the appropriate commons-math4 SNAPSHOT jar, run the following in this directory:

```
mvn install:install-file -Dfile=./ext/commons-math4-4.0-20170916.175014-666.jar \
    -DgroupId=org.apache.commons -DartifactId=commons-math4 \
    -Dversion=4.0-20170916.175014-666 -Dpackaging=jar
```
