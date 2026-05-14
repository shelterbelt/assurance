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
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;
import com.markallenjohnson.assurance.notification.IProgressMonitor;

@Component("BidirectionalMergeEngine")
public class BidirectionalMergeEngine extends MergeEngine
{
	private Logger logger = LogManager.getLogger(BidirectionalMergeEngine.class);

	@Override
	public void mergeResult(ComparisonResult result, IProgressMonitor monitor)
	{
		File sourceFile = result.getSource().getFile();
		File targetFile = result.getTarget().getFile();

		// When both files exist, the newer mtime wins; ties favor source to
		// preserve the engine's prior source-always-wins behavior for the
		// conflict case. When only one side exists, that side is copied back
		// to fill the gap (the historical bidirectional restore semantics).
		boolean keepSource = sourceFile.exists()
			&& (!targetFile.exists() || sourceFile.lastModified() >= targetFile.lastModified());

		if (monitor != null)
		{
			File from = keepSource ? sourceFile : targetFile;
			File to = keepSource ? targetFile : sourceFile;
			StringBuilder message = new StringBuilder(512);
			monitor.publish(message.append("Merging ").append(from.toString()).append(" to ").append(to.toString()).toString());
			message.setLength(0);
		}

		if (keepSource)
		{
			try
			{
				if (sourceFile.isDirectory())
				{
					FileUtils.copyDirectory(sourceFile, targetFile);
				}
				else
				{
					FileUtils.copyFile(sourceFile, targetFile);
				}
				result.getTarget().refreshStoredFileAttributes();
				result.setResolution(AssuranceResultResolution.REPLACE_TARGET);
			}
			catch (IOException e)
			{
				logger.error("An error occurred when replacing the target with the source.");
				result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
				result.setResolutionError(e.getMessage());
			}
		}
		else if (targetFile.exists())
		{
			try
			{
				if (targetFile.isDirectory())
				{
					FileUtils.copyDirectory(targetFile, sourceFile);
				}
				else
				{
					FileUtils.copyFile(targetFile, sourceFile);
				}
				result.getSource().refreshStoredFileAttributes();
				result.setResolution(AssuranceResultResolution.REPLACE_SOURCE);
			}
			catch (IOException e)
			{
				logger.error("An error occurred when replacing the source with the target.");
				result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
				result.setResolutionError(e.getMessage());
			}
		}
	}
}
