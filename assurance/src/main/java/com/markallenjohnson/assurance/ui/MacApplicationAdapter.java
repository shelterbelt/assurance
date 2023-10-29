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

package com.markallenjohnson.assurance.ui;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class MacApplicationAdapter extends ApplicationAdapter {
	
	private IApplicationUI handler;
	
	public MacApplicationAdapter(IApplicationUI handler)
	{
		this.handler = handler;
	}
	
	@Override
	public void handleQuit(ApplicationEvent e)
	{
		System.exit(0);
	}
	
	@Override
	public void handleAbout(ApplicationEvent e)
	{
		// NOTE:  Enable these lines to display the application-provided
		// About dialog on OS X.
		// Choosing to use the Mac provided dialog on OSX.
		//e.setHandled(true);
		//this.handler.displayAboutDialog();
	}
	
	@Override
	public void handlePreferences(ApplicationEvent e)
	{
		this.handler.displayPreferencesDialog();
	}
}
