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

import java.awt.Dialog.ModalityType;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.digitalgeneralists.assurance.IApplicationDelegate;
import com.digitalgeneralists.assurance.model.IListDataProvider;
import com.digitalgeneralists.assurance.model.entities.ScanDefinition;
import com.digitalgeneralists.assurance.notification.IEventObserver;
import com.digitalgeneralists.assurance.notification.events.IAssuranceEvent;
import com.digitalgeneralists.assurance.notification.events.ScanCompletedEvent;
import com.digitalgeneralists.assurance.notification.events.ScanDefinitionDeletedEvent;
import com.digitalgeneralists.assurance.notification.events.ScanDefinitionSavedEvent;
import com.digitalgeneralists.assurance.notification.events.ScanDefinitionsLoadedEvent;
import com.digitalgeneralists.assurance.notification.events.ScanStartedEvent;
import com.digitalgeneralists.assurance.notification.events.SetScanDefinitionMenuStateEvent;
import com.digitalgeneralists.assurance.ui.components.dialogs.AssuranceDialogResult;
import com.digitalgeneralists.assurance.ui.components.dialogs.IDialogResponseHandler;
import com.digitalgeneralists.assurance.ui.factories.IDialogFactory;

@Component("ScanListComponent")
public class ScanLaunchPanel extends JPanel implements ActionListener, IDialogResponseHandler, IEventObserver, IListInputPanelDelegate<ScanDefinition>, IListDataProvider<ScanDefinition>
{
	@Autowired
	private IApplicationDelegate applicationDelegate;

	@Autowired
	private IDialogFactory dialogFactory;

	private boolean initialized = false;

	private List<ScanDefinition> scanDefinitions;

	private ListInputPanel<ScanDefinition> existingScanDefinitionsListPanel = null;

	private final JButton startScanButton = new JButton("Scan");
	private final JButton startScanAndMergeButton = new JButton("Scan and Merge");

	private static final long serialVersionUID = 1L;

	public ScanLaunchPanel()
	{
		this.initializeComponent();
	}

	private void initializeComponent()
	{
		if (!this.initialized)
		{
			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);

			GridBagConstraints existingScansPanelConstraints = new GridBagConstraints();
			existingScansPanelConstraints.anchor = GridBagConstraints.WEST;
			existingScansPanelConstraints.fill = GridBagConstraints.BOTH;
			existingScansPanelConstraints.gridx = 0;
			existingScansPanelConstraints.gridy = 0;
			existingScansPanelConstraints.weightx = 1.0;
			existingScansPanelConstraints.weighty = 1.0;
			existingScansPanelConstraints.gridheight = 2;
			existingScansPanelConstraints.gridwidth = 1;
			existingScansPanelConstraints.insets = new Insets(0, 0, 0, 0);

			JPanel existingScansPanel = new JPanel();
			GridBagLayout panelGridbag = new GridBagLayout();
			existingScansPanel.setLayout(panelGridbag);
			this.add(existingScansPanel, existingScansPanelConstraints);

			GridBagConstraints existingScansListConstraints = new GridBagConstraints();
			existingScansListConstraints.anchor = GridBagConstraints.WEST;
			existingScansListConstraints.fill = GridBagConstraints.BOTH;
			existingScansListConstraints.gridx = 0;
			existingScansListConstraints.gridy = 0;
			existingScansListConstraints.weightx = 1.0;
			existingScansListConstraints.weighty = 0.9;
			existingScansListConstraints.gridheight = 1;
			existingScansListConstraints.gridwidth = 2;
			existingScansListConstraints.insets = new Insets(5, 5, 5, 5);

			GridBagConstraints existingScanDefinitionsListConstraints = new GridBagConstraints();
			existingScanDefinitionsListConstraints.anchor = GridBagConstraints.WEST;
			existingScanDefinitionsListConstraints.fill = GridBagConstraints.BOTH;
			existingScanDefinitionsListConstraints.gridx = 0;
			existingScanDefinitionsListConstraints.gridy = 0;
			existingScanDefinitionsListConstraints.weightx = 1.0;
			existingScanDefinitionsListConstraints.weighty = 0.9;
			existingScanDefinitionsListConstraints.gridheight = 1;
			existingScanDefinitionsListConstraints.gridwidth = 2;
			existingScanDefinitionsListConstraints.insets = new Insets(5, 5, 5, 5);

			this.existingScanDefinitionsListPanel = new ListInputPanel<ScanDefinition>(this, this);
			existingScansPanel.add(this.existingScanDefinitionsListPanel, existingScanDefinitionsListConstraints);

			GridBagConstraints scanButtonConstraints = new GridBagConstraints();
			scanButtonConstraints.anchor = GridBagConstraints.NORTHEAST;
			scanButtonConstraints.fill = GridBagConstraints.BOTH;
			scanButtonConstraints.gridx = 1;
			scanButtonConstraints.gridy = 0;
			scanButtonConstraints.weightx = 1.0;
			scanButtonConstraints.weighty = 1.0;

			this.startScanButton.setActionCommand(AssuranceActions.scanAction);

			this.add(this.startScanButton, scanButtonConstraints);

			GridBagConstraints scanAndMergeButtonConstraints = new GridBagConstraints();
			scanAndMergeButtonConstraints.anchor = GridBagConstraints.SOUTHEAST;
			scanAndMergeButtonConstraints.fill = GridBagConstraints.BOTH;
			scanAndMergeButtonConstraints.gridx = 1;
			scanAndMergeButtonConstraints.gridy = 1;
			scanAndMergeButtonConstraints.weightx = 1.0;
			scanAndMergeButtonConstraints.weighty = 1.0;

			this.startScanAndMergeButton.setActionCommand(AssuranceActions.scanAndMergeAction);

			this.add(this.startScanAndMergeButton, scanAndMergeButtonConstraints);

			this.startScanAndMergeButton.addActionListener(this);
			this.startScanButton.addActionListener(this);

			this.startScanButton.setEnabled(false);
			this.startScanAndMergeButton.setEnabled(false);

			this.addAncestorListener(new AncestorListener()
			{
				public void ancestorAdded(AncestorEvent event)
				{
					applicationDelegate.addEventObserver(ScanStartedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanCompletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanDefinitionDeletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanDefinitionSavedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanDefinitionsLoadedEvent.class, (IEventObserver) event.getSource());
				}

				public void ancestorRemoved(AncestorEvent event)
				{
					applicationDelegate.removeEventObserver(ScanStartedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanCompletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanDefinitionDeletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanDefinitionSavedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanDefinitionsLoadedEvent.class, (IEventObserver) event.getSource());
				}

				public void ancestorMoved(AncestorEvent event)
				{
				}
			});

			this.initialized = true;
		}
	}

	private void displayDefinitionDialog(ScanDefinition selectedItem)
	{
		Window parent = SwingUtilities.getWindowAncestor(this.getParent());
		JDialog scanDefinitionDialog = this.dialogFactory.createScanDefinitionDialogInstance(parent, ModalityType.APPLICATION_MODAL, this, selectedItem);
		scanDefinitionDialog.setVisible(true);
	}

	private void deleteScanDefinition(ScanDefinition selectedItem)
	{
		if (selectedItem != null)
		{
			this.applicationDelegate.deleteScanDefinition(selectedItem);
		}
	}

	private void startScan(ScanDefinition selectedItem)
	{
		this.startScan(selectedItem, false);
	}

	private void startScan(ScanDefinition selectedItem, boolean merge)
	{
		if (selectedItem != null)
		{
			this.applicationDelegate.performScan(selectedItem, merge);
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		if (AssuranceActions.scanAction.equals(e.getActionCommand()))
		{
			this.startScan(this.existingScanDefinitionsListPanel.getSelectedValue());
			return;
		}
		if (AssuranceActions.scanAndMergeAction.equals(e.getActionCommand()))
		{
			this.startScan(this.existingScanDefinitionsListPanel.getSelectedValue(), true);
			return;
		}
		this.existingScanDefinitionsListPanel.actionPerformed(e);
	}

	public void dialogClosed(AssuranceDialogResult result, Object resultObject)
	{
		if (result == AssuranceDialogResult.CONFIRM)
		{
			this.existingScanDefinitionsListPanel.validateFormState();
		}
	}

	public void notify(IAssuranceEvent event)
	{
		if (event instanceof ScanStartedEvent)
		{
			this.startScanButton.setEnabled(false);
			this.startScanAndMergeButton.setEnabled(false);
			this.existingScanDefinitionsListPanel.setEnabled(false);
		}

		if (event instanceof ScanCompletedEvent)
		{
			this.existingScanDefinitionsListPanel.setEnabled(true);
		}

		if (event instanceof ScanDefinitionDeletedEvent)
		{
			this.applicationDelegate.loadScanDefinitions();
		}

		if (event instanceof ScanDefinitionSavedEvent)
		{
			this.applicationDelegate.loadScanDefinitions();
		}

		if (event instanceof ScanDefinitionsLoadedEvent)
		{
			this.scanDefinitions = ((ScanDefinitionsLoadedEvent) event).getScanDefinitions();
			this.existingScanDefinitionsListPanel.loadData();
		}
	}

	public void handlePrimaryButtonClick() 
	{
		this.displayDefinitionDialog(null);
	}

	public void handlePrimaryButtonClick(ScanDefinition item) 
	{
		this.displayDefinitionDialog(item);
	}

	public void handleSecondaryButtonClick() 
	{
		this.handleSecondaryButtonClick(null);
	}

	public void handleSecondaryButtonClick(ScanDefinition item) 
	{
		this.deleteScanDefinition(item);
	}

	public boolean listRequiresRecord() 
	{
		return false;
	}

	public String getPrimaryButtonAction() 
	{
		return AssuranceActions.newScanDefinitonAction;
	}

	public String getPrimaryButtonLabel() 
	{
		return "New";
	}

	public String getSecondaryButtonAction() 
	{
		return AssuranceActions.deleteScanDefinitonAction;
	}

	public String getSecondaryButtonLabel() 
	{
		return "Delete";
	}

	public void listValueChanged(boolean itemIsSelected) 
	{
		startScanButton.setEnabled(itemIsSelected);
		startScanAndMergeButton.setEnabled(itemIsSelected);

		applicationDelegate.fireEvent(new SetScanDefinitionMenuStateEvent(new Boolean(itemIsSelected)));
	}

	public Collection<ScanDefinition> getListData() 
	{
		return this.scanDefinitions;
	}
}
