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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IOUtils;

import junit.framework.TestCase;

public class StreamUtilsTest extends TestCase {

	public void testWriteToFile() throws Exception{
			
		File binaryFile = new File("/tmp/"+UUID.randomUUID().toString());
		binaryFile.deleteOnExit();
				
		assertFalse(binaryFile.exists());
		BytesWritable value = new BytesWritable("This is a test".getBytes());
		
		try {
			StreamUtils.writeToFile(value, binaryFile);
		} catch (IOException e) {
			e.printStackTrace();
			fail("This should not throw an Exception");
		}
		
		assertTrue(binaryFile.exists());
		
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		IOUtils.copyBytes(new FileInputStream(binaryFile), bytes, 100, true);
		assertEquals("This is a test", new String(bytes.toByteArray()));
		}
}
