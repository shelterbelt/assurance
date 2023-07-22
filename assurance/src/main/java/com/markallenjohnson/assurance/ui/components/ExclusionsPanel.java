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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.ui.components.dialogs.AssuranceDialogMode;
import com.markallenjohnson.assurance.ui.components.dialogs.AssuranceDialogResult;

@Component("ExclusionsPanel")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ExclusionsPanel extends AbstractDialogInputPanel
{
	private static final long serialVersionUID = 1L;
	
	protected final FilePickerTextField exclusionPathTextFieldPicker = new FilePickerTextField("Exclusion");

	private FileReference exclusion;

	public void setExclusion(FileReference exclusion)
	{
		this.exclusion = exclusion;

		this.initializeComponent();
	}

	@Override
	public Object getResultObject() 
	{
		return this.exclusion;
	}

	@Override
	protected AssuranceDialogResult processImplementationInputOnConfirm() 
	{
		this.exclusion.setFile(new File(this.exclusionPathTextFieldPicker.getValue()));
		
		return AssuranceDialogResult.CONFIRM;
	}

	@Override
	protected void initializeComponent() 
	{
		if (!this.initialized)
		{
			if (this.exclusion == null)
			{
				this.mode = AssuranceDialogMode.ADD;
				this.dialogTitle = "Add New Exclusion";
				this.exclusion = new FileReference();
			}
			else
			{
				this.mode = AssuranceDialogMode.EDIT;
				this.dialogTitle = "Edit Exclusion";
			}
			
			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);
			
			final JPanel exclusionPathPanel = new JPanel();
			exclusionPathPanel.setLayout(new GridBagLayout());

			Border exclusionPanelBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			exclusionPanelBorder = BorderFactory.createTitledBorder(exclusionPanelBorder, "Exclusion", TitledBorder.CENTER, TitledBorder.TOP);

			GridBagConstraints exclusionPathPanelConstraints = new GridBagConstraints();
			exclusionPathPanelConstraints.anchor = GridBagConstraints.NORTH;
			exclusionPathPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
			exclusionPathPanelConstraints.gridx = 0;
			exclusionPathPanelConstraints.gridy = 0;
			exclusionPathPanelConstraints.weightx = 1.0;
			exclusionPathPanelConstraints.weighty = 1.0;
			exclusionPathPanelConstraints.gridheight = 1;
			exclusionPathPanelConstraints.gridwidth = 2;
			exclusionPathPanelConstraints.insets = new Insets(5, 5, 5, 5);

			exclusionPathPanel.setBorder(exclusionPanelBorder);
			this.add(exclusionPathPanel, exclusionPathPanelConstraints);

			GridBagConstraints exclusionPathFieldConstraints = new GridBagConstraints();
			exclusionPathFieldConstraints.anchor = GridBagConstraints.NORTH;
			exclusionPathFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			exclusionPathFieldConstraints.gridx = 0;
			exclusionPathFieldConstraints.gridy = 1;
			exclusionPathFieldConstraints.weightx = 1.0;
			exclusionPathFieldConstraints.weighty = 1.0;
			exclusionPathFieldConstraints.gridheight = 1;
			exclusionPathFieldConstraints.gridwidth = 1;
			exclusionPathFieldConstraints.insets = new Insets(5, 5, 5, 5);

			exclusionPathPanel.add(this.exclusionPathTextFieldPicker, exclusionPathFieldConstraints);
			
			if (this.exclusion != null)
			{
				File exclusionPath = exclusion.getFile();
				if (exclusionPath != null)
				{
					this.exclusionPathTextFieldPicker.setValue(exclusionPath.getPath());
				}
				else
				{
					this.exclusionPathTextFieldPicker.setValue("");
				}
			}

			this.initialized = true;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		
	}

	@Override
	public boolean validateFormState() 
	{
		boolean result = true;
		
		if (!this.exclusionPathTextFieldPicker.validateFormState())
		{
			result = false;
		}
		
		return result;
	}
}
