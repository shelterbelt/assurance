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

package com.markallenjohnson.assurance.ui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.markallenjohnson.assurance.ui.components.dialogs.AssuranceDialogMode;
import com.markallenjohnson.assurance.ui.components.dialogs.AssuranceDialogResult;
import com.markallenjohnson.assurance.ui.components.dialogs.IDialogInputPanel;
import com.markallenjohnson.assurance.ui.components.validators.IFormStateValidator;

public abstract class AbstractDialogInputPanel extends AbstractInputPanel implements ActionListener, IDialogInputPanel, IFormStateValidator 
{
	private static final long serialVersionUID = 1L;

	protected AssuranceDialogMode mode = AssuranceDialogMode.ADD;

	// NOTE:  This is one of the shortcomings of single-inheritance models
	// in strongly-typed languages.  Common behavior from related abstract 
	// classes may need to be replicated.
	// The solution I'm leveraging is to minimize the impact by replicating
	// the smallest set of common behavior.  This solution can easily fall apart when 
	// the common implementation is/becomes complex.  Picked AbstractInputPanel as the 
	// base class as it was significantly more complex than AbstractDialogPanel.
	protected String dialogTitle;

	public AssuranceDialogMode getMode()
	{
		return this.mode;
	}

	// NOTE:  This is one of the shortcomings of single-inheritance models
	// in strongly-typed languages.  Common behavior from related abstract 
	// classes may need to be replicated.
	// The solution I'm leveraging is to minimize the impact by replicating
	// the smallest set of common behavior.  This solution can easily fall apart when 
	// the common implementation is/becomes complex.  Picked AbstractInputPanel as the 
	// base class as it was significantly more complex than AbstractDialogPanel.
	public String getDialogTitle()
	{
		return dialogTitle;
	}

	public void setDialogTitle(String title)
	{
		this.dialogTitle = title;
	}

	public abstract Object getResultObject();

	public abstract void actionPerformed(ActionEvent e);

	// NOTE:  This is not a great abstraction.  While it works well, all an 
	// implementer would need to do to bypass the base validation is override 
	// processInputOnConfirm().  The impl hints at convention but doesn't enforce it.
	protected abstract AssuranceDialogResult processImplementationInputOnConfirm();

	public AssuranceDialogResult processInputOnConfirm()
	{
		if (!this.validateFormState())
		{
			return AssuranceDialogResult.VALIDATION_FAILED;
		}
		
		return this.processImplementationInputOnConfirm();
	}

	public AssuranceDialogResult processInputOnDiscard() 
	{
		return AssuranceDialogResult.CANCEL;
	}
}
