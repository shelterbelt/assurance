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

package com.markallenjohnson.assurance.model;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.markallenjohnson.assurance.exceptions.AssuranceIncompleteScanDefinitionException;
import com.markallenjohnson.assurance.exceptions.AssuranceNullFileReferenceException;
import com.markallenjohnson.assurance.model.compare.IScanOptions;
import com.markallenjohnson.assurance.model.concurrency.IAssuranceThreadPool;
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;
import com.markallenjohnson.assurance.notification.IProgressMonitor;

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

	ScanDefinition saveScanDefinition(ScanDefinition scanDefinition);
	List<ScanDefinition> getScanDefinitions();
	ScanDefinition getScanDefinitionById(Long id);
	void deleteScanDefinition(ScanDefinition scanDefinition);

	/**
	 * Persists a fresh {@link Scan} bound to {@code scanDefinition} so its id
	 * can be returned to a JSON-RPC caller before the comparison work runs.
	 * Subsequent {@link #completeScan(Long, IAssuranceThreadPool, IScanOptions, IProgressMonitor) completeScan}
	 * calls finish the work that the {@link #performScan(ScanDefinition, IAssuranceThreadPool, IScanOptions)}
	 * overload would otherwise have done atomically.
	 */
	Scan openScan(ScanDefinition scanDefinition);

	/**
	 * Performs the comparison work for an already-persisted {@link Scan}
	 * (created via {@link #openScan(ScanDefinition) openScan}) and finalises
	 * it. Returns the now-completed Scan. Used by the async JSON-RPC
	 * {@code assurance.performScan} pipeline.
	 */
	Scan completeScan(Long scanId, IAssuranceThreadPool threadPool, IScanOptions options, IProgressMonitor monitor)
		throws AssuranceNullFileReferenceException, AssuranceIncompleteScanDefinitionException;
	Scan getScanById(Long id);

	/**
	 * Returns how many {@link ComparisonResult} rows belong to {@code scanId}.
	 * Runs in its own read transaction so callers can use the count after another
	 * transactional method (for example {@link #mergeScan}) has closed the
	 * persistence context and detached the {@link Scan}.
	 */
	int countComparisonResultsForScan(Long scanId);

	/**
	 * Returns how many {@link ComparisonResult} rows belong to {@code scanId} and
	 * have a non-{@code UNRESOLVED} resolution — i.e. results the merge engine
	 * (or a previous per-result merge) has already touched. Runs in its own read
	 * transaction so callers can sandwich it around a {@link #mergeScan} call to
	 * compute how many items were actually merged in this run.
	 */
	int countResolvedComparisonResultsForScan(Long scanId);

	List<ComparisonResult> getScanResults(Scan scan);

	/**
	 * Looks up a single {@link ComparisonResult} by its primary key. Returns
	 * {@code null} when no result with that id exists. Used by the IPC layer
	 * to resolve {@code resultId} from {@code assurance.mergeScanResult} and
	 * {@code assurance.restoreDeletedItem} requests.
	 */
	ComparisonResult getResultById(Long id);
	
	IInitializableEntity initializeEntity(IInitializableEntity entity, String propertyKey);

	List<Scan> getScans();
	void deleteScan(Scan scan);

	ComparisonResult restoreDeletedItem(ComparisonResult result);
	ComparisonResult restoreDeletedItem(ComparisonResult result, IProgressMonitor monitor);
	
	ApplicationConfiguration getApplicationConfiguration();
	ApplicationConfiguration saveApplicationConfiguration(ApplicationConfiguration config);
	IScanOptions getScanOptions();
}
