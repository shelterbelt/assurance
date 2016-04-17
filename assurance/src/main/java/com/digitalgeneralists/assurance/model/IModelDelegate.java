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

package com.digitalgeneralists.assurance.model;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.digitalgeneralists.assurance.exceptions.AssuranceIncompleteScanDefinitionException;
import com.digitalgeneralists.assurance.exceptions.AssuranceNullFileReferenceException;
import com.digitalgeneralists.assurance.model.compare.IScanOptions;
import com.digitalgeneralists.assurance.model.concurrency.IAssuranceThreadPool;
import com.digitalgeneralists.assurance.model.entities.ApplicationConfiguration;
import com.digitalgeneralists.assurance.model.entities.ComparisonResult;
import com.digitalgeneralists.assurance.model.entities.FileReference;
import com.digitalgeneralists.assurance.model.entities.Scan;
import com.digitalgeneralists.assurance.model.entities.ScanDefinition;
import com.digitalgeneralists.assurance.model.enums.AssuranceMergeStrategy;
import com.digitalgeneralists.assurance.notification.IProgressMonitor;

public interface IModelDelegate
{
	Scan performScan(Scan scan, File source, File target, IAssuranceThreadPool threadPool, IScanOptions options);
	Scan performScan(Scan scan, File source, File target, IAssuranceThreadPool threadPool, IScanOptions options, Collection<FileReference> exclusions);
	Scan performScan(Scan scan, File source, File target, IAssuranceThreadPool threadPool, IScanOptions options, Collection<FileReference> exclusions, IProgressMonitor monitor);
	Scan performScan(ScanDefinition scanDefinition, IAssuranceThreadPool threadPool, IScanOptions options) throws AssuranceNullFileReferenceException, AssuranceIncompleteScanDefinitionException;
	Scan performScan(ScanDefinition scanDefinition, IAssuranceThreadPool threadPool, IScanOptions options, IProgressMonitor monitor) throws AssuranceNullFileReferenceException, AssuranceIncompleteScanDefinitionException;

	ComparisonResult mergeScanResult(ComparisonResult result, AssuranceMergeStrategy strategy) throws AssuranceNullFileReferenceException;
	ComparisonResult mergeScanResult(ComparisonResult result, AssuranceMergeStrategy strategy, IProgressMonitor monitor) throws AssuranceNullFileReferenceException;
	Scan mergeScan(Scan scan, IAssuranceThreadPool threadPool) throws AssuranceNullFileReferenceException;
	Scan mergeScan(Scan scan, IAssuranceThreadPool threadPool, IProgressMonitor monitor) throws AssuranceNullFileReferenceException;

	void saveScanDefinition(ScanDefinition scanDefinition);
	List<ScanDefinition> getScanDefinitions();
	void deleteScanDefinition(ScanDefinition scanDefinition);

	List<ComparisonResult> getScanResults(Scan scan);
	
	IInitializableEntity initializeEntity(IInitializableEntity entity, String propertyKey);

	List<Scan> getScans();
	void deleteScan(Scan scan);

	ComparisonResult restoreDeletedItem(ComparisonResult result);
	ComparisonResult restoreDeletedItem(ComparisonResult result, IProgressMonitor monitor);
	
	ApplicationConfiguration getApplicationConfiguration();
	void saveApplicationConfiguration(ApplicationConfiguration config);
	IScanOptions getScanOptions();
}
