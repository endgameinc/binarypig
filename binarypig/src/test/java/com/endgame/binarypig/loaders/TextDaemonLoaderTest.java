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

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.pig.data.Tuple;

import com.endgame.binarypig.loaders.TextDaemonLoader;
import com.endgame.binarypig.util.Server;

public class TextDaemonLoaderTest extends TestCase {
	
	TextDaemonLoader underTest;
	Server server;
	
	@Override
	protected void setUp() throws Exception {
		server = new Server();
		underTest = new TextDaemonLoader("1111");
		underTest.endpoint = new InetSocketAddress("127.0.0.1", server.getPort());
	}
	
	public void testNormal() throws Exception
	{
		server.setReply("{\"matches\":[]}\n");
		server.start();
		try {
			underTest.init(); 
			Tuple tuple = underTest.processFile(new Text("mykey"), new BytesWritable(), new File("/tmp/path/to/file"));
			assertEquals(3, tuple.size());
			assertEquals("mykey", tuple.get(0));
			assertEquals(false, tuple.get(1));
			assertEquals("{\"matches\":[]}", tuple.get(2));
			assertEquals("/tmp/path/to/file", server.getSent());
			
		} catch (IOException e) {
			fail("This should not throw an exception");
		}
	}
	
	public void testTimeout() throws Exception
	{
		// server sleeps for 10 sec after connection made
		server.setSleepMS(10000);
		server.setReply("/tmp/path/to/file");
		server.start();
		try {
			
			underTest.setTimeoutMS(1); // Loader has no patience.
			underTest.init(); 
			
			Tuple tuple = underTest.processFile(new Text("mykey"), new BytesWritable(), new File("/tmp/path/to/file"));
			assertEquals(3, tuple.size());
			assertEquals("mykey", tuple.get(0));
			assertEquals(true, tuple.get(1));
			assertEquals("", tuple.get(2));
			assertEquals(null, server.getSent());
			
		} catch (IOException e) {
			fail("This should not throw an exception");
		}
	}
	
	public void testNoServer() throws Exception
	{
		try {
			// Hopefully nothing it listening on this port...
			underTest.endpoint = new InetSocketAddress("127.0.0.1", 63676);
			underTest.init(); 
			underTest.processFile(new Text("mykey"), new BytesWritable(), new File("/tmp/path/to/file"));
			fail("This should throw an exception");
		} catch (ConnectException e) {}
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		underTest.cleanUp();
		server.interrupt();
		server.join();
	}
}
