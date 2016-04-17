/*
 * Assurance
 * 
 * Created by Mark Johnson
 * 
 * Copyright (c) 2015 Digital Generalists, LLC.
 * 
 */
/*
 * Copyright 2015 Digital Generalists, LLC.
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

package com.digitalgeneralists.assurance.model.compare.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

public abstract class MD5FileCompareValidator extends FileCompareValidator
{
	private Logger logger = Logger.getLogger(MD5FileCompareValidator.class);

	protected boolean performMD5Compare(File file1, File file2) throws NoSuchAlgorithmException, IOException
	{
		byte[] file1Hash = this.calculateHashForFile(file1);
		byte[] file2Hash = this.calculateHashForFile(file2);

		boolean result = MessageDigest.isEqual(file1Hash, file2Hash);
		file1Hash = null;
		file2Hash = null;
		StringBuffer message = new StringBuffer(512);
		logger.info(message.append("Comparison for ").append(file1).append(" and ").append(file2).append(" is: ").append(result));
		message.setLength(0);
		message = null;
		return result;
	}

	public byte[] calculateHashForFile(File file) throws NoSuchAlgorithmException, IOException
	{
		InputStream is = null;
		MessageDigest md5HashGenerator = null;

		try
		{
			md5HashGenerator = MessageDigest.getInstance("MD5");

			is = new FileInputStream(file);

			int len;
			int size = 1024;
			byte[] buffer;

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buffer = new byte[size];
			while ((len = is.read(buffer, 0, size)) != -1)
			{
				bos.reset();
				bos.write(buffer, 0, len);
				md5HashGenerator.update(bos.toByteArray());
			}

			byte[] fileHash = md5HashGenerator.digest(buffer);
			bos.reset();
			bos = null;
			buffer = null;
			
			return fileHash;
		}
		finally
		{
			if (is != null)
			{
				try
				{
					is.close();
					is = null;
				}
				catch (IOException e)
				{
					logger.error(e);
				}
			}
			if (md5HashGenerator != null)
			{
				md5HashGenerator.reset();
				md5HashGenerator = null;
			}
		}
	}
}
