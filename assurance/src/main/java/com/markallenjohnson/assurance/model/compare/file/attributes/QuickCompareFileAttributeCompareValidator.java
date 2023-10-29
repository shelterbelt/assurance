/*
 * Assurance
 * 
 * Created by Mark Johnson
 * 
 * Copyright (c) 2015 - 2023 Mark Johnson
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

package com.markallenjohnson.assurance.model.compare.file.attributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributes;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class QuickCompareFileAttributeCompareValidator extends FileAttributeCompareValidator
{
	private Logger logger = LogManager.getLogger(QuickCompareFileAttributeCompareValidator.class);

	@Override
	public boolean compareFileAttributes(File file1, File file2, boolean includeTimestamps, boolean includeAdvancedAttributes) throws IOException
	{
		if (!this.compareBasicAttributes(file1, file2, includeTimestamps))
		{
			logger.info("The basic attributes do not match.");
			return false;
		}

		if (!this.compareDOSAttributes(file1, file2))
		{
			logger.info("The DOS attributes do not match.");
			return false;
		}
		
		if (includeAdvancedAttributes)
		{
			if (!this.comparePOSIXAttributes(file1, file2))
			{
				logger.info("The POSIX attributes do not match.");
				return false;
			}

			if (!this.compareFileOwnerAttributes(file1, file2))
			{
				logger.info("The file-owner attributes do not match.");
				return false;
			}

			if (!this.compareACLFileAttributes(file1, file2))
			{
				logger.info("The ACL attributes do not match.");
				return false;
			}
		}

		return true;
	}

	private boolean compareBasicAttributes(File file1, File file2, boolean includeTimestamps) throws IOException
	{
		BasicFileAttributes file1Attributes = null;
		BasicFileAttributes file2Attributes = null;
		try
		{
			file1Attributes = Files.readAttributes(file1.toPath(), BasicFileAttributes.class);
		}
		catch (Exception e)
		{
			file1Attributes = null;
			logger.info(e);
		}

		try
		{
			file2Attributes = Files.readAttributes(file2.toPath(), BasicFileAttributes.class);
		}
		catch (Exception e)
		{
			file2Attributes = null;
			logger.info(e);
		}

		if ((file1Attributes == null) && (file2Attributes == null))
		{
			StringBuilder message = new StringBuilder(512);
			logger.info(message.append("Could not query the basic attributes of both files: file1: ").append(file1).append(" and file2: ").append(file2));
			message.setLength(0);
			
			return true;
		}

		if ((file1Attributes == null) || (file2Attributes == null))
		{
			StringBuilder message = new StringBuilder(512);
			logger.info(message.append("Could not query the basic attributes of one of the files: file1: ").append(file1).append(" and file2: ").append(file2));
			message.setLength(0);
			logger.info(message.append("file1Attributes: ").append(file1Attributes));
			message.setLength(0);
			logger.info(message.append("file2Attributes: ").append(file1Attributes));
			message.setLength(0);
			
			return false;
		}

		// File creation time leads to false positives.
		//if (!file1Attributes.creationTime().equals(file2Attributes.creationTime()))
		//{
		//	logger.info("The basic attributes of the two files do not match.");
		//	return false;
		//}

		if (file1Attributes.isDirectory() != file2Attributes.isDirectory())
		{
			logger.info("The basic attributes of the two files do not match.");
			
			return false;
		}

		if (file1Attributes.isOther() != file2Attributes.isOther())
		{
			logger.info("The basic attributes of the two files do not match.");
			
			return false;
		}

		if (file1Attributes.isRegularFile() != file2Attributes.isRegularFile())
		{
			logger.info("The basic attributes of the two files do not match.");
			
			return false;
		}

		if (file1Attributes.isSymbolicLink() != file2Attributes.isSymbolicLink())
		{
			logger.info("The basic attributes of the two files do not match.");
			
			return false;
		}

		if (includeTimestamps)
		{
			if (!file1Attributes.lastAccessTime().equals(file2Attributes.lastAccessTime()))
			{
				logger.info("The basic attributes of the two files do not match.");
				
				return false;
			}

			if (!file1Attributes.lastModifiedTime().equals(file2Attributes.lastModifiedTime()))
			{
				logger.info("The basic attributes of the two files do not match.");
				
				return false;
			}
		}

		if (file1Attributes.size() != file2Attributes.size())
		{
			logger.info("The basic attributes of the two files do not match.");
			
			return false;
		}

		logger.info("The basic attributes of the two files match.");
		
		return true;
	}

	private boolean compareDOSAttributes(File file1, File file2) throws IOException
	{
		DosFileAttributes file1Attributes = null;
		DosFileAttributes file2Attributes = null;
		try
		{
			file1Attributes = Files.readAttributes(file1.toPath(), DosFileAttributes.class);
		}
		catch (Exception e)
		{
			file1Attributes = null;
			logger.info(e);
		}

		try
		{
			file2Attributes = Files.readAttributes(file2.toPath(), DosFileAttributes.class);
		}
		catch (Exception e)
		{
			file2Attributes = null;
			logger.info(e);
		}

		if ((file1Attributes == null) && (file2Attributes == null))
		{
			StringBuilder message = new StringBuilder(512);
			logger.info(message.append("Could not query the DOS attributes of both files: file1: ").append(file1).append(" and file2: ").append(file2));
			message.setLength(0);
			
			return true;
		}

		if ((file1Attributes == null) || (file2Attributes == null))
		{
			StringBuilder message = new StringBuilder(512);
			logger.info(message.append("Could not query the DOS attributes of one of the files: file1: ").append(file1).append(" and file2: ").append(file2));
			message.setLength(0);
			logger.info(message.append("file1Attributes: ").append(file1Attributes));
			message.setLength(0);
			logger.info(message.append("file2Attributes: ").append(file1Attributes));
			message.setLength(0);
			
			return false;
		}

		if (file1Attributes.isArchive() != file2Attributes.isArchive())
		{
			logger.info("The DOS attributes of the two files do not match.");
			
			return false;
		}

		if (file1Attributes.isHidden() != file2Attributes.isHidden())
		{
			logger.info("The DOS attributes of the two files do not match.");
			
			return false;
		}

		if (file1Attributes.isReadOnly() != file2Attributes.isReadOnly())
		{
			logger.info("The DOS attributes of the two files do not match.");
			
			return false;
		}

		if (file1Attributes.isSystem() != file2Attributes.isSystem())
		{
			logger.info("The DOS attributes of the two files do not match.");
			
			return false;
		}

		logger.info("The DOS attributes of the two files match.");
		
		return true;
	}

	private boolean comparePOSIXAttributes(File file1, File file2) throws IOException
	{
		PosixFileAttributes file1Attributes = null;
		PosixFileAttributes file2Attributes = null;
		try
		{
			file1Attributes = Files.readAttributes(file1.toPath(), PosixFileAttributes.class);
		}
		catch (Exception e)
		{
			file1Attributes = null;
			logger.info(e);
		}

		try
		{
			file2Attributes = Files.readAttributes(file2.toPath(), PosixFileAttributes.class);
		}
		catch (Exception e)
		{
			file2Attributes = null;
			logger.info(e);
		}

		if ((file1Attributes == null) && (file2Attributes == null))
		{
			StringBuilder message = new StringBuilder(512);
			logger.info(message.append("Could not query the POSIX attributes of both files: file1: ").append(file1).append(" and file2: ").append(file2));
			message.setLength(0);
			
			return true;
		}

		if ((file1Attributes == null) || (file2Attributes == null))
		{
			StringBuilder message = new StringBuilder(512);
			logger.info(message.append("Could not query the POSIX attributes of one of the files: file1: ").append(file1).append(" and file2: ").append(file2));
			message.setLength(0);
			logger.info(message.append("file1Attributes: ").append(file1Attributes));
			message.setLength(0);
			logger.info(message.append("file2Attributes: ").append(file1Attributes));
			message.setLength(0);
			
			return false;
		}

		if (!file1Attributes.group().equals(file2Attributes.group()))
		{
			logger.info("The POSIX attributes of the two files do not match.");
			
			return false;
		}

		if (!file1Attributes.owner().equals(file2Attributes.owner()))
		{
			logger.info("The POSIX attributes of the two files do not match.");
			
			return false;
		}

		if (!file1Attributes.permissions().equals(file2Attributes.permissions()))
		{
			logger.info("The POSIX attributes of the two files do not match.");
			
			return false;
		}

		logger.info("The POSIX attributes of the two files match.");
		
		return true;
	}

	private boolean compareFileOwnerAttributes(File file1, File file2) throws IOException
	{
		FileOwnerAttributeView file1Attributes = null;
		FileOwnerAttributeView file2Attributes = null;
		try
		{
			file1Attributes = Files.getFileAttributeView(file1.toPath(), FileOwnerAttributeView.class);
		}
		catch (Exception e)
		{
			file1Attributes = null;
			logger.info(e);
		}

		try
		{
			file2Attributes = Files.getFileAttributeView(file2.toPath(), FileOwnerAttributeView.class);
		}
		catch (Exception e)
		{
			file2Attributes = null;
			logger.info(e);
		}

		if ((file1Attributes == null) && (file2Attributes == null))
		{
			StringBuilder message = new StringBuilder(512);
			logger.info(message.append("Could not query the file owner attributes of both files: file1: ").append(file1).append(" and file2: ").append(file2));
			message.setLength(0);
			
			return true;
		}

		if ((file1Attributes == null) || (file2Attributes == null))
		{
			StringBuilder message = new StringBuilder(512);
			logger.info(message.append("Could not query the file owner attributes of one of the files: file1: ").append(file1).append(" and file2: ").append(file2));
			message.setLength(0);
			logger.info(message.append("file1Attributes: ").append(file1Attributes));
			message.setLength(0);
			logger.info(message.append("file2Attributes: ").append(file1Attributes));
			message.setLength(0);
			
			return false;
		}

		if (!file1Attributes.getOwner().equals(file2Attributes.getOwner()))
		{
			logger.info("The file-owner attributes of the two files do not match.");
			
			return false;
		}

		logger.info("The file-owner attributes of the two files match.");
		
		return true;
	}

	private boolean compareACLFileAttributes(File file1, File file2) throws IOException
	{
		AclFileAttributeView file1Attributes = null;
		AclFileAttributeView file2Attributes = null;
		try
		{
			file1Attributes = Files.getFileAttributeView(file1.toPath(), AclFileAttributeView.class);
		}
		catch (Exception e)
		{
			file1Attributes = null;
			logger.info(e);
		}

		try
		{
			file2Attributes = Files.getFileAttributeView(file2.toPath(), AclFileAttributeView.class);
		}
		catch (Exception e)
		{
			file2Attributes = null;
			logger.info(e);
		}

		if ((file1Attributes == null) && (file2Attributes == null))
		{
			StringBuilder message = new StringBuilder(512);
			logger.info(message.append("Could not query the ACL attributes of both files: file1: ").append(file1).append(" and file2: ").append(file2));
			message.setLength(0);
			
			return true;
		}

		if ((file1Attributes == null) || (file2Attributes == null))
		{
			StringBuilder message = new StringBuilder(512);
			logger.info(message.append("Could not query the ACL attributes of one of the files: file1: ").append(file1).append(" and file2: ").append(file2));
			message.setLength(0);
			logger.info(message.append("file1Attributes: ").append(file1Attributes));
			message.setLength(0);
			logger.info(message.append("file2Attributes: ").append(file2Attributes));
			message.setLength(0);
			
			return false;
		}

		if (!file1Attributes.getAcl().equals(file2Attributes.getAcl()))
		{
			logger.info("The ACL attributes of the two files do not match.");
			
			return false;
		}

		logger.info("The ACL attributes of the two files match.");
		
		return true;
	}
}
