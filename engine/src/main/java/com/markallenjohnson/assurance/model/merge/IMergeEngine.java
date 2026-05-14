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

package com.markallenjohnson.assurance.model.merge;

import com.markallenjohnson.assurance.exceptions.AssuranceNullFileReferenceException;
import com.markallenjohnson.assurance.model.concurrency.IAssuranceThreadPool;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.notification.IProgressMonitor;

public interface IMergeEngine
{
	void mergeResult(ComparisonResult result, IProgressMonitor monitor) throws AssuranceNullFileReferenceException;
	void mergeScan(Scan scan, IAssuranceThreadPool threadPool, IProgressMonitor monitor) throws AssuranceNullFileReferenceException;
	void restoreDeletedItem(ComparisonResult result, IProgressMonitor monitor);
}
