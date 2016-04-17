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

package com.digitalgeneralists.assurance;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.digitalgeneralists.assurance.model.entities.ApplicationConfiguration;
import com.digitalgeneralists.assurance.model.entities.ComparisonResult;
import com.digitalgeneralists.assurance.model.entities.Scan;
import com.digitalgeneralists.assurance.model.entities.ScanDefinition;
import com.digitalgeneralists.assurance.model.enums.AssuranceMergeStrategy;
import com.digitalgeneralists.assurance.notification.IEventObserver;
import com.digitalgeneralists.assurance.notification.INotificationProvider;
import com.digitalgeneralists.assurance.notification.events.IAssuranceEvent;
import com.digitalgeneralists.assurance.ui.workers.DeleteScanDefinitionWorker;
import com.digitalgeneralists.assurance.ui.workers.DeleteScanWorker;
import com.digitalgeneralists.assurance.ui.workers.InitializeApplicationStateWorker;
import com.digitalgeneralists.assurance.ui.workers.LoadApplicationConfigurationWorker;
import com.digitalgeneralists.assurance.ui.workers.LoadScanDefinitionsWorker;
import com.digitalgeneralists.assurance.ui.workers.LoadScanResultsWorker;
import com.digitalgeneralists.assurance.ui.workers.LoadScansWorker;
import com.digitalgeneralists.assurance.ui.workers.MergeScanResultWorker;
import com.digitalgeneralists.assurance.ui.workers.MergeScanWorker;
import com.digitalgeneralists.assurance.ui.workers.PerformScanWorker;
import com.digitalgeneralists.assurance.ui.workers.RestoreDeletedItemWorker;
import com.digitalgeneralists.assurance.ui.workers.SaveApplicationConfigurationWorker;
import com.digitalgeneralists.assurance.ui.workers.SaveScanDefinitionWorker;

@Component("ApplicationDelegate")
public class ApplicationDelegate implements IApplicationDelegate
{
	private Logger logger = Logger.getLogger(ApplicationDelegate.class);

	@Autowired
	private INotificationProvider notificationProvider;

	public void loadApplicationInitializationState() 
	{
		StringBuffer message = new StringBuffer(256);
		logger.info(message.append("Starting application state initialization."));
		message.setLength(0);
		message = null;

		InitializeApplicationStateWorker thread = new InitializeApplicationStateWorker(this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void performScan(ScanDefinition scanDefinition)
	{
		this.performScan(scanDefinition, false);
	}

	public void performScan(ScanDefinition scanDefinition, boolean merge)
	{
		StringBuffer message = new StringBuffer(256);
		logger.info(message.append("Starting scan with scan defintion: ").append(scanDefinition));
		message.setLength(0);
		message = null;

		PerformScanWorker thread = new PerformScanWorker(scanDefinition, merge, this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void mergeScan(Scan scan)
	{
		MergeScanWorker thread = new MergeScanWorker(scan, this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void mergeScanResult(ComparisonResult result, AssuranceMergeStrategy strategy)
	{
		MergeScanResultWorker thread = new MergeScanResultWorker(result, strategy, this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void saveScanDefinition(ScanDefinition scanDefinition)
	{
		StringBuffer message = new StringBuffer(256);
		logger.info(message.append("Saving scan defintion: ").append(scanDefinition));
		message.setLength(0);
		message = null;

		SaveScanDefinitionWorker thread = new SaveScanDefinitionWorker(scanDefinition, this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void loadApplicationConfiguration() {
		logger.info("Loading the application configuration.");

		LoadApplicationConfigurationWorker thread = new LoadApplicationConfigurationWorker(this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void saveApplicationConfiguration(ApplicationConfiguration configuration) 
	{
		logger.info("Saving the application configuration.");

		SaveApplicationConfigurationWorker thread = new SaveApplicationConfigurationWorker(configuration, this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void loadScanDefinitions()
	{
		logger.info("Loading scan defintions.");

		LoadScanDefinitionsWorker thread = new LoadScanDefinitionsWorker(this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void deleteScanDefinition(ScanDefinition scanDefinition)
	{
		StringBuffer message = new StringBuffer(256);
		logger.info(message.append("Deleting scan defintion: ").append(scanDefinition));
		message.setLength(0);
		message = null;

		DeleteScanDefinitionWorker thread = new DeleteScanDefinitionWorker(scanDefinition, this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void loadScanResults(Scan scan)
	{
		StringBuffer message = new StringBuffer(256);
		logger.info(message.append("Loading results for scan: ").append(scan));
		message.setLength(0);
		message = null;

		LoadScanResultsWorker thread = new LoadScanResultsWorker(scan, this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void loadScans()
	{
		logger.info("Loading scans.");

		LoadScansWorker thread = new LoadScansWorker(this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void deleteScan(Scan scan)
	{
		StringBuffer message = new StringBuffer(256);
		logger.info(message.append("Deleting scan: ").append(scan));
		message.setLength(0);
		message = null;

		DeleteScanWorker thread = new DeleteScanWorker(scan, this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void restoreDeletedItem(ComparisonResult result)
	{
		StringBuffer message = new StringBuffer(256);
		logger.info(message.append("Restoring deleted item for: ").append(result));
		message.setLength(0);
		message = null;

		RestoreDeletedItemWorker thread = new RestoreDeletedItemWorker(result, this.notificationProvider);
		thread.execute();
		thread = null;
	}

	public void addEventObserver(Class<? extends IAssuranceEvent> eventClass, IEventObserver observer)
	{
		this.notificationProvider.addEventObserver(eventClass, observer);
	}

	public void removeEventObserver(Class<? extends IAssuranceEvent> eventClass, IEventObserver observer)
	{
		this.notificationProvider.removeEventObserver(eventClass, observer);
	}

	public void fireEvent(IAssuranceEvent event)
	{
		this.notificationProvider.fireEvent(event);
	}
}
