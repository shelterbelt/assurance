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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.markallenjohnson.assurance.model.IModelDelegate;
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.notification.INotificationProvider;
import com.markallenjohnson.assurance.notification.events.ApplicationConfigurationLoadedEvent;
import com.markallenjohnson.assurance.notification.events.ScanDefinitionsLoadedEvent;

public class InitializeApplicationStateWorker extends SwingWorker<List<Object>, Object>
{
	private Logger logger = LogManager.getLogger(InitializeApplicationStateWorker.class);

	private INotificationProvider notifier;

	public InitializeApplicationStateWorker(INotificationProvider notifier)
	{
		this.notifier = notifier;
	}

	@Override
	protected List<Object> doInBackground() throws Exception
	{
		List<Object> bootstrapObjects = new ArrayList<Object>();
		List<ScanDefinition> scanDefinitions = null;
		ApplicationConfiguration config = null;

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

			scanDefinitions = modelDelegate.getScanDefinitions();
			config = modelDelegate.getApplicationConfiguration();
			if (scanDefinitions != null)
			{
				bootstrapObjects.add(scanDefinitions);
			}
			if (config != null)
			{
				bootstrapObjects.add(config);
			}
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

		return bootstrapObjects;
	}

	@Override
	protected void done()
	{
		List<Object> loadedObjects = null;
		try
		{
			loadedObjects = this.get();
			for (int i = 0; i < loadedObjects.size(); i++)
			{
				Object loadedObject = loadedObjects.get(i);
				// NOTE:  The nature of this check isn't ideal. It won't scale once we add multiple lists.
				if ((loadedObject != null) && (loadedObject instanceof List<?>))
				{
					this.notifier.fireEvent(new ScanDefinitionsLoadedEvent(loadedObject));
				}
				if ((loadedObject != null) && (loadedObject instanceof ApplicationConfiguration))
				{
					this.notifier.fireEvent(new ApplicationConfigurationLoadedEvent(loadedObject));
				}
			}
		}
		catch (InterruptedException e)
		{
			logger.info("Application bootstrap was aborted.");
		}
		catch (ExecutionException e)
		{
			logger.error(e);
		}
		finally
		{
			loadedObjects = null;
			this.notifier = null;
			this.logger = null;
		}
	}
}
