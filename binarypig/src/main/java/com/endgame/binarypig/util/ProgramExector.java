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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.io.IOUtils;

public class ProgramExector extends Thread 
{
	int retVal = -1;
	long endtime;
	boolean timedOut = false;
	
	Process proc;
	OutputStream stdin;
	InputStream stdout;
	InputStream stderr;
	
	public ProgramExector(String[] cmd, long timeoutMS) throws IOException{
		if(timeoutMS <= 0 || (System.currentTimeMillis() + timeoutMS) <= 0){
			this.endtime = Long.MAX_VALUE;
		}
		else{
			this.endtime = (System.currentTimeMillis() + timeoutMS);
		}
		
		proc = Runtime.getRuntime().exec(cmd);
		stdin = new BufferedOutputStream(proc.getOutputStream());
		stdout = proc.getInputStream();
		stderr = proc.getErrorStream();
	}
	
	@Override
	public void run() {
		while(true)
		{
			try {
				retVal = proc.exitValue();
				// process finished!
				break;
			} catch (IllegalThreadStateException e) 
			{
				// process is still running...
				if(System.currentTimeMillis() >= endtime){
					// Process timed out
					timedOut = true;
					proc.destroy();
					retVal = -1;
					break;
				}
				else
				{
					try {
						Thread.sleep(10);
					} catch (InterruptedException e1) {
						throw new RuntimeException(e1);
					}
				}
			}
		}
	}
	
	public boolean isTimedOut() {
		return timedOut;
	}
	
	public InputStream getStderr() {
		return stderr;
	}
	
	public OutputStream getStdin() {
		return stdin;
	}
	
	public InputStream getStdout() {
		return stdout;
	}
	
	public int getRetVal() {
		return retVal;
	}
	
	public void closeStreams()
	{
		IOUtils.closeStream(getStderr());
		IOUtils.closeStream(getStdin());
		IOUtils.closeStream(getStdout());
	}
}