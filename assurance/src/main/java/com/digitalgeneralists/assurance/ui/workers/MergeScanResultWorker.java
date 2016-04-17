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

package com.digitalgeneralists.assurance.ui.workers;

import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.digitalgeneralists.assurance.model.IModelDelegate;
import com.digitalgeneralists.assurance.model.entities.ComparisonResult;
import com.digitalgeneralists.assurance.model.enums.AssuranceMergeStrategy;
import com.digitalgeneralists.assurance.notification.INotificationProvider;
import com.digitalgeneralists.assurance.notification.IProgressMonitor;
import com.digitalgeneralists.assurance.notification.events.ResultMergeCompletedEvent;
import com.digitalgeneralists.assurance.notification.events.ResultMergeProgressEvent;
import com.digitalgeneralists.assurance.notification.events.ResultMergeStartedEvent;

public class MergeScanResultWorker extends SwingWorker<ComparisonResult, Object> implements IProgressMonitor
{
	private Logger logger = Logger.getLogger(MergeScanResultWorker.class);

	private ComparisonResult result;

	private AssuranceMergeStrategy strategy;

	private INotificationProvider notifier;

	public MergeScanResultWorker(ComparisonResult result, AssuranceMergeStrategy strategy, INotificationProvider notifier)
	{
		this.result = result;
		this.strategy = strategy;
		this.notifier = notifier;
	}

	@Override
	protected ComparisonResult doInBackground() throws Exception
	{
		this.notifier.fireEvent(new ResultMergeStartedEvent(this.result));

		ComparisonResult result = null;

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

			result = modelDelegate.mergeScanResult(this.result, this.strategy, this);
			modelDelegate = null;
		}
		finally
		{
			if (springContext != null)
			{
				springContext.close();
			}
			springContext = null;
		}

		return result;
	}

	@Override
	protected void done()
	{
		try
		{
			this.notifier.fireEvent(new ResultMergeCompletedEvent(this.get()));
		}
		catch (InterruptedException e)
		{
			logger.info("Merge scans was aborted.");
		}
		catch (ExecutionException e)
		{
			logger.error(e);
		}
		finally
		{
			this.result = null;
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
			this.notifier.fireEvent(new ResultMergeProgressEvent(chunk.toString()));
		}
	}
}
