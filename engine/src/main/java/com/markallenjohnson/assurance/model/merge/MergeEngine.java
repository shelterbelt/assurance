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

package com.markallenjohnson.assurance.model.merge;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.markallenjohnson.assurance.Application;
import com.markallenjohnson.assurance.model.concurrency.IAssuranceThreadPool;
import com.markallenjohnson.assurance.model.concurrency.MergeWorker;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;
import com.markallenjohnson.assurance.notification.IProgressMonitor;

public abstract class MergeEngine implements IMergeEngine
{
	private Logger logger = LogManager.getLogger(MergeEngine.class);

	// NOTE:  Since these are statics, using a SB to construct the path is more complex than
	// the possible benefit may warrant.
	private static String deletedItemsLocationPath = System.getProperty("user.home") + File.separator + "." + Application.applicationShortName.toLowerCase();

	public void mergeScan(Scan scan, IAssuranceThreadPool threadPool, IProgressMonitor monitor)
	{
		for (ComparisonResult result : scan.getUnmodifiableResults())
		{
			if (this.shouldMerge(scan.getScanDef(), result))
			{
				MergeWorker worker = new MergeWorker(this, result, threadPool, monitor);
				threadPool.submit(worker);
			}
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
			}

			if ((deletedFile != null) && deletedFile.exists())
			{
				try
				{
					// FileUtils.moveFile rejects directories with
					// IllegalArgumentException, and FileUtils.moveDirectory
					// rejects regular files for the symmetric reason. Dispatch
					// based on what was actually staged so directory restores
					// (which the legacy 1.x flow would also have failed on)
					// work end-to-end.
					if (deletedFile.isDirectory())
					{
						FileUtils.moveDirectory(deletedFile, file);
					}
					else
					{
						FileUtils.moveFile(deletedFile, file);
					}
					boolean restoredSource = (result.getResolution() == AssuranceResultResolution.DELETE_SOURCE);
					result.setResolution(AssuranceResultResolution.UNRESOLVED);
					if (restoredSource)
					{
						result.getSource().refreshStoredFileAttributes();
					}
					else
					{
						result.getTarget().refreshStoredFileAttributes();
					}
				}
				catch (IOException e)
				{
					StringBuilder message = new StringBuilder(512);
					logger.warn(message.append("Could not move item from deleted items location ").append(deletedFile.getPath()));
					message.setLength(0);
				}
			}
			else
			{
				logger.warn("Item to restore does not exist.");
				result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
				result.setResolutionError("Item to restore does not exist.");
			}
		}
	}

	private boolean shouldMerge(ScanDefinition scanDefinition, ComparisonResult result)
	{
		boolean shouldMerge = true;

		if (scanDefinition != null)
		{
			if (Boolean.FALSE.equals(scanDefinition.getAutoResolveConflicts()))
			{
				File sourceFile = result.getSource().getFile();
				File targetFile = result.getTarget().getFile();

				// With auto-resolve off, the user expects to triage anything
				// that isn't trivially identical. If both files exist and a
				// deep comparison says their contents match, the comparison
				// engine flagged the row only because attributes differ —
				// skip the merge and leave it UNRESOLVED for manual review.
				// With auto-resolve off, the user expects to triage any row
				// where both sides exist (the comparison engine only logged a
				// result because they differ in some way). Skip the merge so
				// the row stays UNRESOLVED for manual review. The two
				// "delete-protection" guards below handle the one-side-missing
				// cases for SOURCE / TARGET strategies; this branch handles
				// the both-exist case.
				if ((sourceFile != null) && (sourceFile.exists()) && (targetFile != null) && (targetFile.exists()))
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
		
		return new File(path);
	}
}
