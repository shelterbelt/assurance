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

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.digitalgeneralists.assurance.model.entities.FileAttributes;
import com.digitalgeneralists.assurance.model.entities.FileReference;
import com.digitalgeneralists.assurance.ui.components.dialogs.AssuranceDialogMode;
import com.digitalgeneralists.assurance.ui.components.dialogs.AssuranceDialogResult;
import com.digitalgeneralists.assurance.ui.components.dialogs.IDialogInputPanel;

@Component("FileAttributesComponent")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FileAttributesPanel extends JPanel implements IDialogInputPanel
{
	private static final long serialVersionUID = 1L;

	private boolean initialized = false;

	private String dialogTitle;

	private FileReference file;

	public FileAttributesPanel()
	{
	}

	public AssuranceDialogMode getMode()
	{
		return AssuranceDialogMode.READ_ONLY;
	}

	public String getDialogTitle()
	{
		return this.dialogTitle;
	}

	public void setDialogTitle(String title)
	{
		this.dialogTitle = title;
	}

	public void setFileReference(FileReference file)
	{
		this.file = file;

		this.initializeComponent();
	}

	private void initializeComponent()
	{
		if (!this.initialized)
		{
			if (this.file == null)
			{
				this.dialogTitle = "No File Provided";
				this.file = null;
			}
			else
			{
				StringBuilder title = new StringBuilder(128);
				this.dialogTitle = title.append("Attributes for ").append(file.getFile().getName()).toString();
				title.setLength(0);
				title = null;
			}

			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);

			GridBagConstraints filePanelConstraints = new GridBagConstraints();
			filePanelConstraints.anchor = GridBagConstraints.NORTH;
			filePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
			filePanelConstraints.gridx = 0;
			filePanelConstraints.gridy = 0;
			filePanelConstraints.weightx = 1.0;
			filePanelConstraints.weighty = 0.1;
			filePanelConstraints.gridheight = 1;
			filePanelConstraints.gridwidth = 1;
			filePanelConstraints.insets = new Insets(5, 5, 5, 5);

			final JPanel filePanel = new JPanel();
			filePanel.setLayout(new GridBagLayout());

			GridBagConstraints filePathValueConstraints = new GridBagConstraints();
			filePathValueConstraints.anchor = GridBagConstraints.WEST;
			filePathValueConstraints.gridx = 0;
			filePathValueConstraints.gridy = 0;
			filePathValueConstraints.weightx = 1.0;
			filePathValueConstraints.weighty = 1.0;
			filePathValueConstraints.gridheight = 1;
			filePathValueConstraints.gridwidth = 1;
			filePathValueConstraints.insets = new Insets(5, 5, 5, 5);

			String filePath = "File is null.";
			if (file != null)
			{
				File diskFile = file.getFile();
				if (diskFile == null)
				{
					filePath = "The disk file is not set.";
				}
				else
				{
					filePath = diskFile.getPath();
				}
				diskFile = null;
			}
			JLabel filePathValue = new JLabel(filePath);
			filePath = null;
			filePanel.add(filePathValue, filePathValueConstraints);

			this.add(filePanel, filePanelConstraints);

			Border attributesBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
			attributesBorder = BorderFactory.createTitledBorder(attributesBorder, "File Attributes", TitledBorder.CENTER, TitledBorder.TOP);

			GridBagConstraints attributesPanelConstraints = new GridBagConstraints();
			attributesPanelConstraints.anchor = GridBagConstraints.SOUTH;
			attributesPanelConstraints.fill = GridBagConstraints.BOTH;
			attributesPanelConstraints.gridx = 0;
			attributesPanelConstraints.gridy = 1;
			attributesPanelConstraints.weightx = 1.0;
			attributesPanelConstraints.weighty = 0.9;
			attributesPanelConstraints.gridheight = 1;
			attributesPanelConstraints.gridwidth = 1;
			attributesPanelConstraints.insets = new Insets(0, 5, 5, 5);

			JPanel attributesPanel = this.createFileAttributesPanel(file);

			attributesPanel.setBorder(attributesBorder);

			this.add(attributesPanel, attributesPanelConstraints);

			this.initialized = true;
		}
	}

	private JPanel createFileAttributesPanel(FileReference fileReference)
	{
		JPanel attributePanel = new JPanel();
		GridBagLayout attributePanelGridbag = new GridBagLayout();
		attributePanel.setOpaque(false);
		attributePanel.setLayout(attributePanelGridbag);
		int anchor = GridBagConstraints.WEST;

		if (fileReference != null)
		{
			int index = 0;

			FileAttributes attributes = fileReference.getFileAttributes();

			this.addFileAttributeToPanel(attributePanel, anchor, index, "Content Hash: ", (attributes.getContentsHash() != null) ? attributes.getContentsHash().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Creation Time: ", (attributes.getCreationTime() != null) ? attributes.getCreationTime().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Directory: ", (attributes.getIsDirectory() != null) ? attributes.getIsDirectory().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Other: ", (attributes.getIsOther() != null) ? attributes.getIsOther().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Regular File: ", (attributes.getIsRegularFile() != null) ? attributes.getIsRegularFile().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Symbolic Link: ", (attributes.getIsSymbolicLink() != null) ? attributes.getIsSymbolicLink().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Last Access Time: ", (attributes.getLastAccessTime() != null) ? attributes.getLastAccessTime().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Last Modified Time: ", (attributes.getLastModifiedTime() != null) ? attributes.getLastModifiedTime().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "File Size: ", (attributes.getSize() != null) ? attributes.getSize().toString() : "");
			index++;
			// DOS Attributes
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Archive: ", (attributes.getIsArchive() != null) ? attributes.getIsArchive().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Hidden: ", (attributes.getIsHidden() != null) ? attributes.getIsHidden().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Read Only: ", (attributes.getIsReadOnly() != null) ? attributes.getIsReadOnly().toString() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "System File: ", (attributes.getIsSystem() != null) ? attributes.getIsSystem().toString() : "");
			index++;
			// POSIX Attributes
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Group Name: ", (attributes.getGroupName() != null) ? attributes.getGroupName() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Owner: ", (attributes.getOwner() != null) ? attributes.getOwner() : "");
			index++;
			this.addFileAttributeToPanel(attributePanel, anchor, index, "Permissions: ", (attributes.getPermissions() != null) ? attributes.getPermissions() : "");
			index++;
			// File-owner Attributes
			this.addFileAttributeToPanel(attributePanel, anchor, index, "File Owner: ", (attributes.getFileOwner() != null) ? attributes.getFileOwner() : "");
			index++;
			// ACL Attributes
			this.addFileAttributeToPanel(attributePanel, anchor, index, "ACLs: ", (attributes.getAclDescription() != null) ? attributes.getAclDescription() : "");
			index++;
			// User-defined Attributes
			this.addFileAttributeToPanel(attributePanel, anchor, index, "User-defined Attributes Hash: ", (attributes.getUserDefinedAttributesHash() != null) ? attributes.getUserDefinedAttributesHash() : "");
			
			attributes = null;
		}

		return attributePanel;
	}
	
	private void addFileAttributeToPanel(Container panel, int anchor, int index, String label, String value)
	{
		Font attributeFont = new Font(this.getFont().getName(), Font.BOLD, 8);

		GridBagConstraints attributeLabelConstraints = new GridBagConstraints();
		attributeLabelConstraints.anchor = anchor;
		attributeLabelConstraints.gridx = 0;
		attributeLabelConstraints.gridy = index;
		attributeLabelConstraints.weightx = 1.0;
		attributeLabelConstraints.weighty = 1.0;
		attributeLabelConstraints.gridheight = 1;
		attributeLabelConstraints.gridwidth = 1;
		attributeLabelConstraints.insets = new Insets(5, 5, 5, 5);

		JLabel attributeLabel = new JLabel(label);
		attributeLabel.setForeground(Color.gray);
		attributeLabel.setFont(attributeFont);
		panel.add(attributeLabel, attributeLabelConstraints);

		GridBagConstraints attributeValueConstraints = new GridBagConstraints();
		attributeValueConstraints.anchor = anchor;
		attributeValueConstraints.gridx = 1;
		attributeValueConstraints.gridy = index;
		attributeValueConstraints.weightx = 1.0;
		attributeValueConstraints.weighty = 1.0;
		attributeValueConstraints.gridheight = 1;
		attributeValueConstraints.gridwidth = 1;
		attributeValueConstraints.insets = new Insets(5, 5, 5, 5);

		JLabel attributeValue = new JLabel(value);
		attributeValue.setForeground(Color.gray);
		attributeValue.setFont(attributeFont);
		panel.add(attributeValue, attributeValueConstraints);
	}

	public AssuranceDialogResult processInputOnConfirm()
	{
		return AssuranceDialogResult.CONFIRM;
	}

	public AssuranceDialogResult processInputOnDiscard()
	{
		return AssuranceDialogResult.CANCEL;
	}

	public Object getResultObject() 
	{
		return this.file;
	}
}
