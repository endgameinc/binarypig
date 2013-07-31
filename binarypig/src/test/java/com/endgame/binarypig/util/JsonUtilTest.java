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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.pig.data.NonSpillableDataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JsonUtilTest extends TestCase {
	public void testIt() {
		JsonUtil ju = new JsonUtil();
		
		JSONArray list = new JSONArray();
		list.add("1");
		list.add("2");
		list.add("3");
		
		JSONObject value = new JSONObject();
		value.put("name", "Jason");
		value.put("null", null);
		value.put("num", 7);
		value.put("bool", true);
		value.put("list", list);
		
		Object wrapped = ju.wrap(value);
		assertTrue(wrapped instanceof Map);
		
		Map map = (Map)wrapped;
		assertEquals(map.get("name"), "Jason");
		assertNull(map.get("null"));
		assertEquals(map.get("num"), "7");
		assertEquals(map.get("bool"), "true");		
		List<Tuple> tuples= Arrays.asList(
				TupleFactory.getInstance().newTuple((Object)"1"),
				TupleFactory.getInstance().newTuple((Object)"2"),
				TupleFactory.getInstance().newTuple((Object)"3")
		);
		
		assertEquals(map.get("list"), new NonSpillableDataBag(tuples) );
		
	}
}
