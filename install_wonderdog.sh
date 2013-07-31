#!/bin/bash

DIR=$(pushd $(dirname $0) > /dev/null; pwd ; popd > /dev/null)
LIB=$DIR/lib
mkdir -p $LIB

pushd /tmp/ > /dev/null
git clone git://github.com/infochimps-labs/wonderdog.git
cd wonderdog
mvn package
cp target/wonderdog-1.0-SNAPSHOT.jar $LIB/
popd > /dev/null
