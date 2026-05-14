/*
 * Assurance
 * 
 * Created by Mark Johnson
 * 
 * Copyright (c) 2015 - 2023 Mark Johnson
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

package com.markallenjohnson.assurance;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;
import com.markallenjohnson.assurance.notification.IEventObserver;
import com.markallenjohnson.assurance.notification.INotificationProvider;
import com.markallenjohnson.assurance.notification.events.IAssuranceEvent;
import com.markallenjohnson.assurance.ui.workers.DeleteScanDefinitionWorker;
import com.markallenjohnson.assurance.ui.workers.DeleteScanWorker;
import com.markallenjohnson.assurance.ui.workers.InitializeApplicationStateWorker;
import com.markallenjohnson.assurance.ui.workers.LoadApplicationConfigurationWorker;
import com.markallenjohnson.assurance.ui.workers.LoadScanDefinitionsWorker;
import com.markallenjohnson.assurance.ui.workers.LoadScanResultsWorker;
import com.markallenjohnson.assurance.ui.workers.LoadScansWorker;
import com.markallenjohnson.assurance.ui.workers.MergeScanResultWorker;
import com.markallenjohnson.assurance.ui.workers.MergeScanWorker;
import com.markallenjohnson.assurance.ui.workers.PerformScanWorker;
import com.markallenjohnson.assurance.ui.workers.RestoreDeletedItemWorker;
import com.markallenjohnson.assurance.ui.workers.SaveApplicationConfigurationWorker;
import com.markallenjohnson.assurance.ui.workers.SaveScanDefinitionWorker;

@Component("ApplicationDelegate")
public class ApplicationDelegate implements IApplicationDelegate
{
	private Logger logger = LogManager.getLogger(ApplicationDelegate.class);

	@Autowired
	private INotificationProvider notificationProvider;

	public void loadApplicationInitializationState() 
	{
		StringBuilder message = new StringBuilder(256);
		logger.info(message.append("Starting application state initialization."));
		message.setLength(0);

		InitializeApplicationStateWorker thread = new InitializeApplicationStateWorker(this.notificationProvider);
		thread.execute();
	}

	public void performScan(ScanDefinition scanDefinition)
	{
		this.performScan(scanDefinition, false);
	}

	public void performScan(ScanDefinition scanDefinition, boolean merge)
	{
		StringBuilder message = new StringBuilder(256);
		logger.info(message.append("Starting scan with scan defintion: ").append(scanDefinition));
		message.setLength(0);

		PerformScanWorker thread = new PerformScanWorker(scanDefinition, merge, this.notificationProvider);
		thread.execute();
	}

	public void mergeScan(Scan scan)
	{
		MergeScanWorker thread = new MergeScanWorker(scan, this.notificationProvider);
		thread.execute();
	}

	public void mergeScanResult(ComparisonResult result, AssuranceMergeStrategy strategy)
	{
		MergeScanResultWorker thread = new MergeScanResultWorker(result, strategy, this.notificationProvider);
		thread.execute();
	}

	public void saveScanDefinition(ScanDefinition scanDefinition)
	{
		StringBuilder message = new StringBuilder(256);
		logger.info(message.append("Saving scan defintion: ").append(scanDefinition));
		message.setLength(0);

		SaveScanDefinitionWorker thread = new SaveScanDefinitionWorker(scanDefinition, this.notificationProvider);
		thread.execute();
	}

	public void loadApplicationConfiguration() {
		logger.info("Loading the application configuration.");

		LoadApplicationConfigurationWorker thread = new LoadApplicationConfigurationWorker(this.notificationProvider);
		thread.execute();
	}

	public void saveApplicationConfiguration(ApplicationConfiguration configuration) 
	{
		logger.info("Saving the application configuration.");

		SaveApplicationConfigurationWorker thread = new SaveApplicationConfigurationWorker(configuration, this.notificationProvider);
		thread.execute();
	}

	public void loadScanDefinitions()
	{
		logger.info("Loading scan defintions.");

		LoadScanDefinitionsWorker thread = new LoadScanDefinitionsWorker(this.notificationProvider);
		thread.execute();
	}

	public void deleteScanDefinition(ScanDefinition scanDefinition)
	{
		StringBuilder message = new StringBuilder(256);
		logger.info(message.append("Deleting scan defintion: ").append(scanDefinition));
		message.setLength(0);

		DeleteScanDefinitionWorker thread = new DeleteScanDefinitionWorker(scanDefinition, this.notificationProvider);
		thread.execute();
	}

	public void loadScanResults(Scan scan)
	{
		StringBuilder message = new StringBuilder(256);
		logger.info(message.append("Loading results for scan: ").append(scan));
		message.setLength(0);

		LoadScanResultsWorker thread = new LoadScanResultsWorker(scan, this.notificationProvider);
		thread.execute();
	}

	public void loadScans()
	{
		logger.info("Loading scans.");

		LoadScansWorker thread = new LoadScansWorker(this.notificationProvider);
		thread.execute();
	}

	public void deleteScan(Scan scan)
	{
		StringBuilder message = new StringBuilder(256);
		logger.info(message.append("Deleting scan: ").append(scan));
		message.setLength(0);

		DeleteScanWorker thread = new DeleteScanWorker(scan, this.notificationProvider);
		thread.execute();
	}

	public void restoreDeletedItem(ComparisonResult result)
	{
		StringBuilder message = new StringBuilder(256);
		logger.info(message.append("Restoring deleted item for: ").append(result));
		message.setLength(0);

		RestoreDeletedItemWorker thread = new RestoreDeletedItemWorker(result, this.notificationProvider);
		thread.execute();
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
