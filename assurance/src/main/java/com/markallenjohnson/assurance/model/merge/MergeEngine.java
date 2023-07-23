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

package com.markallenjohnson.assurance.model.merge;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.Application;
import com.markallenjohnson.assurance.exceptions.AssuranceNullFileReferenceException;
import com.markallenjohnson.assurance.model.concurrency.IAssuranceThreadPool;
import com.markallenjohnson.assurance.model.concurrency.MergeWorker;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;
import com.markallenjohnson.assurance.notification.IProgressMonitor;

@Component("MergeEngine")
public abstract class MergeEngine implements IMergeEngine
{
	private Logger logger = Logger.getLogger(MergeEngine.class);

	// NOTE:  Since these are statics, using a SB to construct the path is more complex than
	// the possible benefit may warrant.
	private static String deletedItemsLocationPath = System.getProperty("user.home") + File.separator + "." + Application.applicationShortName.toLowerCase();

	public abstract void mergeResult(ComparisonResult result, IProgressMonitor monitor) throws AssuranceNullFileReferenceException;

	public void mergeScan(Scan scan, IAssuranceThreadPool threadPool, IProgressMonitor monitor)
	{
		for (ComparisonResult result : scan.getUnmodifiableResults())
		{
			if (this.shouldMerge(scan.getScanDef(), result))
			{
				MergeWorker worker = new MergeWorker(this, result, threadPool, monitor);
				threadPool.submit(worker);
				worker = null;
			}
			
			result = null;
		}
	}

	public void restoreDeletedItem(ComparisonResult result, IProgressMonitor monitor)
	{
		File file = null;
		File deletedFile = null;
		if (result.getResolution() == AssuranceResultResolution.DELETE_SOURCE)
		{
			file = result.getSource().getFile();
			deletedFile = result.getSourceDeletedItemLocation(getApplicationDeletedItemsLocation());
		}
		if (result.getResolution() == AssuranceResultResolution.DELETE_TARGET)
		{
			file = result.getTarget().getFile();
			deletedFile = result.getTargetDeletedItemLocation(getApplicationDeletedItemsLocation());
		}

		if (file != null)
		{
			if (monitor != null)
			{
				StringBuilder message = new StringBuilder(512);
				monitor.publish(message.append("Restoring ").append(file.toString()).toString());
				message.setLength(0);
				message = null;
			}

			if (deletedFile.exists())
			{
				try
				{
					FileUtils.moveFile(deletedFile, file);
					result.setResolution(AssuranceResultResolution.UNRESOLVED);
				}
				catch (IOException e)
				{
					StringBuffer message = new StringBuffer(512);
					logger.warn(message.append("Could not move item from deleted items location ").append(deletedFile.getPath()));
					message.setLength(0);
					message = null;
				}
			}
			else
			{
				logger.warn("Item to restore does not exist.");
				result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
				result.setResolutionError("Item to restore does not exist.");
			}
		}
		
		file = null;
		deletedFile = null;
	}

	private boolean shouldMerge(ScanDefinition scanDefinition, ComparisonResult result)
	{
		boolean shouldMerge = true;

		if (scanDefinition != null)
		{
			if (!scanDefinition.getAutoResolveConflicts())
			{
				File sourceFile = result.getSource().getFile();
				File targetFile = result.getTarget().getFile();

				if ((sourceFile != null) && (sourceFile.exists()) && ((targetFile != null) && (targetFile.exists())))
				{
					shouldMerge = false;
				}
				// Prevent the system from deleting files if auto-resolve is off.
				if ((sourceFile != null) && (sourceFile.exists()) && ((targetFile != null) && (!targetFile.exists())) && (scanDefinition.getMergeStrategy() == AssuranceMergeStrategy.TARGET))
				{
					shouldMerge = false;
				}
				if ((sourceFile != null) && (!sourceFile.exists()) && ((targetFile != null) && (targetFile.exists())) && (scanDefinition.getMergeStrategy() == AssuranceMergeStrategy.SOURCE))
				{
					shouldMerge = false;
				}
				
				sourceFile = null;
				targetFile = null;
			}
		}

		if ((shouldMerge) && (result.getResolution() != AssuranceResultResolution.UNRESOLVED))
		{
			shouldMerge = false;
		}

		return shouldMerge;
	}

	// NOTE: I could do a better job of injecting this value.
	// The mechanism feels clumsy.
	public static void setApplicationDeletedItemsLocation(String overridePath)
	{
		MergeEngine.deletedItemsLocationPath = overridePath;
	}

	// NOTE: I could do a better job of injecting this value.
	// The mechanism feels clumsy.
	public static File getApplicationDeletedItemsLocation()
	{
		return new File(MergeEngine.deletedItemsLocationPath);
	}

	protected File getDefaultDeletedItemsLocation()
	{
		StringBuilder pathBuffer = new StringBuilder(512);
		String path = pathBuffer.append(getApplicationDeletedItemsLocation().getPath()).append(File.separator).append("unknown_scan").toString();
		pathBuffer.setLength(0);
		pathBuffer = null;
		
		File result = new File(path);
		
		path = null;
		
		return result;
	}
}
