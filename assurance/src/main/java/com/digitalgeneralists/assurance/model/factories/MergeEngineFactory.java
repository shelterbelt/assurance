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

import com.digitalgeneralists.assurance.model.enums.AssuranceMergeStrategy;
import com.digitalgeneralists.assurance.model.merge.IMergeEngine;

@Component("MergeEngineFactory")
public class MergeEngineFactory implements IMergeEngineFactory
{
	@Autowired
	@Qualifier("SourceMergeEngine")
	private IMergeEngine sourceMergeEngine;
	@Autowired
	@Qualifier("TargetMergeEngine")
	private IMergeEngine targetMergeEngine;
	@Autowired
	@Qualifier("BidirectionalMergeEngine")
	private IMergeEngine bidirectionalMergeEngine;

	public IMergeEngine createInstance(AssuranceMergeStrategy strategy)
	{
		switch (strategy)
		{
			case SOURCE:
				return this.sourceMergeEngine;
			case TARGET:
				return this.targetMergeEngine;
			case BOTH:
				return this.bidirectionalMergeEngine;
			default:
				return this.bidirectionalMergeEngine;
		}
	}
}
