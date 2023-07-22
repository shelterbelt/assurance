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
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.IApplicationDelegate;
import com.markallenjohnson.assurance.model.ModelUtils;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.notification.IEventObserver;
import com.markallenjohnson.assurance.notification.events.ComparisonResultAddedEvent;
import com.markallenjohnson.assurance.notification.events.DeletedItemRestoreCompletedEvent;
import com.markallenjohnson.assurance.notification.events.IAssuranceEvent;
import com.markallenjohnson.assurance.notification.events.ResultMergeCompletedEvent;
import com.markallenjohnson.assurance.notification.events.ScanCompletedEvent;
import com.markallenjohnson.assurance.notification.events.ScanMergeCompletedEvent;
import com.markallenjohnson.assurance.notification.events.ScanMergeProgressEvent;
import com.markallenjohnson.assurance.notification.events.ScanMergeStartedEvent;
import com.markallenjohnson.assurance.notification.events.ScanProgressEvent;
import com.markallenjohnson.assurance.notification.events.ScanResultsLoadedEvent;
import com.markallenjohnson.assurance.notification.events.ScanStartedEvent;
import com.markallenjohnson.assurance.notification.events.SelectedScanChangedEvent;
import com.markallenjohnson.assurance.notification.events.SetScanResultsMenuStateEvent;
import com.markallenjohnson.assurance.ui.renderers.ComparisonResultListRenderer;

@Component("ResultsPanel")
public class ResultsPanel extends JPanel implements IEventObserver
{
	private Logger logger = Logger.getLogger(ResultsPanel.class);

	@Autowired
	private ComparisonResultListRenderer comparisonResultListRenderer;

	private static final long serialVersionUID = 1L;

	private boolean initialized = false;

	@Autowired
	private ResultsMessagePanel messagePanel;

	@Autowired
	private IApplicationDelegate applicationDelegate;

	private final ResultsTableModel resultsTableModel = new ResultsTableModel();
	private final JTable resultsTable = new JTable(resultsTableModel);
	private final JScrollPane resultsScrollPane = new JScrollPane(this.resultsTable);

	private GridBagConstraints progressIndicatorConstraints = new GridBagConstraints();
	private GridBagConstraints resultsListConstraints = new GridBagConstraints();

	private final JLabel resultsLabel = new JLabel(" ");

	private final JProgressBar progressIndicator = new JProgressBar(0, 100);

	public ResultsPanel()
	{
		this.initializeComponent();
	}

	private void initializeComponent()
	{
		if (!this.initialized)
		{
			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);

			((DefaultTableCellRenderer) resultsTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
			this.resultsTable.setRowHeight(150);

			ListSelectionModel selectionModel = this.resultsTable.getSelectionModel();

			selectionModel.addListSelectionListener(new ListSelectionListener()
			{
				public void valueChanged(ListSelectionEvent e)
				{
					applicationDelegate.fireEvent(new SetScanResultsMenuStateEvent(false));
					resultsTable.editCellAt(resultsTable.getSelectedRow(), 0);
				}
			});

			GridBagConstraints resultsLabelConstraints = new GridBagConstraints();
			resultsLabelConstraints.anchor = GridBagConstraints.WEST;
			resultsLabelConstraints.fill = GridBagConstraints.BOTH;
			resultsLabelConstraints.gridx = 0;
			resultsLabelConstraints.gridy = 0;
			resultsLabelConstraints.weightx = 1.0;
			resultsLabelConstraints.weighty = 0.1;
			resultsLabelConstraints.gridheight = 1;
			resultsLabelConstraints.gridwidth = 1;
			resultsLabelConstraints.insets = new Insets(5, 5, 5, 5);

			this.progressIndicatorConstraints.anchor = GridBagConstraints.WEST;
			this.progressIndicatorConstraints.fill = GridBagConstraints.BOTH;
			this.progressIndicatorConstraints.gridx = 0;
			this.progressIndicatorConstraints.gridy = 1;
			this.progressIndicatorConstraints.weightx = 1.0;
			this.progressIndicatorConstraints.weighty = 0.1;
			this.progressIndicatorConstraints.gridheight = 1;
			this.progressIndicatorConstraints.gridwidth = 1;
			this.progressIndicatorConstraints.insets = new Insets(5, 5, 5, 5);

			this.progressIndicator.setIndeterminate(true);

			this.resultsListConstraints.anchor = GridBagConstraints.WEST;
			this.resultsListConstraints.fill = GridBagConstraints.BOTH;
			this.resultsListConstraints.gridx = 0;
			this.resultsListConstraints.gridy = 2;
			this.resultsListConstraints.weightx = 1.0;
			this.resultsListConstraints.weighty = 0.9;
			this.resultsListConstraints.gridheight = 1;
			this.resultsListConstraints.gridwidth = 1;
			this.resultsListConstraints.insets = new Insets(5, 5, 5, 5);

			this.add(this.resultsLabel, resultsLabelConstraints);
			this.add(this.resultsScrollPane, this.resultsListConstraints);

			this.addAncestorListener(new AncestorListener()
			{
				public void ancestorAdded(AncestorEvent event)
				{
					resultsTable.setDefaultRenderer(ComparisonResult.class, comparisonResultListRenderer);
					resultsTable.setDefaultEditor(ComparisonResult.class, comparisonResultListRenderer);

					applicationDelegate.addEventObserver(ScanStartedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ComparisonResultAddedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanResultsLoadedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(SelectedScanChangedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanCompletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanProgressEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ResultMergeCompletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanMergeStartedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanMergeProgressEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(ScanMergeCompletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.addEventObserver(DeletedItemRestoreCompletedEvent.class, (IEventObserver) event.getSource());
				}

				public void ancestorRemoved(AncestorEvent event)
				{
					applicationDelegate.removeEventObserver(ScanStartedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ComparisonResultAddedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanResultsLoadedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(SelectedScanChangedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanCompletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanProgressEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ResultMergeCompletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanMergeStartedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanMergeProgressEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(ScanMergeCompletedEvent.class, (IEventObserver) event.getSource());
					applicationDelegate.removeEventObserver(DeletedItemRestoreCompletedEvent.class, (IEventObserver) event.getSource());
				}

				public void ancestorMoved(AncestorEvent event)
				{
				}
			});

			this.initialized = true;
		}
	}

	public void resetPanel()
	{
		this.resultsTableModel.removeAllElements();
		this.conditionallyShowMessagePanel(false, "");
		this.resultsLabel.setText(" ");
	}

	private void conditionallyShowMessagePanel(boolean showMessagePanel, String message)
	{
		this.messagePanel.setMessage("");
		if (showMessagePanel)
		{
			this.messagePanel.setMessage(message);
			this.remove(this.resultsScrollPane);
			this.add(this.messagePanel, resultsListConstraints);
			this.validate();
			this.messagePanel.repaint();
		}
		else
		{
			this.add(this.resultsScrollPane, resultsListConstraints);
			this.remove(this.messagePanel);
			this.validate();
			this.resultsScrollPane.repaint();
		}
	}

	public void notify(IAssuranceEvent event)
	{
		StringBuffer loggingMessage = new StringBuffer(256);
		logger.info(loggingMessage.append("ResultsPanel received event: ").append(event));
		loggingMessage.setLength(0);
		loggingMessage = null;

		if (event instanceof SelectedScanChangedEvent)
		{
			this.resetPanel();
			StringBuilder labelText = new StringBuilder(512);
			this.resultsLabel.setText(labelText.append("Results for ").append(event.getSource().toString()).toString());
			labelText.setLength(0);
			labelText = null;
		}

		if (event instanceof ScanStartedEvent)
		{
			this.resetPanel();
			StringBuilder labelText = new StringBuilder(512);
			this.resultsLabel.setText(labelText.append("Loading results for ").append(event.getSource().toString()).toString());
			labelText.setLength(0);
			labelText = null;
			this.add(this.progressIndicator, this.progressIndicatorConstraints);
			this.validate();
			this.progressIndicator.repaint();
		}

		if (event instanceof ScanCompletedEvent)
		{
			this.applicationDelegate.loadScanResults((Scan) event.getSource());
			this.remove(this.progressIndicator);
			this.validate();
		}

		if (event instanceof ScanProgressEvent)
		{
			this.resultsLabel.setText(event.getSource().toString());
			this.validate();
		}

		if (event instanceof ScanResultsLoadedEvent)
		{
			int selectedResultIndex = this.resultsTable.getSelectedRow();

			Scan scan = (Scan) event.getSource();
			// NOTE:  Leaking model initialization like this into the UI is less than ideal.
			scan = (Scan) ModelUtils.initializeEntity(scan, Scan.RESULTS_PROPERTY);
			Collection<ComparisonResult> list = scan.getUnmodifiableResults();

			// NOTE:  Swapping the list model out to suppress change
			// notifications during a data reload feels less than ideal.
			this.resultsTable.setModel(new DefaultTableModel());

			this.resultsTableModel.removeAllElements();

			for (ComparisonResult result : list)
			{
				this.resultsTableModel.addRow(result);
				result = null;
			}

			this.resultsTable.setModel(this.resultsTableModel);

			this.conditionallyShowMessagePanel(list.size() == 0, "The source and target locations are identical.");
			list = null;

			StringBuilder labelText = new StringBuilder(512);
			this.resultsLabel.setText(labelText.append("Results for ").append(scan.toString()).toString());
			labelText.setLength(0);
			labelText = null;

			if ((selectedResultIndex >= 0) && (this.resultsTableModel.getRowCount() > selectedResultIndex))
			{
				this.resultsTable.setRowSelectionInterval(selectedResultIndex, selectedResultIndex);
			}
			
			scan = null;
		}

		if ((event instanceof ResultMergeCompletedEvent) || (event instanceof DeletedItemRestoreCompletedEvent))
		{
			for (int index = 0; index < this.resultsTableModel.getRowCount(); index++)
			{
				List<List<ComparisonResult>> resultList = new ArrayList<List<ComparisonResult>>(this.resultsTableModel.data);
				if (((ComparisonResult) event.getSource()).getId() == resultList.get(index).get(0).getId())
				{
					this.resultsTableModel.updateRow(index, (ComparisonResult) event.getSource());
				}
				resultList = null;
			}
		}

		if (event instanceof ScanMergeStartedEvent)
		{
			this.resetPanel();
			StringBuilder labelText = new StringBuilder(512);
			this.resultsLabel.setText(labelText.append("Merging ").append(event.getSource().toString()).toString());
			labelText.setLength(0);
			labelText = null;
			this.add(this.progressIndicator, this.progressIndicatorConstraints);
			this.validate();
			this.progressIndicator.repaint();
			StringBuilder message = new StringBuilder(512);
			this.conditionallyShowMessagePanel(true, message.append("Merging ").append(event.getSource().toString()).toString());
			message.setLength(0);
			message = null;
		}

		if (event instanceof ScanMergeProgressEvent)
		{
			this.resultsLabel.setText(event.getSource().toString());
			this.validate();
		}

		if (event instanceof ScanMergeCompletedEvent)
		{
			this.applicationDelegate.loadScanResults((Scan) event.getSource());
			this.remove(this.progressIndicator);
			this.validate();
		}
	}

	public ActionListener getResultMenuListener()
	{
		return this.comparisonResultListRenderer;
	}

	class ResultsTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;

		private String[] columnNames = { "Results" };
		private List<List<ComparisonResult>> data = new ArrayList<List<ComparisonResult>>();

		public int getRowCount()
		{
			return data.size();
		}

		public int getColumnCount()
		{
			return 1;
		}

		public String getColumnName(int columnIndex)
		{
			return columnNames[columnIndex];
		}

		public Class<? extends Object> getColumnClass(int columnIndex)
		{
			return ComparisonResult.class;
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			if (data.size() == 0)
			{
				return null;
			}
			List<ComparisonResult> row = data.get(rowIndex);

			ComparisonResult result = row.get(0);
			row = null;
			
			return result;
		}

		public void setValueAt(Object value, int rowIndex, int columnIndex)
		{
			if (data.size() <= rowIndex)
			{
				List<ComparisonResult> row = new ArrayList<ComparisonResult>();
				row.add((ComparisonResult) value);
				data.add(row);
				row = null;
			}
			else
			{
				List<ComparisonResult> row = data.get(rowIndex);
				row.set(columnIndex, (ComparisonResult) value);
				row = null;
			}

			fireTableCellUpdated(rowIndex, columnIndex);
		}

		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return true;
		}

		public void removeAllElements()
		{
			data.clear();
		}

		public void addRow(ComparisonResult result)
		{
			this.setValueAt(result, data.size(), 0);
		}

		public void updateRow(int rowIndex, ComparisonResult result)
		{
			this.setValueAt(result, rowIndex, 0);
		}
	}
}
