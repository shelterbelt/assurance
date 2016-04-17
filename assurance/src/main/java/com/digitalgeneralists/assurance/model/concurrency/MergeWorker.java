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

package com.digitalgeneralists.assurance.model.concurrency;

import org.apache.log4j.Logger;

import com.digitalgeneralists.assurance.exceptions.AssuranceNullFileReferenceException;
import com.digitalgeneralists.assurance.model.entities.ComparisonResult;
import com.digitalgeneralists.assurance.model.entities.Scan;
import com.digitalgeneralists.assurance.model.enums.AssuranceResultResolution;
import com.digitalgeneralists.assurance.model.merge.IMergeEngine;
import com.digitalgeneralists.assurance.notification.IProgressMonitor;

public class MergeWorker implements Runnable
{
	private Logger logger = Logger.getLogger(MergeWorker.class);

	private IMergeEngine engine;
	private IAssuranceThreadPool threadPool;
	private ComparisonResult result;
	private IProgressMonitor monitor;

	public MergeWorker(IMergeEngine engine, ComparisonResult result, IAssuranceThreadPool threadPool, IProgressMonitor monitor)
	{
		this.engine = engine;
		this.threadPool = threadPool;
		this.result = result;
		this.monitor = monitor;
	}

	public void run()
	{
		Scan scan = this.result.getScan();
		try
		{
			this.engine.mergeResult(this.result, monitor);
		}
		catch (AssuranceNullFileReferenceException anfre)
		{
			logger.error("Either the source or target file was null when merging result.");
			this.result.setResolution(AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED);
			this.result.setResolutionError(anfre.getMessage());
		}
		finally
		{
			if (scan != null)
			{
				this.threadPool.unregister();
			}
		}
	}
}
