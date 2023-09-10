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

package com.markallenjohnson.assurance.ui.workers;

import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.markallenjohnson.assurance.exceptions.AssuranceIncompleteScanDefinitionException;
import com.markallenjohnson.assurance.exceptions.AssuranceNullFileReferenceException;
import com.markallenjohnson.assurance.model.IModelDelegate;
import com.markallenjohnson.assurance.model.ModelUtils;
import com.markallenjohnson.assurance.model.compare.IScanOptions;
import com.markallenjohnson.assurance.model.concurrency.IAssuranceThreadPool;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.notification.INotificationProvider;
import com.markallenjohnson.assurance.notification.IProgressMonitor;
import com.markallenjohnson.assurance.notification.events.ScanCompletedEvent;
import com.markallenjohnson.assurance.notification.events.ScanProgressEvent;
import com.markallenjohnson.assurance.notification.events.ScanStartedEvent;

public class PerformScanWorker extends SwingWorker<Scan, Object> implements IProgressMonitor
{
	private Logger logger = LogManager.getLogger(PerformScanWorker.class);

	private ScanDefinition scanDefinition;

	private INotificationProvider notifier;

	private boolean merge;

	public PerformScanWorker(ScanDefinition scanDefinition, boolean merge, INotificationProvider notifier)
	{
		this.scanDefinition = scanDefinition;
		this.merge = merge;
		this.notifier = notifier;
	}

	@Override
	public Scan doInBackground()
	{
		this.notifier.fireEvent(new ScanStartedEvent(scanDefinition));

		Scan scan = null;

		// Because we are operating in a Swing application, the session context
		// closes after the initial bootstrap run. There appears to be an
		// "application"-level session configuration that should enable the
		// behavior we want for a desktop application, but there is no documentation
		// for it that I can find. 
		// So, Assurance mimics a web app request/response
		// cycle for calls into the model delegate through the Swing
		// application. All calls to the model initiated directly from the UI
		// essentially operate as a new "request" and rebuild the Hibernate and
		// Spring session contexts for use within that operation.
		ClassPathXmlApplicationContext springContext = null;
		try
		{
			springContext = new ClassPathXmlApplicationContext("/META-INF/spring/app-context.xml");
			IModelDelegate modelDelegate = (IModelDelegate) springContext.getBean("ModelDelegate");
			IAssuranceThreadPool threadPool = (IAssuranceThreadPool) springContext.getBean("ThreadPool");
			threadPool.setNumberOfThreads(modelDelegate.getApplicationConfiguration().getNumberOfScanThreads());

			// NOTE:  Leaking model initialization like this into the UI is less than ideal.
			scanDefinition = (ScanDefinition) ModelUtils.initializeEntity(scanDefinition, ScanDefinition.SCAN_MAPPING_PROPERTY);
			IScanOptions options = modelDelegate.getScanOptions();
			scan = modelDelegate.performScan(scanDefinition, threadPool, options, this);
			if (this.merge)
			{
				scan = modelDelegate.mergeScan(scan, threadPool, this);
			}
			modelDelegate = null;
			threadPool = null;
		}
		catch (AssuranceNullFileReferenceException e)
		{
			logger.error("An error was encountered when attempting to perform the scan", e);
		}
		catch (AssuranceIncompleteScanDefinitionException e)
		{
			logger.error("An error was encountered when attempting to perform the scan", e);
		}
		finally
		{
			if (springContext != null)
			{
				springContext.close();
			}
			springContext = null;
		}

		return scan;
	}

	@Override
	protected void done()
	{
		try
		{
			this.notifier.fireEvent(new ScanCompletedEvent(this.get()));
		}
		catch (InterruptedException e)
		{
			logger.info("Perform scan was aborted.");
		}
		catch (ExecutionException e)
		{
			logger.error(e);
		}
		finally
		{
			this.scanDefinition = null;
			this.notifier = null;
			this.logger = null;
		}
	}

	public void publish(Object chunk)
	{
		synchronized (this)
		{
			if (chunk == null)
			{
				chunk = "";
			}
			this.notifier.fireEvent(new ScanProgressEvent(chunk.toString()));
		}
	}
}
