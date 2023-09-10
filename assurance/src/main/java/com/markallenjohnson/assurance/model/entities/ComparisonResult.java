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

import org.h2.util.StringUtils;

import com.markallenjohnson.assurance.model.compare.file.IFileComparer;
import com.markallenjohnson.assurance.model.enums.AssuranceResultReason;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;

@Entity
@Table(name = "COMPARISON_RESULT")
public class ComparisonResult
{

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "SCAN_ID")
	private Scan scan;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "SOURCE_REFERENCE")
	private FileReference source;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "TARGET_REFERENCE")
	private FileReference target;

	@Column(name = "REASON")
	private AssuranceResultReason reason = AssuranceResultReason.COMPARE_FAILED;

	@Column(name = "RESOLUTION")
	private AssuranceResultResolution resolution = AssuranceResultResolution.UNRESOLVED;

	@Column(name = "RESOLUTION_ERROR")
	private String resolutionError = "";

	public ComparisonResult()
	{

	}

	protected ComparisonResult(FileReference source, FileReference target, AssuranceResultReason reason)
	{
		this.setSource(source);
		this.setTarget(target);
		this.setReason(reason);
	}

	public ComparisonResult(File source, File target, AssuranceResultReason reason)
	{
		this.setSource(source);
		this.setTarget(target);
		this.setReason(reason);
	}

	// NOTE:  Cascading the comparer down in this fashion just to facilitate the hash calculation is 
	// far from ideal and a good example of a problem with some of the assumptions made about the 
	// comparison architecture early on.
	public ComparisonResult(File source, File target, AssuranceResultReason reason, IFileComparer comparer)
	{
		this.setSource(source, comparer);
		this.setTarget(target, comparer);
		this.setReason(reason);
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public Scan getScan()
	{
		return scan;
	}

	protected void setScan(Scan scan)
	{
		this.scan = scan;
	}

	public FileReference getSource()
	{
		return source;
	}

	protected void setSource(FileReference source)
	{
		if (source != null)
		{
			FileAttributes attributes = new FileAttributes(source.getFile(), source.getComparer());
			source.setFileAttributes(attributes);
			attributes = null;
		}
		this.source = source;
	}

	public void setSource(File source)
	{
		FileReference sourceRef = new FileReference(source);
		this.setSource(sourceRef);
		sourceRef = null;
	}

	// NOTE:  Cascading the comparer down in this fashion just to facilitate the hash calculation is 
	// far from ideal and a good example of a problem with some of the assumptions made about the 
	// comparison architecture early on.
	public void setSource(File source, IFileComparer comparer)
	{
		FileReference sourceRef = new FileReference(source, comparer);
		this.setSource(sourceRef);
		sourceRef = null;
	}

	public FileReference getTarget()
	{
		return target;
	}

	protected void setTarget(FileReference target)
	{
		if (target != null)
		{
			FileAttributes attributes = new FileAttributes(target.getFile(), target.getComparer());
			target.setFileAttributes(attributes);
			attributes = null;
		}
		this.target = target;
	}

	public void setTarget(File target)
	{
		FileReference targetRef = new FileReference(target);
		this.setTarget(targetRef);
		targetRef = null;
	}

	// NOTE:  Cascading the comparer down in this fashion just to facilitate the hash calculation is 
	// far from ideal and a good example of a problem with some of the assumptions made about the 
	// comparison architecture early on.
	public void setTarget(File target, IFileComparer comparer)
	{
		FileReference targetRef = new FileReference(target, comparer);
		this.setTarget(targetRef);
		targetRef = null;
	}

	public AssuranceResultReason getReason()
	{
		return reason;
	}

	public void setReason(AssuranceResultReason reason)
	{
		this.reason = reason;
	}

	public AssuranceResultResolution getResolution()
	{
		return resolution;
	}

	public void setResolution(AssuranceResultResolution resolution)
	{
		this.resolution = resolution;
	}

	public String getResolutionError()
	{
		return resolutionError;
	}

	public void setResolutionError(String resolutionError)
	{
		if ((resolutionError != null) && (resolutionError.length() > 255))
		{
			resolutionError = resolutionError.substring(0, 255);
		}
		this.resolutionError = resolutionError;
	}

	public File getSourceDeletedItemLocation(File appDeletedItemsLocation)
	{
		return this.getFileDeletedItemLocation(appDeletedItemsLocation, this.getSource().getFile());
	}

	public File getTargetDeletedItemLocation(File appDeletedItemsLocation)
	{
		return this.getFileDeletedItemLocation(appDeletedItemsLocation, this.getTarget().getFile());
	}

	private File getFileDeletedItemLocation(File appDeletedItemsLocation, File deletedFile)
	{
		File scanDeletedItemLocation = null;

		if (this.getScan() != null)
		{
			scanDeletedItemLocation = this.getScan().getScanDeletedItemsLocation(appDeletedItemsLocation);
		}

		if (scanDeletedItemLocation != null)
		{
			String deletedFilePath = "";

			if (deletedFile != null)
			{
				deletedFilePath = deletedFile.getPath();
				if (deletedFilePath.charAt(1) == ':')
				{
					deletedFilePath.replace(':', '~');
				}
			}

			if (!StringUtils.isNullOrEmpty(deletedFilePath))
			{
				StringBuilder path = new StringBuilder(512);
				scanDeletedItemLocation = new File(path.append(scanDeletedItemLocation.getPath()).append(File.separator).append(deletedFilePath).toString());
				path.setLength(0);
				path = null;
			}
			
			deletedFilePath = null;
		}

		return scanDeletedItemLocation;
	}
}
