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

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.Tuple;

import com.endgame.binarypig.util.ProgramExector;
import com.endgame.binarypig.util.StreamUtils;
import com.google.common.base.Stopwatch;

/**
 * Hello world!
 *
 */
public abstract class AbstractExecutingLoader extends AbstractFileDroppingLoader 
{
	String script = null;
	File programFile;
	
	Stopwatch tupleCreationOverhead = new Stopwatch();
	
	public AbstractExecutingLoader(String script){
		this(script, Long.toString(Long.MAX_VALUE));
	}
	
	public AbstractExecutingLoader(String script, String timeoutMS){
		this(script, timeoutMS, "false");		
	}
	
	public AbstractExecutingLoader(String script, String timeoutMS, String useDevShm){
		super(timeoutMS, useDevShm);
		this.script = script;
	}
	
	@Override
	public void prepareToRead(RecordReader reader, PigSplit split) throws IOException {
		super.prepareToRead(reader, split);
		ensureProgramFilePermissions();
	}
	
	public void ensureProgramFilePermissions()
	{
		programFile = new File(workingDir, this.script);
		if(programFile.exists()) {
			programFile.setExecutable(true);
			programFile.setReadable(true);
		}
		else{
			throw new RuntimeException("Program doesn't exist: "+programFile);
		}
	}
	
	String[] getCommand(File inputFile)
	{
		return new String[]{programFile.getPath(), inputFile.getPath()};
	}
	
	public void dumpStats()
	{
		System.err.println("script      = "+script);
		System.err.println("programFile = "+programFile);
		super.dumpStats();
	}

	@Override
	public Tuple processFile(Text key, BytesWritable value, File binaryFile) throws IOException{
		
		ProgramExector exec = new ProgramExector(getCommand(binaryFile), timeoutMS);
		exec.start();
		waitOnProgramExecutor(exec);
		String output = StreamUtils.drainInputStream(exec.getStdout());
		exec.closeStreams();
		
		tupleCreationOverhead.start();
		Tuple tuple = outputToTuple(key, value, output, exec.isTimedOut());
		tupleCreationOverhead.stop();
		return tuple;
	}
	
	public abstract Tuple outputToTuple(Text key, BytesWritable value, String output, boolean timedOut);

	void waitOnProgramExecutor(ProgramExector exec) throws IOException
	{
		while(exec.isAlive())
		{	
			try {
				Thread.sleep(100);
			} catch (InterruptedException e){
				throw new RuntimeException(e);
			}
			System.err.print(StreamUtils.drainInputStream(exec.getStderr()));
		}// end while
		
		System.err.print(StreamUtils.drainInputStream(exec.getStderr()));
		try {
			exec.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public File getProgramFile() {
		return programFile;
	}
	
	public String getScript() {
		return script;
	}	
}
