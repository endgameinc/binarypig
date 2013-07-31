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

import java.io.File;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class BuildSequenceFileFromDir extends Configured implements Tool{
	
	public static void main(String[] args) throws Exception {
		if(args.length < 2)
		{
			System.err.println("Usage: hadoop jar JAR "+BuildSequenceFileFromDir.class.getName()+" <dirOfBinaries> <HDFSOutputDir>");
			System.exit(-1);
		}
		
		ToolRunner.run(new BuildSequenceFileFromDir(), args);
	}
	
	@Override
	public int run(String[] args) throws Exception {
		
		File inDir = new File(args[0]);
		Path name = new Path(args[1]);
		
		Text key = new Text();
		BytesWritable val = new BytesWritable();
		
		Configuration conf = getConf();
		FileSystem fs = FileSystem.get(conf);
		SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, name, Text.class, BytesWritable.class, CompressionType.RECORD);
		
		for(File file : inDir.listFiles())
		{
			if(!file.isFile())
			{
				System.out.println("Skipping "+file+" (not a file) ...");
				continue;
			}
			
			byte[] bytes = FileUtils.readFileToByteArray(file);
			val.set(bytes, 0, bytes.length);
			key.set(DigestUtils.md5Hex(bytes));
			writer.append(key, val);
		}
		writer.close();
		
		return 0;
	}
}
