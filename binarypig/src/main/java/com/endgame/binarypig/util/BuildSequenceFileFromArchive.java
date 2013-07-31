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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.bzip2.CBZip2InputStream;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class BuildSequenceFileFromArchive extends Configured implements Tool{
	
	public static void main(String[] args) throws Exception {
		
		if(args.length < 2)
		{
			System.err.println("Usage: hadoop jar JAR "+BuildSequenceFileFromArchive.class.getName()+" <ArchiveOrdirOfArchives> <HDFSOutputDir>");
			System.exit(-1);
		}
		
		ToolRunner.run(new BuildSequenceFileFromArchive(), args);
	}

	public void load(FileSystem fs, Configuration conf, File archive, Path outputDir) throws Exception
	{
		Text key = new Text();
		BytesWritable val = new BytesWritable();
		
		SequenceFile.Writer writer = null;
		ArchiveInputStream archiveInputStream = null;
		
		try {
			Path sequenceName = new Path(outputDir, archive.getName() + ".seq");
			System.out.println("Writing to " + sequenceName);
			writer = SequenceFile.createWriter(fs, conf, sequenceName, Text.class, BytesWritable.class, CompressionType.RECORD);
		    String lowerName = archive.toString().toLowerCase();
		    
		    if(lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz"))
		    {
		    	archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream("tar", new GZIPInputStream(new FileInputStream(archive)));
		    }
		    else if(lowerName.endsWith(".tar.bz") || lowerName.endsWith(".tar.bz2") || lowerName.endsWith(".tbz"))
		    {
		    	FileInputStream is = new FileInputStream(archive);
		    	is.read(); // read 'B'
		    	is.read(); // read 'Z'
		    	archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream("tar", new CBZip2InputStream(is));
		    }
		    else if(lowerName.endsWith(".tar"))
		    {
		    	archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream("tar", new FileInputStream(archive));
		    }
		    else if(lowerName.endsWith(".zip"))
		    {
		    	archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream("zip", new FileInputStream(archive));
		    }
		    else
		    {
		    	throw new RuntimeException("Can't handle archive format for: "+archive);
		    }
		    
		    ArchiveEntry entry = null; 
		    while ((entry = archiveInputStream.getNextEntry()) != null) {
		        if (!entry.isDirectory()) {
		            try {
						byte[] outputFile = IOUtils.toByteArray(archiveInputStream);
						val.set(outputFile, 0, outputFile.length);
						key.set(DigestUtils.md5Hex(outputFile));
						
						writer.append(key, val);
					} catch (IOException e) {
						System.err.println("Warning: archive may be truncated: "+archive);
						// Truncated Archive
						break;
					}
		        }
		    }
		} finally {
			archiveInputStream.close(); 
		    writer.close();
		}
	}
	
	@Override
	public int run(String[] args) throws Exception {
		File inDirOrFile = new File(args[0]);
		Path outputDir = new Path(args[1]);
		
		Configuration conf = getConf();
		FileSystem fs = FileSystem.get(conf);
		if (!fs.exists(outputDir)){
			fs.mkdirs(outputDir);
		}
		
		if(inDirOrFile.isFile())
		{
			load(fs, conf, inDirOrFile, outputDir);
		}
		else
		{
			for(File file : inDirOrFile.listFiles())
			{
				if(!file.isFile())
				{
					System.out.println("Skipping "+file+" (not a file) ...");
					continue;
				}
				
				load(fs, conf, file, outputDir);
			}
		}
		
		return 0;
	}
}


