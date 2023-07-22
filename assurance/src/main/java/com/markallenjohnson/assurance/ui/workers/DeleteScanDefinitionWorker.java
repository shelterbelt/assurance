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

import javax.swing.SwingWorker;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.markallenjohnson.assurance.model.IModelDelegate;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.notification.INotificationProvider;
import com.markallenjohnson.assurance.notification.events.ScanDefinitionDeletedEvent;

public class DeleteScanDefinitionWorker extends SwingWorker<Object, Object>
{
	private ScanDefinition scanDefinition;

	private INotificationProvider notifier;

	public DeleteScanDefinitionWorker(ScanDefinition scanDefinition, INotificationProvider notifier)
	{
		this.scanDefinition = scanDefinition;
		this.notifier = notifier;
	}

	@Override
	public SwingWorker<Object, Object> doInBackground()
	{
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

			modelDelegate.deleteScanDefinition(scanDefinition);
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

		return this;
	}

	@Override
	protected void done()
	{
		try
		{
			this.notifier.fireEvent(new ScanDefinitionDeletedEvent(this));
		}
		finally
		{
			this.scanDefinition = null;
			this.notifier = null;
		}
	}
}
