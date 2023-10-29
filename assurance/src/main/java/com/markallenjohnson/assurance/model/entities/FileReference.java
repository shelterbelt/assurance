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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.util.StringUtils;

import com.markallenjohnson.assurance.model.compare.file.IFileComparer;

@Entity
@Table(name = "FILE_REFERENCE")
public class FileReference
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	private Long id;

	@Column(name = "LOCATION")
	private String location;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "ATTRIBUTES")
	private FileAttributes fileAttributes;

	@ManyToOne
	@JoinColumn(name = "SCAN_MAPPING_DEF_ID")
	private ScanMappingDefinition scanMappingDefinition;

	@Transient
	private IFileComparer comparer;

	public FileReference()
	{

	}

	public FileReference(File file)
	{
		this(file, null);
	}

	// NOTE:  Cascading the comparer down in this fashion just to facilitate the hash calculation is 
	// far from ideal and a good example of a problem with some of the assumptions made about the 
	// comparison architecture early on.
	public FileReference(File file, IFileComparer comparer)
	{
		this.setFile(file);
		this.setComparer(comparer);
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	protected String getLocation()
	{
		return location;
	}

	protected void setLocation(String location)
	{
		this.location = location;
	}

	public File getFile()
	{
		File result = null;

		String fileLocation = this.getLocation();
		if (StringUtils.hasText(fileLocation))
		{
			result = new File(fileLocation);
		}
		return result;
	}

	public void setFile(File file)
	{
		if (file != null)
		{
			this.setLocation(file.getPath());
		}
		else
		{
			this.setLocation("");
		}
	}

	public FileAttributes getFileAttributes()
	{
		return fileAttributes;
	}

	public void setFileAttributes(FileAttributes fileAttributes)
	{
		if (fileAttributes != null)
		{
			fileAttributes.setFileReference(this);
		}
		this.fileAttributes = fileAttributes;
	}

	public ScanMappingDefinition getScanMappingDefinition() 
	{
		return scanMappingDefinition;
	}

	public void setScanMappingDefinition(ScanMappingDefinition scanMappingDefinition)
	{
		this.scanMappingDefinition = scanMappingDefinition;
	}

	// NOTE:  Cascading the comparer down in this fashion just to facilitate the hash calculation is 
	// far from ideal and a good example of a problem with some of the assumptions made about the 
	// comparison architecture early on.
	public IFileComparer getComparer()
	{
		return comparer;
	}

	// NOTE:  Cascading the comparer down in this fashion just to facilitate the hash calculation is 
	// far from ideal and a good example of a problem with some of the assumptions made about the 
	// comparison architecture early on.
	protected void setComparer(IFileComparer comparer)
	{
		this.comparer = comparer;
	}
	
	@Override
	public String toString()
	{
		File reference = this.getFile();
		return (reference != null) ? reference.toPath().getFileName().toString() : "<unknown file>";
	}
	
	public void mergeFileReference(FileReference reference)
	{
		this.setFile(reference.getFile());
		this.setFileAttributes(reference.getFileAttributes());
	}
}
