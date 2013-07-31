/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *  Copyright 2013 Endgame Inc.
 *
 */


package com.endgame.binarypig.loaders;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.pig.data.NonSpillableDataBag;
import org.apache.pig.data.Tuple;
import org.json.simple.parser.JSONParser;

import com.endgame.binarypig.util.JsonUtil;

public class ExecutingJsonLoader extends AbstractExecutingLoader 
{
	JSONParser jsonParser = new JSONParser();
	JsonUtil jsonUtil = new JsonUtil();
	
	public ExecutingJsonLoader(String script) {
		super(script);
	}
	
	public ExecutingJsonLoader(String script, String timeoutMS) {
		super(script, timeoutMS);
	}
	
	public ExecutingJsonLoader(String script, String timeoutMS, String useDevShm) {
		super(script, timeoutMS, useDevShm);
	}
	
	public Tuple outputToTuple(Text key, BytesWritable value, String output, boolean timedOut)
	{
		protoTuple.clear();
		protoTuple.add(key.toString());
		protoTuple.add(timedOut);
		
		// when adding, might want to consider doing explicit casts from Writables to Pig datatypes - does not appear to be needed at this time
		//This is the spot to do the generic JSON loading. some override function for data formatting would be here
		try{
			protoTuple.add(jsonUtil.wrap(jsonParser.parse(output)));
		} catch (Exception e){
			protoTuple.add(new NonSpillableDataBag());
		}
		
		return tupleFactory.newTuple(protoTuple);
	}
}
