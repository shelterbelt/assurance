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
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.digitalgeneralists.assurance.model.compare.file.attributes.IFileAttributeComparor;

public class ComprehensiveMD5FileCompareValidator extends MD5FileCompareValidator
{
	private Logger logger = Logger.getLogger(ComprehensiveMD5FileCompareValidator.class);

	@Autowired
	@Qualifier("DeepScanFileAttributeCompareValidator")
	protected IFileAttributeComparor attributeComparor;

	public ComprehensiveMD5FileCompareValidator()
	{
	}

	@Override
	public boolean compare(File file1, File file2, boolean includeTimestamps, boolean includeAdvancedAttributes) throws IOException, NoSuchAlgorithmException
	{
		logger.info("Using Comprehensive MD5 Validator");

		InputStream is = null;

		try
		{
			if (this.areFilesComparable(file1, file2))
			{
				boolean fileAttributesResult = attributeComparor.compareFileAttributes(file1, file2, includeTimestamps, includeAdvancedAttributes);
				boolean fileContentsResult = this.performMD5Compare(file1, file2);
				return(fileAttributesResult && fileContentsResult);
			}
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
		}

		return false;
	}

}
