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
package com.endgame.binarypig.loaders.av;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
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
public class ClamScanDaemonLoader extends AbstractFileDroppingLoader
{
	SocketAddress clamdEndoint = new InetSocketAddress("127.0.0.1", 3310);
	
	public ClamScanDaemonLoader(){
		super();
	}
	
	public ClamScanDaemonLoader(String timeoutMS){
		super(timeoutMS);
	}
	
	public ClamScanDaemonLoader(String timeoutMS, String useDevShm){
		super(timeoutMS, useDevShm);
	}

	@Override
	public Tuple processFile(Text key, BytesWritable value, File binaryFile) throws IOException {
		boolean timedOut = false;
		Socket sock = null;
		BufferedReader in = null;
		OutputStream out = null;
		String result = "";
		try {
			sock = new Socket();
			if(getTimeoutMS() < (long)Integer.MAX_VALUE)
			{
				sock.setSoTimeout((int)getTimeoutMS());
			}
			
			sock.connect(clamdEndoint);
			
			out = sock.getOutputStream();			
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			out.write( ("nSCAN "+binaryFile.getAbsolutePath()+"\n" ).getBytes());
			String data = in.readLine();
			if(data != null){
				result = data.
						substring(data.indexOf(':')+1). // "remove the /path/to/file: "
						replace(" FOUND", ""). // no need for the "FOUND" string
						replaceAll("\\([a-f0-9:]+\\)$", "").trim(); // on some versions of clamscan, it adds (MD5:NUM)
			}
		} catch (SocketTimeoutException e) {
			result = "";
			timedOut = true;
		}
		finally{
			IOUtils.closeSocket(sock);
			IOUtils.closeStream(in);
			IOUtils.closeStream(out);
		}
		
		getProtoTuple().clear();
		getProtoTuple().add(key.toString());
		getProtoTuple().add(timedOut);
		getProtoTuple().add(result);
		return getTupleFactory().newTuple(getProtoTuple());
	}
}
