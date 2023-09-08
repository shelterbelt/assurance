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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.IApplicationDelegate;

@Component("ResultsMessageComponent")
public class ResultsMessagePanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	@Autowired
	private IApplicationDelegate applicationDelegate;

	private boolean initialized = false;

	private String message = "";
	private JLabel messageLabel = new JLabel("", SwingConstants.CENTER);

	public ResultsMessagePanel()
	{
		this.initializeComponent();
	}

	private void initializeComponent()
	{
		if (!this.initialized)
		{
			BorderLayout layout = new BorderLayout();
			this.setLayout(layout);

			this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
			this.setBackground(Color.white);

			Font messageFont = new Font(this.getFont().getName(), Font.BOLD, 24);

			this.messageLabel.setForeground(Color.darkGray);
			this.messageLabel.setFont(messageFont);

			this.add(this.messageLabel, BorderLayout.CENTER);
		}

		this.initialized = true;
	}

	public String getMessage()
	{
		return this.message;
	}

	public void setMessage(String message)
	{
		this.message = message;
		StringBuilder messageContent = new StringBuilder(1024);
		this.messageLabel.setText(messageContent.append("<html><body style='text-align: center;'>").append(this.getMessage()).append("</body></html>").toString());
		messageContent.setLength(0);
		messageContent = null;
	}
}
