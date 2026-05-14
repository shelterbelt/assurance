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

package com.markallenjohnson.assurance.ui.renderers;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.IApplicationDelegate;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;
import com.markallenjohnson.assurance.notification.events.SetScanResultsMenuStateEvent;
import com.markallenjohnson.assurance.ui.components.ComparisonResultPanel;

@Component("ComparisonResultListRenderer")
public class ComparisonResultListRenderer extends JPanel implements TableCellRenderer, TableCellEditor, ActionListener
{
	@Autowired
	private IApplicationDelegate applicationDelegate;

	private ComparisonResultPanel panel;

	private ComparisonResult activeResult;

	private static final long serialVersionUID = 1L;

	public ComparisonResultPanel getPanel()
	{
		return panel;
	}

	public void setPanel(ComparisonResultPanel panel)
	{
		this.panel = panel;
		if (this.panel != null)
		{
			this.applicationDelegate.fireEvent(new SetScanResultsMenuStateEvent(true));
		}
		else
		{
			this.applicationDelegate.fireEvent(new SetScanResultsMenuStateEvent(false));
		}
	}

	public java.awt.Component getTableCellRendererComponent(JTable list, Object value, boolean isSelected, boolean cellHasFocus, int row, int column)
	{
		ComparisonResultPanel localPanel = new ComparisonResultPanel((ComparisonResult) value);

		if (isSelected)
		{
			localPanel.setBackground(list.getSelectionBackground());
			localPanel.setForeground(list.getSelectionForeground());
		}
		else
		{
			if ((row % 2.0) == 0)
			{
				localPanel.setBackground(Color.white);
			}
			else
			{
				localPanel.setBackground(Color.lightGray);
			}
		}

		list.setRowHeight(row, ((localPanel.getNumberOfLines() * 18) + 70));

		return localPanel;
	}

	public Object getCellEditorValue()
	{
		return this.activeResult;
	}

	public boolean isCellEditable(EventObject anEvent)
	{
		return true;
	}

	public boolean shouldSelectCell(EventObject anEvent)
	{
		return true;
	}

	public boolean stopCellEditing()
	{
		return true;
	}

	public void cancelCellEditing()
	{
		if (this.getPanel() != null)
		{
			if (!this.getPanel().isMerging())
			{
				if (this.getPanel().getParent() != null)
		{
			this.getPanel().getParent().remove(this.getPanel());
}
			}
		}
	}

	public void addCellEditorListener(CellEditorListener l)
	{
		// No op
	}

	public void removeCellEditorListener(CellEditorListener l)
	{
		// No op
	}

	public java.awt.Component getTableCellEditorComponent(JTable list, Object value, boolean isSelected, int row, int column)
	{
		this.cancelCellEditing();

		this.activeResult = (ComparisonResult) value;

		this.setPanel(new ComparisonResultPanel(this.activeResult, this.applicationDelegate, ((this.activeResult != null) && (this.activeResult.getResolution() == AssuranceResultResolution.UNRESOLVED))));

		if (isSelected)
		{
			// No need to set the background properties here since the editor panel instance
			// will be displayed.
		}
		else
		{
			if ((row % 2.0) == 0)
			{
				this.getPanel().setBackground(Color.white);
			}
			else
			{
				this.getPanel().setBackground(Color.lightGray);
			}
		}

		list.setRowHeight(row, ((this.getPanel().getNumberOfLines() * 18) + 70));

		return this.getPanel();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (this.getPanel() != null)
		{
			this.getPanel().actionPerformed(e);
		}
	}
}
