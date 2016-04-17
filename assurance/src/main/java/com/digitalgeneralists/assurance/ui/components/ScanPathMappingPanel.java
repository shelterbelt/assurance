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
import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.digitalgeneralists.assurance.model.ModelUtils;
import com.digitalgeneralists.assurance.model.entities.FileReference;
import com.digitalgeneralists.assurance.model.entities.ScanMappingDefinition;
import com.digitalgeneralists.assurance.ui.components.dialogs.AssuranceDialogMode;
import com.digitalgeneralists.assurance.ui.components.dialogs.AssuranceDialogResult;
import com.digitalgeneralists.assurance.ui.components.dialogs.IDialogResponseHandler;
import com.digitalgeneralists.assurance.ui.factories.IDialogFactory;

@Component("ScanPathMappingPanel")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ScanPathMappingPanel extends AbstractDialogInputPanel implements IDialogResponseHandler, IListInputPanelDelegate<FileReference>
{
	@Autowired
	private IDialogFactory dialogFactory;
	
	private static final long serialVersionUID = 1L;

	private ScanMappingDefinition mappingDefinition;

	protected final FilePickerTextField sourcePathPickerField = new FilePickerTextField("Source");
	protected final FilePickerTextField targetPathPickerField = new FilePickerTextField("Target");

	private ListInputPanel<FileReference> exclusionsPanel = null;
	
	public void setMapping(ScanMappingDefinition mappingDefinition)
	{
		this.mappingDefinition = mappingDefinition;

		this.initializeComponent();
	}

	protected void initializeComponent()
	{
		if (!this.initialized)
		{
			if (this.mappingDefinition == null)
			{
				this.mode = AssuranceDialogMode.ADD;
				this.dialogTitle = "Add New Path Mapping";
				this.mappingDefinition = new ScanMappingDefinition();
			}
			else
			{
				this.mode = AssuranceDialogMode.EDIT;
				this.dialogTitle = "Edit Path Mapping";
			}
			
			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);

			final JPanel scanPathsPanel = new JPanel();
			scanPathsPanel.setLayout(new GridBagLayout());

			Border pathsPanelBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			pathsPanelBorder = BorderFactory.createTitledBorder(pathsPanelBorder, "Paths", TitledBorder.CENTER, TitledBorder.TOP);

			GridBagConstraints scanPathsPanelConstraints = new GridBagConstraints();
			scanPathsPanelConstraints.anchor = GridBagConstraints.NORTH;
			scanPathsPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
			scanPathsPanelConstraints.gridx = 0;
			scanPathsPanelConstraints.gridy = 0;
			scanPathsPanelConstraints.weightx = 1.0;
			scanPathsPanelConstraints.weighty = 1.0;
			scanPathsPanelConstraints.gridheight = 1;
			scanPathsPanelConstraints.gridwidth = 2;
			scanPathsPanelConstraints.insets = new Insets(5, 5, 5, 5);

			scanPathsPanel.setBorder(pathsPanelBorder);
			this.add(scanPathsPanel, scanPathsPanelConstraints);

			GridBagConstraints sourcePathFieldConstraints = new GridBagConstraints();
			sourcePathFieldConstraints.anchor = GridBagConstraints.NORTH;
			sourcePathFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			sourcePathFieldConstraints.gridx = 0;
			sourcePathFieldConstraints.gridy = 0;
			sourcePathFieldConstraints.weightx = 1.0;
			sourcePathFieldConstraints.weighty = 1.0;
			sourcePathFieldConstraints.gridheight = 1;
			sourcePathFieldConstraints.gridwidth = 1;
			sourcePathFieldConstraints.insets = new Insets(0, 5, 5, 5);

			GridBagConstraints targetPathFieldConstraints = new GridBagConstraints();
			targetPathFieldConstraints.anchor = GridBagConstraints.NORTH;
			targetPathFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			targetPathFieldConstraints.gridx = 0;
			targetPathFieldConstraints.gridy = 1;
			targetPathFieldConstraints.weightx = 1.0;
			targetPathFieldConstraints.weighty = 1.0;
			targetPathFieldConstraints.gridheight = 1;
			targetPathFieldConstraints.gridwidth = 1;
			targetPathFieldConstraints.insets = new Insets(5, 5, 5, 5);

			scanPathsPanel.add(this.sourcePathPickerField, sourcePathFieldConstraints);
			scanPathsPanel.add(this.targetPathPickerField, targetPathFieldConstraints);
			
			if (mappingDefinition != null)
			{
				File source = mappingDefinition.getSource();
				if (source != null)
				{
					this.sourcePathPickerField.setValue(source.getPath());
				}
				else
				{
					this.sourcePathPickerField.setValue("");
				}
				File target = mappingDefinition.getTarget();
				if (target != null)
				{
					this.targetPathPickerField.setValue(target.getPath());
				}
				else
				{
					this.targetPathPickerField.setValue("");
				}
			}

			Border existingExclusionsPanelBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			existingExclusionsPanelBorder = BorderFactory.createTitledBorder(existingExclusionsPanelBorder, "Exclusions", TitledBorder.CENTER, TitledBorder.TOP);

			GridBagConstraints existingExclusionsPanelConstraints = new GridBagConstraints();
			existingExclusionsPanelConstraints.anchor = GridBagConstraints.WEST;
			existingExclusionsPanelConstraints.fill = GridBagConstraints.BOTH;
			existingExclusionsPanelConstraints.gridx = 0;
			existingExclusionsPanelConstraints.gridy = 1;
			existingExclusionsPanelConstraints.weightx = 1.0;
			existingExclusionsPanelConstraints.weighty = 0.9;
			existingExclusionsPanelConstraints.gridheight = 1;
			existingExclusionsPanelConstraints.gridwidth = 2;
			existingExclusionsPanelConstraints.insets = new Insets(0, 5, 0, 5);

			JPanel existingExclusionsPanel = new JPanel();
			GridBagLayout panelGridbag = new GridBagLayout();
			existingExclusionsPanel.setLayout(panelGridbag);
			existingExclusionsPanel.setBorder(existingExclusionsPanelBorder);
			this.add(existingExclusionsPanel, existingExclusionsPanelConstraints);

			GridBagConstraints existingExclusionsListConstraints = new GridBagConstraints();
			existingExclusionsListConstraints.anchor = GridBagConstraints.WEST;
			existingExclusionsListConstraints.fill = GridBagConstraints.BOTH;
			existingExclusionsListConstraints.gridx = 0;
			existingExclusionsListConstraints.gridy = 0;
			existingExclusionsListConstraints.weightx = 1.0;
			existingExclusionsListConstraints.weighty = 0.9;
			existingExclusionsListConstraints.gridheight = 1;
			existingExclusionsListConstraints.gridwidth = 2;
			existingExclusionsListConstraints.insets = new Insets(5, 5, 5, 5);

			this.mappingDefinition = (ScanMappingDefinition) ModelUtils.initializeEntity(this.mappingDefinition, ScanMappingDefinition.EXCLUSIONS_PROPERTY);
			this.exclusionsPanel = new ListInputPanel<FileReference>(this.mappingDefinition, this);
			existingExclusionsPanel.add(this.exclusionsPanel, existingExclusionsListConstraints);

			this.initialized = true;
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		this.exclusionsPanel.actionPerformed(e);
	}

	private void displayExclusionDialog(FileReference selectedItem)
	{
		Window parent = SwingUtilities.getWindowAncestor(this.getParent());
		JDialog exclusionDialog = this.dialogFactory.createExclusionDialogInstance(parent, ModalityType.APPLICATION_MODAL, this, selectedItem);
		exclusionDialog.setVisible(true);
	}

	private void deleteExclusion(FileReference selectedItem)
	{
		if (selectedItem != null)
		{
			if (this.mappingDefinition.getUnmodifiableExclusions().size() > 0)
			{
				this.mappingDefinition.removeExclusion(selectedItem);
			}
			
			this.exclusionsPanel.loadData();
		}
	}

	public Object getResultObject()
	{
		return this.mappingDefinition;
	}

	protected AssuranceDialogResult processImplementationInputOnConfirm()
	{
		this.mappingDefinition.setSource(new File(this.sourcePathPickerField.getValue()));
		this.mappingDefinition.setTarget(new File(this.targetPathPickerField.getValue()));

		return AssuranceDialogResult.CONFIRM;
	}

	public boolean validateFormState()
	{
		boolean result = true;
		
		if (!this.sourcePathPickerField.validateFormState())
		{
			result = false;
		}
		
		if (!this.targetPathPickerField.validateFormState())
		{
			result = false;
		}
		
		// Exclusions are not required.  The collection can be empty.
		
		return result;
	}

	public void dialogClosed(AssuranceDialogResult result, Object resultObject)
	{
		if (result == AssuranceDialogResult.CONFIRM)
		{
			if ((resultObject != null) && (resultObject instanceof FileReference))
			{
				boolean bypass = false;
				for (FileReference exclusion : this.mappingDefinition.getUnmodifiableExclusions())
				{
					if ((exclusion.getId() != null) && (exclusion.getId().equals(((FileReference)resultObject).getId())))
					{
						// NOTE:  I worry I'm bypassing the point of Hibernate here.
						// Only need this because the resultObject ends up as a dis-associated
						// instance from the definition through the child property initialization
						// process when values are loaded to the UI.  Should identify a way
						// to keep those in sync better.  This is just a bridge patch to get
						// over that challenge.
						exclusion.mergeFileReference((FileReference)resultObject);
						bypass = true;
						break;
					}
				}
				
				if (bypass != true)
				{
					if (!this.mappingDefinition.getUnmodifiableExclusions().contains(resultObject))
					{
						this.mappingDefinition.addExclusion((FileReference)resultObject);
					}
				}
			}
			
			this.exclusionsPanel.loadData();
			
			this.validateFormState();
		}
	}
	
	public Collection<FileReference> getListData() 
	{
		return this.mappingDefinition.getUnmodifiableExclusions();
	}

	public String getInitializationPropertyName() 
	{
		return ScanMappingDefinition.EXCLUSIONS_PROPERTY;
	}

	public boolean listRequiresRecord() 
	{
		return false;
	}

	public String getPrimaryButtonAction() 
	{
		return AssuranceActions.newExclusionAction;
	}

	public String getPrimaryButtonLabel() 
	{
		return "New";
	}

	public void handlePrimaryButtonClick()
	{
		this.handlePrimaryButtonClick(null);
	}

	public void handlePrimaryButtonClick(FileReference item) 
	{
		this.displayExclusionDialog(item);
	}

	public String getSecondaryButtonAction() 
	{
		return AssuranceActions.deleteExclusionAction;
	}

	public String getSecondaryButtonLabel() 
	{
		return "Delete";
	}

	public void handleSecondaryButtonClick() 
	{
		this.handleSecondaryButtonClick(null);
	}

	public void handleSecondaryButtonClick(FileReference item) 
	{
		this.deleteExclusion(item);
	}

	public void listValueChanged(boolean itemIsSelected) 
	{
		
	}
}