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

package com.digitalgeneralists.assurance.model.factories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.digitalgeneralists.assurance.model.compare.file.IFileComparor;

@Component("FileComparorFactory")
public class FileCompareValidatorFactory implements IFileComparorFactory
{
	@Autowired
	@Qualifier("DeepScanFileCompareValidator")
	private IFileComparor deepScanComparor;
	@Autowired
	@Qualifier("LightweightFileCompareValidator")
	private IFileComparor lightweightComparor;
	@Autowired
	@Qualifier("FileCompareValidator")
	private IFileComparor comparor;

	public IFileComparor createInstance()
	{
		return comparor;
	}

	public IFileComparor createInstance(boolean enableDeepScan)
	{
		if (enableDeepScan)
		{
			return deepScanComparor;
		}

		return lightweightComparor;
	}
}
