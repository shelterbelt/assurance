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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.markallenjohnson.assurance.UnitTestUtils;
import com.markallenjohnson.assurance.exceptions.AssuranceNullFileReferenceException;
import com.markallenjohnson.assurance.model.concurrency.AssuranceThreadPool;
import com.markallenjohnson.assurance.model.concurrency.IAssuranceThreadPool;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;
import com.markallenjohnson.assurance.model.enums.AssuranceResultReason;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ModelDelegateTests
{
	@Autowired
	private IModelDelegate modelDelegate;

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
		testHarness.createTestArtifactsLocation("ModelDelegateTests" + File.separator + name.getMethodName());
		testHarness.buildSimpleTestDataDirectoryStructure();
		testHarness.buildComplexTestDataDirectoryStructures();
		testHarness.buildIgnoredFilesTestDataDirectoryStructures();
	}

	@Test
	@Transactional
	public void testSimpleIdenticalSourceTargetDirectoriesWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = testHarness.getTestDirectory1();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertTrue(scan.getUnmodifiableResults().size() == 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleDifferentSourceTargetDirectoriesWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = testHarness.getTestDirectory2();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertTrue(scan.getUnmodifiableResults().size() == 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceFileTargetDirectoryWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestDirectory2();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertNotNull(scan.getScanStarted());
			assertTrue(scan.getUnmodifiableResults().size() == 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(AssuranceResultReason.FILE_DIRECTORY_MISMATCH, result.getReason());
				assertEquals(AssuranceResultResolution.UNRESOLVED, result.getResolution());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceDirectoryTargetFileWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = testHarness.getTestFile2();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertNotNull(scan.getScanStarted());
			assertTrue(scan.getUnmodifiableResults().size() == 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(AssuranceResultReason.FILE_DIRECTORY_MISMATCH, result.getReason());
				assertEquals(AssuranceResultResolution.UNRESOLVED, result.getResolution());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceFileIdenticalTargetFileWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile2();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertNotNull(scan.getScanStarted());
			assertTrue(scan.getUnmodifiableResults().size() == 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSimpleSourceFileDifferentTargetFileWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertNotNull(scan.getScanStarted());
			assertTrue(scan.getUnmodifiableResults().size() == 1);
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(AssuranceResultReason.COMPARE_FAILED, result.getReason());
				assertEquals(AssuranceResultResolution.UNRESOLVED, result.getResolution());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testComplexSourceDirectoryIdenticalComplexTargetDirectoryWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory2();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertNotNull(scan.getScanStarted());
			assertTrue(scan.getUnmodifiableResults().size() == 0);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testComplexSourceDirectoryAlternateComplexTargetDirectoryWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertNotNull(scan.getScanStarted());
			assertEquals(12, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(AssuranceResultResolution.UNRESOLVED, result.getResolution());
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testAlternateComplexSourceDirectoryComplexTargetDirectoryWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory3();
			File testFile2 = testHarness.getComplexTestDirectory1();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertNotNull(scan.getScanStarted());
			assertEquals(12, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(AssuranceResultResolution.UNRESOLVED, result.getResolution());
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testComplexSourceDirectorySimpleTargetDirectoryWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getTestDirectory2();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertNotNull(scan.getScanStarted());
			assertEquals(10, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.COMPARE_FAILED)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				assertEquals(AssuranceResultResolution.UNRESOLVED, result.getResolution());
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testNullSourceTargetDirectoryWithScanDefinition() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getTestDirectory2();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			@SuppressWarnings("unused")
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testSourceDirectoryNullTargetWithScanDefinition() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = null;
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			@SuppressWarnings("unused")
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testNullSourceNullTargetWithScanDefinition() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = null;
			File testFile2 = null;
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			@SuppressWarnings("unused")
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testMergeSimpleScanWithSourceStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.SOURCE);
			String originalSourceContents = testHarness.readTestFileContents(testHarness.getTestFile1());

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
			assertEquals(1, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(AssuranceResultReason.COMPARE_FAILED, result.getReason());
				assertEquals(AssuranceResultResolution.REPLACE_TARGET, result.getResolution());
				String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
				String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
				assertEquals(sourceContents, targetContents);
				assertEquals(originalSourceContents, sourceContents);
				assertEquals(originalSourceContents, targetContents);
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeComplexScanWithSourceStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.SOURCE);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
			assertEquals(12, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.DELETE_SOURCE)));
				assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.REPLACE_SOURCE)));
				assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.UNRESOLVED)));
				if (result.getSource().getFile().isDirectory())
				{
					assertTrue(result.getTarget().getFile().isDirectory());
					assertEquals(result.getSource().getFile().listFiles().length, result.getTarget().getFile().listFiles().length);
				}
				else
				{
					if (result.getResolution() == AssuranceResultResolution.DELETE_TARGET)
					{
						assertFalse(result.getTarget().getFile().exists());
					}
					else
					{
						String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
						String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
						assertEquals(sourceContents, targetContents);
					}
				}
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullSourceScanWithSourceStrategy() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.SOURCE);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testMergeNullTargetScanWithSourceStrategy() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = null;
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.SOURCE);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testMergeSimpleScanWithTargetStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);
			String originalTargetContents = testHarness.readTestFileContents(testHarness.getTestFile3());

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
			assertEquals(1, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(AssuranceResultReason.COMPARE_FAILED, result.getReason());
				assertEquals(AssuranceResultResolution.REPLACE_SOURCE, result.getResolution());
				String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
				String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
				assertEquals(targetContents, sourceContents);
				assertEquals(originalTargetContents, sourceContents);
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeComplexScanWithTargetStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
			assertEquals(12, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				if (result.getReason() == AssuranceResultReason.TARGET_DOES_NOT_EXIST)
				{
					assertEquals(AssuranceResultResolution.DELETE_SOURCE, result.getResolution());
				}
				else
				{
					assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.REPLACE_TARGET)));
					assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.DELETE_SOURCE)));
					assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.DELETE_TARGET)));
				}
				if (result.getTarget().getFile().isDirectory())
				{
					// The relevant strategy may not pull sources and targets over.
					if (result.getSource().getFile().exists())
					{
						assertTrue(result.getSource().getFile().isDirectory());
						assertEquals(result.getTarget().getFile().listFiles().length, result.getSource().getFile().listFiles().length);
					}
				}
				else
				{
					if (result.getResolution() == AssuranceResultResolution.DELETE_SOURCE)
					{
						assertFalse(result.getSource().getFile().exists());
					}
					else
					{
						String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
						String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
						assertEquals(targetContents, sourceContents);
					}
				}
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullSourceScanWithTargetStrategy() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testMergeNullTargetScanWithTargetStrategy() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = null;
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testMergeSimpleScanWithBiDirectionalStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.BOTH);
			String originalSourceContents = testHarness.readTestFileContents(testHarness.getTestFile1());

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
			assertEquals(1, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(AssuranceResultReason.COMPARE_FAILED, result.getReason());
				assertEquals(AssuranceResultResolution.REPLACE_TARGET, result.getResolution());
				String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
				String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
				assertEquals(sourceContents, targetContents);
				assertEquals(originalSourceContents, sourceContents);
				assertEquals(originalSourceContents, targetContents);
			}
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeComplexScanWithBiDirectionalStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.BOTH);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
			assertEquals(12, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.DELETE_SOURCE)));
				assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.DELETE_TARGET)));
				assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.UNRESOLVED)));
				if ((result.getSource().getFile().isDirectory()) || (result.getSource().getFile().isDirectory()))
				{
					assertEquals(result.getSource().getFile().listFiles().length, result.getTarget().getFile().listFiles().length);
				}
				else
				{
					String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
					String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
					assertEquals(sourceContents, targetContents);
				}
			}
		}
		finally
		{
		}
	}
	@Test
	@Transactional
	public void testMergeNullSourceScanWithBiDirectionalStrategy() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.BOTH);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testMergeNullTargetScanWithBiDirectionalStrategy() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = null;
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.BOTH);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testMergeWithAutoResolveOff() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setAutoResolveConflicts(false);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			scan = modelDelegate.mergeScan(scan, threadPool);
			assertEquals(12, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
				if (result.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					assertEquals(AssuranceResultResolution.UNRESOLVED, result.getResolution());

					if (result.getSource().getFile().isDirectory())
					{
						assertTrue(result.getTarget().getFile().isDirectory());
						assertEquals(result.getSource().getFile().listFiles().length, result.getTarget().getFile().listFiles().length);
					}
					else
					{
						if (result.getResolution() == AssuranceResultResolution.DELETE_TARGET)
						{
							assertFalse(result.getTarget().getFile().exists());
						}
						else
						{
							String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
							String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
							if (result.getReason() == AssuranceResultReason.COMPARE_FAILED)
							{
								assertThat(sourceContents, not(equalTo(targetContents)));
							}
							else
							{
								assertEquals(sourceContents, targetContents);
							}
						}
					}
				}
				else
				{
					assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.REPLACE_SOURCE)));
					assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.DELETE_TARGET)));
					assertThat(result.getResolution(), not(equalTo(AssuranceResultResolution.DELETE_SOURCE)));
				}
			}
		}
		finally
		{
		}
	}
	
	@Test
	@Transactional
	public void testMergeSimpleScanResultWithSourceStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			ComparisonResult result = null;
			for (ComparisonResult fetchedResult : scan.getUnmodifiableResults())
			{
				if (fetchedResult.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					result = fetchedResult;
					break;
				}
			}
			result = modelDelegate.mergeScanResult(result, AssuranceMergeStrategy.SOURCE);
			assertEquals(AssuranceResultReason.COMPARE_FAILED, result.getReason());
			assertEquals(AssuranceResultResolution.REPLACE_TARGET, result.getResolution());
			String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
			String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
			assertEquals(sourceContents, targetContents);
		}
		finally
		{
		}
	}

	@Ignore
	@Test
	@Transactional
	public void testMergeComplexScanResultWithSourceStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);
			String originalSourceContents = testHarness.readTestFileContents(testHarness.getTestFile1());

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			ComparisonResult result = null;
			for (ComparisonResult fetchedResult : scan.getUnmodifiableResults())
			{
				if (fetchedResult.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					result = fetchedResult;
					break;
				}
			}
			result = modelDelegate.mergeScanResult(result, AssuranceMergeStrategy.SOURCE);
			assertEquals(AssuranceResultReason.COMPARE_FAILED, result.getReason());
			assertEquals(AssuranceResultResolution.REPLACE_TARGET, result.getResolution());
			String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
			String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
			assertEquals(sourceContents, targetContents);
			assertEquals(originalSourceContents, targetContents);
		}
		finally
		{
		}
	}
	
	@Test
	@Transactional
	public void testMergeNullSourceScanResultWithSourceStrategy() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			ComparisonResult result = null;
			for (ComparisonResult fetchedResult : scan.getUnmodifiableResults())
			{
				if (fetchedResult.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					result = fetchedResult;
					break;
				}
			}
			result = modelDelegate.mergeScanResult(result, AssuranceMergeStrategy.SOURCE);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testMergeNullTargetScanResultWithSourceStrategy() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = null;
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			ComparisonResult result = null;
			for (ComparisonResult fetchedResult : scan.getUnmodifiableResults())
			{
				if (fetchedResult.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					result = fetchedResult;
					break;
				}
			}
			result = modelDelegate.mergeScanResult(result, AssuranceMergeStrategy.SOURCE);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testMergeSimpleScanResultWithTargetStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);
			String originalTargetContents = testHarness.readTestFileContents(testHarness.getTestFile3());

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			ComparisonResult result = null;
			for (ComparisonResult fetchedResult : scan.getUnmodifiableResults())
			{
				if (fetchedResult.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					result = fetchedResult;
					break;
				}
			}
			result = modelDelegate.mergeScanResult(result, AssuranceMergeStrategy.TARGET);
			assertEquals(AssuranceResultReason.COMPARE_FAILED, result.getReason());
			assertEquals(AssuranceResultResolution.REPLACE_SOURCE, result.getResolution());
			String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
			String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
			assertEquals(targetContents, sourceContents);
			assertEquals(originalTargetContents, targetContents);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeComplexScanResultWithTargetStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			ComparisonResult result = null;
			for (ComparisonResult fetchedResult : scan.getUnmodifiableResults())
			{
				if (fetchedResult.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					result = fetchedResult;
					break;
				}
			}
			result = modelDelegate.mergeScanResult(result, AssuranceMergeStrategy.TARGET);
			assertEquals(AssuranceResultReason.COMPARE_FAILED, result.getReason());
			assertEquals(AssuranceResultResolution.REPLACE_SOURCE, result.getResolution());
			String sourceContents = testHarness.readTestFileContents(result.getSource().getFile());
			String targetContents = testHarness.readTestFileContents(result.getTarget().getFile());
			assertEquals(targetContents, sourceContents);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullSourceScanResultWithTargetStrategy() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			ComparisonResult result = null;
			for (ComparisonResult fetchedResult : scan.getUnmodifiableResults())
			{
				if (fetchedResult.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					result = fetchedResult;
					break;
				}
			}
			result = modelDelegate.mergeScanResult(result, AssuranceMergeStrategy.TARGET);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testMergeNullTargetScanResultWithTargetStrategy() throws Exception
	{
		boolean exceptionThrown = false;

		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = null;
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);
			scanDefinition.setMergeStrategy(AssuranceMergeStrategy.TARGET);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			ComparisonResult result = null;
			for (ComparisonResult fetchedResult : scan.getUnmodifiableResults())
			{
				if (fetchedResult.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					result = fetchedResult;
					break;
				}
			}
			result = modelDelegate.mergeScanResult(result, AssuranceMergeStrategy.TARGET);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			exceptionThrown = true;
		}
		finally
		{
		}

		assertTrue(exceptionThrown);
	}

	@Test
	@Transactional
	public void testSaveSimpleScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			modelDelegate.saveScanDefinition(scanDefinition);

			// NOTE: There is more that can be done to validate this test.
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSaveComplexScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			modelDelegate.saveScanDefinition(scanDefinition);

			// NOTE: There is more that can be done to validate this test.
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testGetScanDefinitions() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			modelDelegate.saveScanDefinition(scanDefinition);
			List<ScanDefinition> definitions = modelDelegate.getScanDefinitions();

			assertTrue(definitions.size() > 0);
			boolean definitionFound = false;
			for (ScanDefinition queriedDefinition : definitions)
			{
				// NOTE: This doesn't feel great.  Should have a better way to 
				// verify what we're validating.
				if (queriedDefinition.getName() == "Test Scan Definition")
				{
					definitionFound = true;
					break;
				}
			}

			assertTrue(definitionFound);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testDeleteSimpleScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			modelDelegate.deleteScanDefinition(scanDefinition);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testDeleteComplexScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			modelDelegate.deleteScanDefinition(scanDefinition);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testGetSimpleScanResults() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			List<ComparisonResult> results = modelDelegate.getScanResults(scan);

			assertEquals(1, results.size());
		}
		finally
		{
		}
	}
	@Test
	@Transactional
	public void testGetComplexScanResults() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			List<ComparisonResult> results = modelDelegate.getScanResults(scan);

			assertEquals(12, results.size());
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testGetScans() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());

			List<Scan> scans = modelDelegate.getScans();

			assertTrue(scans.size() > 0);
			boolean scanFound = false;
			for (Scan queriedScan : scans)
			{
				if (queriedScan.getId() == scan.getId())
				{
					scanFound = true;
					break;
				}
			}

			assertTrue(scanFound);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testDeleteSimpleScan() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());

			modelDelegate.deleteScan(scan);

			List<Scan> scans = modelDelegate.getScans();

			boolean scanFound = false;
			for (Scan queriedScan : scans)
			{
				if (queriedScan.getId() == scan.getId())
				{
					scanFound = true;
					break;
				}
			}

			assertFalse(scanFound);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testDeleteComplexScan() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());

			modelDelegate.deleteScan(scan);

			List<Scan> scans = modelDelegate.getScans();

			boolean scanFound = false;
			for (Scan queriedScan : scans)
			{
				if (queriedScan.getId() == scan.getId())
				{
					scanFound = true;
					break;
				}
			}

			assertFalse(scanFound);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testRestoreDeletedSourceItem() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());

			ComparisonResult result = null;
			for (ComparisonResult fetchedResult : scan.getUnmodifiableResults())
			{
				if (fetchedResult.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					result = fetchedResult;
					break;
				}
			}
			result.setResolution(AssuranceResultResolution.DELETE_SOURCE);
			testHarness.buildDeletedItemsStructure(result);
			assertTrue(!(testHarness.getTestFile1().exists()));
			assertEquals(AssuranceResultResolution.DELETE_SOURCE, result.getResolution());

			result = modelDelegate.restoreDeletedItem(result);

			assertEquals(AssuranceResultResolution.UNRESOLVED, result.getResolution());
			assertTrue(testHarness.getTestFile1().exists());
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testRestoreDeletedTargetItem() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());

			ComparisonResult result = null;
			for (ComparisonResult fetchedResult : scan.getUnmodifiableResults())
			{
				if (fetchedResult.getReason() == AssuranceResultReason.COMPARE_FAILED)
				{
					result = fetchedResult;
					break;
				}
			}
			result.setResolution(AssuranceResultResolution.DELETE_TARGET);
			testHarness.buildDeletedItemsStructure(result);
			assertTrue(!(testHarness.getTestFile3().exists()));
			assertEquals(AssuranceResultResolution.DELETE_TARGET, result.getResolution());

			result = modelDelegate.restoreDeletedItem(result);

			assertEquals(AssuranceResultResolution.UNRESOLVED, result.getResolution());
			assertTrue(testHarness.getTestFile3().exists());
		}
		finally
		{
		}
	}
	
	@Test
	@Transactional
	public void testExcludedDirectories() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinitionWithExclusions(testFile1, testFile2);

			IAssuranceThreadPool threadPool = new AssuranceThreadPool();
			threadPool.register();
			
			Scan scan = modelDelegate.performScan(scanDefinition, threadPool, testHarness.getScanOptions());
			assertNotNull(scan.getScanStarted());
			assertEquals(9, scan.getUnmodifiableResults().size());
			for (ComparisonResult result : scan.getUnmodifiableResults())
			{
				assertEquals(AssuranceResultResolution.UNRESOLVED, result.getResolution());
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_DIRECTORY_MISMATCH)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.FILE_NULL)));
				assertThat(result.getReason(), not(equalTo(AssuranceResultReason.UNDETERMINED)));
			}
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
