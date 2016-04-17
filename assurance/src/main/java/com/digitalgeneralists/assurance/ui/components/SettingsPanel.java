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

package com.digitalgeneralists.assurance.ui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.digitalgeneralists.assurance.Application;
import com.digitalgeneralists.assurance.IApplicationDelegate;
import com.digitalgeneralists.assurance.model.entities.ApplicationConfiguration;
import com.digitalgeneralists.assurance.ui.components.dialogs.AssuranceDialogMode;
import com.digitalgeneralists.assurance.ui.components.dialogs.AssuranceDialogResult;

@Component("SettingsPanel")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SettingsPanel extends AbstractDialogInputPanel
{
	private static final long serialVersionUID = 1L;
	
	@Autowired
	private IApplicationDelegate applicationDelegate;

	protected final JTextField ignoredFileNamesTextField = new JTextField();
	protected final JTextField ignoredFileExtensionsTextField = new JTextField();
	protected final SpinnerModel numberScanThreadsSpinnerModel = new SpinnerNumberModel(4, 2, 32, 1);
	protected JSpinner numberScanThreadsSpinner = new JSpinner(numberScanThreadsSpinnerModel);
	
    private ApplicationConfiguration configuration;

	public void setConfiguration(ApplicationConfiguration configuration)
	{
		this.configuration = configuration;

		this.initializeComponent();
	}

	@Override
	public Object getResultObject() 
	{
		return this.configuration;
	}

	@Override
	protected AssuranceDialogResult processImplementationInputOnConfirm() 
	{
		this.configuration.setIgnoredFileNames(this.ignoredFileNamesTextField.getText());
		this.configuration.setIgnoredFileExtensions(this.ignoredFileExtensionsTextField.getText());
		this.configuration.setNumberOfScanThreads((Integer)this.numberScanThreadsSpinnerModel.getValue());
		
		this.applicationDelegate.saveApplicationConfiguration(this.configuration);
		
		return AssuranceDialogResult.CONFIRM;
	}

	@Override
	protected void initializeComponent() 
	{
		if (!this.initialized)
		{
			this.dialogTitle = Application.applicationShortName + " Settings";
			
			// NOTE:  There is no notion of add-mode in this dialog.  There should
			// always be a single instance of the application configuration.
			if (this.configuration == null)
			{
				this.configuration = ApplicationConfiguration.createDefaultConfiguration();
			}
			this.mode = AssuranceDialogMode.EDIT;
			
			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);
			
			final JPanel scanSettingsPanel = new JPanel();
			scanSettingsPanel.setLayout(new GridBagLayout());

			Border scanSettingsPanelBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			scanSettingsPanelBorder = BorderFactory.createTitledBorder(scanSettingsPanelBorder, "Scan Settings", TitledBorder.CENTER, TitledBorder.TOP);

			GridBagConstraints scanSettingsPanelConstraints = new GridBagConstraints();
			scanSettingsPanelConstraints.anchor = GridBagConstraints.NORTH;
			scanSettingsPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
			scanSettingsPanelConstraints.gridx = 0;
			scanSettingsPanelConstraints.gridy = 0;
			scanSettingsPanelConstraints.weightx = 1.0;
			scanSettingsPanelConstraints.weighty = 1.0;
			scanSettingsPanelConstraints.gridheight = 1;
			scanSettingsPanelConstraints.gridwidth = 2;
			scanSettingsPanelConstraints.insets = new Insets(5, 5, 5, 5);

			scanSettingsPanel.setBorder(scanSettingsPanelBorder);
			this.add(scanSettingsPanel, scanSettingsPanelConstraints);

			GridBagConstraints ignoredFileNamesLabelConstraints = new GridBagConstraints();
			ignoredFileNamesLabelConstraints.anchor = GridBagConstraints.NORTHWEST;
			ignoredFileNamesLabelConstraints.fill = GridBagConstraints.NONE;
			ignoredFileNamesLabelConstraints.gridx = 0;
			ignoredFileNamesLabelConstraints.gridy = 0;
			ignoredFileNamesLabelConstraints.weightx = 1.0;
			ignoredFileNamesLabelConstraints.weighty = 1.0;
			ignoredFileNamesLabelConstraints.gridheight = 1;
			ignoredFileNamesLabelConstraints.gridwidth = 1;
			ignoredFileNamesLabelConstraints.insets = new Insets(5, 10, 0, 5);

			final JLabel ignoredFileNamesLabel = new JLabel("Ignored Files", SwingConstants.LEFT);
			scanSettingsPanel.add(ignoredFileNamesLabel, ignoredFileNamesLabelConstraints);
			
			GridBagConstraints ignoredFileNamesTextFieldConstraints = new GridBagConstraints();
			ignoredFileNamesTextFieldConstraints.anchor = GridBagConstraints.NORTH;
			ignoredFileNamesTextFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			ignoredFileNamesTextFieldConstraints.gridx = 0;
			ignoredFileNamesTextFieldConstraints.gridy = 1;
			ignoredFileNamesTextFieldConstraints.weightx = 1.0;
			ignoredFileNamesTextFieldConstraints.weighty = 1.0;
			ignoredFileNamesTextFieldConstraints.gridheight = 1;
			ignoredFileNamesTextFieldConstraints.gridwidth = 2;
			ignoredFileNamesTextFieldConstraints.insets = new Insets(2, 5, 5, 5);
			
			GridBagConstraints ignoredFileExtensionsLabelConstraints = new GridBagConstraints();
			ignoredFileExtensionsLabelConstraints.anchor = GridBagConstraints.NORTHWEST;
			ignoredFileExtensionsLabelConstraints.fill = GridBagConstraints.NONE;
			ignoredFileExtensionsLabelConstraints.gridx = 0;
			ignoredFileExtensionsLabelConstraints.gridy = 2;
			ignoredFileExtensionsLabelConstraints.weightx = 1.0;
			ignoredFileExtensionsLabelConstraints.weighty = 1.0;
			ignoredFileExtensionsLabelConstraints.gridheight = 1;
			ignoredFileExtensionsLabelConstraints.gridwidth = 1;
			ignoredFileExtensionsLabelConstraints.insets = new Insets(5, 10, 0, 5);

			final JLabel ignoredFileExtensionsLabel = new JLabel("Ignored File Extensions", SwingConstants.LEFT);
			scanSettingsPanel.add(ignoredFileExtensionsLabel, ignoredFileExtensionsLabelConstraints);

			GridBagConstraints ignoredFileExtensionsTextFieldConstraints = new GridBagConstraints();
			ignoredFileExtensionsTextFieldConstraints.anchor = GridBagConstraints.NORTH;
			ignoredFileExtensionsTextFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			ignoredFileExtensionsTextFieldConstraints.gridx = 0;
			ignoredFileExtensionsTextFieldConstraints.gridy = 3;
			ignoredFileExtensionsTextFieldConstraints.weightx = 1.0;
			ignoredFileExtensionsTextFieldConstraints.weighty = 1.0;
			ignoredFileExtensionsTextFieldConstraints.gridheight = 1;
			ignoredFileExtensionsTextFieldConstraints.gridwidth = 2;
			ignoredFileExtensionsTextFieldConstraints.insets = new Insets(2, 5, 5, 5);

			scanSettingsPanel.add(this.ignoredFileNamesTextField, ignoredFileNamesTextFieldConstraints);
			this.ignoredFileNamesTextField.getDocument().addDocumentListener(this.textPropertyValidationListener);
			scanSettingsPanel.add(this.ignoredFileExtensionsTextField, ignoredFileExtensionsTextFieldConstraints);
			this.ignoredFileExtensionsTextField.getDocument().addDocumentListener(this.textPropertyValidationListener);
			
			GridBagConstraints numberScanThreadsLabelConstraints = new GridBagConstraints();
			numberScanThreadsLabelConstraints.anchor = GridBagConstraints.NORTH;
			numberScanThreadsLabelConstraints.fill = GridBagConstraints.NONE;
			numberScanThreadsLabelConstraints.gridx = 0;
			numberScanThreadsLabelConstraints.gridy = 4;
			numberScanThreadsLabelConstraints.weightx = 1.0;
			numberScanThreadsLabelConstraints.weighty = 1.0;
			numberScanThreadsLabelConstraints.gridheight = 1;
			numberScanThreadsLabelConstraints.gridwidth = 1;
			numberScanThreadsLabelConstraints.insets = new Insets(10, 5, 0, 5);

			final JLabel numberOfThreadsLabel = new JLabel("Number of Threads", SwingConstants.RIGHT);
			scanSettingsPanel.add(numberOfThreadsLabel, numberScanThreadsLabelConstraints);

			GridBagConstraints numberScanThreadsSpinnerConstraints = new GridBagConstraints();
			numberScanThreadsSpinnerConstraints.anchor = GridBagConstraints.NORTHWEST;
			numberScanThreadsSpinnerConstraints.fill = GridBagConstraints.NONE;
			numberScanThreadsSpinnerConstraints.gridx = 1;
			numberScanThreadsSpinnerConstraints.gridy = 4;
			numberScanThreadsSpinnerConstraints.weightx = 1.0;
			numberScanThreadsSpinnerConstraints.weighty = 1.0;
			numberScanThreadsSpinnerConstraints.gridheight = 1;
			numberScanThreadsSpinnerConstraints.gridwidth = 1;
			numberScanThreadsSpinnerConstraints.insets = new Insets(5, 5, 5, 5);
			
			scanSettingsPanel.add(this.numberScanThreadsSpinner, numberScanThreadsSpinnerConstraints);

			if (this.configuration != null)
			{
				this.ignoredFileNamesTextField.setText(this.configuration.getIgnoredFileNames());
				this.ignoredFileExtensionsTextField.setText(this.configuration.getIgnoredFileExtensions());
				this.numberScanThreadsSpinner.setValue(this.configuration.getNumberOfScanThreads());
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
		
		// No fields require validation.  The thread value is the only 
		// required field and it's input control governs input.
		// NOTE:  Assumptions like above tend to not age well.
		
		return result;
	}

}
