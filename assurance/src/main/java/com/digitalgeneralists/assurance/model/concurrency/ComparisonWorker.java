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

import java.io.File;
import java.util.Collection;

import com.digitalgeneralists.assurance.model.compare.IComparisonEngine;
import com.digitalgeneralists.assurance.model.compare.IScanOptions;
import com.digitalgeneralists.assurance.model.entities.FileReference;
import com.digitalgeneralists.assurance.model.entities.Scan;
import com.digitalgeneralists.assurance.notification.IProgressMonitor;

public class ComparisonWorker implements Runnable
{
	private IComparisonEngine engine;
	private IAssuranceThreadPool threadPool;
	private File source;
	private File target;
	private Scan scan;
	private IScanOptions options;
	private Collection<FileReference> exclusions;
	private IProgressMonitor monitor;

	public ComparisonWorker(IComparisonEngine engine, File source, File target, Scan scan, IAssuranceThreadPool threadPool, IScanOptions options, Collection<FileReference> exclusions, IProgressMonitor monitor)
	{
		this.engine = engine;
		this.threadPool = threadPool;
		this.source = source;
		this.target = target;
		this.scan = scan;
		this.options = options;
		this.exclusions = exclusions;
		this.monitor = monitor;
	}

	public void run()
	{
		try
		{
			this.engine.determineDifferences(this.source, this.target, this.scan, this.threadPool, this.options, this.exclusions, this.monitor);
		}
		finally
		{
			this.threadPool.unregister();
			
			this.engine = null;
			this.source = null;
			this.target = null;
			this.scan = null;
			this.options = null;
			this.exclusions = null;
			this.monitor = null;
			this.threadPool = null;
		}
	}
}
