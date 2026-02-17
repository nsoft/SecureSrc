#bin/bash
mkdir -p build
cd build
git clone 'https://github.com/apache/lucene.git'
cd lucene
git fetch --tags
git checkout 'releases/lucene/10.3.2'
cp -a lucene/core/src/java/* ../../src/main/java
rm ../../src/main/java/module-info.java
mv ../../src/main/java/overview.html ../../src/main/java/org
cp -a lucene/core/src/resources/* ../../src/main/resources
