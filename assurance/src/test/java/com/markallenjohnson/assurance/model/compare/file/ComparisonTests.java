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

package com.markallenjohnson.assurance.model.compare.file;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

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

import com.markallenjohnson.assurance.UnitTestUtils;
import com.markallenjohnson.assurance.model.factories.IFileComparerFactory;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ComparisonTests
{
	@Autowired
	private IFileComparerFactory comparerFactory;

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
		testHarness.createTestArtifactsLocation("ComparisonTests" + File.separator + name.getMethodName());
	}

	@Test
	public void testSimpleIdenticalFileCompare() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(false);

			File testFile = testHarness.createTestFile("test.txt", UnitTestUtils.basicTestContent);

			boolean result = fileComparer.compare(testFile, testFile, true, true);
			assertTrue(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testSimpleDifferentFileCompare() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(false);

			File testFile1 = testHarness.createTestFile("test1.txt", UnitTestUtils.basicTestContent);
			File testFile2 = testHarness.createTestFile("test2.txt", UnitTestUtils.alternateBasicTestContent);

			boolean result = fileComparer.compare(testFile1, testFile2, true, true);
			assertFalse(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testSimpleIdenticalSourceTargetCompare() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(false);

			File testFile1 = testHarness.createTestFile("test1" + File.separator + "test.txt", UnitTestUtils.basicTestContent);
			File testFile2 = testHarness.createTestFile("test2" + File.separator + "test.txt", UnitTestUtils.basicTestContent);

			boolean result = fileComparer.compare(testFile1, testFile2, true, true);
			assertTrue(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testNullSourceCompare() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(false);

			File testFile = testHarness.createTestFile("test.txt", UnitTestUtils.basicTestContent);

			boolean result = fileComparer.compare(null, testFile, true, true);
			assertFalse(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testNullTargetCompare() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(false);

			File testFile = testHarness.createTestFile("test.txt", UnitTestUtils.basicTestContent);

			boolean result = fileComparer.compare(testFile, null, true, true);
			assertFalse(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testSimpleDifferentNamesSourceTargetCompare() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(false);

			File testFile1 = testHarness.createTestFile("test1" + File.separator + "test.txt", UnitTestUtils.basicTestContent);
			File testFile2 = testHarness.createTestFile("test2" + File.separator + "test.txt", UnitTestUtils.alternateBasicTestContent);

			boolean result = fileComparer.compare(testFile1, testFile2, true, true);
			assertFalse(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testSimpleDifferentContentSourceTargetCompare() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(false);

			File testFile1 = testHarness.createTestFile("test1" + File.separator + "test.txt", UnitTestUtils.basicTestContent);
			File testFile2 = testHarness.createTestFile("test2" + File.separator + "test.txt", UnitTestUtils.alternateBasicTestContent);

			boolean result = fileComparer.compare(testFile1, testFile2, true, true);
			assertFalse(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testSimpleIdenticalFileCompareUsingComprehensiveScan() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(true);

			File testFile = testHarness.createTestFile("test.txt", UnitTestUtils.basicTestContent);

			boolean result = fileComparer.compare(testFile, testFile, true, true);
			assertTrue(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testSimpleDifferentFileCompareUsingComprehensiveScan() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(true);

			File testFile1 = testHarness.createTestFile("test1.txt", UnitTestUtils.basicTestContent);
			File testFile2 = testHarness.createTestFile("test2.txt", UnitTestUtils.alternateBasicTestContent);

			boolean result = fileComparer.compare(testFile1, testFile2, true, true);
			assertFalse(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testSimpleIdenticalSourceTargetCompareUsingComprehensiveScan() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(true);

			File testFile1 = testHarness.createTestFile("test1" + File.separator + "test.txt", UnitTestUtils.basicTestContent);
			File testFile2 = testHarness.createTestFile("test2" + File.separator + "test.txt", UnitTestUtils.basicTestContent);

			boolean result = fileComparer.compare(testFile1, testFile2, true, true);
			assertTrue(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testNullSourceCompareUsingComprehensiveScan() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(true);

			File testFile = testHarness.createTestFile("test.txt", UnitTestUtils.basicTestContent);

			boolean result = fileComparer.compare(null, testFile, true, true);
			assertFalse(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testNullTargetCompareUsingComprehensiveScan() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(true);

			File testFile = testHarness.createTestFile("test.txt", UnitTestUtils.basicTestContent);

			boolean result = fileComparer.compare(testFile, null, true, true);
			assertFalse(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testSimpleDifferentNamesSourceTargetCompareUsingComprehensiveScan() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(true);

			File testFile1 = testHarness.createTestFile("test1" + File.separator + "test.txt", UnitTestUtils.basicTestContent);
			File testFile2 = testHarness.createTestFile("test2" + File.separator + "test.txt", UnitTestUtils.alternateBasicTestContent);

			boolean result = fileComparer.compare(testFile1, testFile2, true, true);
			assertFalse(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@Test
	public void testSimpleDifferentContentSourceTargetCompareUsingComprehensiveScan() throws Exception
	{
		try
		{
			IFileComparer fileComparer = comparerFactory.createInstance(true);

			File testFile1 = testHarness.createTestFile("test1" + File.separator + "test.txt", UnitTestUtils.basicTestContent);
			File testFile2 = testHarness.createTestFile("test2" + File.separator + "test.txt", UnitTestUtils.alternateBasicTestContent);

			boolean result = fileComparer.compare(testFile1, testFile2, true, true);
			assertFalse(result);
		}
		finally
		{
			testHarness.removeTestArtifacts();
		}
	}

	@After
	public void tearDown() throws IOException
	{
		testHarness.removeTestArtifacts();
	}
}
