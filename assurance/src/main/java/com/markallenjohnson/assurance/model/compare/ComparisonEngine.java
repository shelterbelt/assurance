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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.model.compare.file.IFileComparer;
import com.markallenjohnson.assurance.model.concurrency.ComparisonWorker;
import com.markallenjohnson.assurance.model.concurrency.IAssuranceThreadPool;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceResultReason;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;
import com.markallenjohnson.assurance.model.factories.IFileComparerFactory;
import com.markallenjohnson.assurance.notification.IProgressMonitor;
import com.markallenjohnson.assurance.utils.AssuranceUtils;

@Component("ComparisonEngine")
public class ComparisonEngine implements IComparisonEngine
{
	private Logger logger = Logger.getLogger(ComparisonEngine.class);

	@Autowired
	private IFileComparerFactory comparerFactory;

	public void determineDifferences(File source, File target, Scan scan, IAssuranceThreadPool threadPool, IScanOptions options)
	{
		this.determineDifferences(source, target, scan, threadPool, options, null);
	}

	public void determineDifferences(File source, File target, Scan scan, IAssuranceThreadPool threadPool, IScanOptions options, Collection<FileReference> exclusions)
	{
		this.determineDifferences(source, target, scan, threadPool, options, exclusions, null);
	}

	public void determineDifferences(File source, File target, Scan scan, IAssuranceThreadPool threadPool, IScanOptions options, Collection<FileReference> exclusions, IProgressMonitor monitor)
	{
		IFileComparer comparer = comparerFactory.createInstance();
		this.determineDifferences(source, target, scan, comparer, threadPool, options, exclusions, monitor);
	}

	public void determineDifferences(File source, File target, Scan scan, IAssuranceThreadPool threadPool, IScanOptions options, boolean performDeepScan)
	{
		this.determineDifferences(source, target, scan, threadPool, options, null, performDeepScan);
	}
	
	public void determineDifferences(File source, File target, Scan scan, IAssuranceThreadPool threadPool, IScanOptions options, Collection<FileReference> exclusions, boolean performDeepScan)
	{
		this.determineDifferences(source, target, scan, threadPool, options, exclusions, null, performDeepScan);
	}

	public void determineDifferences(File source, File target, Scan scan, IAssuranceThreadPool threadPool, IScanOptions options, Collection<FileReference> exclusions, IProgressMonitor monitor, boolean performDeepScan)
	{
		IFileComparer comparer = comparerFactory.createInstance(performDeepScan);
		this.determineDifferences(source, target, scan, comparer, threadPool, options, exclusions, monitor);
		comparer = null;
	}
	
	private void determineDifferences(File source, File target, Scan scan, IFileComparer comparer, IAssuranceThreadPool threadPool, IScanOptions options, Collection<FileReference> exclusions, IProgressMonitor monitor)
	{
		if (monitor != null)
		{
			StringBuffer message = new StringBuffer(512);
			monitor.publish(message.append("Comparing ").append(source.toString()).append(" to ").append(target.toString()));
			message.setLength(0);
			message = null;
		}

		if ((source == null) && (target == null))
		{
			StringBuffer message = new StringBuffer(512);
			logger.info(message.append(source).append(" and ").append(target).append(" are both null."));
			message.setLength(0);
			message = null;
		}
		else
		{
			// If the source file or target file is in the the global
			// ignore or exclusion list, just bypass it.
			// Checking both files in case one is null, in which case
			// we would have a mismatch.
			if (((!this.fileIsInGlobalIgnore(source, options)) && 
				(!this.fileIsInGlobalIgnore(target, options))) && 
				((!this.fileIsInExclusions(source, exclusions)) && 
				(!this.fileIsInExclusions(target, exclusions))))
			{
				// Neither source or target is in the global ignore lists.
				
				// Only if both the source and target are not null.
				// NOTE: This logical condition isn't ideal. It works but
				// it is still not great in terms of readability.
				if ((source != null) && (target != null))
				{
					if ((source.isDirectory()) && (!target.isDirectory()) || ((!source.isDirectory()) && (target.isDirectory())))
					{
						ComparisonResult result = new ComparisonResult(source, target, AssuranceResultReason.FILE_DIRECTORY_MISMATCH, comparer);
						scan.addResult(result);
						result = null;
						StringBuffer message = new StringBuffer(512);
						logger.info(message.append(source).append(" does not match ").append(target));
						message.setLength(0);
						message = null;
					}
					else
					{
						// Both files are either both simple files or both
						// directories.

						boolean sourceIsSymbolicLink = AssuranceUtils.checkIfFileIsSymbolicLink(source);
						boolean targetIsSymbolicLink = AssuranceUtils.checkIfFileIsSymbolicLink(target);

						if (sourceIsSymbolicLink && targetIsSymbolicLink)
						{
							// Both files are symbolic links.  Only compare the paths.
							if (!(source.getPath().equals(target.getPath())))
							{
								ComparisonResult result = new ComparisonResult(source, target, AssuranceResultReason.COMPARE_FAILED, comparer);
								scan.addResult(result);
								result = null;
								StringBuffer message = new StringBuffer(512);
								logger.info(message.append(source).append(" does not match ").append(target));
								message.setLength(0);
								message = null;
							}
						}
						else
						{
							// Either file could be symbolic links.  Check for a mismatch.
							
							if ( (sourceIsSymbolicLink && !targetIsSymbolicLink) || 
								 (!sourceIsSymbolicLink && targetIsSymbolicLink) )
							{
								ComparisonResult result = new ComparisonResult(source, target, AssuranceResultReason.SYMBOLIC_LINK_MISMATCH, comparer);
								scan.addResult(result);
								result = null;
								StringBuffer message = new StringBuffer(512);
								logger.info(message.append(source).append(" does not match ").append(target));
								message.setLength(0);
								message = null;
							}
						}

						if (!sourceIsSymbolicLink && !targetIsSymbolicLink)
						{
							// Both files are not symbolic links.
							
							if (source.isDirectory())
							{
								// The files are both directories.
								for (File sourceFile : source.listFiles())
								{
									StringBuilder path = new StringBuilder(512);
									File targetFile = new File(path.append(target.getPath()).append(File.separator).append(sourceFile.getName()).toString());
									path.setLength(0);
									path = null;
									// If the source file or target file is in the the global
									// ignore or exclusion list, just bypass it.
									// Checking both files in case one is null, in which case
									// we would have a mismatch.
									if (((!this.fileIsInGlobalIgnore(sourceFile, options)) && 
											(!this.fileIsInGlobalIgnore(targetFile, options))) && 
											((!this.fileIsInExclusions(sourceFile, exclusions)) && 
											(!this.fileIsInExclusions(targetFile, exclusions))))
									{
										if (targetFile.exists())
										{
											threadPool.submit(new ComparisonWorker(this, sourceFile, targetFile, scan, threadPool, options, exclusions, monitor));
										}
										else
										{
											ComparisonResult result = new ComparisonResult(sourceFile, targetFile, AssuranceResultReason.TARGET_DOES_NOT_EXIST, comparer);
											scan.addResult(result);
											result = null;
											StringBuffer message = new StringBuffer(512);
											logger.info(message.append(targetFile).append(" does not exist."));
											message.setLength(0);
											logger.info(message.append(sourceFile).append(" does not match ").append(targetFile));
											message.setLength(0);
											message = null;
										}
									}
									targetFile = null;
								}
	
								this.identifyTargetItemsNotInSource(source, target, scan, comparer, options, exclusions, monitor);
							}
							else
							{
								// The files are both simple files.
								try
								{
									boolean includeTimestamps = true;
									boolean includeAdvancedAttributes = true;
									ScanDefinition scanDefinition = scan.getScanDef();
									if (scanDefinition != null)
									{
										includeTimestamps = scanDefinition.getIncludeNonCreationTimestamps();
										includeAdvancedAttributes = scanDefinition.getIncludeAdvancedAttributes();
									}
									scanDefinition = null;
									
									if (comparer.compare(source, target, includeTimestamps, includeAdvancedAttributes))
									{
										StringBuffer message = new StringBuffer(512);
										logger.info(message.append(source).append(" is identical to ").append(target));
										message.setLength(0);
										message = null;
									}
									else
									{
										ComparisonResult result = new ComparisonResult(source, target, AssuranceResultReason.COMPARE_FAILED, comparer);
										scan.addResult(result);
										result = null;
										StringBuffer message = new StringBuffer(512);
										logger.info(message.append(source).append(" does not match ").append(target));
										message.setLength(0);
										message = null;
									}
								}
								catch (NoSuchAlgorithmException e)
								{
									ComparisonResult result = new ComparisonResult(source, target, AssuranceResultReason.UNDETERMINED, comparer);
									result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
									result.setResolutionError(e.getMessage());
									scan.addResult(result);
									result = null;
									StringBuffer message = new StringBuffer(512);
									logger.error(message.append("Error comparing ").append(source).append(" to ").append(target), e);
									message.setLength(0);
									message = null;
								}
								catch (IOException e)
								{
									ComparisonResult result = new ComparisonResult(source, target, AssuranceResultReason.UNDETERMINED, comparer);
									result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
									result.setResolutionError(e.getMessage());
									scan.addResult(result);
									result = null;
									StringBuffer message = new StringBuffer(512);
									logger.error(message.append("Error comparing ").append(source).append(" to ").append(target), e);
									message.setLength(0);
									message = null;
								}
							}
						}
					}
				}
				else
				{
					ComparisonResult result = new ComparisonResult(source, target, AssuranceResultReason.FILE_NULL, comparer);
					scan.addResult(result);
					result = null;
					StringBuffer message = new StringBuffer(512);
					logger.info(message.append(source).append(" does not match ").append(target));
					message.setLength(0);
					message = null;
				}
			}
		}
	}

	private void identifyTargetItemsNotInSource(File source, File target, Scan scan, IFileComparer comparer, IScanOptions options, Collection<FileReference> exclusions, IProgressMonitor monitor)
	{
		if (target.isDirectory() && source.isDirectory())
		{
			for (File targetFile : target.listFiles())
			{
				StringBuilder path = new StringBuilder(512);
				File sourceFile = new File(path.append(source.getPath()).append(File.separator).append(targetFile.getName()).toString());
				path.setLength(0);
				path = null;
				// If the source file or target file is in the the global
				// ignore or exclusion list, just bypass it.
				// Checking both files in case one is null, in which case
				// we would have a mismatch.
				if (((!this.fileIsInGlobalIgnore(sourceFile, options)) && 
						(!this.fileIsInGlobalIgnore(targetFile, options))) && 
						((!this.fileIsInExclusions(sourceFile, exclusions)) &&
						(!this.fileIsInExclusions(targetFile, exclusions))))
				{
					if (!sourceFile.exists())
					{
						ComparisonResult result = new ComparisonResult(sourceFile, targetFile, AssuranceResultReason.SOURCE_DOES_NOT_EXIST, comparer);
						scan.addResult(result);
						result = null;
						StringBuffer message = new StringBuffer(512);
						logger.info(message.append(sourceFile).append(" does not exist."));
						message.setLength(0);
						logger.info(message.append(sourceFile).append(" does not match ").append(targetFile));
						message.setLength(0);
						message = null;
					}
				}
				
				sourceFile = null;
				targetFile = null;
			}
		}
	}

	private boolean fileIsInGlobalIgnore(File file, IScanOptions options)
	{
		if (file != null)
		{
			// NOTE: May want to reconsider the toLowercase conversion.
			if (options.getIngnoredFileNames().contains(file.getName().toLowerCase()))
			{
				return true;
			}
			
			// NOTE: May want to reconsider the toLowercase conversion.
			if (options.getIngnoredFileExtensions().contains(FilenameUtils.getExtension(file.getName()).toLowerCase()))
			{
				return true;
			}
		}

		return false;
	}
	
	private boolean fileIsInExclusions(File file, Collection<FileReference> exclusions)
	{
		if ((file != null) && (exclusions != null))
		{
			for (FileReference exclusion : exclusions)
			{
				if (exclusion.getFile().getPath().equals(file.getPath()))
				{
					return true;
				}
			}
		}
		return false;
	}
}
