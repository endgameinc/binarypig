#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
#  Copyright 2013 Endgame Inc.
# --------------------------------------------------------------------------------

# Simple script to change the default search analyzer
# index must be closed to perform the update
#

if [ $# -ne 2 ]; then
    echo "usage: $0 [host] [index]"
    exit 1
fi

HOST="$1"
INDEX="$2"
curl -XPOST "$HOST:9200/$INDEX/_close"
curl -XPUT "$HOST:9200/$INDEX/_settings" -d '
{
"analysis": {
    "analyzer": {
      "default_search": {
        "type": "custom",
        "tokenizer": "nGramTokenizer",
        "filter": "lowercase"
      }
   }
 }
}
'
curl -XPOST "$HOST:9200/$INDEX/_open"
