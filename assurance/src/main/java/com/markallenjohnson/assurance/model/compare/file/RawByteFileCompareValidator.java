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

package com.markallenjohnson.assurance.model.compare.file;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class RawByteFileCompareValidator extends FileCompareValidator
{
	private Logger logger = LogManager.getLogger(RawByteFileCompareValidator.class);

	@Override
	public boolean compare(File file1, File file2, boolean includeTimestamps, boolean includeAdvancedAttributes) throws IOException
	{
		if (this.areFilesComparable(file1, file2))
		{
			try
			{
				if (attributeComparer.compareFileAttributes(file1, file2, includeTimestamps, includeAdvancedAttributes))
				{
					// NOTE:  This implementation is not built out, but demonstrates the strategy-based 
					// notion of how to provide different comparison algorithms.
					logger.warn("Raw file comparer is not implemented.");
				}
			}
			finally
			{

			}
		}
		
		return false;
	}

	@Override
	public byte[] calculateHashForFile(File file) throws NoSuchAlgorithmException, IOException 
	{
		// NOTE:  This implementation is not built out, but demonstrates the strategy-based 
		// notion of how to provide different comparison algorithms.
		logger.warn("Raw file comparer is not implemented.");
		
		return null;
	}
}
