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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;

import com.markallenjohnson.assurance.model.IInitializableEntity;
import com.markallenjohnson.assurance.model.IListDataProvider;

@Entity
@Table(name = "SCAN_MAPPING_DEF")
public class ScanMappingDefinition implements IInitializableEntity, IListDataProvider<FileReference>
{
	public static final String EXCLUSIONS_PROPERTY = "exclusions";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "SCAN_DEF_ID")
	private ScanDefinition scanDefinition;

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public ScanDefinition getScanDefinition()
	{
		return scanDefinition;
	}

	public void setScanDefinition(ScanDefinition scanDefinition)
	{
		this.scanDefinition = scanDefinition;
	}

	protected FileReference getSourceReference()
	{
		return source;
	}

	public File getSource()
	{
		return (source == null) ? null : source.getFile();
	}

	public void setSourceReference(FileReference source)
	{
		this.source = source;
	}

	public void setSource(File source)
	{
		if (source != null)
		{
			if (this.source == null)
			{
				this.setSourceReference(new FileReference(source));
			}
			else
			{
				this.source.setFile(source);
			}
		}
		else
		{
			this.setSourceReference(null);
		}
	}

	protected FileReference getTargetReference()
	{
		return target;
	}

	public File getTarget()
	{
		return (target == null) ? null : target.getFile();
	}

	public void setTargetReference(FileReference target)
	{
		this.target = target;
	}

	public void setTarget(File target)
	{
		if (target != null)
		{
			if (this.target == null)
			{
				this.setTargetReference(new FileReference(target));
			}
			else
			{
				this.target.setFile(target);
			}
		}
		else
		{
			this.setTargetReference(null);
		}
	}
	
	@Override
	public String toString()
	{
		return (this.source.toString());
	}

	protected Collection<FileReference> getExclusions()
	{
		synchronized(this) 
		{
			if (!Hibernate.isInitialized(exclusions))
			{
				Hibernate.initialize(exclusions);
			}
		}
		return exclusions;
	}

	// NOTE: Review if immutable should be the public default.
	public Collection<FileReference> getUnmodifiableExclusions()
	{
		return Collections.unmodifiableCollection(this.getExclusions());
	}

	protected void setExclusions(Collection<FileReference> exclusions)
	{
		this.exclusions = exclusions;
	}

	public void addExclusion(FileReference exclusion)
	{
		this.getExclusions().add(exclusion);
		exclusion.setScanMappingDefinition(this);
	}

	public void removeExclusion(FileReference exclusion)
	{
		this.getExclusions().remove(exclusion);
		exclusion.setScanMappingDefinition(null);
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "SOURCE_REFERENCE")
	private FileReference source;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "TARGET_REFERENCE")
	private FileReference target;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "scanMappingDefinition", orphanRemoval=true)
	private Collection<FileReference> exclusions = new LinkedHashSet<>();

	public Object getPropertyToInitialize(String key) 
	{
		if (ScanMappingDefinition.EXCLUSIONS_PROPERTY.equals(key))
		{
			return this.exclusions;
		}
		
		return null;
	}
	
	public void mergeMappingDefinition(ScanMappingDefinition definition)
	{
		this.setSourceReference(definition.getSourceReference());
		this.setTargetReference(definition.getTargetReference());
		this.setExclusions(definition.getExclusions());
	}

	public Collection<FileReference> getListData() 
	{
		return this.getUnmodifiableExclusions();
	}

	public String getInitializationPropertyName() 
	{
		return ScanMappingDefinition.EXCLUSIONS_PROPERTY;
	}
}
