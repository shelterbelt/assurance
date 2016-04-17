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

package com.digitalgeneralists.assurance.ui.components.dialogs;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;

public class AssuranceDialog extends JDialog implements ActionListener
{
	private boolean initialized = false;

	protected final JButton okButton = new JButton("OK");
	protected final JButton cancelButton = new JButton("Close");

	protected IDialogResponseHandler responseHandler;

	private static final long serialVersionUID = 1L;

	public AssuranceDialog()
	{
		super();
		this.initializeComponent();
	}

	public AssuranceDialog(Dialog owner, boolean modal)
	{
		super(owner, modal);
		this.initializeComponent();
	}

	public AssuranceDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc)
	{
		super(owner, title, modal, gc);
		this.initializeComponent();
	}

	public AssuranceDialog(Dialog owner, String title, boolean modal)
	{
		super(owner, title, modal);
		this.initializeComponent();
	}

	public AssuranceDialog(Dialog owner, String title)
	{
		super(owner, title);
		this.initializeComponent();
	}

	public AssuranceDialog(Dialog owner)
	{
		super(owner);
		this.initializeComponent();
	}

	public AssuranceDialog(Frame owner, boolean modal)
	{
		super(owner, modal);
		this.initializeComponent();
	}

	public AssuranceDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc)
	{
		super(owner, title, modal, gc);
		this.initializeComponent();
	}

	public AssuranceDialog(Frame owner, String title, boolean modal)
	{
		super(owner, title, modal);
		this.initializeComponent();
	}

	public AssuranceDialog(Frame owner, String title)
	{
		super(owner, title);
		this.initializeComponent();
	}

	public AssuranceDialog(Frame owner)
	{
		super(owner);
		this.initializeComponent();
	}

	public AssuranceDialog(Window owner, ModalityType modalityType)
	{
		super(owner, modalityType);
		this.initializeComponent();
	}

	public AssuranceDialog(Window owner, String title, ModalityType modalityType, GraphicsConfiguration gc)
	{
		super(owner, title, modalityType, gc);
		this.initializeComponent();
	}

	public AssuranceDialog(Window owner, String title, ModalityType modalityType)
	{
		super(owner, title, modalityType);
		this.initializeComponent();
	}

	public AssuranceDialog(Window owner, String title, ModalityType modalityType, IDialogResponseHandler responseHandler)
	{
		super(owner, title, modalityType);
		this.responseHandler = responseHandler;
		this.initializeComponent();
	}

	public AssuranceDialog(Window owner, String title)
	{
		super(owner, title);
		this.initializeComponent();
	}

	public AssuranceDialog(Window owner)
	{
		super(owner);
		this.initializeComponent();
	}

	public AssuranceDialogMode getMode()
	{
		AssuranceDialogMode result = AssuranceDialogMode.ADD;

		IDialogInputPanel pane = null;
		if (this.getContentPane() instanceof IDialogInputPanel)
		{
			pane = (IDialogInputPanel) this.getContentPane();

			if (pane != null)
			{
				result = pane.getMode();
			}
		}
		else
		{
			result = AssuranceDialogMode.READ_ONLY;
		}

		return result;
	}

	@Override
	public void setContentPane(Container pane)
	{
		DialogPanel container;
		if (pane instanceof IDialogInputPanel)
		{
			container = new DialogInputPanel(pane);
		}
		else
		{
			container = new DialogPanel(pane);
		}

		GridBagLayout gridbag = new GridBagLayout();
		container.setLayout(gridbag);

		if (pane != null)
		{
			GridBagConstraints contentPaneConstraints = new GridBagConstraints();
			contentPaneConstraints.anchor = GridBagConstraints.WEST;
			contentPaneConstraints.fill = GridBagConstraints.BOTH;
			contentPaneConstraints.gridx = 0;
			contentPaneConstraints.gridy = 0;
			contentPaneConstraints.weightx = 1.0;
			contentPaneConstraints.weighty = 0.75;
			contentPaneConstraints.gridheight = 1;
			contentPaneConstraints.gridwidth = 2;
			contentPaneConstraints.insets = new Insets(0, 0, 0, 0);
			container.add(pane, contentPaneConstraints);
		}

		int cancelButtonGridX = 0;
		if (container.getMode() != AssuranceDialogMode.READ_ONLY)
		{
			GridBagConstraints okButtonConstraints = new GridBagConstraints();
			okButtonConstraints.anchor = GridBagConstraints.WEST;
			okButtonConstraints.fill = GridBagConstraints.BOTH;
			okButtonConstraints.gridx = 0;
			okButtonConstraints.gridy = 1;
			okButtonConstraints.weightx = 1.0;
			okButtonConstraints.weighty = 0.25;
			okButtonConstraints.gridheight = 1;
			okButtonConstraints.gridwidth = 1;
			okButtonConstraints.insets = new Insets(5, 5, 5, 5);
			container.add(this.okButton, okButtonConstraints);

			this.cancelButton.setText("Cancel");
			cancelButtonGridX = 1;
		}

		GridBagConstraints cancelButtonConstraints = new GridBagConstraints();
		cancelButtonConstraints.anchor = GridBagConstraints.EAST;
		cancelButtonConstraints.fill = GridBagConstraints.BOTH;
		cancelButtonConstraints.gridx = cancelButtonGridX;
		cancelButtonConstraints.gridy = 1;
		cancelButtonConstraints.weightx = 1.0;
		cancelButtonConstraints.weighty = 0.25;
		cancelButtonConstraints.gridheight = 1;
		cancelButtonConstraints.gridwidth = 1;
		cancelButtonConstraints.insets = new Insets(5, 5, 5, 5);
		container.add(this.cancelButton, cancelButtonConstraints);

		okButton.addActionListener(this);
		cancelButton.addActionListener(this);

		super.setContentPane(container);
	}

	private void initializeComponent()
	{
		if (!this.initialized)
		{
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent we)
				{
				}
			});

			this.initialized = true;
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		AssuranceDialogResult result = this.processButtonPress(e);
		if (result != AssuranceDialogResult.VALIDATION_FAILED)
		{
			if (this.responseHandler != null)
			{
				Object resultObject = null;
				
				IDialogInputPanel pane = null;
				if (this.getContentPane() instanceof IDialogInputPanel)
				{
					pane = (IDialogInputPanel) this.getContentPane();
				}

				if (pane != null)
				{
					resultObject = pane.getResultObject();
				}

				this.responseHandler.dialogClosed(result, resultObject);
			}
			this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		}
	}

	protected AssuranceDialogResult processButtonPress(ActionEvent e)
	{
		AssuranceDialogResult result = AssuranceDialogResult.CANCEL;

		IDialogInputPanel pane = null;
		if (this.getContentPane() instanceof IDialogInputPanel)
		{
			pane = (IDialogInputPanel) this.getContentPane();
		}

		if (pane != null)
		{
			if (e.getSource() == this.okButton)
			{
				result = pane.processInputOnConfirm();
			}
			else
			{
				result = pane.processInputOnDiscard();
			}
		}

		return result;
	}
}
