#!/usr/bin/python

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

import os
import glob
import yara
import sys
import json
import codecs
import datetime

sys.stdin = os.fdopen(sys.stdin.fileno(), 'rb', 0)
sys.stdout = codecs.getwriter('UTF-8')(sys.stdout)
sys.stderr = codecs.getwriter('UTF-8')(sys.stderr)

def log(msg):
    sys.stderr.write("[%s] "%datetime.datetime.now())
    sys.stderr.write(msg)
    sys.stderr.write("\n")
    sys.stderr.flush()

def output(msg):
    sys.stdout.write(msg)
    sys.stdout.write("\n")
    sys.stdout.flush()

log("PID=%d"%os.getpid())
log("PARENT PID=%d"%os.getppid())
log("CWD=%s"%os.getcwd())

start = datetime.datetime.now()
sigs = dict([(name.replace(".yara", "").split("/")[-1], name) for name in glob.glob("yara_rules/*.yara")])
rules = yara.compile(filepaths=sigs)
end = datetime.datetime.now()
log("Loaded yara rules in %s: %s"%( end-start, json.dumps(sigs, indent=4)))

matches = []

def match_callback(data):
    log("match_callback")

    if data.get("matches", False):
        del data["matches"]
        if "strings" in data: del data["strings"]
        if "rule" in data: data["rule"] = unicode(data["rule"], errors='ignore')
        if "tags" in data and not data["tags"]: del data["tags"]
        if "meta" in data and "description" in data["meta"]:
            data["meta"]["description"] = unicode(data["meta"]["description"], errors='ignore')
        matches.append(data)
    return yara.CALLBACK_CONTINUE

while True:
    log("reading size from stdin ...")
    size = sys.stdin.readline()

    if not size: 
        log("size was null, breaking loop ...")
        break

    size = int(size.strip("\n\r"))

    log("reading %d bytes into 'data' from stdin ..."%size)
    data = sys.stdin.read(size)

    log("Performing matching on %d bytes of data ..."%len(data))
    matches = []

    start = datetime.datetime.now()
    rules.match(data=data, callback=match_callback)
    end = datetime.datetime.now()

    log("Done matching %d bytes in %s, printing results ..."%(len(data), end-start))
    output(json.dumps(matches))

log("Process Exiting")
