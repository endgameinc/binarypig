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

import junit.framework.TestCase;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.Tuple;

import com.google.gson.JsonObject;

public class HashingLoaderTest extends TestCase {
	public void testIt() throws ExecException {
		HashingLoader underTest = new HashingLoader();
		assertEquals("scripts/hasher.py", underTest.getScript());
		
		underTest = new HashingLoader("7777");
		assertEquals("scripts/hasher.py", underTest.getScript());
		assertEquals(7777L, underTest.getTimeoutMS());
		
		JsonObject res = new JsonObject();
		res.addProperty("md5", "mymd5");
		res.addProperty("sha1", "mysha1");
		res.addProperty("sha256", "mysha256");
		res.addProperty("sha512", "mysha512");
		res.addProperty("pe_hash", "mypehash");
		
		
		Tuple tuple = underTest.outputToTuple(new Text("mykey"), new BytesWritable("data".getBytes()), res.toString(), false);
		assertEquals(7, tuple.size());
		assertEquals("mykey", tuple.get(0));
		assertEquals(false, tuple.get(1));
		assertEquals("mymd5", tuple.get(2));
		assertEquals("mysha1", tuple.get(3));
		assertEquals("mysha256", tuple.get(4));
		assertEquals("mysha512", tuple.get(5));
		assertEquals("mypehash", tuple.get(6));
		
		tuple = underTest.outputToTuple(new Text("mykey"), new BytesWritable("data".getBytes()), "NOT JSON", false);
		assertEquals(7, tuple.size());
		assertEquals("mykey", tuple.get(0));
		assertEquals(false, tuple.get(1));
		assertEquals("", tuple.get(2));
		assertEquals("", tuple.get(3));
		assertEquals("", tuple.get(4));
		assertEquals("", tuple.get(5));
		assertEquals("", tuple.get(6));
	}
}
