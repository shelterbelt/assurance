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

package com.markallenjohnson.assurance.ui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("FilePickerTextField")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FilePickerTextField extends AbstractInputPanel implements ActionListener
{
	private static final long serialVersionUID = 1L;
	
	private final JFileChooser filePicker = new JFileChooser();

	protected final JTextField pathTextField = new JTextField();
	protected final JButton pathFileChooserButton = new JButton("...");
	
	private String fieldName = "Path";
	
	public FilePickerTextField(String fieldName)
	{
		this.setFieldName(fieldName);
	}
	
	public void setFieldName(String name)
	{
		if (StringUtils.hasText(name))
		{
			this.fieldName = name;
		}
	}
	
	public void setValue(String value)
	{
		if (value != null)
		{
			this.pathTextField.setText(value);
		}

		this.initializeComponent();
	}
	
	public String getValue()
	{
		return this.pathTextField.getText();
	}
	
	protected void initializeComponent()
	{
		if (!this.initialized)
		{
			this.filePicker.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);

			final JPanel filePathPanel = new JPanel();
			filePathPanel.setLayout(new GridBagLayout());

			GridBagConstraints filePathPanelConstraints = new GridBagConstraints();
			filePathPanelConstraints.anchor = GridBagConstraints.NORTH;
			filePathPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
			filePathPanelConstraints.gridx = 0;
			filePathPanelConstraints.gridy = 0;
			filePathPanelConstraints.weightx = 1.0;
			filePathPanelConstraints.weighty = 1.0;
			filePathPanelConstraints.gridheight = 1;
			filePathPanelConstraints.gridwidth = 2;
			filePathPanelConstraints.insets = new Insets(0, 0, 0, 0);

			GridBagConstraints pathTextFieldConstraints = new GridBagConstraints();
			pathTextFieldConstraints.anchor = GridBagConstraints.NORTH;
			pathTextFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			pathTextFieldConstraints.gridx = 0;
			pathTextFieldConstraints.gridy = 0;
			pathTextFieldConstraints.weightx = 0.9;
			pathTextFieldConstraints.weighty = 1.0;
			pathTextFieldConstraints.gridheight = 1;
			pathTextFieldConstraints.gridwidth = 1;
			pathTextFieldConstraints.insets = new Insets(0, 0, 0, 0);

			GridBagConstraints pathFileChooserButtonConstraints = new GridBagConstraints();
			pathFileChooserButtonConstraints.anchor = GridBagConstraints.NORTH;
			pathFileChooserButtonConstraints.fill = GridBagConstraints.HORIZONTAL;
			pathFileChooserButtonConstraints.gridx = 1;
			pathFileChooserButtonConstraints.gridy = 0;
			pathFileChooserButtonConstraints.weightx = 0.1;
			pathFileChooserButtonConstraints.weighty = 1.0;
			pathFileChooserButtonConstraints.gridheight = 1;
			pathFileChooserButtonConstraints.gridwidth = 1;
			pathFileChooserButtonConstraints.insets = new Insets(0, 0, 0, 0);

			filePathPanel.add(this.pathTextField, pathTextFieldConstraints);
			filePathPanel.add(this.pathFileChooserButton, pathFileChooserButtonConstraints);
			this.add(filePathPanel, filePathPanelConstraints);
			this.pathFileChooserButton.setActionCommand(AssuranceActions.chooseFilePathAction);
			this.pathTextField.getDocument().addDocumentListener(this.textPropertyValidationListener);
			this.pathFileChooserButton.addActionListener(this);

			this.initialized = true;
		}
	}

	public void actionPerformed(ActionEvent e) 
	{
		if (AssuranceActions.chooseFilePathAction.equals(e.getActionCommand()))
		{
			this.filePicker.setDialogTitle("Choose " + this.fieldName);

			int returnVal = this.filePicker.showOpenDialog(this);

			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				File file = filePicker.getSelectedFile();
				if (AssuranceActions.chooseFilePathAction.equals(e.getActionCommand()))
				{
					this.pathTextField.setText(file.getAbsolutePath());
				}
			}
		}
	}

	public boolean validateFormState() 
	{
		boolean result = true;
		
		if (!StringUtils.hasText(this.pathTextField.getText()))
		{
			this.pathTextField.setBackground(this.controlInErrorBackgroundColor);
			result = false;
		}
		else
		{
			this.pathTextField.setBackground(this.defaultControlBackgroundColor);
		}
		
		return result;
	}
}
