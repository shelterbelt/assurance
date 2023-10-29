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

package com.markallenjohnson.assurance.model.entities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.markallenjohnson.assurance.model.compare.file.IFileComparer;

@Entity
@Table(name = "FILE_ATTRIBUTES")
public class FileAttributes
{
	@Transient
	private Logger logger = LogManager.getLogger(FileAttributes.class);

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	private Long id;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FILE_REFERENCE")
	private FileReference fileReference;

	@Column(name = "CONTENTS_HASH")
	private String contentsHash;

	// Basic File Attributes
	@Column(name = "CREATION_TIME")
	private Date creationTime;

	@Column(name = "IS_DIRECTORY")
	private Boolean isDirectory;

	@Column(name = "IS_OTHER")
	private Boolean isOther;

	@Column(name = "IS_REGULAR_FILE")
	private Boolean isRegularFile;

	@Column(name = "IS_SYMBOLIC_LINK")
	private Boolean isSymbolicLink;

	@Column(name = "LAST_ACCESS_TIME")
	private Date lastAccessTime;

	@Column(name = "LAST_MODIFIED_TIME")
	private Date lastModifiedTime;

	@Column(name = "FILE_SIZE")
	private Long size;

	// DOS Attributes
	@Column(name = "IS_ARCHIVE")
	private Boolean isArchive;

	@Column(name = "IS_HIDDEN")
	private Boolean isHidden;

	@Column(name = "IS_READ_ONLY")
	private Boolean isReadOnly;

	@Column(name = "IS_SYSTEM")
	private Boolean isSystem;

	// POSIX Attributes
	@Column(name = "GROUP_NAME")
	private String groupName;

	@Column(name = "OWNER")
	private String owner;

	@Column(name = "PERMISSIONS")
	private String permissions;

	// File Owner Attributes
	@Column(name = "FILE_OWNER")
	private String fileOwner;

	// ACL Attributes
	@Column(name = "ACL_DESCRIPTION")
	private String aclDescription;

	// User-defined Attributes
	@Column(name = "USER_DEFINED_ATTRIBUTES_HASH")
	private String userDefinedAttributesHash;

	public FileAttributes()
	{

	}

	public FileAttributes(File file)
	{
		this(file, null);
	}

	public FileAttributes(File file, IFileComparer comparer)
	{
		this.captureFileAttributes(file, comparer);
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public FileReference getFileReference()
	{
		return fileReference;
	}

	public void setFileReference(FileReference fileReference)
	{
		this.fileReference = fileReference;
	}

	public String getContentsHash() 
	{
		return contentsHash;
	}

	public void setContentsHash(String contentsHash) 
	{
		this.contentsHash = contentsHash;
	}

	public Date getCreationTime()
	{
		return creationTime;
	}

	public void setCreationTime(Date creationTime)
	{
		this.creationTime = creationTime;
	}

	public Boolean getIsDirectory()
	{
		return isDirectory;
	}

	public void setIsDirectory(Boolean isDirectory)
	{
		this.isDirectory = isDirectory;
	}

	public Boolean getIsOther()
	{
		return isOther;
	}

	public void setIsOther(Boolean isOther)
	{
		this.isOther = isOther;
	}

	public Boolean getIsRegularFile()
	{
		return isRegularFile;
	}

	public void setIsRegularFile(Boolean isRegularFile)
	{
		this.isRegularFile = isRegularFile;
	}

	public Boolean getIsSymbolicLink()
	{
		return isSymbolicLink;
	}

	public void setIsSymbolicLink(Boolean isSymbolicLink)
	{
		this.isSymbolicLink = isSymbolicLink;
	}

	public Date getLastAccessTime()
	{
		return lastAccessTime;
	}

	public void setLastAccessTime(Date lastAccessTime)
	{
		this.lastAccessTime = lastAccessTime;
	}

	public Date getLastModifiedTime()
	{
		return lastModifiedTime;
	}

	public void setLastModifiedTime(Date lastModifiedTime)
	{
		this.lastModifiedTime = lastModifiedTime;
	}

	public Long getSize()
	{
		return size;
	}

	public void setSize(Long size)
	{
		this.size = size;
	}

	public Boolean getIsArchive()
	{
		return isArchive;
	}

	public void setIsArchive(Boolean isArchive)
	{
		this.isArchive = isArchive;
	}

	public Boolean getIsHidden()
	{
		return isHidden;
	}

	public void setIsHidden(Boolean isHidden)
	{
		this.isHidden = isHidden;
	}

	public Boolean getIsReadOnly()
	{
		return isReadOnly;
	}

	public void setIsReadOnly(Boolean isReadOnly)
	{
		this.isReadOnly = isReadOnly;
	}

	public Boolean getIsSystem()
	{
		return isSystem;
	}

	public void setIsSystem(Boolean isSystem)
	{
		this.isSystem = isSystem;
	}

	public String getGroupName()
	{
		return groupName;
	}

	public void setGroupName(String groupName)
	{
		this.groupName = groupName;
	}

	public String getOwner()
	{
		return owner;
	}

	public void setOwner(String owner)
	{
		this.owner = owner;
	}

	public String getPermissions()
	{
		return permissions;
	}

	public void setPermissions(String permissions)
	{
		this.permissions = permissions;
	}

	public String getFileOwner()
	{
		return fileOwner;
	}

	public void setFileOwner(String fileOwner)
	{
		this.fileOwner = fileOwner;
	}

	public String getAclDescription()
	{
		return aclDescription;
	}

	public void setAclDescription(String aclDescription)
	{
		this.aclDescription = aclDescription;
	}

	public String getUserDefinedAttributesHash()
	{
		return userDefinedAttributesHash;
	}

	public void setUserDefinedAttributesHash(String userDefinedAttributesHash)
	{
		this.userDefinedAttributesHash = userDefinedAttributesHash;
	}

	private void captureFileAttributes(File file, IFileComparer comparer)
	{
		// NOTE:  Reading all of these attributes a second time for each failed result isn't great.  
		// It would be nice to store these the first time we read them.
		if ((file != null) && (file.exists()))
		{

			if (comparer != null)
			{
				try 
				{
					this.contentsHash = Arrays.toString(comparer.calculateHashForFile(file));
				}
				catch (NoSuchAlgorithmException | IOException e)
				{
					this.contentsHash = "";
				}
			}
			
			BasicFileAttributes basicFileAttributes = null;
			try
			{
				basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			}
			catch (Exception e)
			{
				basicFileAttributes = null;
				logger.info(e);
			}

			if (basicFileAttributes == null)
			{
				StringBuilder message = new StringBuilder(512);
				logger.info(message.append("Could not query the basic attributes of the file: ").append(file));
				message.setLength(0);
				message = null;
			}
			else
			{
				if (basicFileAttributes.creationTime() != null)
				{
					this.creationTime = new Date(basicFileAttributes.creationTime().toMillis());
				}
				this.isDirectory = basicFileAttributes.isDirectory();
				this.isOther = basicFileAttributes.isOther();
				this.isRegularFile = basicFileAttributes.isRegularFile();
				this.isSymbolicLink = basicFileAttributes.isSymbolicLink();
				if (basicFileAttributes.lastAccessTime() != null)
				{
					this.lastAccessTime = new Date(basicFileAttributes.lastAccessTime().toMillis());
				}
				if (basicFileAttributes.lastModifiedTime() != null)
				{
					this.lastModifiedTime = new Date(basicFileAttributes.lastModifiedTime().toMillis());
				}
				this.size = basicFileAttributes.size();
			}
			basicFileAttributes = null;
			
			DosFileAttributes dosFileAttributes = null;
			try
			{
				dosFileAttributes = Files.readAttributes(file.toPath(), DosFileAttributes.class);
			}
			catch (Exception e)
			{
				dosFileAttributes = null;
				logger.info(e);
			}

			if (dosFileAttributes == null)
			{
				StringBuilder message = new StringBuilder(512);
				logger.info(message.append("Could not query the DOS attributes of the file: ").append(file));
				message.setLength(0);
				message = null;
			}
			else
			{
				this.isArchive = dosFileAttributes.isArchive();
				this.isHidden = dosFileAttributes.isHidden();
				this.isReadOnly = dosFileAttributes.isReadOnly();
				this.isSystem = dosFileAttributes.isSystem();
			}
			dosFileAttributes = null;

			PosixFileAttributes posixFileAttributes = null;
			try
			{
				posixFileAttributes = Files.readAttributes(file.toPath(), PosixFileAttributes.class);
			}
			catch (Exception e)
			{
				posixFileAttributes = null;
				logger.info(e);
			}

			if (posixFileAttributes == null)
			{
				StringBuilder message = new StringBuilder(512);
				logger.info(message.append("Could not query the POSIX attributes of the file: ").append(file));
				message.setLength(0);
				message = null;
			}
			else
			{
				if (posixFileAttributes.group() != null)
				{
					this.groupName = posixFileAttributes.group().getName();
				}
				if (posixFileAttributes.owner() != null)
				{
					this.owner = posixFileAttributes.owner().getName();
				}

				String localPermissions = "";
				StringBuilder buffer = new StringBuilder(128);
				for (PosixFilePermission permission : posixFileAttributes.permissions())
				{
					if (buffer.length() > 0)
					{
						buffer.append(", ");
					}
					buffer.append(permission.toString());
				}
				localPermissions = buffer.toString();
				buffer.setLength(0);
				if (localPermissions.length() > 0)
				{
					this.permissions = localPermissions;
				}
				localPermissions = null;
				buffer = null;
			}
			posixFileAttributes = null;

			FileOwnerAttributeView fileOwnerFileAttributes = null;
			try
			{
				fileOwnerFileAttributes = Files.getFileAttributeView(file.toPath(), FileOwnerAttributeView.class);
			}
			catch (Exception e)
			{
				fileOwnerFileAttributes = null;
				logger.info(e);
			}

			if (fileOwnerFileAttributes == null)
			{
				StringBuilder message = new StringBuilder(512);
				logger.info(message.append("Could not query the file-owner attributes of the file: ").append(file));
				message.setLength(0);
				message = null;
			}
			else
			{
				try
				{
					if (fileOwnerFileAttributes.getOwner() != null)
					{
						this.fileOwner = fileOwnerFileAttributes.getOwner().getName();
					}
				}
				catch (IOException e)
				{
					logger.error(e);
				}
			}
			fileOwnerFileAttributes = null;
			
			AclFileAttributeView aclFileAttributes = null;
			try
			{
				aclFileAttributes = Files.getFileAttributeView(file.toPath(), AclFileAttributeView.class);
			}
			catch (Exception e)
			{
				aclFileAttributes = null;
				logger.info(e);
			}

			if (aclFileAttributes == null)
			{
				StringBuilder message = new StringBuilder(512);
				logger.info(message.append("Could not query the ACL attributes of the file: ").append(file));
				message.setLength(0);
				message = null;
			}
			else
			{
				String description = "";
				StringBuilder buffer = new StringBuilder(128);
				try
				{
					for (AclEntry acl : aclFileAttributes.getAcl())
					{
						if (buffer.length() > 0)
						{
							buffer.append(", ");
						}
						buffer.append(acl.toString());
					}
					description = buffer.toString();
					if (description.length() > 0)
					{
						this.aclDescription = description;
					}
				}
				catch (IOException e)
				{
					logger.error(e);
				}
				finally
				{
					buffer.setLength(0);
					description = null;
					buffer = null;
				}
			}

			UserDefinedFileAttributeView userDefinedFileAttributes = null;
			try
			{
				userDefinedFileAttributes = Files.getFileAttributeView(file.toPath(), UserDefinedFileAttributeView.class);
			}
			catch (Exception e)
			{
				userDefinedFileAttributes = null;
				logger.info(e);
			}

			if (userDefinedFileAttributes == null)
			{
				StringBuilder message = new StringBuilder(512);
				logger.info(message.append("Could not query the user-defined attributes of the file: ").append(file));
				message.setLength(0);
				message = null;
			}
			else
			{
				// TODO:  Build out.  Currently not comparing the UDFAs in a Windows environment.
				StringBuilder hash = new StringBuilder(128);
				this.userDefinedAttributesHash = hash.append("Not Implemented: ").append(Integer.toString(userDefinedFileAttributes.hashCode())/*Integer(userDefinedFileAttributes.hashCode())*/).toString();
				hash.setLength(0);
				hash = null;
			}
		}
	}
}
