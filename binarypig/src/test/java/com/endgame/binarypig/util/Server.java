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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.hadoop.io.IOUtils;

public class Server extends Thread
{
	ServerSocket sock;
	int port;
	String sent;
	String reply;
	long sleepMS = -1;
	
	public Server() throws IOException{			
		this.sock = new ServerSocket(0);
		this.port = sock.getLocalPort();
	}
	
	public void setSleepMS(long sleepMS) {
		this.sleepMS = sleepMS;
	}
	
	public void setReply(String reply) {
		this.reply = reply;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getSent() {
		return sent;
	}
	
	public void run() {
		Socket client = null;
		BufferedReader in = null;
		PrintWriter out = null;
		
		try {
			client = sock.accept();
			if(sleepMS > 0)
			{
				try {
					Thread.sleep(sleepMS);
				} catch (InterruptedException e) {}
			}
			
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			String line = in.readLine();
			sent = line;
			
			out = new PrintWriter(client.getOutputStream());
			out.println(reply);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		finally
		{
			IOUtils.closeStream(out);
			IOUtils.closeStream(in);
			IOUtils.closeStream(sock);
			IOUtils.closeStream(client);
		}
	}
}