#!/bin/bash

set -e

cd `dirname $0`

PACKAGES=""
PACKAGES+="https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.2.1.deb "
PACKAGES+="http://apache.tradebit.com/pub/hadoop/common/hadoop-1.2.1/hadoop_1.2.1-1_x86_64.deb "
PACKAGES+="http://www.carfab.com/apachesoftware/pig/pig-0.12.1/pig-0.12.1.tar.gz "
PACKAGES+="http://apache.osuosl.org/maven/maven-3/3.2.2/binaries/apache-maven-3.2.2-bin.tar.gz "

for URL in $PACKAGES;
do
    FILE=`basename $URL`
    if [ ! -e $FILE ]; then
        wget $URL
    else
        echo "$FILE exists locally, not downloading again"
    fi
done
