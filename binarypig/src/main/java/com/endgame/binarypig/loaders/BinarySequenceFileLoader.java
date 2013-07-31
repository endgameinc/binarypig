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

import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileRecordReader;
import org.apache.pig.FileInputLoadFunc;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

/**
 * Hello world!
 *
 */
public class BinarySequenceFileLoader extends FileInputLoadFunc 
{
	TupleFactory tupleFactory = TupleFactory.getInstance();
	ArrayList<Object> protoTuple = new ArrayList<Object>(2);
	
	SequenceFileRecordReader<Text, BytesWritable> reader;
	Text key;
	BytesWritable value;
	
	public BinarySequenceFileLoader(){}

	@Override
	public InputFormat getInputFormat() throws IOException {
		// Define the kind of file in HDFS that we are dealing with. Here, we define that we wish to operate on a sequence file.
		return new SequenceFileInputFormat<Text, BytesWritable>();
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

	@Override
	public Tuple getNext() throws IOException {
		if(!shouldContinue())
			return null;
		
		key = reader.getCurrentKey();
		value = reader.getCurrentValue();
		
		protoTuple.clear();
		protoTuple.add(key);
		protoTuple.add(value);
		return tupleFactory.newTuple(protoTuple);
	}

	@Override
	public void prepareToRead(RecordReader reader, PigSplit split) throws IOException {
		this.reader = (SequenceFileRecordReader<Text, BytesWritable>) reader;
	}

	@Override
	public void setLocation(String location, Job job) throws IOException {
		FileInputFormat.setInputPaths(job, location);		
	}
}
