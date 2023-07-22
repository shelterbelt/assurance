/*
 * Assurance
 * 
 * Created by Mark Johnson
 * 
 * Copyright (c) 2015 Mark Johnson
 * 
 */
/*
 * Copyright 2015 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.markallenjohnson.assurance.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.log4j.Logger;

public class AssuranceUtils
{
	public static Platform getPlatform()
	{
		String os = System.getProperty("os.name").toLowerCase();
	
		if (os.indexOf("mac") >= 0)
		{
			return Platform.MAC;
		}
		if (os.indexOf("win") >= 0)
		{
			return Platform.WINDOWS;
		}
		if (os.indexOf("linux") >= 0)
		{
			return Platform.LINUX;
		}
		
		os = null;
	
		return Platform.UNRECOGNIZED;
	}

	public static boolean checkIfFileIsSymbolicLink(File file)
	{
		Logger logger = Logger.getLogger(AssuranceUtils.class);

		boolean result = false;
		
		BasicFileAttributes fileAttributes = null;
		try
		{
			fileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
		}
		catch (Exception e)
		{
			logger.info(e);
			fileAttributes = null;
		}

		if (fileAttributes != null)
		{
			if (fileAttributes.isSymbolicLink())
			{
				result = true;
			}
		}
		
		logger = null;
		fileAttributes = null;

		return result;
	}
	
	public static String convertUnixTimestampToString(long timestamp)
	{
		String formattedProcessingTime = "";

		StringBuilder timestampString = new StringBuilder(128);
		formattedProcessingTime = timestampString.append(new Long(timestamp / (24 * 60 * 60 *1000)).toString()).append(" days, ").append(String.format("%02d", new Long((timestamp / (60 * 60 *1000) % 24)))).append(":").append(String.format("%02d", new Long((timestamp / (60 *1000) % 60)))).append(":").append(String.format("%02d", new Long((timestamp / (1000) % 60)))).toString();
		timestampString.setLength(0);
		timestampString = null;
		
		return formattedProcessingTime;
	}
}
