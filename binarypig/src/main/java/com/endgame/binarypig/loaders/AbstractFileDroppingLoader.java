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
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileRecordReader;
import org.apache.pig.FileInputLoadFunc;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import com.endgame.binarypig.util.StreamUtils;
import com.google.common.base.Stopwatch;

/**
 * Hello world!
 *
 */
public abstract class AbstractFileDroppingLoader extends FileInputLoadFunc 
{
	long timeoutMS = Long.MAX_VALUE;
	File dataDir;
	File workingDir;
	
	//might not need this.
	SequenceFileRecordReader<Text, BytesWritable> reader;
	Text key;
	BytesWritable value;
	
	ArrayList<Object> protoTuple = new ArrayList<>();
	TupleFactory tupleFactory = TupleFactory.getInstance();
	
	long numRecords = 0;
	Stopwatch copyOverhead = new Stopwatch();
	Stopwatch execOverhead = new Stopwatch();
	Stopwatch deleteOverhead = new Stopwatch();
	Stopwatch tupleCreationOverhead = new Stopwatch();
	Stopwatch totalOverhead = new Stopwatch();
	
	boolean useDevShm;
	
	public AbstractFileDroppingLoader(){
		this(Long.toString(Long.MAX_VALUE));
	}
	
	public AbstractFileDroppingLoader(String timeoutMS){
		this(timeoutMS, "false");		
	}
	
	public AbstractFileDroppingLoader(String timeoutMS, String useDevShm){
		this.timeoutMS = Long.parseLong(timeoutMS);
		this.useDevShm = Boolean.parseBoolean(useDevShm);
	}
	
	@Override
	public void prepareToRead(RecordReader reader, PigSplit split) throws IOException {
		// cast any reader object into a SequenceFileRecordReader. We do not care about the split. We keep with sequencefile, as our base
		// filetype is a sequence file. All the fanciness is post-sequence.
		
		try {
			FileSplit filesplit = (FileSplit)split.getWrappedSplit();
			System.out.println("filesplit: "+filesplit.getPath());
		}catch (NullPointerException e) {}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		this.reader = (SequenceFileRecordReader) reader;
		workingDir = new File(".").getAbsoluteFile();
		makeDataDir();
		init();
	}
	
	public void makeDataDir()
	{
		if(useDevShm) {
			dataDir = new File("/dev/shm/"+UUID.randomUUID().toString());
		}
		else {
			// data dir is here so the input file doesn't clobber any ofn the files in the CWD
			dataDir = new File(workingDir, "_data");
		}
		
		if(!dataDir.mkdir() && !dataDir.exists())
		{
			throw new RuntimeException("Could not create data dir: "+dataDir);
		}
		dataDir.deleteOnExit();
	}
	
	@Override
	public InputFormat getInputFormat() throws IOException {
		// Define the kind of file in HDFS that we are dealing with. Here, we define that we wish to operate on a sequence file.
		return new SequenceFileInputFormat();
	}
	
	boolean shouldContinue() throws IOException
	{
		//Determine if there is another key/value
		try {
			return reader.nextKeyValue();
		} catch (InterruptedException e){
			throw new IOException(e);
		}
	}
	
	public static void dumpStat(String name, Stopwatch stat, long totalUsec, long records)
	{
		long uSec = stat.elapsedTime(TimeUnit.MICROSECONDS);
		double percentTotal = (100.0*uSec)/totalUsec; 
		double usecPerRecord = (double)uSec/(double)records;
		System.err.printf(String.format("STAT:%s: %.4f uSec/rec, %s, %.2f percent total overhead\n", name, usecPerRecord, stat.toString(4), percentTotal));
	}
	
	public void dumpStats()
	{
		System.err.println("workDir     = "+workingDir);
		System.err.println("dataDir     = "+dataDir);
		System.err.println("useDevShm     = "+useDevShm);
		System.err.println("numRecords     = "+numRecords);
		
		long usec = totalOverhead.elapsedTime(TimeUnit.MICROSECONDS);
		dumpStat("copyOverhead", copyOverhead, usec, numRecords);
		dumpStat("deleteOverhead", deleteOverhead, usec, numRecords);
		dumpStat("execOverhead", execOverhead, usec, numRecords);
		dumpStat("tupleCreationOverhead", tupleCreationOverhead, usec, numRecords);
		dumpStat("totalOverhead", totalOverhead, usec, numRecords);
	}

	@Override
	public Tuple getNext() throws IOException {
		if(!shouldContinue()){
			dumpStats();
			cleanUp();
			return null;
		}
		totalOverhead.start();
		
		key = reader.getCurrentKey();
		value = reader.getCurrentValue();
		
		++numRecords;
		
		copyOverhead.start();
		File binaryFile = new File(dataDir, key.toString());
		binaryFile.deleteOnExit();
		try {
			StreamUtils.writeToFile((BytesWritable) value, binaryFile);
			copyOverhead.stop();
			
			execOverhead.start();
			Tuple tuple = processFile(key, value, binaryFile);
			execOverhead.stop();
			
			return tuple;
		} catch (Exception e) {
			throw new IOException(e);
		}
		finally
		{
			// do our best to prevent orphan files (important when using /dev/shm)
			deleteOverhead.start();
			binaryFile.delete();
			deleteOverhead.stop();
			totalOverhead.stop();
		}
	}
	
	public void cleanUp() {		
		try {
			FileUtils.deleteDirectory(dataDir);
		} catch (NullPointerException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void init() throws IOException {}
	
	public abstract Tuple processFile(Text key, BytesWritable value, File binaryFile) throws IOException;
	
	@Override
	public void setLocation(String location, Job job) throws IOException {
		// implement distributed cache loading here.
		FileInputFormat.setInputPaths(job, location);
	}
	
	public File getDataDir() {
		return dataDir;
	}
	
	public File getWorkingDir() {
		return workingDir;
	}
	
	public ArrayList<Object> getProtoTuple() {
		return protoTuple;
	}
	
	public TupleFactory getTupleFactory() {
		return tupleFactory;
	}
	
	public long getTimeoutMS() {
		return timeoutMS;
	}
	
	public void setTimeoutMS(long timeoutMS) {
		this.timeoutMS = timeoutMS;
	}
}
