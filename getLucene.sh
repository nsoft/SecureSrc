#bin/bash
mkdir -p build
cd build
# Get a copy of the lucene source code (requires git installed, google it if you don't know what that is)
git clone 'https://github.com/apache/lucene.git'
cd lucene
git fetch --tags

# this is the version of lucene we are using
git -c advice.detachedHead=false checkout 'releases/lucene/10.3.2'

# we only use 3 parts of lucene: Core, Queries, and Query Parsers.
cp -a lucene/core/src/java/* ../../src/main/java
cp -a lucene/queries/src/java/* ../../src/main/java
cp -a lucene/queryparser/src/java/* ../../src/main/java

# This query parser has dependency problems and would force us to use
# a bunch of other modules, so we delete it.
rm -rf ../../src/main/java/org/apache/lucene/queryparser/xml # avoid needing sandbox

# don't want this, it's specific to lucene, not appropriate for us
rm ../../src/main/java/module-info.java

# this is an overview of Lucene, not us so move it down one.
mv ../../src/main/java/overview.html ../../src/main/java/org

# copy some bits lucene needs to load it's classes
mkdir -p ../../src/main/resources
cp -a lucene/core/src/resources/* ../../src/main/resources
