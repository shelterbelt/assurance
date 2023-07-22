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

import org.apache.log4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.markallenjohnson.assurance.model.IModelDelegate;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.notification.INotificationProvider;
import com.markallenjohnson.assurance.notification.IProgressMonitor;
import com.markallenjohnson.assurance.notification.events.DeletedItemRestoreCompletedEvent;
import com.markallenjohnson.assurance.notification.events.DeletedItemRestoreProgressEvent;
import com.markallenjohnson.assurance.notification.events.DeletedItemRestoreStartedEvent;

public class RestoreDeletedItemWorker extends SwingWorker<ComparisonResult, Object> implements IProgressMonitor
{
	private Logger logger = Logger.getLogger(RestoreDeletedItemWorker.class);

	private ComparisonResult result;

	private INotificationProvider notifier;

	public RestoreDeletedItemWorker(ComparisonResult result, INotificationProvider notifier)
	{
		this.result = result;
		this.notifier = notifier;
	}

	@Override
	protected ComparisonResult doInBackground() throws Exception
	{
		this.notifier.fireEvent(new DeletedItemRestoreStartedEvent(this.result));

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

			result = modelDelegate.restoreDeletedItem(this.result, this);
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
			this.notifier.fireEvent(new DeletedItemRestoreCompletedEvent(this.get()));
		}
		catch (InterruptedException e)
		{
			logger.info("Restore deleted item was aborted.");
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
			this.notifier.fireEvent(new DeletedItemRestoreProgressEvent(chunk.toString()));
		}
	}
}
