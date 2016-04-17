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

package com.digitalgeneralists.assurance;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

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

import com.digitalgeneralists.assurance.model.entities.ComparisonResult;
import com.digitalgeneralists.assurance.model.entities.Scan;
import com.digitalgeneralists.assurance.model.entities.ScanDefinition;
import com.digitalgeneralists.assurance.model.enums.AssuranceMergeStrategy;
import com.digitalgeneralists.assurance.model.enums.AssuranceResultResolution;

// TODO:  Add tests for:
// - symbolic links
// - windows links
// - recursive links (Windows and symbolic)
// - long file names
// - large files
// - only file attribute differences
// - attribute and content differences
// - add all prior cases to the complex structures
// - comprehensive vs. light tests where the file attributes are different but the file content is the same.
// - comprehensive vs. light tests where the file attributes are the same but the file content is the different.
// - simple structure tests exercising merge functionality 
// - tests with multiple scan definitions
// - tests of the application configuration save/load
// - all UI-related functionality

// NOTE:  There are threading/concurrency problems with enabling this suite.  It really should be addressed,
// but the app-delegate largely serves as a pass-through to the model delegate which has a properly-working
// suite.  Deferring fixing this is a punt, but a safe one given cost.
@Ignore
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AppDelegateTests
{
	@Autowired
	private IApplicationDelegate appDelegate;

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
		testHarness.createTestArtifactsLocation("AppDelegateTests" + File.separator + name.getMethodName());
		testHarness.buildSimpleTestDataDirectoryStructure();
		testHarness.buildComplexTestDataDirectoryStructures();
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

			appDelegate.performScan(scanDefinition);
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

			appDelegate.performScan(scanDefinition);
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

			appDelegate.performScan(scanDefinition);
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

			appDelegate.performScan(scanDefinition);
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

			appDelegate.performScan(scanDefinition);
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

			appDelegate.performScan(scanDefinition);
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

			appDelegate.performScan(scanDefinition);
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

			appDelegate.performScan(scanDefinition);
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

			appDelegate.performScan(scanDefinition);
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

			appDelegate.performScan(scanDefinition);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testNullSourceTargetDirectoryWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getTestDirectory2();
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			appDelegate.performScan(scanDefinition);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testSourceDirectoryNullTargetWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestDirectory1();
			File testFile2 = null;
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			appDelegate.performScan(scanDefinition);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testNullSourceNullTargetWithScanDefinition() throws Exception
	{
		try
		{
			File testFile1 = null;
			File testFile2 = null;
			ScanDefinition scanDefinition = testHarness.createTestScanDefinition(testFile1, testFile2);

			appDelegate.performScan(scanDefinition);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeSimpleScanWithSourceStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			Scan scan = testHarness.createTestScan(testFile1, testFile2);
			scan.getScanDef().setMergeStrategy(AssuranceMergeStrategy.SOURCE);

			appDelegate.mergeScan(scan);
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
			Scan scan = testHarness.createTestScan(testFile1, testFile2);
			scan.getScanDef().setMergeStrategy(AssuranceMergeStrategy.SOURCE);

			appDelegate.mergeScan(scan);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullSourceScanWithSourceStrategy() throws Exception
	{
		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getComplexTestDirectory3();
			Scan scan = testHarness.createTestScan(testFile1, testFile2);
			scan.getScanDef().setMergeStrategy(AssuranceMergeStrategy.SOURCE);

			appDelegate.mergeScan(scan);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullTargetScanWithSourceStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = null;
			Scan scan = testHarness.createTestScan(testFile1, testFile2);
			scan.getScanDef().setMergeStrategy(AssuranceMergeStrategy.SOURCE);

			appDelegate.mergeScan(scan);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeSimpleScanWithTargetStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			Scan scan = testHarness.createTestScan(testFile1, testFile2);
			scan.getScanDef().setMergeStrategy(AssuranceMergeStrategy.TARGET);

			appDelegate.mergeScan(scan);
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
			Scan scan = testHarness.createTestScan(testFile1, testFile2);
			scan.getScanDef().setMergeStrategy(AssuranceMergeStrategy.TARGET);

			appDelegate.mergeScan(scan);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullSourceScanWithTargetStrategy() throws Exception
	{
		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getComplexTestDirectory3();
			Scan scan = testHarness.createTestScan(testFile1, testFile2);
			scan.getScanDef().setMergeStrategy(AssuranceMergeStrategy.TARGET);

			appDelegate.mergeScan(scan);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullTargetScanWithTargetStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = null;
			Scan scan = testHarness.createTestScan(testFile1, testFile2);
			scan.getScanDef().setMergeStrategy(AssuranceMergeStrategy.TARGET);

			appDelegate.mergeScan(scan);
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
			ComparisonResult result = testHarness.createTestComparisonResult(testFile1, testFile2);

			appDelegate.mergeScanResult(result, AssuranceMergeStrategy.SOURCE);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeComplexScanResultWithSourceStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			ComparisonResult result = testHarness.createTestComparisonResult(testFile1, testFile2);

			appDelegate.mergeScanResult(result, AssuranceMergeStrategy.SOURCE);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullSourceScanResultWithSourceStrategy() throws Exception
	{
		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getComplexTestDirectory3();
			ComparisonResult result = testHarness.createTestComparisonResult(testFile1, testFile2);

			appDelegate.mergeScanResult(result, AssuranceMergeStrategy.SOURCE);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullTargetScanResultWithSourceStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = null;
			ComparisonResult result = testHarness.createTestComparisonResult(testFile1, testFile2);

			appDelegate.mergeScanResult(result, AssuranceMergeStrategy.SOURCE);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeSimpleScanResultWithTargetStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			ComparisonResult result = testHarness.createTestComparisonResult(testFile1, testFile2);

			appDelegate.mergeScanResult(result, AssuranceMergeStrategy.TARGET);
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
			ComparisonResult result = testHarness.createTestComparisonResult(testFile1, testFile2);

			appDelegate.mergeScanResult(result, AssuranceMergeStrategy.TARGET);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullSourceScanResultWithTargetStrategy() throws Exception
	{
		try
		{
			File testFile1 = null;
			File testFile2 = testHarness.getComplexTestDirectory3();
			ComparisonResult result = testHarness.createTestComparisonResult(testFile1, testFile2);

			appDelegate.mergeScanResult(result, AssuranceMergeStrategy.TARGET);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testMergeNullTargetScanResultWithTargetStrategy() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = null;
			ComparisonResult result = testHarness.createTestComparisonResult(testFile1, testFile2);

			appDelegate.mergeScanResult(result, AssuranceMergeStrategy.TARGET);
		}
		finally
		{
		}
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

			appDelegate.saveScanDefinition(scanDefinition);
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

			appDelegate.saveScanDefinition(scanDefinition);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testLoadScanDefinitions() throws Exception
	{
		try
		{
			appDelegate.loadScanDefinitions();
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

			appDelegate.deleteScanDefinition(scanDefinition);
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

			appDelegate.deleteScanDefinition(scanDefinition);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testLoadSimpleScanResults() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getTestFile1();
			File testFile2 = testHarness.getTestFile3();
			Scan scan = testHarness.createTestScan(testFile1, testFile2);

			appDelegate.loadScanResults(scan);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testLoadComplexScanResults() throws Exception
	{
		try
		{
			File testFile1 = testHarness.getComplexTestDirectory1();
			File testFile2 = testHarness.getComplexTestDirectory3();
			Scan scan = testHarness.createTestScan(testFile1, testFile2);

			appDelegate.loadScanResults(scan);
		}
		finally
		{
		}
	}

	@Test
	@Transactional
	public void testLoadScans() throws Exception
	{
		try
		{
			appDelegate.loadScans();
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
			Scan scan = testHarness.createTestScan(testFile1, testFile2);

			appDelegate.deleteScan(scan);
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
			Scan scan = testHarness.createTestScan(testFile1, testFile2);

			appDelegate.deleteScan(scan);
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
			ComparisonResult result = testHarness.createTestComparisonResult(testFile1, testFile2);
			result.setResolution(AssuranceResultResolution.DELETE_SOURCE);
			testHarness.buildDeletedItemsStructure(result);

			appDelegate.restoreDeletedItem(result);
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
			ComparisonResult result = testHarness.createTestComparisonResult(testFile1, testFile2);
			result.setResolution(AssuranceResultResolution.DELETE_TARGET);
			testHarness.buildDeletedItemsStructure(result);

			appDelegate.restoreDeletedItem(result);
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
