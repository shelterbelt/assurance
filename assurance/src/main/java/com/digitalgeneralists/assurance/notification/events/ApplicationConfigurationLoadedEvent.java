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

package com.digitalgeneralists.assurance.notification.events;

import java.util.EventObject;

import com.digitalgeneralists.assurance.model.entities.ApplicationConfiguration;

public class ApplicationConfigurationLoadedEvent extends EventObject implements IAssuranceEvent
{
	private static final long serialVersionUID = 1L;

	public ApplicationConfigurationLoadedEvent(Object source)
	{
		super(source);
	}

	public ApplicationConfiguration getApplicationConfiguration()
	{
		return (ApplicationConfiguration) this.getSource();
	}
}
