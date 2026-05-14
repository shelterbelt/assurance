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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.Hibernate;

import com.markallenjohnson.assurance.model.IInitializableEntity;
import com.markallenjohnson.assurance.utils.AssuranceUtils;

@Entity
@Table(name = "SCAN")
public class Scan implements IInitializableEntity
{
	public static final String RESULTS_PROPERTY = "results";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	private Long id;

	// Unidirectional OneToMany associations are not allowed in JPA 1.0 without
	// a Join Table. Best to treat them as bi-directional unless there is a
	// compelling reason not to.
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "scan")
	private Collection<ComparisonResult> results = new LinkedHashSet<>();

	@OneToOne
	@JoinColumn(name = "SCAN_DEF_ID")
	private ScanDefinition scanDef;

	@Transient
	private List<Thread> processingThreads = new ArrayList<>();

	@Column(name = "WHEN_STARTED")
	private Date scanStarted = null;
	
	@Column(name = "WHEN_COMPLETED")
	private Date whenCompleted = null;

	public Scan()
	{
		this.setScanStarted(new Date());
	}

	public synchronized ScanDefinition getScanDef()
	{
		return scanDef;
	}

	public void setScanDef(ScanDefinition scanDef)
	{
		synchronized (this)
		{
			this.scanDef = scanDef;
		}
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	protected Collection<ComparisonResult> getResults()
	{
		synchronized(this) 
		{
			if (!Hibernate.isInitialized(results))
			{
				Hibernate.initialize(results);
			}

			return results;
		}
	}

	// NOTE: Review if immutable should be the public default.
	public Collection<ComparisonResult> getUnmodifiableResults()
	{
		return Collections.unmodifiableCollection(this.getResults());
	}

	protected void setResults(Collection<ComparisonResult> results)
	{
		synchronized (this)
		{
			this.results = results;
		}
	}

	public void addResult(ComparisonResult result)
	{
		synchronized (this)
		{
			this.getResults().add(result);
			result.setScan(this);
		}
	}
	
	// Currently, results are not required to be deleted, so no mechanism for removing 
	// a result is provided.  Because of the ThreadLocal implementation, if one is provided,
	// special thread considerations likely exist.

	public String toString()
	{
		String scanName = "Unknown";
		if ((this.scanDef != null) && (this.scanDef.getName() != null))
		{
			scanName = this.scanDef.getName();
		}
		long processingTime = 0;
		if ((this.getScanCompleted() != null) && (this.getScanStarted() != null))
		{
			processingTime = this.getScanCompleted().getTime() - this.getScanStarted().getTime();
		}
		String formattedProcessingTime = AssuranceUtils.convertUnixTimestampToString(processingTime);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
		StringBuilder descriptionBuffer = new StringBuilder(128);
		String description = (this.scanDef != null) ? descriptionBuffer.append(scanName).append(" - ").append(dateFormat.format(this.getScanStarted())).append(", Time: ").append(formattedProcessingTime).toString() : "<annonymous scan>";
		descriptionBuffer.setLength(0);
		
		return description;
	}

	public Date getScanStarted()
	{
		return scanStarted;
	}

	private void setScanStarted(Date scanStarted)
	{
		// There is a database constraint on this field being null				
		if (scanStarted == null)
		{
			scanStarted = new Date();
		}
		this.scanStarted = scanStarted;
	}

	public Date getScanCompleted() 
	{
		return whenCompleted;
	}

	public void setScanCompleted(Date whenCompleted) 
	{
		this.whenCompleted = whenCompleted;
	}

	public File getScanDeletedItemsLocation(File appDeletedItemsLocation)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss");
		StringBuilder buffer = new StringBuilder(128);
		String scanFolderName = buffer.append(((this.scanDef != null) ? this.scanDef.getName() : "annonymous_scan")).append("_").append(dateFormat.format(this.getScanStarted())).toString();
		buffer.setLength(0);
		String path = buffer.append(appDeletedItemsLocation.getPath()).append(File.separator).append(scanFolderName).toString();
		buffer.setLength(0);

		return new File(path);
	}

	public Object getPropertyToInitialize(String key) 
	{
		if (Scan.RESULTS_PROPERTY.equals(key))
		{
			return this.results;
		}
		
		return null;
	}

	public String getInitializationPropertyName() 
	{
		return Scan.RESULTS_PROPERTY;
	}
}
