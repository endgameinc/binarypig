-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements. See the NOTICE file distributed with this
-- work for additional information regarding copyright ownership. The ASF
-- licenses this file to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
-- http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
-- WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
-- License for the specific language governing permissions and limitations under
-- the License.
--
--  Copyright 2013 Endgame Inc.

register 'binarypig/target/binarypig-1.0-SNAPSHOT-jar-with-dependencies.jar';

SET mapred.cache.files /tmp/scripts#scripts;
SET mapred.create.symlink yes;

%default INPUT 'malware.seq'
%default OUTPUT 'clamscan.out'
%default TIMEOUT_MS '180000'
%default USE_DEVSHM 'true'

data = load '$INPUT' using com.endgame.binarypig.loaders.av.ClamScanLoader('$TIMEOUT_MS', '$USE_DEVSHM');
STORE data INTO '$OUTPUT';
