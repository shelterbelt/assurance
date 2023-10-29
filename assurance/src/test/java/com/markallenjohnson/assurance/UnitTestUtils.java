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

package com.markallenjohnson.assurance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;

import com.markallenjohnson.assurance.model.compare.IScanOptions;
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.entities.ScanMappingDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;
import com.markallenjohnson.assurance.model.enums.AssuranceResultReason;
import com.markallenjohnson.assurance.model.merge.MergeEngine;

public class UnitTestUtils
{
	private String testArtifactLocation = "." + File.separator + "test_output";
	private String testArtifactDeletedItemsLocation = this.testArtifactLocation + File.separator + ".assurance";

	public static String basicTestContent = "Hi";
	public static String alternateBasicTestContent = "Hi!";

	public File createTestFile(String path, String content) throws IOException
	{
		path = this.testArtifactLocation + File.separator + path;
		File testFile = new File(path);
		File parentDir = new File(testFile.getParent());
		parentDir.mkdirs();
		testFile.createNewFile();

		FileWriter fw = new FileWriter(testFile.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(content);
		bw.close();
		fw.close();

		return testFile;
	}

	public String readTestFileContents(File file) throws FileNotFoundException, IOException
	{
		String contents = null;

		FileReader fr = new FileReader(file.getAbsoluteFile());
		BufferedReader br = new BufferedReader(fr);
		String currentLine = null;
		while ((currentLine = br.readLine()) != null)
		{
			contents += currentLine;
		}
		br.close();
		fr.close();

		return contents;
	}

	public void createTestArtifactsLocation(String baseDirectory)
	{
		this.testArtifactLocation = this.testArtifactLocation + File.separator + baseDirectory;
		this.testArtifactDeletedItemsLocation = this.testArtifactDeletedItemsLocation + File.separator + baseDirectory;

		File artifactsLocation = new File(this.testArtifactLocation);
		artifactsLocation.mkdirs();
		MergeEngine.setApplicationDeletedItemsLocation(this.testArtifactDeletedItemsLocation);
	}

	public void removeTestArtifacts() throws IOException
	{
		File artifactsLocation = new File(this.testArtifactLocation);
		FileUtils.deleteDirectory(artifactsLocation);
	}

	public void buildSimpleTestDataDirectoryStructure() throws IOException
	{
		this.createTestFile("test1" + File.separator + "test.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("test2" + File.separator + "test.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("test3" + File.separator + "test.txt", UnitTestUtils.alternateBasicTestContent);
	}

	public void buildComplexTestDataDirectoryStructures() throws IOException
	{
		this.buildComplexTestDataDirectoryStructure("test1");
		this.buildComplexTestDataDirectoryStructure("test2");
		this.buildAlternateComplexTestDataDirectoryStructure("test3");
	}

	public void buildIgnoredFilesTestDataDirectoryStructures() throws IOException
	{
		this.buildIgnoredFilesStructure("test1");
		this.buildAlternateIgnoredFilesStructure("test2");
	}

	public void buildComplexTestDataDirectoryStructure(String directoryName) throws IOException
	{
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest1" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest1" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest1" + File.separator + "test3.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest2" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest2" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest2" + File.separator + "test3.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest3" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest3" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest3" + File.separator + "test3.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest3" + File.separator + "excluded" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest3" + File.separator + "excluded" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest4" + File.separator + "test.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest5" + File.separator + "subSubTest1" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest5" + File.separator + "subSubTest1" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest5" + File.separator + "subSubTest1" + File.separator + "test3.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest1" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest1" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest1" + File.separator + "test3.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest2" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest2" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest2" + File.separator + "test3.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest7" + File.separator + "subSubTest1" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest7" + File.separator + "subSubTest1" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest7" + File.separator + "subSubTest1" + File.separator + "test3.txt", UnitTestUtils.basicTestContent);
	}
	
	public void buildAlternateComplexTestDataDirectoryStructure(String directoryName) throws IOException
	{
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "test1.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "test2.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest1" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest1" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest1" + File.separator + "test3.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest2" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest2" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest2" + File.separator + "test3.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest3" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest3" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest3" + File.separator + "excluded" + File.separator + "test1.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest3" + File.separator + "excluded" + File.separator + "test2.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest4" + File.separator + "test.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest5" + File.separator + "subSubTest1" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest5" + File.separator + "subSubTest1" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest5" + File.separator + "subSubTest1" + File.separator + "test3.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest5" + File.separator + "subSubTest2" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest5" + File.separator + "subSubTest2" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest5" + File.separator + "subSubTest2" + File.separator + "test3.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest1" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest1" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest1" + File.separator + "test3.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest1" + File.separator + "test4.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest2" + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest2" + File.separator + "test2.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "subSubTest2" + File.separator + "test3.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "excluded" + File.separator + "test1.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("complex" + File.separator + directoryName + File.separator + "subTest6" + File.separator + "excluded" + File.separator + "test2.txt", UnitTestUtils.basicTestContent);
	}

	public void buildDeletedItemsStructure(ComparisonResult result) throws IOException
	{
		if (result.getSourceDeletedItemLocation(MergeEngine.getApplicationDeletedItemsLocation()).exists())
		{
			FileUtils.deleteQuietly(result.getSourceDeletedItemLocation(MergeEngine.getApplicationDeletedItemsLocation()));
		}
		FileUtils.moveFile(result.getSource().getFile(), result.getSourceDeletedItemLocation(MergeEngine.getApplicationDeletedItemsLocation()));
		if (result.getTargetDeletedItemLocation(MergeEngine.getApplicationDeletedItemsLocation()).exists())
		{
			FileUtils.deleteQuietly(result.getTargetDeletedItemLocation(MergeEngine.getApplicationDeletedItemsLocation()));
		}
		FileUtils.moveFile(result.getTarget().getFile(), result.getTargetDeletedItemLocation(MergeEngine.getApplicationDeletedItemsLocation()));
	}
	
	public void buildIgnoredFilesStructure(String directoryName) throws IOException
	{
		this.createTestFile("ignored" + File.separator + directoryName + File.separator + ".DS_Store", UnitTestUtils.basicTestContent);
		this.createTestFile("ignored" + File.separator + directoryName + File.separator + "test1.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("ignored" + File.separator + directoryName + File.separator + "thumbs.db", UnitTestUtils.basicTestContent);
		this.createTestFile("ignored" + File.separator + directoryName + File.separator + "test2.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("ignored" + File.separator + directoryName + File.separator + "Thumbs.db", UnitTestUtils.basicTestContent);

	}
	
	public void buildAlternateIgnoredFilesStructure(String directoryName) throws IOException
	{
		this.createTestFile("ignored" + File.separator + directoryName + File.separator + ".DS_Store", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("ignored" + File.separator + directoryName + File.separator + "test1.txt", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("ignored" + File.separator + directoryName + File.separator + "thumbs.db", UnitTestUtils.alternateBasicTestContent);
		this.createTestFile("ignored" + File.separator + directoryName + File.separator + "test2.txt", UnitTestUtils.basicTestContent);
		this.createTestFile("ignored" + File.separator + directoryName + File.separator + "Thumbs.db", UnitTestUtils.alternateBasicTestContent);

	}

	public File getTestDirectory1()
	{
		return new File(this.testArtifactLocation + File.separator + "test1");
	}

	public File getTestDirectory2()
	{
		return new File(this.testArtifactLocation + File.separator + "test2");
	}

	public File getTestDirectory3()
	{
		return new File(this.testArtifactLocation + File.separator + "test3");
	}

	public File getComplexTestDirectory1()
	{
		return new File(this.testArtifactLocation + File.separator + "complex" + File.separator + "test1");
	}

	public File getComplexTestDirectory2()
	{
		return new File(this.testArtifactLocation + File.separator + "complex" + File.separator + "test2");
	}

	public File getComplexTestDirectory3()
	{
		return new File(this.testArtifactLocation + File.separator + "complex" + File.separator + "test3");
	}

	public File getIgnoredTestDirectory1()
	{
		return new File(this.testArtifactLocation + File.separator + "ignored" + File.separator + "test1");
	}

	public File getIgnoredTestDirectory2()
	{
		return new File(this.testArtifactLocation + File.separator + "ignored" + File.separator + "test2");
	}

	public File getExcludedTestDirectory1()
	{
		return new File(this.testArtifactLocation + File.separator + "complex" + File.separator + "test1" + File.separator + "subTest3" + File.separator + "excluded");
	}

	public File getExcludedTestDirectory2()
	{
		return new File(this.testArtifactLocation + File.separator + "complex" + File.separator + "test3" + File.separator + "subTest6" + File.separator + "excluded");
	}

	public File getTestFile1()
	{
		return new File(this.testArtifactLocation + File.separator + "test1/test.txt");
	}

	public File getTestFile2()
	{
		return new File(this.testArtifactLocation + File.separator + "test2/test.txt");
	}

	public File getTestFile3()
	{
		return new File(this.testArtifactLocation + File.separator + "test3/test.txt");
	}
	
	public Collection<FileReference> getTestExclusions()
	{
		Collection<FileReference> exclusions = new ArrayList<FileReference>();
		
		exclusions.add(new FileReference(this.getExcludedTestDirectory1()));
		exclusions.add(new FileReference(this.getExcludedTestDirectory2()));
		
		return exclusions;
	}

	public ScanDefinition createTestScanDefinition(File file1, File file2)
	{
		return this.createTestScanDefinition(file1, file2, "Test Scan Definition", AssuranceMergeStrategy.SOURCE, true);
	}

	public ScanDefinition createTestScanDefinition(File file1, File file2, String name, AssuranceMergeStrategy strategy, boolean autoResolveConflicts)
	{
		ScanDefinition scanDefinition = new ScanDefinition();
		ScanMappingDefinition mappingDefinition = this.createTestScanMappingDefinition(file1, file2);
		scanDefinition.addMappingDefinition(mappingDefinition);
		scanDefinition.setName(name);
		scanDefinition.setMergeStrategy(strategy);
		scanDefinition.setAutoResolveConflicts(autoResolveConflicts);
		scanDefinition.setIncludeNonCreationTimestamps(false);
		scanDefinition.setIncludeAdvancedAttributes(false);

		return scanDefinition;
	}

	public ScanDefinition createTestScanDefinitionWithExclusions(File file1, File file2)
	{
		return this.createTestScanDefinitionWithExclusions(file1, file2, "Test Scan Definition", AssuranceMergeStrategy.SOURCE, true);
	}

	public ScanDefinition createTestScanDefinitionWithExclusions(File file1, File file2, String name, AssuranceMergeStrategy strategy, boolean autoResolveConflicts)
	{
		ScanDefinition scanDefinition = new ScanDefinition();
		ScanMappingDefinition mappingDefinition = this.createTestScanMappingDefinitionWithExclusions(file1, file2);
		scanDefinition.addMappingDefinition(mappingDefinition);
		scanDefinition.setName(name);
		scanDefinition.setMergeStrategy(strategy);
		scanDefinition.setAutoResolveConflicts(autoResolveConflicts);
		scanDefinition.setIncludeNonCreationTimestamps(false);
		scanDefinition.setIncludeAdvancedAttributes(false);

		return scanDefinition;
	}

	public Scan createTestScan(File file1, File file2)
	{
		Scan scan = new Scan();
		ScanDefinition scanDef = this.createTestScanDefinition(file1, file2);
		scan.setScanDef(scanDef);

		scan.addResult(new ComparisonResult(file1, file2, AssuranceResultReason.COMPARE_FAILED));

		return scan;
	}

	public ScanMappingDefinition createTestScanMappingDefinition(File file1, File file2)
	{
		ScanMappingDefinition mappingDefinition = new ScanMappingDefinition();
		mappingDefinition.setSource(file1);
		mappingDefinition.setTarget(file2);

		return mappingDefinition;
	}

	public ScanMappingDefinition createTestScanMappingDefinitionWithExclusions(File file1, File file2)
	{
		ScanMappingDefinition mappingDefinition = new ScanMappingDefinition();
		mappingDefinition.setSource(file1);
		mappingDefinition.setTarget(file2);
		
		Collection<FileReference> exclusions = this.getTestExclusions();
		for (FileReference exclusion : exclusions)
		{
			mappingDefinition.addExclusion(exclusion);
		}

		return mappingDefinition;
	}

	public ComparisonResult createTestComparisonResult(File file1, File file2)
	{
		Scan scan = this.createTestScan(file1, file2);
		ComparisonResult result = scan.getUnmodifiableResults().iterator().next();

		return result;
	}

	public IScanOptions getScanOptions()
	{
		return ApplicationConfiguration.createDefaultConfiguration();
	}
	
	public IScanOptions getScanOptions(String ignoredFileNames, String ignoredFileExtensions, Integer numberOfThreads)
	{
		ApplicationConfiguration options = new ApplicationConfiguration();
		options.setIgnoredFileNames(ignoredFileNames);
		options.setIgnoredFileNames(ignoredFileExtensions);
		options.setNumberOfScanThreads(numberOfThreads);
		
		return options;
	}

	public static void installDb() throws IOException, SQLException
	{
		String workingDir = System.getProperty("user.dir");
		String propertiesFileName = workingDir + "/src/test/resources/properties/database.properties";
		String databaseScriptFileName = workingDir + "/src/main/resources/database/assurance.sql";
		InputStream propertiesFileStream = new FileInputStream(propertiesFileName);
		InputStream dbScriptStream = new FileInputStream(databaseScriptFileName);

		Application.installDb(propertiesFileStream, dbScriptStream);
	}
}
