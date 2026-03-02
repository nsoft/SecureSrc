#!/bin/bash

# Super simple build. If this fails ensure you have Java 21 installed,
# and that it is a JDK not just a JRE.

mkdir -p distBuild

# grab the lucene code required for our search functionality
./getLucene.sh

# remember where we started
HOMEDIR=`pwd`

# compile our .java files into .class files
cd src/main/java
find . -name "*.java" > sources.txt
javac -d $HOMEDIR/distBuild @sources.txt

# grab a few files lucene needs to load it's parts.
cd ../resources
cp -a * $HOMEDIR/distBuild
cd $HOMEDIR

# create the executable jar file
jar --create --verbose --file securesrc-1.1.0.jar --main-class com.needhamsoftware.securesrc.Main -C distBuild/ .

echo 'Build complete! If you type java -jar securesrc-1.1.0.jar on the command line right now, the application should run'