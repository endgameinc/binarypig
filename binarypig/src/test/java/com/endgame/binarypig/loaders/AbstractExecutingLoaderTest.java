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
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileRecordReader;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.easymock.EasyMock;

public class AbstractExecutingLoaderTest extends TestCase {
	AbstractExecutingLoader underTest;
	File dataDir = new File("./_data");
		
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		underTest = new AbstractExecutingLoader("") {
			@Override
			public Tuple outputToTuple(Text key, BytesWritable value, String output,boolean timedOut) {
				return TupleFactory.getInstance().newTuple(Arrays.asList((Object)key, value, output, timedOut));
			}
		};		
	}
	
	@Override
	protected void tearDown() throws Exception {
		
		if(dataDir.exists())
		{
			dataDir.delete();
		}
	}
	
	public void testPrepareToRead()
	{
		PigSplit split = null;
		RecordReader reader = EasyMock.createMock(SequenceFileRecordReader.class);
		EasyMock.replay(reader);
		
		underTest.script = "src/test/resources/echo.sh";
		
		try {
			underTest.prepareToRead(reader, split);
		} catch (IOException e) {			
			e.printStackTrace();
			fail("This should not fail");
		}
		
		assertTrue(underTest.dataDir.exists());
		assertTrue(reader == underTest.reader);
	}
	
	public void testPrepareToReadErrors()
	{
		underTest.script = "src/test/resources/doesNotExist.sh";
		
		try {
			underTest.prepareToRead(null, null);
			fail("This should fail, that file doesn't exist");
		} catch (Exception e) {			
			
		}
	}
	
	public void testGetInputFormat() throws IOException
	{
		InputFormat f = underTest.getInputFormat();
		assertEquals(f.getClass(), SequenceFileInputFormat.class);
	}
	
	public void testgetCommand()
	{
		underTest.programFile = new File("/tmp/myProgramFile"); 
		File inputFile = new File("/tmp/Hello.exe");
		
		String[] cmd = underTest.getCommand(inputFile);
		Arrays.equals(new String[]{"/tmp/myProgramFile", inputFile.getAbsolutePath()},  cmd);
	}
	
	public void testShouldContinue() throws IOException, InterruptedException
	{
		underTest.reader = EasyMock.createMock(SequenceFileRecordReader.class);
		EasyMock.expect(underTest.reader.nextKeyValue()).andReturn(true);
		EasyMock.replay(underTest.reader);
		assertTrue(underTest.shouldContinue());
		
		underTest.reader = EasyMock.createMock(SequenceFileRecordReader.class);
		EasyMock.expect(underTest.reader.nextKeyValue()).andReturn(false);
		EasyMock.replay(underTest.reader);
		assertFalse(underTest.shouldContinue());
	}
	
	public void testGetNext() throws IOException, InterruptedException
	{
		SequenceFileRecordReader reader = EasyMock.createMock(SequenceFileRecordReader.class);
		EasyMock.expect(reader.nextKeyValue()).andReturn(true);
		EasyMock.expect(reader.getCurrentKey()).andReturn(new Text("mykey1"));
		EasyMock.expect(reader.getCurrentValue()).andReturn(new BytesWritable("test123".getBytes()));
		EasyMock.replay(reader);
		
		underTest.script = "src/test/resources/echo.sh";
		underTest.prepareToRead(reader, null);
		Tuple tuple = underTest.getNext();
		assertEquals(4, tuple.size());
		assertEquals(tuple.get(0), new Text("mykey1"));
		assertEquals(tuple.get(1), new BytesWritable("test123".getBytes()));
		assertEquals(tuple.get(2), new File(underTest.dataDir, "mykey1").getAbsolutePath()+"\n");
		assertEquals(tuple.get(3), false);
		
		// returns null when no more tuples are available
		reader = EasyMock.createMock(SequenceFileRecordReader.class);
		EasyMock.expect(reader.nextKeyValue()).andReturn(false);
		EasyMock.replay(reader);
		underTest.reader = reader;
		
		tuple = underTest.getNext();
		assertNull(tuple);
	}
	
	public void testSetLocation() throws IOException
	{
		Job job = new Job();
		underTest.setLocation("/tmp/some/path", job);
		assertEquals("file:/tmp/some/path", job.getConfiguration().get("mapred.input.dir"));
	}
}
