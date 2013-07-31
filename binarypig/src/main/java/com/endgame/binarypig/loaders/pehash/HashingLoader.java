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

package com.endgame.binarypig.loaders.pehash;

import java.util.ArrayList;
import java.util.Map;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import com.endgame.binarypig.loaders.AbstractExecutingLoader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Hello world!
 *
 */
public class HashingLoader extends AbstractExecutingLoader
{
	ArrayList<Object> protoTuple = new ArrayList<>();
	TupleFactory tupleFactory = TupleFactory.getInstance();
	Gson gson = new Gson();
	
	public HashingLoader(){
		super("scripts/hasher.py");
	}
	
	public HashingLoader(String timeoutMS){
       super("scripts/hasher.py", timeoutMS);
	}
	
	public HashingLoader(String timeoutMS, String useDevShm){
		super("scripts/hasher.py", timeoutMS, useDevShm);
	}


	@Override
	public Tuple outputToTuple(Text key, BytesWritable value, String output, boolean timedOut) {		
		protoTuple.clear();
		protoTuple.add(key.toString());
		protoTuple.add(timedOut);
		
		// when adding, might want to consider doing explicit casts from Writables to Pig datatypes - does not appear to be needed at this time
		//This is the spot to do the generic JSON loading. some override function for data formatting would be here
		try{
			Map<String, String> events = gson.fromJson(output, new TypeToken<Map<String, String>>(){}.getType());
			protoTuple.add(events.get("md5"));
			protoTuple.add(events.get("sha1"));
			protoTuple.add(events.get("sha256"));
			protoTuple.add(events.get("sha512"));
			protoTuple.add(events.get("pe_hash"));
		} catch (Exception e){
			
			protoTuple.add("");
			protoTuple.add("");
			protoTuple.add("");
			protoTuple.add("");
			protoTuple.add("");
		}
		return tupleFactory.newTuple(protoTuple);
	}	
}
