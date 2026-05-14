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

package com.markallenjohnson.assurance.ui.components;

public final class AssuranceActions
{
	private AssuranceActions() {
		throw new IllegalStateException("Utility class");
	}

	public static final String quitApplicationAction = "quitApplication";
	public static final String aboutApplicationAction = "aboutApplication";
	public static final String newScanDefinitonAction = "newScanDefiniton";
	public static final String deleteScanDefinitonAction = "deleteScanDefiniton";
	public static final String newScanMappingDefinitonAction = "newScanMappingDefiniton";
	public static final String deleteScanMappingDefinitonAction = "deleteScanMappingDefiniton";
	public static final String newExclusionAction = "newExclusion";
	public static final String deleteExclusionAction = "deleteExclusion";
	public static final String scanAction = "scan";
	public static final String scanAndMergeAction = "scanAndMerge";
	public static final String deleteScanAction = "deleteScan";
	public static final String resolveScanAction = "resolveScan";
	public static final String chooseFilePathAction = "chooseFilePath";
	public static final String replaceSourceAction = "replaceSource";
	public static final String replaceTargetAction = "replaceTarget";
	public static final String restoreDeletedItemAction = "restoreDeletedItem";
	public static final String sourceAttributesAction = "sourceAttributes";
	public static final String targetAttributesAction = "targetAttributes";
	public static final String viewScanAction = "viewScan";
	public static final String viewHistoryAction = "viewHistory";
	public static final String displaySettingsAction = "displaySettings";
}
