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
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.digitalgeneralists.assurance.exceptions.AssuranceIncompleteScanDefinitionException;
import com.digitalgeneralists.assurance.exceptions.AssuranceNullFileReferenceException;
import com.digitalgeneralists.assurance.model.compare.IComparisonEngine;
import com.digitalgeneralists.assurance.model.compare.IScanOptions;
import com.digitalgeneralists.assurance.model.concurrency.IAssuranceThreadPool;
import com.digitalgeneralists.assurance.model.entities.ApplicationConfiguration;
import com.digitalgeneralists.assurance.model.entities.ComparisonResult;
import com.digitalgeneralists.assurance.model.entities.FileReference;
import com.digitalgeneralists.assurance.model.entities.Scan;
import com.digitalgeneralists.assurance.model.entities.ScanDefinition;
import com.digitalgeneralists.assurance.model.entities.ScanMappingDefinition;
import com.digitalgeneralists.assurance.model.enums.AssuranceMergeStrategy;
import com.digitalgeneralists.assurance.model.enums.AssuranceResultResolution;
import com.digitalgeneralists.assurance.model.factories.IMergeEngineFactory;
import com.digitalgeneralists.assurance.model.merge.IMergeEngine;
import com.digitalgeneralists.assurance.notification.IProgressMonitor;

@Component("ModelDelegate")
public class ModelDelegate implements IModelDelegate
{
	private Logger logger = Logger.getLogger(ModelDelegate.class);

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private IComparisonEngine comparisonEngine;

	@Autowired
	private IMergeEngineFactory mergeEngineFactory;

	ModelDelegate()
	{
		logger.info("Creating the Model Delegate.");
	}

	@Transactional
	public Scan performScan(Scan scan, File source, File target, IAssuranceThreadPool threadPool, IScanOptions options)
	{
		return this.performScan(scan, source, target, threadPool, options, null);
	}

	@Transactional
	public Scan performScan(Scan scan, File source, File target, IAssuranceThreadPool threadPool, IScanOptions options, Collection<FileReference> exclusions)
	{
		return this.performScan(scan, source, target, threadPool, options, exclusions, null);
	}

	@Transactional
	public Scan performScan(Scan scan, File source, File target, IAssuranceThreadPool threadPool, IScanOptions options, Collection<FileReference> exclusions, IProgressMonitor monitor)
	{
		boolean scanProvided = false;

		if (scan == null)
		{
			scan = new Scan();
		}
		else
		{
			scanProvided = true;
		}

		this.comparisonEngine.determineDifferences(source, target, scan, threadPool, options, exclusions, monitor);

		if (!scanProvided)
		{
			threadPool.await();
			
			synchronized (scan)
			{
				if (scan != null)
				{
					Date whenCompleted = new Date();
					scan.setScanCompleted(whenCompleted);
					whenCompleted = null;
				}
			}
		}

		synchronized (scan)
		{
			this.entityManager.persist(scan);
			this.entityManager.flush();
		}

		return scan;
	}

	@Transactional
	public Scan performScan(ScanDefinition scanDefinition, IAssuranceThreadPool threadPool, IScanOptions options) throws AssuranceNullFileReferenceException, AssuranceIncompleteScanDefinitionException
	{
		return this.performScan(scanDefinition, threadPool, options, null);
	}

	@Transactional
	public Scan performScan(ScanDefinition scanDefinition, IAssuranceThreadPool threadPool, IScanOptions options, IProgressMonitor monitor) throws AssuranceNullFileReferenceException, AssuranceIncompleteScanDefinitionException
	{
		if ((scanDefinition == null) || (scanDefinition.getUnmodifiableScanMapping() == null))
		{
			throw new AssuranceIncompleteScanDefinitionException("No scan definition provided to performScan.");
		}

		for (ScanMappingDefinition mapping : scanDefinition.getUnmodifiableScanMapping())
		{
			if ((mapping.getSource() == null) || (mapping.getTarget() == null))
			{
				throw new AssuranceNullFileReferenceException("A source or target definition is null.");
			}
			mapping = null;
		}

		Scan scan = new Scan();
		if (!entityManager.contains(scanDefinition))
		{
			scanDefinition = entityManager.merge(scanDefinition);
		}
		scan.setScanDef(scanDefinition);

		for (ScanMappingDefinition definitionMapping : scanDefinition.getUnmodifiableScanMapping())
		{
			scan = this.performScan(scan, definitionMapping.getSource(), definitionMapping.getTarget(), threadPool, options, definitionMapping.getUnmodifiableExclusions(), monitor);
			definitionMapping = null;
		}

		threadPool.await();
		
		synchronized (scan)
		{
			if (scan != null)
			{
				Date whenCompleted = new Date();
				scan.setScanCompleted(whenCompleted);
				whenCompleted = null;
				entityManager.persist(scan);
				entityManager.flush();
			}
		}

		return scan;
	}

	@Transactional
	public void saveScanDefinition(ScanDefinition scanDefinition)
	{
		if (!entityManager.contains(scanDefinition))
		{
			scanDefinition = entityManager.merge(scanDefinition);
		}
		entityManager.persist(scanDefinition);
		entityManager.flush();
	}

	@Transactional
	public List<ScanDefinition> getScanDefinitions()
	{
		Query query = entityManager.createQuery("from ScanDefinition");
		@SuppressWarnings("unchecked")
		List<ScanDefinition> results = query.getResultList();
		query = null;
		return results;
	}

	@Transactional
	public void deleteScanDefinition(ScanDefinition scanDefinition)
	{
		if (!entityManager.contains(scanDefinition))
		{
			scanDefinition = entityManager.merge(scanDefinition);
		}

		entityManager.remove(scanDefinition);
		entityManager.flush();
	}

	@Transactional
	public List<ComparisonResult> getScanResults(Scan scan)
	{
		Query query = entityManager.createQuery("from ComparisonResult cr where cr.scan = :scan").setParameter("scan", scan);
		@SuppressWarnings("unchecked")
		List<ComparisonResult> results = query.getResultList();
		query = null;
		return results;
	}
	
	@Transactional 
	public IInitializableEntity initializeEntity(IInitializableEntity entity, String propertyKey)
	{
		
		if (entity != null)
		{
			if (!entityManager.contains(entity))
			{
				entity = entityManager.merge(entity);
			}
			
			Hibernate.initialize(entity.getPropertyToInitialize(propertyKey));
		}
		
		return entity;
	}

	@Transactional
	public List<Scan> getScans()
	{
		Query query = entityManager.createQuery("from Scan");
		@SuppressWarnings("unchecked")
		List<Scan> results = query.getResultList();
		query = null;
		return results;
	}

	@Transactional
	public void deleteScan(Scan scan)
	{
		if (!entityManager.contains(scan))
		{
			scan = entityManager.merge(scan);
		}

		entityManager.remove(scan);
		entityManager.flush();
	}

	@Transactional
	public ComparisonResult mergeScanResult(ComparisonResult result, AssuranceMergeStrategy strategy)
	{
		return this.mergeScanResult(result, strategy, null);
	}

	@Transactional
	public ComparisonResult mergeScanResult(ComparisonResult result, AssuranceMergeStrategy strategy, IProgressMonitor monitor)
	{
		if (!entityManager.contains(result))
		{
			result = entityManager.merge(result);
		}

		IMergeEngine mergeEngine = this.mergeEngineFactory.createInstance(strategy);
		try
		{
			mergeEngine.mergeResult(result, monitor);;
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			logger.error("An error occurred when merging the source with the target.");
			result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
			result.setResolutionError(anfre.getMessage());
		}
		finally
		{
			mergeEngine = null;
		}

		synchronized (result)
		{
			entityManager.persist(result);
			entityManager.flush();
		}

		return result;
	}

	@Transactional
	public Scan mergeScan(Scan scan, IAssuranceThreadPool threadPool) throws AssuranceNullFileReferenceException
	{
		return this.mergeScan(scan, threadPool, null);
	}

	@Transactional
	public Scan mergeScan(Scan scan, IAssuranceThreadPool threadPool, IProgressMonitor monitor) throws AssuranceNullFileReferenceException
	{
		if (!entityManager.contains(scan))
		{
			scan = entityManager.merge(scan);
		}

		AssuranceMergeStrategy strategy = AssuranceMergeStrategy.BOTH;
		ScanDefinition scanDefinition = scan.getScanDef();
		if (scanDefinition != null)
		{
			strategy = scanDefinition.getMergeStrategy();
		}
		scanDefinition = null;
		IMergeEngine mergeEngine = this.mergeEngineFactory.createInstance(strategy);
		mergeEngine.mergeScan(scan, threadPool, monitor);

		threadPool.await();
		
		synchronized (scan)
		{
			entityManager.persist(scan);
			entityManager.flush();
		}

		return scan;
	}

	@Transactional
	public ComparisonResult restoreDeletedItem(ComparisonResult result)
	{
		return this.restoreDeletedItem(result, null);
	}

	@Transactional
	public ComparisonResult restoreDeletedItem(ComparisonResult result, IProgressMonitor monitor)
	{
		if (!entityManager.contains(result))
		{
			result = entityManager.merge(result);
		}

		AssuranceMergeStrategy strategy = AssuranceMergeStrategy.BOTH;
		if (result.getResolution() == AssuranceResultResolution.DELETE_SOURCE)
		{
			strategy = AssuranceMergeStrategy.TARGET;
		}
		if (result.getResolution() == AssuranceResultResolution.DELETE_TARGET)
		{
			strategy = AssuranceMergeStrategy.SOURCE;
		}

		IMergeEngine mergeEngine = this.mergeEngineFactory.createInstance(strategy);
		mergeEngine.restoreDeletedItem(result, monitor);
		mergeEngine = null;

		entityManager.persist(result);
		entityManager.flush();

		return result;
	}

	@Transactional
	public ApplicationConfiguration getApplicationConfiguration() 
	{
		Query query = entityManager.createQuery("from ApplicationConfiguration");
		@SuppressWarnings("unchecked")
		List<ApplicationConfiguration> results = query.getResultList();
		query = null;
		if (results.size() > 1)
		{
			logger.warn("Multiple application configuration records detected.  Using the first.");
		}
		else
		{
			if (results.size() == 0)
			{
				logger.warn("No application configuration found.  Creating default.");
				results.add(ApplicationConfiguration.createDefaultConfiguration());
			}
		}
		
		ApplicationConfiguration result = results.get(0);
		if (result != null)
		{
			// NOTE:  This is not ideal.  I would like to apply something to the Hibernate
			// query more globally.
			result.initialize();
		}
		
		return result;
	}

	@Transactional
	public void saveApplicationConfiguration(ApplicationConfiguration config) 
	{
		if (!entityManager.contains(config))
		{
			config = entityManager.merge(config);
		}
		entityManager.persist(config);
		entityManager.flush();
	}
	
	@Transactional
	public IScanOptions getScanOptions() 
	{
		return this.getApplicationConfiguration();
	}
}
