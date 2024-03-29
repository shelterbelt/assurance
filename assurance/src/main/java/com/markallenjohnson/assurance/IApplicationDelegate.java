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

package com.markallenjohnson.assurance;

import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;
import com.markallenjohnson.assurance.notification.INotificationProvider;

public interface IApplicationDelegate extends INotificationProvider
{
	void loadApplicationInitializationState();
	void loadApplicationConfiguration();
	void saveApplicationConfiguration(ApplicationConfiguration configuration);
	void performScan(ScanDefinition scanDefinition);
	void performScan(ScanDefinition scanDefinition, boolean merge);
	void mergeScan(Scan scan);
	void mergeScanResult(ComparisonResult result, AssuranceMergeStrategy strategy);
	void saveScanDefinition(ScanDefinition scanDefinition);
	void loadScanDefinitions();
	void deleteScanDefinition(ScanDefinition scanDefinition);
	void loadScanResults(Scan scan);
	void loadScans();
	void deleteScan(Scan scan);
	void restoreDeletedItem(ComparisonResult result);
}
