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
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.digitalgeneralists.assurance.IApplicationDelegate;
import com.digitalgeneralists.assurance.model.IListDataProvider;
import com.digitalgeneralists.assurance.model.entities.Scan;
import com.digitalgeneralists.assurance.notification.IEventObserver;
import com.digitalgeneralists.assurance.notification.events.IAssuranceEvent;
import com.digitalgeneralists.assurance.notification.events.ScanCompletedEvent;
import com.digitalgeneralists.assurance.notification.events.ScanDeletedEvent;
import com.digitalgeneralists.assurance.notification.events.ScanResultsLoadedEvent;
import com.digitalgeneralists.assurance.notification.events.ScansLoadedEvent;
import com.digitalgeneralists.assurance.notification.events.SelectedScanChangedEvent;

@Component("ScanHistoryComponent")
public class ScanHistoryPanel extends JPanel implements IEventObserver, IListInputPanelDelegate<Scan>, IListDataProvider<Scan>
{
	private static final long serialVersionUID = 1L;

	@Autowired
	private IApplicationDelegate applicationDelegate;

	private boolean initialized = false;

	private List<Scan> scans;
	
	private ListInputPanel<Scan> existingScansListPanel = null;

	public ScanHistoryPanel()
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
			existingScansPanelConstraints.gridheight = 1;
			existingScansPanelConstraints.gridwidth = 1;
			existingScansPanelConstraints.insets = new Insets(0, 0, 0, 0);

			this.existingScansListPanel = new ListInputPanel<Scan>(this, this, true, true);
			this.add(this.existingScansListPanel, existingScansPanelConstraints);

			this.addAncestorListener(new AncestorListener()
			{
				public void ancestorAdded(AncestorEvent event)
				{
					applicationDelegate.addEventObserver(ScanCompletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScansLoadedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanDeletedEvent.class, (IEventObserver) event.getSource());

					applicationDelegate.loadScans();
				}

				public void ancestorRemoved(AncestorEvent event)
				{
					applicationDelegate.removeEventObserver(ScanCompletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScansLoadedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanDeletedEvent.class, (IEventObserver) event.getSource());
				}

				public void ancestorMoved(AncestorEvent event)
				{
				}
			});

			this.initialized = true;
		}
	}

	public void notify(IAssuranceEvent event)
	{
		if (event instanceof ScanCompletedEvent)
		{
			this.applicationDelegate.loadScans();
		}

		if (event instanceof ScanDeletedEvent)
		{
			this.applicationDelegate.loadScans();
		}

		if (event instanceof ScansLoadedEvent)
		{
			this.scans = ((ScansLoadedEvent) event).getScans();
			this.existingScansListPanel.loadData();
		}
	}

	public Collection<Scan> getListData()
	{
		return this.scans;
	}

	public void handlePrimaryButtonClick()
	{
		this.handlePrimaryButtonClick(null);
	}

	public void handlePrimaryButtonClick(Scan item)
	{
		this.applicationDelegate.deleteScan(this.existingScansListPanel.getSelectedValue());;
	}

	public void handleSecondaryButtonClick() 
	{
		this.handleSecondaryButtonClick(null);
	}

	public void handleSecondaryButtonClick(Scan item)
	{
		this.applicationDelegate.mergeScan(this.existingScansListPanel.getSelectedValue());;
	}

	public boolean listRequiresRecord()
	{
		return false;
	}

	public String getPrimaryButtonAction()
	{
		return AssuranceActions.deleteScanAction;
	}

	public String getPrimaryButtonLabel()
	{
		return "Delete";
	}

	public String getSecondaryButtonAction()
	{
		return AssuranceActions.resolveScanAction;
	}

	public String getSecondaryButtonLabel()
	{
		return "Resolve";
	}

	public void listValueChanged(boolean itemIsSelected)
	{
		this.existingScansListPanel.setActionButtonStates(itemIsSelected);

		// NOTE: Having to pass a value to the event on error isn't ideal.
		applicationDelegate.fireEvent(new SelectedScanChangedEvent(((existingScansListPanel.getSelectedIndex() < 0) || (existingScansListPanel.getSelectedValue() == null)) ? new Scan() : existingScansListPanel.getSelectedValue()));
		
		if (itemIsSelected == true)
		{
			Scan selectedValue = existingScansListPanel.getSelectedValue();
			if (selectedValue != null)
			{
				applicationDelegate.fireEvent(new ScanResultsLoadedEvent(selectedValue));
			}
		}
	}
}
