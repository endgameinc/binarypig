package com.endgame.binarypig.loaders;

import java.io.File;
import java.io.IOException;

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
import org.easymock.EasyMock;

public class BinarySequenceFileLoaderTest extends TestCase {
	BinarySequenceFileLoader underTest;
	File dataDir = new File("./_data");
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		underTest = new BinarySequenceFileLoader();
	}
	
	public void testPrepareToRead() throws IOException
	{
		PigSplit split = null;
		RecordReader reader = EasyMock.createMock(SequenceFileRecordReader.class);
		EasyMock.replay(reader);
		underTest.prepareToRead(reader, null);
		assertTrue(reader == underTest.reader);
	}
	
	public void testGetInputFormat() throws IOException
	{
		InputFormat f = underTest.getInputFormat();
		assertEquals(f.getClass(), SequenceFileInputFormat.class);
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
		
		underTest.prepareToRead(reader, null);
		Tuple tuple = underTest.getNext();
		assertEquals(2, tuple.size());
		assertEquals(tuple.get(0), new Text("mykey1"));
		assertEquals(tuple.get(1), new BytesWritable("test123".getBytes()));
		
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
