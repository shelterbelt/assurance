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

package com.markallenjohnson.assurance.ui.factories;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.JDialog;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.IApplicationDelegate;
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;
import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.entities.ScanMappingDefinition;
import com.markallenjohnson.assurance.ui.components.AboutPanel;
import com.markallenjohnson.assurance.ui.components.ExclusionsPanel;
import com.markallenjohnson.assurance.ui.components.FileAttributesPanel;
import com.markallenjohnson.assurance.ui.components.ScanDefinitionPanel;
import com.markallenjohnson.assurance.ui.components.ScanPathMappingPanel;
import com.markallenjohnson.assurance.ui.components.SettingsPanel;
import com.markallenjohnson.assurance.ui.components.dialogs.AssuranceDialog;
import com.markallenjohnson.assurance.ui.components.dialogs.IDialogPanel;
import com.markallenjohnson.assurance.ui.components.dialogs.IDialogResponseHandler;

@Component("DialogFactory")
public class DialogFactory implements IDialogFactory, BeanFactoryAware
{
	@Autowired
	private IApplicationDelegate applicationDelegate;

	private BeanFactory factory;

	private static JDialog createDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, IDialogPanel contentPane, Dimension size)
	{
		if (size == null)
		{
			size = new Dimension((parent.getSize().width / 2), (parent.getSize().height / 2));
		}
		AssuranceDialog dialog = new AssuranceDialog(parent, contentPane.getDialogTitle(), Dialog.ModalityType.DOCUMENT_MODAL, responseHandler);
		dialog.setResizable(false);
		dialog.setSize(size);
		dialog.setLocationRelativeTo(parent);
		if (contentPane instanceof Container)
		{
			dialog.setContentPane((Container) contentPane);
		}

		return dialog;
	}

	public JDialog createScanDefinitionDialogInstance(Window parent, ModalityType modality, ScanDefinition scanDefinition)
	{
		return this.createScanDefinitionDialogInstance(parent, modality, null, scanDefinition);
	}

	public JDialog createScanDefinitionDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, ScanDefinition scanDefinition)
	{
		ScanDefinitionPanel scanDefinitionPanel = this.factory.getBean(ScanDefinitionPanel.class);

		scanDefinitionPanel.setDefinition(scanDefinition);
		// NOTE:  Cascading the applicationDeleage into the panel instance like this is far from ideal.  
		// Feel I have a problem with my Spring hierarchy because of it.
		scanDefinitionPanel.setApplicationDelegate(this.applicationDelegate);

		JDialog scanDefinitionDialog = createDialogInstance(parent, modality, responseHandler, scanDefinitionPanel, new Dimension(400, 500));
		return scanDefinitionDialog;
	}

	public JDialog createFileAttributesDialogInstance(Window parent, ModalityType modality, FileReference file)
	{
		return this.createFileAttributesDialogInstance(parent, modality, null, file);
	}

	public JDialog createFileAttributesDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, FileReference file)
	{
		FileAttributesPanel fileAttributesPanel = this.factory.getBean(FileAttributesPanel.class);

		fileAttributesPanel.setFileReference(file);

		JDialog scanDefinitionDialog = createDialogInstance(parent, modality, responseHandler, fileAttributesPanel, new Dimension(600, 600));
		scanDefinitionDialog.setResizable(true);
		return scanDefinitionDialog;
	}

	public JDialog createAboutDialogInstance(Window parent, ModalityType modality)
	{
		return this.createAboutDialogInstance(parent, modality, null);
	}

	public JDialog createAboutDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler)
	{
		AboutPanel aboutApplicationPanel = this.factory.getBean(AboutPanel.class);

		JDialog aboutApplicationDialog = createDialogInstance(parent, modality, responseHandler, aboutApplicationPanel, new Dimension(300, 200));
		aboutApplicationDialog.setResizable(false);
		return aboutApplicationDialog;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException
	{
		this.factory = beanFactory;
	}

	public JDialog createScanMappingDefinitionDialogInstance(Window parent, ModalityType modality, ScanMappingDefinition mappingDefinition) 
	{
		return this.createScanMappingDefinitionDialogInstance(parent, modality, null, mappingDefinition);
	}

	public JDialog createScanMappingDefinitionDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, ScanMappingDefinition mappingDefinition) 
	{
		ScanPathMappingPanel scanMappingDefinitionPanel = this.factory.getBean(ScanPathMappingPanel.class);

		scanMappingDefinitionPanel.setMapping(mappingDefinition);

		JDialog scanDefinitionDialog = createDialogInstance(parent, modality, responseHandler, scanMappingDefinitionPanel, new Dimension(400, 400));
		return scanDefinitionDialog;
	}
	
	public JDialog createExclusionDialogInstance(Window parent, ModalityType modality, FileReference exclusion)
	{
		return this.createExclusionDialogInstance(parent, modality, null, exclusion);
	}

	public JDialog createExclusionDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, FileReference exclusion)
	{
		ExclusionsPanel exclusionsPanel = this.factory.getBean(ExclusionsPanel.class);

		exclusionsPanel.setExclusion(exclusion);

		JDialog exclusionsDialog = createDialogInstance(parent, modality, responseHandler, exclusionsPanel, new Dimension(400, 135));
		return exclusionsDialog;
	}

	public JDialog createSettingsDialogInstance(Window parent, ModalityType modality, ApplicationConfiguration configuration)
	{
		return this.createSettingsDialogInstance(parent, modality, null, configuration);
	}
	
	public JDialog createSettingsDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, ApplicationConfiguration configuration)
	{
		SettingsPanel settingsPanel = this.factory.getBean(SettingsPanel.class);

		settingsPanel.setConfiguration(configuration);

		JDialog settingsDialog = createDialogInstance(parent, modality, responseHandler, settingsPanel, new Dimension(400, 250));
		return settingsDialog;
	}
}
