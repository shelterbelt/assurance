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

import com.markallenjohnson.assurance.exceptions.AssuranceNullFileReferenceException;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;
import com.markallenjohnson.assurance.notification.IProgressMonitor;

@Component("TargetMergeEngine")
public class TargetMergeEngine extends MergeEngine implements IMergeEngine
{
	private Logger logger = Logger.getLogger(TargetMergeEngine.class);

	@Override
	public void mergeResult(ComparisonResult result, IProgressMonitor monitor) throws AssuranceNullFileReferenceException
	{
		File sourceFile = result.getSource().getFile();
		File targetFile = result.getTarget().getFile();

		if ((sourceFile == null) || (targetFile == null))
		{
			throw new AssuranceNullFileReferenceException("The source or target file is null.");
		}

		if (monitor != null)
		{
			StringBuilder message = new StringBuilder(512);
			monitor.publish(message.append("Merging ").append(targetFile.toString()).append(" to ").append(sourceFile.toString()).toString());
			message.setLength(0);
			message = null;
		}

		if (targetFile.exists())
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
				result.setResolution(AssuranceResultResolution.REPLACE_SOURCE);
			}
			catch (IOException e)
			{
				logger.error("An error occurred when replacing the source with the target.");
				result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
				result.setResolutionError(e.getMessage());
			}
		}
		else
		{
			if (sourceFile.exists())
			{
				File scanDeletedItemsLocation = result.getSourceDeletedItemLocation(getApplicationDeletedItemsLocation());
				if (scanDeletedItemsLocation == null)
				{
					StringBuilder deletedFilePath = new StringBuilder(512);
					scanDeletedItemsLocation = new File(deletedFilePath.append(this.getDefaultDeletedItemsLocation()).append(File.separator).append(sourceFile.getName()).toString());
					deletedFilePath.setLength(0);
				}
				try
				{
					if (sourceFile.isDirectory())
					{
						FileUtils.moveDirectory(sourceFile, scanDeletedItemsLocation);
					}
					else
					{
						FileUtils.moveFile(sourceFile, scanDeletedItemsLocation);
					}
					result.setResolution(AssuranceResultResolution.DELETE_SOURCE);
				}
				catch (IOException e)
				{
					StringBuffer message = new StringBuffer(512);
					logger.error(message.append("Could not move item to deleted items location ").append(sourceFile.getPath()));
					message.setLength(0);
					message = null;
					result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
					result.setResolutionError(e.getMessage());
				}
				scanDeletedItemsLocation = null;
			}
		}
		
		sourceFile = null;
		targetFile = null;
	}
}
