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

package com.markallenjohnson.assurance.model.compare;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.markallenjohnson.assurance.UnitTestUtils;
import com.markallenjohnson.assurance.model.concurrency.AssuranceThreadPool;
import com.markallenjohnson.assurance.model.concurrency.IAssuranceThreadPool;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.enums.AssuranceResultReason;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ComparisonEngineTests
{
	@Autowired
	private IComparisonEngine comparisonEngine;

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
		testHarness.createTestArtifactsLocation("ComparisonEngineTests" + File.separator + name.getMethodName());
		testHarness.buildSimpleTestDataDirectoryStructure();
		testHarness.buildComplexTestDataDirectoryStructures();
		testHarness.buildIgnoredFilesTestDataDirectoryStructures();
	}

	@Test
	@Transactional
	public void testSimpleIdenticalSourceTargetDirectories() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = testHarness.getTestDirectory1();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertTrue(scan.getUnmodifiableResults().size() == 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleDifferentSourceTargetDirectories() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = testHarness.getTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertTrue(scan.getUnmodifiableResults().size() == 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceFileTargetDirectory() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(result.getReason(), AssuranceResultReason.FILE_DIRECTORY_MISMATCH);
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertEquals(result.getSource().getFile().getPath(), testFile1.getPath());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertEquals(result.getTarget().getFile().getPath(), testFile2.getPath());
				assertNotNull(result.getSource().getFileAttributes());
				assertFalse(result.getSource().getFileAttributes().getIsDirectory());
				assertNotNull(result.getTarget().getFileAttributes());
				assertTrue(result.getTarget().getFileAttributes().getIsDirectory());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceDirectoryTargetFile() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = testHarness.getTestFile2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(result.getReason(), AssuranceResultReason.FILE_DIRECTORY_MISMATCH);
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertEquals(result.getSource().getFile().getPath(), testFile1.getPath());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertEquals(result.getTarget().getFile().getPath(), testFile2.getPath());
				assertNotNull(result.getSource().getFileAttributes());
				assertTrue(result.getSource().getFileAttributes().getIsDirectory());
				assertNotNull(result.getTarget().getFileAttributes());
				assertFalse(result.getTarget().getFileAttributes().getIsDirectory());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceFileIdenticalTargetFile() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceFileDifferentTargetFile() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(result.getReason(), AssuranceResultReason.COMPARE_FAILED);
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertEquals(result.getSource().getFile().getPath(), testFile1.getPath());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertEquals(result.getTarget().getFile().getPath(), testFile2.getPath());
				assertNotNull(result.getSource().getFileAttributes());
				assertFalse(result.getSource().getFileAttributes().getIsDirectory());
				assertTrue(result.getSource().getFileAttributes().getIsRegularFile());
				assertNotNull(result.getTarget().getFileAttributes());
				assertFalse(result.getTarget().getFileAttributes().getIsDirectory());
				assertTrue(result.getTarget().getFileAttributes().getIsRegularFile());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testComplexSourceDirectoryIdenticalComplexTargetDirectory() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testComplexSourceDirectoryAlternateComplexTargetDirectory() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(12, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertNotNull(result.getSource().getFileAttributes());
				if (result.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					assertFalse(result.getSource().getFileAttributes().getIsDirectory());
					assertTrue(result.getSource().getFileAttributes().getIsRegularFile());
				}
				assertNotNull(result.getTarget().getFileAttributes());
				if (result.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					assertFalse(result.getTarget().getFileAttributes().getIsDirectory());
					assertTrue(result.getTarget().getFileAttributes().getIsRegularFile());
				}
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testAlternateComplexSourceDirectoryComplexTargetDirectory() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(12, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertNotNull(result.getSource().getFileAttributes());
				if (result.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					assertFalse(result.getSource().getFileAttributes().getIsDirectory());
					assertTrue(result.getSource().getFileAttributes().getIsRegularFile());
				}
				assertNotNull(result.getTarget().getFileAttributes());
				if (result.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					assertFalse(result.getTarget().getFileAttributes().getIsDirectory());
					assertTrue(result.getTarget().getFileAttributes().getIsRegularFile());
				}
			}
		}
		finally
		{
		}
	}
	@Test
	@Transactional
	public void testComplexSourceDirectorySimpleTargetDirectory() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(10, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertNotNull(result.getSource().getFileAttributes());
				assertNotNull(result.getTarget().getFileAttributes());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testNullSourceTargetDirectory() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = null;
			File testFile2 = testHarness.getTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(result.getReason(), AssuranceResultReason.FILE_NULL);
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNull(result.getSource().getFile());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertNotNull(result.getTarget().getFileAttributes());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSourceDirectoryNullTarget() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = null;

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(result.getReason(), AssuranceResultReason.FILE_NULL);
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertNotNull(result.getTarget());
				assertNull(result.getTarget().getFile());
				assertNotNull(result.getSource().getFileAttributes());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testNullSourceNullTarget() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = null;
			File testFile2 = null;

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleIdenticalSourceTargetDirectoriesUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = testHarness.getTestDirectory1();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertTrue(scan.getUnmodifiableResults().size() == 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleDifferentSourceTargetDirectoriesUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = testHarness.getTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertTrue(scan.getUnmodifiableResults().size() == 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceFileTargetDirectoryUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(result.getReason(), AssuranceResultReason.FILE_DIRECTORY_MISMATCH);
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertEquals(result.getSource().getFile().getPath(), testFile1.getPath());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertEquals(result.getTarget().getFile().getPath(), testFile2.getPath());
				assertNotNull(result.getSource().getFileAttributes());
				assertFalse(result.getSource().getFileAttributes().getIsDirectory());
				assertNotNull(result.getTarget().getFileAttributes());
				assertTrue(result.getTarget().getFileAttributes().getIsDirectory());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceDirectoryTargetFileUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = testHarness.getTestFile2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(result.getReason(), AssuranceResultReason.FILE_DIRECTORY_MISMATCH);
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertEquals(result.getSource().getFile().getPath(), testFile1.getPath());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertEquals(result.getTarget().getFile().getPath(), testFile2.getPath());
				assertNotNull(result.getSource().getFileAttributes());
				assertTrue(result.getSource().getFileAttributes().getIsDirectory());
				assertNotNull(result.getTarget().getFileAttributes());
				assertFalse(result.getTarget().getFileAttributes().getIsDirectory());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceFileIdenticalTargetFileUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions());

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceFileDifferentTargetFileUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), true);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(result.getReason(), AssuranceResultReason.COMPARE_FAILED);
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertEquals(result.getSource().getFile().getPath(), testFile1.getPath());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertEquals(result.getTarget().getFile().getPath(), testFile2.getPath());
				assertNotNull(result.getSource().getFileAttributes());
				assertFalse(result.getSource().getFileAttributes().getIsDirectory());
				assertTrue(result.getSource().getFileAttributes().getIsRegularFile());
				assertNotNull(result.getTarget().getFileAttributes());
				assertFalse(result.getTarget().getFileAttributes().getIsDirectory());
				assertTrue(result.getTarget().getFileAttributes().getIsRegularFile());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testIgnoredFilesUsingSimpleScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getIgnoredTestDirectory1();
			File testFile2 = testHarness.getIgnoredTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), false);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(1, scan.getUnmodifiableResults().size());
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testExcludedFilesUsingSimpleScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), testHarness.getTestExclusions(), false);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(9, scan.getUnmodifiableResults().size());
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testComplexSourceDirectoryIdenticalComplexTargetDirectoryUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), true);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testComplexSourceDirectoryAlternateComplexTargetDirectoryUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), true);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(12, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertNotNull(result.getSource().getFileAttributes());
				if (result.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					assertFalse(result.getSource().getFileAttributes().getIsDirectory());
					assertTrue(result.getSource().getFileAttributes().getIsRegularFile());
				}
				assertNotNull(result.getTarget().getFileAttributes());
				if (result.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					assertFalse(result.getTarget().getFileAttributes().getIsDirectory());
					assertTrue(result.getTarget().getFileAttributes().getIsRegularFile());
				}
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testAlternateComplexSourceDirectoryComplexTargetDirectoryUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getComplexTestDirectory3();
			File testFile2 = testHarness.getComplexTestDirectory1();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), true);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(12, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertNotNull(result.getSource().getFileAttributes());
				if (result.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					assertFalse(result.getSource().getFileAttributes().getIsDirectory());
					assertTrue(result.getSource().getFileAttributes().getIsRegularFile());
				}
				assertNotNull(result.getTarget().getFileAttributes());
				if (result.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					assertFalse(result.getTarget().getFileAttributes().getIsDirectory());
					assertTrue(result.getTarget().getFileAttributes().getIsRegularFile());
				}
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testComplexSourceDirectorySimpleTargetDirectoryUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), true);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(10, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertNotNull(result.getSource().getFileAttributes());
				assertNotNull(result.getTarget().getFileAttributes());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testNullSourceTargetDirectoryUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = null;
			File testFile2 = testHarness.getTestDirectory2();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), true);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(result.getReason(), AssuranceResultReason.FILE_NULL);
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNull(result.getSource().getFile());
				assertNotNull(result.getTarget());
				assertNotNull(result.getTarget().getFile());
				assertNotNull(result.getTarget().getFileAttributes());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSourceDirectoryNullTargetUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = null;

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), true);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(result.getReason(), AssuranceResultReason.FILE_NULL);
				assertEquals(result.getResolution(), AssuranceResultResolution.UNRESOLVED);
				assertNotNull(result.getScan());
				assertNotNull(result.getSource());
				assertNotNull(result.getSource().getFile());
				assertNotNull(result.getTarget());
				assertNull(result.getTarget().getFile());
				assertNotNull(result.getSource().getFileAttributes());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testNullSourceNullTargetUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = null;
			File testFile2 = null;

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), true);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(scan.getUnmodifiableResults().size(), 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testExcludedFilesUsingComprehensiveScan() throws Exception
	{
		try
		{
			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			
			Scan scan = new Scan();

			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();

			threadPool.register();
			
			comparisonEngine.determineDifferences(testFile1, testFile2, scan, threadPool, testHarness.getScanOptions(), testHarness.getTestExclusions(), true);

			threadPool.await();

			entityManager.persist(scan);
			entityManager.flush();

			assertEquals(9, scan.getUnmodifiableResults().size());
		}
		finally
		{
		}
	}

	@After
	public void tearDown() throws IOException
	{
		testHarness.removeTestArtifacts();
	}
}
