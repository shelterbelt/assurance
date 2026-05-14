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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.markallenjohnson.assurance.model.IInitializableEntity;
import com.markallenjohnson.assurance.model.IListDataProvider;
import com.markallenjohnson.assurance.model.ModelUtils;

// NOTE:  This implementation isn't as scalable as it could be.  The notion of applying behavior
// to the button controls specifically won't scale well.
public class ListInputPanel<T> extends AbstractInputPanel implements ActionListener 
{
	private static final long serialVersionUID = 1L;

	private int selectedIndex = 0;
	
	private final DefaultListModel<T> dataListModel = new DefaultListModel<>();
	private final JList<T> existingDataList = new JList<>(this.dataListModel);
	private final JButton primaryButton = new JButton("New");
	private final JButton secondaryButton = new JButton("Delete");
	
	private final IListInputPanelDelegate<T> delegate;
	private IListDataProvider<T> dataProvider;
	
	private boolean primaryButtonRequiresSelection;
	private boolean secondaryButtonRequiresSelection;
	
	public ListInputPanel(IListDataProvider<T> dataProvider)
	{
		this(dataProvider, null, false, true);
	}
	
	public ListInputPanel(IListDataProvider<T> dataProvider, IListInputPanelDelegate<T> delegate)
	{
		this(dataProvider, delegate, false, true);
	}
	
	public ListInputPanel(IListDataProvider<T> dataProvider, IListInputPanelDelegate<T> delegate, boolean primaryRequiresSelection, boolean secondaryRequiresSelection)
	{
		this.dataProvider = dataProvider;
		this.delegate = delegate;
		if (this.delegate != null)
		{
			this.primaryButton.setText(this.delegate.getPrimaryButtonLabel());
			this.primaryButton.setActionCommand(this.delegate.getPrimaryButtonAction());
			this.secondaryButton.setText(this.delegate.getSecondaryButtonLabel());
			this.secondaryButton.setActionCommand(this.delegate.getSecondaryButtonAction());
		}
		this.primaryButtonRequiresSelection = primaryRequiresSelection;
		this.secondaryButtonRequiresSelection = secondaryRequiresSelection;

		this.initializeComponent();
	}
	
	protected void initializeComponent()
	{
		if (!this.initialized)
		{
			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);

			GridBagConstraints existingDataListConstraints = new GridBagConstraints();
			existingDataListConstraints.anchor = GridBagConstraints.WEST;
			existingDataListConstraints.fill = GridBagConstraints.BOTH;
			existingDataListConstraints.gridx = 0;
			existingDataListConstraints.gridy = 0;
			existingDataListConstraints.weightx = 1.0;
			existingDataListConstraints.weighty = 0.9;
			existingDataListConstraints.gridheight = 1;
			existingDataListConstraints.gridwidth = 2;
			existingDataListConstraints.insets = new Insets(0, 0, 0, 0);

			this.existingDataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.existingDataList.setLayoutOrientation(JList.VERTICAL);
			JScrollPane existingDataScrollPanel = new JScrollPane(this.existingDataList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			this.add(existingDataScrollPanel, existingDataListConstraints);

			GridBagConstraints primaryButtonConstraints = new GridBagConstraints();
			primaryButtonConstraints.anchor = GridBagConstraints.NORTHEAST;
			primaryButtonConstraints.fill = GridBagConstraints.BOTH;
			primaryButtonConstraints.gridx = 0;
			primaryButtonConstraints.gridy = 1;
			primaryButtonConstraints.weightx = 1.0;
			primaryButtonConstraints.weighty = 0.1;
			primaryButtonConstraints.insets = new Insets(0, 0, 0, 0);

			if (this.delegate != null)
			{
				this.primaryButton.setActionCommand(delegate.getPrimaryButtonAction());
			}

			this.add(this.primaryButton, primaryButtonConstraints);

			GridBagConstraints secondaryButtonConstraints = new GridBagConstraints();
			secondaryButtonConstraints.anchor = GridBagConstraints.NORTHEAST;
			secondaryButtonConstraints.fill = GridBagConstraints.BOTH;
			secondaryButtonConstraints.gridx = 1;
			secondaryButtonConstraints.gridy = 1;
			secondaryButtonConstraints.weightx = 1.0;
			secondaryButtonConstraints.weighty = 0.1;
			secondaryButtonConstraints.insets = new Insets(0, 0, 0, 0);

			if (this.delegate != null)
			{
				this.secondaryButton.setActionCommand(delegate.getSecondaryButtonAction());
			}

			this.add(this.secondaryButton, secondaryButtonConstraints);

			this.primaryButton.addActionListener(this);
			this.secondaryButton.addActionListener(this);

			if (this.primaryButtonRequiresSelection)
			{
				this.primaryButton.setEnabled(false);
			}
			if (this.secondaryButtonRequiresSelection)
			{
				this.secondaryButton.setEnabled(false);
			}
			
			if (this.delegate == null)
			{
				this.primaryButton.setEnabled(false);
				this.secondaryButton.setEnabled(false);
			}

			MouseListener mouseListener = new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						T selectedItem = existingDataList.getSelectedValue();
						if (delegate != null)
						{
							delegate.handlePrimaryButtonClick(selectedItem);
						}
					}
				}
			};
			this.existingDataList.addMouseListener(mouseListener);

			this.existingDataList.addListSelectionListener(event -> {
				if (!event.getValueIsAdjusting())
				{
					if (delegate != null)
					{
						@SuppressWarnings("unchecked")
						JList<T> source = (JList<T>) event.getSource();
						selectedIndex = source.getSelectedIndex();
						if (selectedIndex < 0)
						{
							if (primaryButtonRequiresSelection)
							{
								primaryButton.setEnabled(false);
							}
							if (secondaryButtonRequiresSelection)
							{
								secondaryButton.setEnabled(false);
							}
							if (delegate != null)
							{
								delegate.listValueChanged(false);
							}
						}
						else
						{
							if (primaryButtonRequiresSelection)
							{
								primaryButton.setEnabled(true);
							}
							if (secondaryButtonRequiresSelection)
							{
								secondaryButton.setEnabled(true);
							}
							if (delegate != null)
							{
								delegate.listValueChanged(true);
							}
						}
					}
				}
			});

			ListDataListener listDataListener = new ListDataListener()
			{
				public void contentsChanged(ListDataEvent listDataEvent)
				{
					applySelection(listDataEvent);
				}

				public void intervalAdded(ListDataEvent listDataEvent)
				{
					applySelection(listDataEvent);
				}

				public void intervalRemoved(ListDataEvent listDataEvent)
				{
					applySelection(listDataEvent);
				}

				private void applySelection(ListDataEvent listDataEvent)
				{
					existingDataList.setSelectedIndex(selectedIndex);
				}
			};
			this.dataListModel.addListDataListener(listDataListener);
			
			this.loadData();

			this.initialized = true;
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		String primaryAction = this.primaryButton.getActionCommand();
		if (primaryAction != null)
		{
			if (primaryAction.equals(e.getActionCommand()))
			{
				if (this.delegate != null)
				{
					if (this.primaryButtonRequiresSelection)
					{
						this.delegate.handlePrimaryButtonClick(existingDataList.getSelectedValue());
					}
					else
					{
						this.delegate.handlePrimaryButtonClick();
					}
				}
			}
		}
		String secondaryAction = this.secondaryButton.getActionCommand();
		if (secondaryAction != null)
		{
			if (secondaryAction.equals(e.getActionCommand()))
			{
				if (this.delegate != null)
				{
					if (this.secondaryButtonRequiresSelection)
					{
						this.delegate.handleSecondaryButtonClick(existingDataList.getSelectedValue());
					}
					else
					{
						this.delegate.handleSecondaryButtonClick();
					}
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadData()
	{
		int localSelectedIndex = existingDataList.getSelectedIndex();
		
		// NOTE:  Swapping the list model out to suppress change
		// notifications during a data reload feels less than ideal.
		this.existingDataList.setModel(new DefaultListModel<>());

		Collection<T> list = null;
		this.dataListModel.removeAllElements();
		
		if (this.dataProvider != null)
		{
			if (this.dataProvider instanceof IInitializableEntity)
			{
				// NOTE:  Leaking model initialization like this into the UI is less than ideal.
				this.dataProvider = (IListDataProvider<T>) ModelUtils.initializeEntity((IInitializableEntity)this.dataProvider, ((IInitializableEntity)this.dataProvider).getInitializationPropertyName());
			}
			list = this.dataProvider.getListData();
		}

		if (list != null)
		{
			for (T exclusion : list)
			{
				if (!this.dataListModel.contains(exclusion))
				{
					if (localSelectedIndex < 0)
					{
						localSelectedIndex = 0;
					}
					this.dataListModel.addElement(exclusion);
				}
			}
			
			this.existingDataList.setModel(this.dataListModel);

			if ((localSelectedIndex >= 0) && (this.dataListModel.getSize() > localSelectedIndex))
			{
				this.existingDataList.setSelectedIndex(localSelectedIndex);
			}
		}
	}

	@Override
	public boolean validateFormState() 
	{
		boolean result = true;
		
		if (this.delegate != null)
		{
			if (this.delegate.listRequiresRecord())
			{
				if (this.dataListModel.isEmpty())
				{
					this.existingDataList.setBackground(this.controlInErrorBackgroundColor);
					result = false;
				}
				else
				{
					this.existingDataList.setBackground(this.defaultControlBackgroundColor);
				}
			}
		}
		
		return result;
	}
	
	public void setActionButtonStates(boolean state)
	{
		this.primaryButton.setEnabled(state);
		this.secondaryButton.setEnabled(state);
	}
	
	public int getSelectedIndex ()
	{
		return this.existingDataList.getSelectedIndex();
	}
	
	public T getSelectedValue ()
	{
		if (!this.dataListModel.isEmpty())
		{
			return this.existingDataList.getSelectedValue();
		}
		
		return null;
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		this.primaryButton.setEnabled(enabled);
		this.secondaryButton.setEnabled(enabled);
		this.existingDataList.setEnabled(enabled);
		this.existingDataList.setSelectedIndex(-1);
		this.existingDataList.setSelectedIndex(this.selectedIndex);
	}
}
