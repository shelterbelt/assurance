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

package com.markallenjohnson.assurance.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.markallenjohnson.assurance.UnitTestUtils;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.FileAttributes;
import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.entities.ScanMappingDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;
import com.markallenjohnson.assurance.model.enums.AssuranceResultReason;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PersistenceTests
{
	@PersistenceContext
	private EntityManager entityManager;

	private UnitTestUtils testHarness = new UnitTestUtils();

	@Rule
	public TestName name = new TestName();

	@BeforeClass
	public static void initialSetUp() throws IOException, SQLException
	{
		UnitTestUtils.installDb();
	}

	@Before
	public void setUp() throws IOException
	{
		testHarness.removeTestArtifacts();
		testHarness.createTestArtifactsLocation("PersistenceTests" + File.separator + name.getMethodName());
		testHarness.buildSimpleTestDataDirectoryStructure();
		testHarness.buildComplexTestDataDirectoryStructures();
	}

	@Test
	@Transactional
	@Rollback(true)
	public void testScanSaveWithResults() throws Exception
	{
		Scan scan = new Scan();

		ComparisonResult result = new ComparisonResult(testHarness.getTestFile1(), testHarness.getTestFile3(), AssuranceResultReason.COMPARE_FAILED);
		scan.addResult(result);

		entityManager.persist(scan);
		entityManager.flush();
		assertNotNull(scan.getId());
		ComparisonResult otherResult = scan.getUnmodifiableResults().iterator().next();
		assertNotNull(otherResult.getId());
		assertNotNull(otherResult.getSource().getId());
		assertNotNull(otherResult.getTarget().getId());
		assertNotNull(otherResult.getSource().getFileAttributes().getId());
		assertNotNull(otherResult.getTarget().getFileAttributes().getId());
	}

	@Test
	@Transactional
	@Rollback(true)
	public void testScanSaveAndGet() throws Exception
	{
		Scan scan = new Scan();
		scan.addResult(new ComparisonResult(testHarness.getTestFile1(), testHarness.getTestFile3(), AssuranceResultReason.COMPARE_FAILED));
		entityManager.persist(scan);
		entityManager.flush();
		entityManager.clear();
		Scan other = (Scan) entityManager.find(Scan.class, scan.getId());
		assertNotNull(other.getScanStarted());
		assertEquals(1, other.getUnmodifiableResults().size());
		ComparisonResult otherResult = scan.getUnmodifiableResults().iterator().next();
		assertEquals(other.getId(), otherResult.getScan().getId());
		assertEquals(otherResult.getReason(), AssuranceResultReason.COMPARE_FAILED);
		assertEquals(otherResult.getResolution(), AssuranceResultResolution.UNRESOLVED);
		assertEquals(otherResult.getSource().getFile().getPath(), testHarness.getTestFile1().getPath());
		assertEquals(otherResult.getTarget().getFile().getPath(), testHarness.getTestFile3().getPath());
		assertEquals(otherResult.getSource().getFileAttributes().getFileReference().getId(), otherResult.getSource().getId());
		assertEquals(otherResult.getTarget().getFileAttributes().getFileReference().getId(), otherResult.getTarget().getId());
		assertTrue(otherResult.getSource().getFileAttributes().getIsRegularFile());
		assertFalse(otherResult.getSource().getFileAttributes().getIsDirectory());
		assertTrue(otherResult.getTarget().getFileAttributes().getIsRegularFile());
		assertFalse(otherResult.getTarget().getFileAttributes().getIsDirectory());
	}

	@Test
	@Transactional
	@Rollback(true)
	public void testScanSaveAndFind() throws Exception
	{
		Scan scan = new Scan();
		ComparisonResult result = new ComparisonResult(testHarness.getTestFile1(), testHarness.getTestFile3(), AssuranceResultReason.COMPARE_FAILED);
		scan.addResult(result);
		entityManager.persist(scan);
		entityManager.flush();
		entityManager.clear();
		Scan other = (Scan) entityManager.createQuery("select s from Scan s join s.results r where r.scan = :scan").setParameter("scan", scan).getSingleResult();
		assertNotNull(other.getScanStarted());
		assertEquals(1, other.getUnmodifiableResults().size());
		ComparisonResult otherResult = scan.getUnmodifiableResults().iterator().next();
		assertEquals(other.getId(), otherResult.getScan().getId());
		assertEquals(otherResult.getReason(), AssuranceResultReason.COMPARE_FAILED);
		assertEquals(otherResult.getResolution(), AssuranceResultResolution.UNRESOLVED);
		assertEquals(otherResult.getSource().getFile().getPath(), testHarness.getTestFile1().getPath());
		assertEquals(otherResult.getTarget().getFile().getPath(), testHarness.getTestFile3().getPath());
		assertEquals(otherResult.getSource().getFileAttributes().getFileReference().getId(), otherResult.getSource().getId());
		assertEquals(otherResult.getTarget().getFileAttributes().getFileReference().getId(), otherResult.getTarget().getId());
		assertTrue(otherResult.getSource().getFileAttributes().getIsRegularFile());
		assertFalse(otherResult.getSource().getFileAttributes().getIsDirectory());
		assertTrue(otherResult.getTarget().getFileAttributes().getIsRegularFile());
		assertFalse(otherResult.getTarget().getFileAttributes().getIsDirectory());
	}

	@Test
	@Transactional
	@Rollback(true)
	public void testScanDelete() throws Exception
	{
		Scan scan = new Scan();
		ComparisonResult result = new ComparisonResult(testHarness.getTestFile1(), testHarness.getTestFile3(), AssuranceResultReason.COMPARE_FAILED);
		scan.addResult(result);
		entityManager.persist(scan);
		entityManager.flush();
		entityManager.clear();
		Scan other = (Scan) entityManager.createQuery("select s from Scan s join s.results r where r.scan = :scan").setParameter("scan", scan).getSingleResult();

		assertNotNull(other.getId());
		ComparisonResult otherResult = other.getUnmodifiableResults().iterator().next();
		assertNotNull(otherResult.getId());
		assertNotNull(otherResult.getSource().getId());
		assertNotNull(otherResult.getTarget().getId());
		assertNotNull(otherResult.getSource().getFileAttributes().getId());
		assertNotNull(otherResult.getTarget().getFileAttributes().getId());

		entityManager.remove(other);
		entityManager.flush();

		boolean exceptionThrown = false;
		try
		{
			@SuppressWarnings("unused")
			Scan deletedScan = (Scan) entityManager.createQuery("select s from Scan s join s.results r where r.scan = :scan").setParameter("scan", scan).getSingleResult();
		}
		catch (NoResultException nre)
		{
			exceptionThrown = true;
		}

		assertTrue(exceptionThrown);

		exceptionThrown = false;
		try
		{
			@SuppressWarnings("unused")
			ComparisonResult deletedResult = (ComparisonResult) entityManager.createQuery("select s from ComparisonResult s where s.id = :id").setParameter("id", result.getId()).getSingleResult();
		}
		catch (NoResultException nre)
		{
			exceptionThrown = true;
		}

		exceptionThrown = false;
		try
		{
			@SuppressWarnings("unused")
			FileReference deletedSourceReference = (FileReference) entityManager.createQuery("select s from FileReference s where s.id = :id").setParameter("id", result.getSource().getId()).getSingleResult();
		}
		catch (NoResultException nre)
		{
			exceptionThrown = true;
		}

		assertTrue(exceptionThrown);

		exceptionThrown = false;
		try
		{
			@SuppressWarnings("unused")
			FileReference deletedTargetReference = (FileReference) entityManager.createQuery("select s from FileReference s where s.id = :id").setParameter("id", result.getTarget().getId()).getSingleResult();
		}
		catch (NoResultException nre)
		{
			exceptionThrown = true;
		}

		assertTrue(exceptionThrown);

		exceptionThrown = false;
		try
		{
			@SuppressWarnings("unused")
			FileAttributes deletedSourceAttributes = (FileAttributes) entityManager.createQuery("select s from FileAttributes s where s.id = :id").setParameter("id", result.getSource().getFileAttributes().getId()).getSingleResult();
		}
		catch (NoResultException nre)
		{
			exceptionThrown = true;
		}

		assertTrue(exceptionThrown);

		exceptionThrown = false;
		try
		{
			@SuppressWarnings("unused")
			FileAttributes deletedTargetAttributes = (FileAttributes) entityManager.createQuery("select s from FileAttributes s where s.id = :id").setParameter("id", result.getTarget().getFileAttributes().getId()).getSingleResult();
		}
		catch (NoResultException nre)
		{
			exceptionThrown = true;
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	@Rollback(true)
	public void testScanDefinitionSaveWithMapping() throws Exception
	{
		ScanDefinition scanDefinition = new ScanDefinition();

		ScanMappingDefinition mappingDefinition = new ScanMappingDefinition();
		mappingDefinition.setSource(testHarness.getTestFile1());
		mappingDefinition.setTarget(testHarness.getTestFile3());
		scanDefinition.addMappingDefinition(mappingDefinition);

		entityManager.persist(scanDefinition);
		entityManager.flush();
		assertNotNull(scanDefinition.getId());
		ScanMappingDefinition otherMappingDefinition = scanDefinition.getUnmodifiableScanMapping().iterator().next();
		assertNotNull(otherMappingDefinition.getId());
		assertNotNull(otherMappingDefinition.getSource());
		assertNotNull(otherMappingDefinition.getTarget());
	}

	@Test
	@Transactional
	@Rollback(true)
	public void testScanDefinitionSaveAndGet() throws Exception
	{
		ScanDefinition scanDefinition = new ScanDefinition();
		ScanMappingDefinition mappingDefinition = new ScanMappingDefinition();
		mappingDefinition.setSource(testHarness.getTestFile1());
		mappingDefinition.setTarget(testHarness.getTestFile2());
		scanDefinition.addMappingDefinition(mappingDefinition);
		scanDefinition.setName("Test Scan Definition");
		scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);
		scanDefinition.setAutoResolveConflicts(true);
		entityManager.persist(scanDefinition);
		entityManager.flush();
		entityManager.clear();
		ScanDefinition other = (ScanDefinition) entityManager.find(ScanDefinition.class, scanDefinition.getId());
		assertEquals(other.getName(), "Test Scan Definition");
		assertEquals(other.getMergeStrategy(), AssuranceMergeStrategy.TARGET);
		assertTrue(other.getAutoResolveConflicts());
		assertEquals(1, other.getUnmodifiableScanMapping().size());
		ScanDefinition originalScanDefinition = other.getUnmodifiableScanMapping().iterator().next().getScanDefinition();
		assertEquals(other, originalScanDefinition);
		ScanMappingDefinition fetchedMappingDefinition = other.getUnmodifiableScanMapping().iterator().next();
		assertEquals(fetchedMappingDefinition.getSource().getAbsolutePath(), testHarness.getTestFile1().getAbsolutePath());
		assertEquals(fetchedMappingDefinition.getTarget().getAbsolutePath(), testHarness.getTestFile2().getAbsolutePath());
	}

	@Test
	@Transactional
	@Rollback(true)
	public void testScanDefinitionSaveAndFind() throws Exception
	{
		ScanDefinition scanDefinition = new ScanDefinition();
		ScanMappingDefinition mappingDefinition = new ScanMappingDefinition();
		mappingDefinition.setSource(testHarness.getTestFile1());
		mappingDefinition.setTarget(testHarness.getTestFile3());
		scanDefinition.addMappingDefinition(mappingDefinition);
		scanDefinition.setName("Test Scan Definition");
		scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);
		scanDefinition.setAutoResolveConflicts(true);
		entityManager.persist(scanDefinition);
		entityManager.flush();
		entityManager.clear();
		ScanDefinition other = (ScanDefinition) entityManager.createQuery("select s from ScanDefinition s join s.scanMapping r where r.scanDefinition = :scanDefinition").setParameter("scanDefinition", scanDefinition).getSingleResult();
		assertEquals(other.getName(), "Test Scan Definition");
		assertEquals(other.getMergeStrategy(), AssuranceMergeStrategy.TARGET);
		assertTrue(other.getAutoResolveConflicts());
		assertEquals(1, other.getUnmodifiableScanMapping().size());
		ScanDefinition originalScanDefinition = other.getUnmodifiableScanMapping().iterator().next().getScanDefinition();
		assertEquals(other, originalScanDefinition);
		ScanMappingDefinition fetchedMappingDefinition = other.getUnmodifiableScanMapping().iterator().next();
		assertEquals(fetchedMappingDefinition.getSource().getAbsolutePath(), testHarness.getTestFile1().getAbsolutePath());
		assertEquals(fetchedMappingDefinition.getTarget().getAbsolutePath(), testHarness.getTestFile3().getAbsolutePath());
	}

	@Test
	@Transactional
	@Rollback(true)
	public void testScanDefinitionDelete() throws Exception
	{
		ScanDefinition scanDefinition = new ScanDefinition();
		ScanMappingDefinition mappingDefinition = new ScanMappingDefinition();
		mappingDefinition.setSource(testHarness.getTestFile1());
		mappingDefinition.setTarget(testHarness.getTestFile2());
		scanDefinition.addMappingDefinition(mappingDefinition);
		scanDefinition.setName("Test Scan Definition");
		scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);
		scanDefinition.setAutoResolveConflicts(true);
		entityManager.persist(scanDefinition);
		entityManager.flush();
		entityManager.clear();
		ScanDefinition other = (ScanDefinition) entityManager.createQuery("select s from ScanDefinition s join s.scanMapping r where r.scanDefinition = :scanDefinition").setParameter("scanDefinition", scanDefinition).getSingleResult();

		assertNotNull(other.getId());
		ScanMappingDefinition otherMappingDefinition = scanDefinition.getUnmodifiableScanMapping().iterator().next();
		assertNotNull(otherMappingDefinition.getId());
		assertNotNull(otherMappingDefinition.getSource());
		assertNotNull(otherMappingDefinition.getTarget());

		entityManager.remove(other);
		entityManager.flush();

		boolean exceptionThrown = false;
		try
		{
			@SuppressWarnings("unused")
			ScanDefinition deletedScanDefinition = (ScanDefinition) entityManager.createQuery("select s from ScanDefinition s join s.scanMapping r where r.scanDefinition = :scanDefinition").setParameter("scanDefinition", scanDefinition).getSingleResult();
		}
		catch (NoResultException nre)
		{
			exceptionThrown = true;
		}

		assertTrue(exceptionThrown);

		exceptionThrown = false;
		try
		{
			@SuppressWarnings("unused")
			ScanMappingDefinition deletedScanMapping = (ScanMappingDefinition) entityManager.createQuery("select s from ScanMappingDefinition s where s.id = :id").setParameter("id", mappingDefinition.getId()).getSingleResult();
		}
		catch (NoResultException nre)
		{
			exceptionThrown = true;
		}

		assertTrue(exceptionThrown);
	}

	@After
	public void tearDown() throws IOException
	{
		testHarness.removeTestArtifacts();
	}
}
