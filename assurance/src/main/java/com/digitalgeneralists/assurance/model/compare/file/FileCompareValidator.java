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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.digitalgeneralists.assurance.model.compare.file.attributes.IFileAttributeComparor;

public abstract class FileCompareValidator implements IFileComparor
{
	private Logger logger = Logger.getLogger(FileCompareValidator.class);

	@Autowired
	@Qualifier("DeepScanFileAttributeCompareValidator")
	protected IFileAttributeComparor attributeComparor;

	public abstract boolean compare(File file1, File file2, boolean includeTimestamps, boolean includeAdvancedAttributes) throws NoSuchAlgorithmException, IOException;
	
	public abstract byte[] calculateHashForFile(File file) throws NoSuchAlgorithmException, IOException;
	
	protected boolean areFilesComparable(File file1, File file2)
	{
		if ((file1 == null) || (file2 == null))
		{
			StringBuffer message = new StringBuffer(512);
			logger.info(message.append("One or more files is not a valid file instance - File 1: ").append(file1).append(" File 2: ").append(file2));
			message.setLength(0);
			message = null;
			return false;
		}

		return true;
	}
}
