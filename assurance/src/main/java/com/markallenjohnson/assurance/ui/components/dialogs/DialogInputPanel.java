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

package com.markallenjohnson.assurance.ui.components.dialogs;

import java.awt.Container;

public class DialogInputPanel extends DialogPanel implements IDialogInputPanel
{
	private static final long serialVersionUID = 1L;

	private IDialogInputPanel inputPanel;

	public DialogInputPanel(Container panel)
	{
		super(panel);
		
		if (panel instanceof IDialogInputPanel)
		{
			this.inputPanel = (IDialogInputPanel) panel;
		}
	}
	public DialogInputPanel(IDialogInputPanel panel)
	{
		super(panel);
		
		this.inputPanel = panel;
	}

	public AssuranceDialogResult processInputOnConfirm()
	{
		return this.inputPanel.processInputOnConfirm();
	}

	public AssuranceDialogResult processInputOnDiscard()
	{
		return this.inputPanel.processInputOnDiscard();
	}
	
	public Object getResultObject()
	{
		return this.inputPanel.getResultObject();
	}
}
