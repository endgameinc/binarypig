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


package com.endgame.binarypig.util;

import java.util.Map;

import org.apache.pig.data.DataBag;
import org.apache.pig.data.NonSpillableDataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.collect.Maps;

public class JsonUtil {
	
	final protected TupleFactory tupleFactory = TupleFactory.getInstance();
	
	/**
	 * Note: copied from elephantbird pig JsonLoader
	 */
	boolean isNestedLoadEnabled = true;
	public Object wrap(Object value) {

		if (isNestedLoadEnabled && value instanceof JSONObject) {
			return walkJson((JSONObject) value);
		} else if (isNestedLoadEnabled && value instanceof JSONArray) {

			JSONArray a = (JSONArray) value;
			DataBag mapValue = new NonSpillableDataBag(a.size());
			for (int i = 0; i < a.size(); i++) {
				Tuple t = tupleFactory.newTuple(wrap(a.get(i)));
				mapValue.add(t);
			}
			return mapValue;

		} else {
			return value != null ? value.toString() : null;
		}
	}
	
	/**
	 * Note: copied from elephantbird pig JsonLoader
	 */
	public Map<String, Object> walkJson(JSONObject jsonObj) {
		Map<String, Object> v = Maps.newHashMap();
		for (Object key : jsonObj.keySet()) {
			v.put(key.toString(), wrap(jsonObj.get(key)));
		}
		return v;
	}
}
