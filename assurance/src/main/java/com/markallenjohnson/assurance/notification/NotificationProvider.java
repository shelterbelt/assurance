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

package com.markallenjohnson.assurance.notification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.notification.events.IAssuranceEvent;

@Component("NotificationProvider")
public class NotificationProvider implements INotificationProvider
{
	private Logger logger = LogManager.getLogger(NotificationProvider.class);

	protected Map<Class<? extends IAssuranceEvent>, Collection<IEventObserver>> eventObserverList = new HashMap<Class<? extends IAssuranceEvent>, Collection<IEventObserver>>();

	public void addEventObserver(Class<? extends IAssuranceEvent> eventClass, IEventObserver observer)
	{
		Collection<IEventObserver> eventObservers = eventObserverList.get(eventClass);
		if (eventObservers == null)
		{
			eventObservers = new ArrayList<IEventObserver>();
		}
		eventObservers.add(observer);
		eventObserverList.put(eventClass, eventObservers);
		
		eventObservers = null;
	}

	@SuppressWarnings("unlikely-arg-type")
	public void removeEventObserver(Class<? extends IAssuranceEvent> eventClass, IEventObserver observer)
	{
		Collection<IEventObserver> eventObservers = eventObserverList.get(eventClass);
		if (eventObservers != null)
		{
			eventObservers.remove(observer);
			if (eventObservers.size() == 0)
			{
				eventObserverList.remove(eventObservers);
			}
		}
		
		eventObservers = null;
	}

	public void fireEvent(IAssuranceEvent event)
	{
		StringBuffer message = new StringBuffer(128);
		logger.info(message.append("Firing event: ").append(event));
		message.setLength(0);
		message = null;
		Collection<IEventObserver> eventObservers = eventObserverList.get(event.getClass());
		if (eventObservers != null)
		{
			for (IEventObserver observer : eventObservers)
			{
				observer.notify(event);
			}
		}
		
		eventObservers = null;
	}
}
