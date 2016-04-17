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

package com.digitalgeneralists.assurance.ui.components.dialogs;

import java.awt.Container;

import javax.swing.JPanel;

public class DialogPanel extends JPanel implements IDialogPanel
{
	private static final long serialVersionUID = 1L;

	private IDialogPanel panel;

	public DialogPanel(Container panel)
	{
		if (panel instanceof IDialogPanel)
		{
			this.panel = (IDialogPanel) panel;
		}
	}
	
	public DialogPanel(IDialogPanel panel)
	{
		this.panel = panel;
	}

	public String getDialogTitle()
	{
		return this.panel.getDialogTitle();
	}

	public void setDialogTitle(String title)
	{
		this.panel.setDialogTitle(title);
	}
	
	public AssuranceDialogMode getMode() 
	{
		return this.panel.getMode();
	}
}
