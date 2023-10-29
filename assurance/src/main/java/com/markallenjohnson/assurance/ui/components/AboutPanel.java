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

import com.markallenjohnson.assurance.Application;
import com.markallenjohnson.assurance.ui.components.dialogs.AssuranceDialogMode;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

@Component("AboutPanel")
public class AboutPanel extends AbstractDialogPanel
{
	private Logger logger = LogManager.getLogger(AboutPanel.class);
	private static final long serialVersionUID = 1L;

	// NOTE:  Since these are statics, using a SB to construct the labels is more complex than
	// the possible benefit may warrant.
	private static String dialogTitle = "About " + Application.applicationShortName;
	
	public AboutPanel()
	{
		initializeComponent();
	}

	protected void initializeComponent()
	{
		if (!this.initialized)
	    {
		    GridBagLayout gridbag = new GridBagLayout();
		    setLayout(gridbag);
		
		    GridBagConstraints applicationIconConstraints = new GridBagConstraints();
		    applicationIconConstraints.anchor = 11;
		    applicationIconConstraints.fill = 1;
		    applicationIconConstraints.gridx = 0;
		    applicationIconConstraints.gridy = 0;
		    applicationIconConstraints.weightx = 1.0D;
		    applicationIconConstraints.weighty = 0.8D;
		    applicationIconConstraints.gridheight = 1;
		    applicationIconConstraints.gridwidth = 1;
		    applicationIconConstraints.insets = new Insets(5, 5, 5, 5);
		    try
		    {
		    	Image iconImage = ImageIO.read(getClass().getClassLoader().getResource("assurance.png"));
		        iconImage = iconImage.getScaledInstance(48, 48, 0);
		        JLabel applicationIcon = new JLabel(new ImageIcon(iconImage));
		        add(applicationIcon, applicationIconConstraints);
		    }
		    catch (IOException e)
		    {
		        this.logger.warn(e);
		    }
		
		    GridBagConstraints applicationNameLabelConstraints = new GridBagConstraints();
		    applicationNameLabelConstraints.anchor = 11;
		    applicationNameLabelConstraints.fill = 1;
		    applicationNameLabelConstraints.gridx = 0;
		    applicationNameLabelConstraints.gridy = 1;
		    applicationNameLabelConstraints.weightx = 1.0D;
		    applicationNameLabelConstraints.weighty = 0.1D;
		    applicationNameLabelConstraints.gridheight = 1;
		    applicationNameLabelConstraints.gridwidth = 1;
		    applicationNameLabelConstraints.insets = new Insets(5, 5, 5, 5);
		
		    JLabel applicationNameLabel = new JLabel(Application.applicationName, 0);
		    add(applicationNameLabel, applicationNameLabelConstraints);
		
		    GridBagConstraints applicationVersionLabelConstraints = new GridBagConstraints();
		    applicationVersionLabelConstraints.anchor = 11;
		    applicationVersionLabelConstraints.fill = 1;
		    applicationVersionLabelConstraints.gridx = 0;
		    applicationVersionLabelConstraints.gridy = 2;
		    applicationVersionLabelConstraints.weightx = 1.0D;
		    applicationVersionLabelConstraints.weighty = 0.1D;
		    applicationVersionLabelConstraints.gridheight = 1;
		    applicationVersionLabelConstraints.gridwidth = 1;
		    applicationVersionLabelConstraints.insets = new Insets(5, 5, 5, 5);
		
		    StringBuilder labelText = new StringBuilder(128);
		    JLabel applicationVersionLabel = new JLabel(labelText.append(Application.applicationVersion).append(" (").append(Application.applicationBuildNumber).append(")").toString(), 0);
		    labelText.setLength(0);
		      
		    add(applicationVersionLabel, applicationVersionLabelConstraints);
		
		    this.initialized = true;
	    }
	}

	@Override
	public String getDialogTitle()
	{
		return dialogTitle;
	}
	
	@Override
	public void setDialogTitle(String title)
	{
		// No-op
	}

	public AssuranceDialogMode getMode() 
	{
		return AssuranceDialogMode.READ_ONLY;
	}
}
