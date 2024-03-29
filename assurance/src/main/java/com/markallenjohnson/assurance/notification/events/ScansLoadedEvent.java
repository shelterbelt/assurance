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

package com.markallenjohnson.assurance.notification.events;

import java.util.EventObject;
import java.util.List;

import com.markallenjohnson.assurance.model.entities.Scan;

public class ScansLoadedEvent extends EventObject implements IAssuranceEvent
{
	private static final long serialVersionUID = 1L;

	public ScansLoadedEvent(Object source)
	{
		super(source);
	}

	@SuppressWarnings("unchecked")
	public List<Scan> getScans()
	{
		return (List<Scan>) this.getSource();
	}
}
