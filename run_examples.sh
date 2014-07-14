#!/bin/bash

if [ $(whoami) != "root" ];
then
    echo "This script needs to run as root"
    exit 1  
fi

set -e

export PIG_HOME=/opt/pig-0.12.1
export JAVA_HOME=/usr/lib/jvm/java-7-oracle

cd /opt/binarypig

# prepare to run a script
hadoop fs -rmr /tmp/scripts /tmp/yara_rules || true
hadoop fs -put scripts yara_rules /tmp/

# create some sample data using all the src files in "src" dir as input
cd /opt/binarypig/binarypig
mkdir -p /tmp/data
find src -type f -exec cp -f {} /tmp/data/ \;

./bin/dir_to_sequencefile /tmp/data test-files

# run some jobs
cd /opt/binarypig/

pig -f examples/strings.pig -p INPUT=test-files -p OUTPUT=test-files-strings
hadoop fs -ls test-files-strings
hadoop fs -text /user/root/test-files-strings/part-m-00000

pig -f examples/yara_daemon.pig -p INPUT=test-files -p OUTPUT=test-files-yara
hadoop fs -ls test-files-yara
hadoop fs -text /user/root/test-files-yara/part-m-00000

pig -f examples/clamscan.pig -p INPUT=test-files -p OUTPUT=test-files-clamscan
hadoop fs -ls test-files-clamscan
hadoop fs -text /user/root/test-files-clamscan/part-m-00000

pig -f examples/hasher.pig -p INPUT=test-files -p OUTPUT=test-files-hasher
hadoop fs -ls test-files-hasher
hadoop fs -text /user/root/test-files-hasher/part-m-00000

pig -f examples/pehash_deamon.pig -p INPUT=test-files -p OUTPUT=test-files-pehash_deamon
hadoop fs -ls test-files-pehash_deamon
hadoop fs -text /user/root/test-files-pehash_deamon/part-m-00000

PIG_ES_LOAD="pig -f examples/es_load_json.pig 
    -p INDEX=binarypig 
    -p ES_CONFIG=/etc/elasticsearch/elasticsearch.yml 
    -p ES_JAR_DIR=/usr/share/elasticsearch/lib 
    -p ES_PLUGINS_DIR=/usr/share/elasticsearch/plugins"

# load some of the data into ES
$PIG_ES_LOAD -p INPUT=test-files-yara -p OBJ=yara
$PIG_ES_LOAD -p INPUT=test-files-clamscan -p OBJ=clamscan
$PIG_ES_LOAD -p INPUT=test-files-hasher -p OBJ=hasher
$PIG_ES_LOAD -p INPUT=test-files-pehash -p OBJ=pehash
