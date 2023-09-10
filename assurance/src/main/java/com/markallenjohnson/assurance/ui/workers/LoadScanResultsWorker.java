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

import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.notification.INotificationProvider;
import com.markallenjohnson.assurance.notification.events.ScanResultsLoadedEvent;

public class LoadScanResultsWorker extends SwingWorker<Scan, Object>
{
	private Logger logger = LogManager.getLogger(LoadScanResultsWorker.class);

	private Scan scan;
	private INotificationProvider notifier;

	public LoadScanResultsWorker(Scan scan, INotificationProvider notifier)
	{
		this.scan = scan;
		this.notifier = notifier;
	}

	@Override
	protected Scan doInBackground() throws Exception
	{
		// List<ComparisonResult> list = null;

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
			// NOTE:  I'm letting Hibernate do the heavy lifting in terms of collection initialization. 
			// Rather than re-work the complete application architecture, we'll leave
			// the worker in place.
			// springContext = new ClassPathXmlApplicationContext("/META-INF/spring/app-context.xml");
			// IModelDelegate modelDelegate = (IModelDelegate)
			// springContext.getBean("ModelDelegate");

			// list = modelDelegate.getScanResults(this.scan);
		}
		finally
		{
			if (springContext != null)
			{
				springContext.close();
			}
			springContext = null;
		}

		return this.scan;
	}

	@Override
	protected void done()
	{
		try
		{
			this.notifier.fireEvent(new ScanResultsLoadedEvent(this.get()));
		}
		catch (InterruptedException e)
		{
			logger.info("Load scans was aborted.");
		}
		catch (ExecutionException e)
		{
			logger.error(e);
		}
		finally
		{
			this.scan = null;
			this.notifier = null;
			this.logger = null;
		}
	}
}
