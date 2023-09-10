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

import java.util.List;
import java.util.ListIterator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.commons.lang3.text.StrTokenizer;
import org.springframework.util.StringUtils;

import com.markallenjohnson.assurance.model.compare.IScanOptions;

@Entity
@Table(name = "APP_CONFIGURATION")
public class ApplicationConfiguration implements IScanOptions
{
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	private Long id;

	@Column(name = "IGNORED_FILES")
	private String ignoredFileNames;

	@Column(name = "IGNORED_EXT")
	private String ignoredFileExtensions;
	
	@Column(name = "NUM_THREADS")
	private Integer numberOfScanThreads;
	
	@Transient
	List<String> ignoredFileNamesCollection;
	
	@Transient
	List<String> ignoredFileExtensionsCollection;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getIgnoredFileNames() {
		return ignoredFileNames;
	}

	public void setIgnoredFileNames(String ignoredFileNames) {
		this.ignoredFileNames = ignoredFileNames;
		this.ignoredFileNamesCollection = this.transformStringPropertyToListProperty(this.ignoredFileNames);
	}

	public String getIgnoredFileExtensions() {
		return ignoredFileExtensions;
	}

	public void setIgnoredFileExtensions(String ignoredFileExtensions) {
		this.ignoredFileExtensions = ignoredFileExtensions;
		this.ignoredFileExtensionsCollection = this.transformStringPropertyToListProperty(this.ignoredFileExtensions, true);
	}

	public Integer getNumberOfScanThreads() {
		return numberOfScanThreads;
	}

	public void setNumberOfScanThreads(Integer numberOfScanThreads) {
		this.numberOfScanThreads = numberOfScanThreads;
	}
	
	public void mergeApplicationConfiguration(ApplicationConfiguration configuration)
	{
		this.setIgnoredFileNames(configuration.getIgnoredFileNames());
		this.setIgnoredFileExtensions(configuration.getIgnoredFileExtensions());
		this.setNumberOfScanThreads(configuration.getNumberOfScanThreads());
	}
	
	public static ApplicationConfiguration createDefaultConfiguration()
	{
		ApplicationConfiguration config = new ApplicationConfiguration();
		
		config.setIgnoredFileNames(".DS_Store, Thumbs.db, desktop.ini");
		config.setIgnoredFileExtensions("");
		config.setNumberOfScanThreads(4);
		
		return config;
	}
	
	public List<String> getIngnoredFileNames()
	{
		return this.ignoredFileNamesCollection;
	}
	
	public List<String> getIngnoredFileExtensions()
	{
		return this.ignoredFileExtensionsCollection;
	}
	
	private List<String> transformStringPropertyToListProperty(String property)
	{
		return this.transformStringPropertyToListProperty(property, false);
	}
	
	private List<String> transformStringPropertyToListProperty(String property, boolean removeExtensionPatterns)
	{
		StrTokenizer tokenizer = new StrTokenizer(property, ',');
		List<String> tokenizedList = tokenizer.getTokenList();
		// NOTE: May want to reconsider the toLowercase conversion.
		ListIterator<String> iterator = tokenizedList.listIterator();
		while (iterator.hasNext())
		{
			String extension = iterator.next();
			// NOTE: This is probably a bit overly-aggressive and less-sophisticated than it could be,
			// but it should handle 99% of the input that will be entered.
			if (removeExtensionPatterns)
			{
				extension = StringUtils.replace(extension, "*.", "");
				extension = StringUtils.replace(extension, "*", "");
				extension = StringUtils.replace(extension, ".", "");
			}
			iterator.set(extension.toLowerCase().trim());
		}
		return tokenizedList;
	}

	public void initialize() 
	{
		this.ignoredFileNamesCollection = this.transformStringPropertyToListProperty(this.ignoredFileNames);
		this.ignoredFileExtensionsCollection = this.transformStringPropertyToListProperty(this.ignoredFileExtensions, true);
	}
}
