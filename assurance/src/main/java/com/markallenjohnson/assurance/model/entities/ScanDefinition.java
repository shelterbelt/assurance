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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;

import com.markallenjohnson.assurance.model.IInitializableEntity;
import com.markallenjohnson.assurance.model.IListDataProvider;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;

@Entity
@Table(name = "SCAN_DEF")
public class ScanDefinition implements IInitializableEntity, IListDataProvider<ScanMappingDefinition>
{
	public static final String SCAN_MAPPING_PROPERTY = "scanMapping";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	private Long id;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "scanDefinition", orphanRemoval=true)
	private Collection<ScanMappingDefinition> scanMapping = new LinkedHashSet<ScanMappingDefinition>();

	@Column(name = "STRATEGY")
	private AssuranceMergeStrategy mergeStrategy = AssuranceMergeStrategy.SOURCE;

	@Column(name = "NAME")
	private String name = "";

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	protected Collection<ScanMappingDefinition> getScanMapping()
	{
		synchronized(this) 
		{
			if (!Hibernate.isInitialized(scanMapping))
			{
				Hibernate.initialize(scanMapping);
			}
		}
		return scanMapping;
	}

	// NOTE: Review if immutable should be the public default.
	public Collection<ScanMappingDefinition> getUnmodifiableScanMapping()
	{
		return Collections.unmodifiableCollection(this.getScanMapping());
	}

	protected void setScanMapping(Collection<ScanMappingDefinition> scanMapping)
	{
		this.scanMapping = scanMapping;
	}

	public void addMappingDefinition(ScanMappingDefinition mappingDefinition)
	{
		this.getScanMapping().add(mappingDefinition);
		mappingDefinition.setScanDefinition(this);
	}

	public void removeMappingDefinition(ScanMappingDefinition mappingDefinition)
	{
		this.getScanMapping().remove(mappingDefinition);
		mappingDefinition.setScanDefinition(null);
	}

	public AssuranceMergeStrategy getMergeStrategy()
	{
		return mergeStrategy;
	}

	public void setMergeStrategy(AssuranceMergeStrategy mergeStrategy)
	{
		this.mergeStrategy = mergeStrategy;
	}

	public Boolean getAutoResolveConflicts()
	{
		return autoResolveConflicts;
	}

	public void setAutoResolveConflicts(Boolean autoResolveConflicts)
	{
		this.autoResolveConflicts = autoResolveConflicts;
	}

	@Column(name = "AUTO_RESOLVE")
	private Boolean autoResolveConflicts = Boolean.valueOf(false);//new Boolean(false);

	public Boolean getIncludeNonCreationTimestamps() {
		if (includeNonCreationTimestamps == null)
		{
			includeNonCreationTimestamps = false;
		}
		return includeNonCreationTimestamps;
	}

	public void setIncludeNonCreationTimestamps(Boolean includeNonCreationTimestamps) {
		this.includeNonCreationTimestamps = includeNonCreationTimestamps;
	}

	@Column(name = "INCLUDE_TIMESTAMPS")
	private Boolean includeNonCreationTimestamps = Boolean.valueOf(false);//new Boolean(false);

	public Boolean getIncludeAdvancedAttributes() {
		if (includeAdvancedAttributes == null)
		{
			includeAdvancedAttributes = false;
		}
		return includeAdvancedAttributes;
	}

	public void setIncludeAdvancedAttributes(Boolean includeAdvancedAttributes) {
		this.includeAdvancedAttributes = includeAdvancedAttributes;
	}

	@Column(name = "INCLUDE_ADVANCED_ATTRIBUTES")
	private Boolean includeAdvancedAttributes = Boolean.valueOf(false);//new Boolean(false);

	@Override
	public String toString()
	{
		return this.getName();
	}

	public Object getPropertyToInitialize(String key) 
	{
		if (ScanDefinition.SCAN_MAPPING_PROPERTY.equals(key))
		{
			return this.scanMapping;
		}
		
		return null;
	}

	public Collection<ScanMappingDefinition> getListData() 
	{
		return this.getUnmodifiableScanMapping();
	}

	public String getInitializationPropertyName() 
	{
		return ScanDefinition.SCAN_MAPPING_PROPERTY;
	}
}
