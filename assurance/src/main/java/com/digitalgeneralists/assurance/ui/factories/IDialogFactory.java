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

package com.digitalgeneralists.assurance.ui.factories;

import java.awt.Dialog.ModalityType;
import java.awt.Window;

import javax.swing.JDialog;

import com.digitalgeneralists.assurance.model.entities.ApplicationConfiguration;
import com.digitalgeneralists.assurance.model.entities.FileReference;
import com.digitalgeneralists.assurance.model.entities.ScanDefinition;
import com.digitalgeneralists.assurance.model.entities.ScanMappingDefinition;
import com.digitalgeneralists.assurance.ui.components.dialogs.IDialogResponseHandler;

public interface IDialogFactory
{
	JDialog createScanDefinitionDialogInstance(Window parent, ModalityType modality, ScanDefinition scanDefinition);
	JDialog createScanDefinitionDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, ScanDefinition scanDefinition);

	JDialog createFileAttributesDialogInstance(Window parent, ModalityType modality, FileReference file);
	JDialog createFileAttributesDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, FileReference file);

	JDialog createAboutDialogInstance(Window parent, ModalityType modality);
	JDialog createAboutDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler);
	
	JDialog createScanMappingDefinitionDialogInstance(Window parent, ModalityType modality, ScanMappingDefinition mappingDefinition);
	JDialog createScanMappingDefinitionDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, ScanMappingDefinition mappingDefinition);
	
	JDialog createExclusionDialogInstance(Window parent, ModalityType modality, FileReference exclusion);
	JDialog createExclusionDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, FileReference exclusion);

	JDialog createSettingsDialogInstance(Window parent, ModalityType modality, ApplicationConfiguration configuration);
	JDialog createSettingsDialogInstance(Window parent, ModalityType modality, IDialogResponseHandler responseHandler, ApplicationConfiguration configuration);
}
