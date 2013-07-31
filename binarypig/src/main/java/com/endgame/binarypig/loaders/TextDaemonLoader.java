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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.pig.data.Tuple;

import com.endgame.binarypig.loaders.AbstractFileDroppingLoader;

/**
 * Hello world!
 *
 */
public class TextDaemonLoader extends AbstractFileDroppingLoader
{
	SocketAddress endpoint;
	
	Socket sock = null;
	BufferedReader in = null;
	OutputStream out = null;
	
	public TextDaemonLoader(String port){
		super();
		endpoint = new InetSocketAddress("127.0.0.1", Integer.parseInt(port));
	}
	
	public TextDaemonLoader(String port, String timeoutMS){
		super(timeoutMS);
		endpoint = new InetSocketAddress("127.0.0.1", Integer.parseInt(port));
	}
	
	public TextDaemonLoader(String port, String timeoutMS, String useDevShm){
		super(timeoutMS, useDevShm);
		endpoint = new InetSocketAddress("127.0.0.1", Integer.parseInt(port));
	}
	
	@Override
	public void init() throws IOException {
		super.init();
		sock = new Socket();
		if(getTimeoutMS() < (long)Integer.MAX_VALUE)
		{
			sock.setSoTimeout((int)getTimeoutMS());
		}
		System.err.println("Connecting to "+endpoint+" ...");
		sock.connect(endpoint);
		out = sock.getOutputStream();			
		in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	}

	@Override
	public Tuple processFile(Text key, BytesWritable value, File binaryFile) throws IOException {
		boolean timedOut = false;
		String result = "";
		try {
			out.write( (binaryFile.getAbsolutePath()+"\n" ).getBytes());
			String data = in.readLine();
			if(data != null){
				result = data;
			}
		} catch (SocketTimeoutException e) {
			result = "";
			timedOut = true;
		} catch (SocketException e) {
			System.err.println("WARN: Exception occurred, attempting to re-connect...");
			e.printStackTrace();
			close();
			init();
			out.write( (binaryFile.getAbsolutePath()+"\n" ).getBytes());
			String data = in.readLine();
			if(data != null){
				result = data;
			}
		}
		
		getProtoTuple().clear();
		getProtoTuple().add(key.toString());
		getProtoTuple().add(timedOut);
		getProtoTuple().add(result);
		return getTupleFactory().newTuple(getProtoTuple());
	}
	
	private void close() {
		IOUtils.closeSocket(sock);
		IOUtils.closeStream(in);
		IOUtils.closeStream(out);
	}
	
	@Override
	public void cleanUp() {
		super.cleanUp();
		close();
	}
}
