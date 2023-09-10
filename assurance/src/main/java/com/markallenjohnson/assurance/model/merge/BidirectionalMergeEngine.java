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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;
import com.markallenjohnson.assurance.notification.IProgressMonitor;

@Component("BidirectionalMergeEngine")
public class BidirectionalMergeEngine extends MergeEngine implements IMergeEngine
{
	private Logger logger = LogManager.getLogger(BidirectionalMergeEngine.class);

	@Override
	public void mergeResult(ComparisonResult result, IProgressMonitor monitor)
	{
		File sourceFile = result.getSource().getFile();
		File targetFile = result.getTarget().getFile();

		if (monitor != null)
		{
			StringBuilder message = new StringBuilder(512);
			monitor.publish(message.append("Merging ").append(targetFile.toString()).append(" to ").append(sourceFile.toString()).toString());
			message.setLength(0);
			message = null;
		}

		if (sourceFile.exists())
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
				result.setResolution(AssuranceResultResolution.REPLACE_TARGET);
			}
			catch (IOException e)
			{
				logger.error("An error occurred when replacing the target with the source.");
				result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
				result.setResolutionError(e.getMessage());
			}
		}
		else
		{
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
		}
		
		sourceFile = null;
		targetFile = null;
	}
}
