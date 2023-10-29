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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.markallenjohnson.assurance.IApplicationDelegate;
import com.markallenjohnson.assurance.model.ModelUtils;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.entities.ScanMappingDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;
import com.markallenjohnson.assurance.ui.components.dialogs.AssuranceDialogMode;
import com.markallenjohnson.assurance.ui.components.dialogs.AssuranceDialogResult;
import com.markallenjohnson.assurance.ui.components.dialogs.IDialogResponseHandler;
import com.markallenjohnson.assurance.ui.factories.IDialogFactory;

@Component("ScanDefinitionComponent")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ScanDefinitionPanel extends AbstractDialogInputPanel implements IDialogResponseHandler, IListInputPanelDelegate<ScanMappingDefinition>
{
	@Autowired
	private IApplicationDelegate applicationDelegate;

	@Autowired
	private IDialogFactory dialogFactory;

	// NOTE: This is less than ideal.  A new instance of the auto-wired app delegate 
	// spins up whenever I interact with prototype-scoped bean instances. Need to 
	// identify a better solution.
	public void setApplicationDelegate(IApplicationDelegate applicationDelegate)
	{
		this.applicationDelegate = applicationDelegate;
	}

	private ScanDefinition definition;

	private final JTextField nameTextField = new JTextField();

	private ListInputPanel<ScanMappingDefinition> scanMappingsList = null;

	private JComboBox<String> strategyComboBox;
	private final JCheckBox autoMergeCheckBox = new JCheckBox("Automatically Merge");
	private final JCheckBox includeNonCreationTimestampCheckBox = new JCheckBox("Include Timestamps Other than Create Date");
	private final JCheckBox includeAdvancedAttributesCheckBox = new JCheckBox("Include Advanced File Attributes");

	private static final long serialVersionUID = 1L;

	public ScanDefinitionPanel()
	{
	}

	public void setDefinition(ScanDefinition definition)
	{
		this.definition = definition;

		this.initializeComponent();
	}

	@Override
	protected void initializeComponent()
	{
		if (!this.initialized)
		{
			if (this.definition == null)
			{
				mode = AssuranceDialogMode.ADD;
				this.dialogTitle = "Add New Scan Definition";
				this.definition = new ScanDefinition();
			}
			else
			{
				mode = AssuranceDialogMode.EDIT;
				this.dialogTitle = "Edit Scan Definition";
			}

			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);

			final JPanel optionsPanel = new JPanel();
			optionsPanel.setLayout(new GridBagLayout());

			Border optionsBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			optionsBorder = BorderFactory.createTitledBorder(optionsBorder, "Merge Options", TitledBorder.CENTER, TitledBorder.TOP);

			GridBagConstraints nameTextFieldConstraints = new GridBagConstraints();
			nameTextFieldConstraints.anchor = GridBagConstraints.NORTH;
			nameTextFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			nameTextFieldConstraints.gridx = 0;
			nameTextFieldConstraints.gridy = 0;
			nameTextFieldConstraints.weightx = 1.0;
			nameTextFieldConstraints.weighty = 1.0;
			nameTextFieldConstraints.gridheight = 1;
			nameTextFieldConstraints.gridwidth = 2;
			nameTextFieldConstraints.insets = new Insets(10, 5, 0, 5);

			this.nameTextField.setText(this.definition.getName());
			this.nameTextField.getDocument().addDocumentListener(this.textPropertyValidationListener);
			this.add(this.nameTextField, nameTextFieldConstraints);

			Border existingScanMappingsPanelBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			existingScanMappingsPanelBorder = BorderFactory.createTitledBorder(existingScanMappingsPanelBorder, "Paths", TitledBorder.CENTER, TitledBorder.TOP);

			GridBagConstraints existingScanMappingsPanelConstraints = new GridBagConstraints();
			existingScanMappingsPanelConstraints.anchor = GridBagConstraints.WEST;
			existingScanMappingsPanelConstraints.fill = GridBagConstraints.BOTH;
			existingScanMappingsPanelConstraints.gridx = 0;
			existingScanMappingsPanelConstraints.gridy = 1;
			existingScanMappingsPanelConstraints.weightx = 1.0;
			existingScanMappingsPanelConstraints.weighty = 1.0;
			existingScanMappingsPanelConstraints.gridheight = 1;
			existingScanMappingsPanelConstraints.gridwidth = 2;
			existingScanMappingsPanelConstraints.insets = new Insets(5, 5, 0, 5);

			JPanel existingScanMappingsPanel = new JPanel();
			GridBagLayout panelGridbag = new GridBagLayout();
			existingScanMappingsPanel.setLayout(panelGridbag);
			existingScanMappingsPanel.setBorder(existingScanMappingsPanelBorder);
			this.add(existingScanMappingsPanel, existingScanMappingsPanelConstraints);

			GridBagConstraints existingScanMappingsListConstraints = new GridBagConstraints();
			existingScanMappingsListConstraints.anchor = GridBagConstraints.WEST;
			existingScanMappingsListConstraints.fill = GridBagConstraints.BOTH;
			existingScanMappingsListConstraints.gridx = 0;
			existingScanMappingsListConstraints.gridy = 0;
			existingScanMappingsListConstraints.weightx = 1.0;
			existingScanMappingsListConstraints.weighty = 0.9;
			existingScanMappingsListConstraints.gridheight = 1;
			existingScanMappingsListConstraints.gridwidth = 2;
			existingScanMappingsListConstraints.insets = new Insets(5, 5, 5, 5);

			this.definition = (ScanDefinition) ModelUtils.initializeEntity(this.definition, ScanDefinition.SCAN_MAPPING_PROPERTY);
			this.scanMappingsList = new ListInputPanel<>(this.definition, this);
			existingScanMappingsPanel.add(this.scanMappingsList, existingScanMappingsListConstraints);

			GridBagConstraints optionsPanelConstraints = new GridBagConstraints();
			optionsPanelConstraints.anchor = GridBagConstraints.SOUTH;
			optionsPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
			optionsPanelConstraints.gridx = 0;
			optionsPanelConstraints.gridy = 3;
			optionsPanelConstraints.weightx = 1.0;
			optionsPanelConstraints.weighty = 1.0;
			optionsPanelConstraints.gridheight = 1;
			optionsPanelConstraints.gridwidth = 2;
			optionsPanelConstraints.insets = new Insets(5, 5, 5, 5);

			optionsPanel.setBorder(optionsBorder);
			this.add(optionsPanel, optionsPanelConstraints);
			
			GridBagConstraints strategyLabelConstraints = new GridBagConstraints();
			strategyLabelConstraints.anchor = GridBagConstraints.WEST;
			strategyLabelConstraints.fill = GridBagConstraints.BOTH;
			strategyLabelConstraints.gridx = 0;
			strategyLabelConstraints.gridy = 0;
			strategyLabelConstraints.weightx = 1.0;
			strategyLabelConstraints.weighty = 1.0;
			strategyLabelConstraints.gridheight = 1;
			strategyLabelConstraints.gridwidth = 1;
			strategyLabelConstraints.insets = new Insets(5, 5, 0, 5);

			final JLabel strategyLabel = new JLabel("Strategy", SwingConstants.RIGHT);
			optionsPanel.add(strategyLabel, strategyLabelConstraints);

			GridBagConstraints strategyComboBoxConstraints = new GridBagConstraints();
			strategyComboBoxConstraints.anchor = GridBagConstraints.WEST;
			strategyComboBoxConstraints.fill = GridBagConstraints.VERTICAL;
			strategyComboBoxConstraints.gridx = 1;
			strategyComboBoxConstraints.gridy = 0;
			strategyComboBoxConstraints.weightx = 1.0;
			strategyComboBoxConstraints.weighty = 1.0;
			strategyComboBoxConstraints.gridheight = 1;
			strategyComboBoxConstraints.gridwidth = 1;
			strategyComboBoxConstraints.insets = new Insets(5, 5, 0, 5);

			String[] strategyLabels = { "Source", "Target", "Both" };
			this.strategyComboBox = new JComboBox<>(strategyLabels);
			// NOTE: We should have better validation of the data state for these controls.
			// We could run into problems as the application versions over time.
			this.strategyComboBox.setSelectedIndex(this.definition.getMergeStrategy().ordinal());
			this.strategyComboBox.addActionListener(e -> validateFormState());
			optionsPanel.add(this.strategyComboBox, strategyComboBoxConstraints);

			GridBagConstraints autoMergeCheckBoxConstraints = new GridBagConstraints();
			autoMergeCheckBoxConstraints.gridx = 0;
			autoMergeCheckBoxConstraints.gridy = 1;
			autoMergeCheckBoxConstraints.weightx = 1.0;
			autoMergeCheckBoxConstraints.weighty = 1.0;
			autoMergeCheckBoxConstraints.gridheight = 1;
			autoMergeCheckBoxConstraints.gridwidth = 2;
			autoMergeCheckBoxConstraints.insets = new Insets(2, 5, 5, 5);

			this.autoMergeCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
			this.autoMergeCheckBox.setSelected(this.definition.getAutoResolveConflicts());
			this.autoMergeCheckBox.addItemListener(e -> validateFormState());
			optionsPanel.add(this.autoMergeCheckBox, autoMergeCheckBoxConstraints);

			GridBagConstraints includeNonCreationTimestampsCheckBoxConstraints = new GridBagConstraints();
			includeNonCreationTimestampsCheckBoxConstraints.gridx = 0;
			includeNonCreationTimestampsCheckBoxConstraints.gridy = 2;
			includeNonCreationTimestampsCheckBoxConstraints.weightx = 1.0;
			includeNonCreationTimestampsCheckBoxConstraints.weighty = 1.0;
			includeNonCreationTimestampsCheckBoxConstraints.gridheight = 1;
			includeNonCreationTimestampsCheckBoxConstraints.gridwidth = 2;
			includeNonCreationTimestampsCheckBoxConstraints.insets = new Insets(2, 5, 5, 5);

			this.includeNonCreationTimestampCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
			this.includeNonCreationTimestampCheckBox.setSelected(this.definition.getIncludeNonCreationTimestamps());
			this.includeNonCreationTimestampCheckBox.addItemListener(e -> validateFormState());
			optionsPanel.add(this.includeNonCreationTimestampCheckBox, includeNonCreationTimestampsCheckBoxConstraints);

			GridBagConstraints advancedAttributesCheckBoxConstraints = new GridBagConstraints();
			advancedAttributesCheckBoxConstraints.gridx = 0;
			advancedAttributesCheckBoxConstraints.gridy = 3;
			advancedAttributesCheckBoxConstraints.weightx = 1.0;
			advancedAttributesCheckBoxConstraints.weighty = 1.0;
			advancedAttributesCheckBoxConstraints.gridheight = 1;
			advancedAttributesCheckBoxConstraints.gridwidth = 2;
			advancedAttributesCheckBoxConstraints.insets = new Insets(2, 5, 5, 5);

			this.includeAdvancedAttributesCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
			this.includeAdvancedAttributesCheckBox.setSelected(this.definition.getAutoResolveConflicts());
			this.includeAdvancedAttributesCheckBox.addItemListener(e -> validateFormState());
			optionsPanel.add(this.includeAdvancedAttributesCheckBox, advancedAttributesCheckBoxConstraints);
			
			this.scanMappingsList.loadData();
			
			if (this.getMode() == AssuranceDialogMode.EDIT)
			{
				this.validateFormState();
			}

			this.initialized = true;
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		this.scanMappingsList.actionPerformed(e);
	}

	private void displayMappingDefinitionDialog(ScanMappingDefinition selectedItem)
	{
		Window parent = SwingUtilities.getWindowAncestor(this.getParent());
		JDialog scanMappingDefinitionDialog = this.dialogFactory.createScanMappingDefinitionDialogInstance(parent, ModalityType.APPLICATION_MODAL, this, selectedItem);
		scanMappingDefinitionDialog.setVisible(true);
	}

	private void deleteScanMappingDefinition(ScanMappingDefinition selectedItem)
	{
		if (selectedItem != null)
		{
			// NOTE:  Leaking model initialization like this into the UI is less than ideal.
			this.definition = (ScanDefinition) ModelUtils.initializeEntity(this.definition, ScanDefinition.SCAN_MAPPING_PROPERTY);
			if (!this.definition.getUnmodifiableScanMapping().isEmpty())
			{
				this.definition.removeMappingDefinition(selectedItem);
			}
			
			this.scanMappingsList.loadData();
		}
	}

	@Override
	protected AssuranceDialogResult processImplementationInputOnConfirm()
	{
		if (!this.validateFormState())
		{
			return AssuranceDialogResult.VALIDATION_FAILED;
		}
		
		this.definition.setName(this.nameTextField.getText());
		// NOTE: We should have better validation of the data state for these controls.
		// We could run into problems as the application versions over time.
		this.definition.setMergeStrategy(AssuranceMergeStrategy.values()[strategyComboBox.getSelectedIndex()]);
		this.definition.setAutoResolveConflicts(this.autoMergeCheckBox.isSelected());
		this.definition.setIncludeNonCreationTimestamps(this.includeNonCreationTimestampCheckBox.isSelected());
		this.definition.setIncludeAdvancedAttributes(this.includeAdvancedAttributesCheckBox.isSelected());

		this.applicationDelegate.saveScanDefinition(this.definition);

		return AssuranceDialogResult.CONFIRM;
	}

	public boolean validateFormState()
	{
		Color controlInErrorBackgroundColor = Color.red;
		Color defaultControlBackgroundColor = Color.white;

		boolean result = true;

		if (!StringUtils.hasText(this.nameTextField.getText()))
		{
			this.nameTextField.setBackground(controlInErrorBackgroundColor);
			result = false;
		}
		else
		{
			this.nameTextField.setBackground(defaultControlBackgroundColor);
		}
		
		if (!this.scanMappingsList.validateFormState())
		{
			result = false;
		}

		return result;
	}

	public Object getResultObject()
	{
		return this.definition;
	}

	public void dialogClosed(AssuranceDialogResult result, Object resultObject)
	{
		if (result == AssuranceDialogResult.CONFIRM)
		{
			if (resultObject instanceof ScanMappingDefinition)
			{
				boolean bypass = false;
				for (ScanMappingDefinition mapping : this.definition.getUnmodifiableScanMapping())
				{
					if ((mapping.getId() != null) && (mapping.getId().equals(((ScanMappingDefinition)resultObject).getId())))
					{
						// NOTE:  I worry I'm bypassing the point of Hibernate here.
						// Only need this because the resultObject ends up as a dis-associated
						// instance from the definition through the child property initialization
						// process when values are loaded to the UI.  Should identify a way
						// to keep those in sync better.  This is just a bridge patch to get
						// over that challenge.
						mapping.mergeMappingDefinition((ScanMappingDefinition)resultObject);
						bypass = true;
						break;
					}
				}
				
				if (!bypass)
				{
					if (!this.definition.getUnmodifiableScanMapping().contains(resultObject))
					{
						this.definition.addMappingDefinition((ScanMappingDefinition)resultObject);
					}
				}
			}
			
			this.scanMappingsList.loadData();
			
			this.validateFormState();
		}
	}

	public void handlePrimaryButtonClick() 
	{
		this.handlePrimaryButtonClick(null);
	}

	public void handlePrimaryButtonClick(ScanMappingDefinition item) 
	{
		this.displayMappingDefinitionDialog(item);
	}

	public void handleSecondaryButtonClick() 
	{
		this.handleSecondaryButtonClick(null);
	}

	public void handleSecondaryButtonClick(ScanMappingDefinition item) 
	{
		this.deleteScanMappingDefinition(item);
	}

	public boolean listRequiresRecord() 
	{
		return true;
	}

	public String getPrimaryButtonAction() 
	{
		return AssuranceActions.newScanMappingDefinitonAction;
	}

	public String getPrimaryButtonLabel() 
	{
		return "New";
	}

	public String getSecondaryButtonAction() 
	{
		return AssuranceActions.deleteScanMappingDefinitonAction;
	}

	public String getSecondaryButtonLabel() 
	{
		return "Delete";
	}

	public void listValueChanged(boolean itemIsSelected) 
	{
		// No op
	}
}
