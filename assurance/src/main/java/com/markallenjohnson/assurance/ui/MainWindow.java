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

package com.markallenjohnson.assurance.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.Application;
import com.markallenjohnson.assurance.IApplicationDelegate;
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;
import com.markallenjohnson.assurance.notification.IEventObserver;
import com.markallenjohnson.assurance.notification.events.ApplicationConfigurationLoadedEvent;
import com.markallenjohnson.assurance.notification.events.ApplicationConfigurationSavedEvent;
import com.markallenjohnson.assurance.notification.events.IAssuranceEvent;
import com.markallenjohnson.assurance.notification.events.ScanCompletedEvent;
import com.markallenjohnson.assurance.notification.events.ScanStartedEvent;
import com.markallenjohnson.assurance.notification.events.SetScanDefinitionMenuStateEvent;
import com.markallenjohnson.assurance.notification.events.SetScanResultsMenuStateEvent;
import com.markallenjohnson.assurance.ui.components.AssuranceActions;
import com.markallenjohnson.assurance.ui.components.ResultsPanel;
import com.markallenjohnson.assurance.ui.components.ScanHistoryPanel;
import com.markallenjohnson.assurance.ui.components.ScanLaunchPanel;
import com.markallenjohnson.assurance.ui.factories.IDialogFactory;
import com.markallenjohnson.assurance.utils.AssuranceUtils;
import com.markallenjohnson.assurance.utils.Platform;

@Component("ApplicationUI")
public class MainWindow extends JFrame implements IApplicationUI, IEventObserver, ActionListener
{
	private Logger logger = LogManager.getLogger(MainWindow.class);

	private boolean initialized = false;

	private int scanMenuIndex = 0;
	private int resultsMenuIndex = 1;
	private int viewMenuIndex = 2;
	private int toolsMenuIndex = 3;

	private int viewHistoryMenuItemIndex = 1;

	private JMenuBar menuBar;
	private JRadioButtonMenuItem viewScanMenuItem;
	private JRadioButtonMenuItem viewHistoryMenuItem;

	@Autowired
	private IApplicationDelegate applicationDelegate;

	@Autowired
	private ScanLaunchPanel scanLaunchPanel;

	@Autowired
	private ScanHistoryPanel scanHistoryPanel;

	@Autowired
	private ResultsPanel resultsPanel;

	@Autowired
	IDialogFactory dialogFactory;

	private JTabbedPane topArea;
	
	private ApplicationConfiguration applicationConfiguration= null;

	// NOTE:  Since these are statics, using a SB to construct the labels is more complex than
	// the possible benefit may warrant.
	private static String quitApplicationMenuLabel = "Quit " + Application.applicationShortName;
	private static String aboutApplicationMenuLabel = "About " + Application.applicationShortName;
	private static String settingsMenuLabel = "Settings...";
	private static String newScanDefinitonMenuLabel = "New Scan Definition...";
	private static String deleteScanDefinitonMenuLabel = "Delete Scan Definition";
	private static String scanMenuLabel = "Scan";
	private static String scanAndMergeMenuLabel = "Scan & Merge";
	private static String replaceSourceMenuLabel = "Replace Source";
	private static String replaceTargetMenuLabel = "Replace Target";
	private static String sourceAttributesMenuLabel = "View Source Attributes...";
	private static String targetAttributesMenuLabel = "View Target Attributes...";
	private static String viewScanMenuLabel = "View Scan";
	private static String viewHistoryMenuLabel = "View History";

	private static final long serialVersionUID = 1L;

	public MainWindow()
	{
		if (AssuranceUtils.getPlatform() != Platform.MAC)
		{
			this.scanMenuIndex = 1;
			this.resultsMenuIndex = 2;
			this.viewMenuIndex = 3;
			this.toolsMenuIndex = 4;
		}
	}

	private void initializeComponent()
	{
		if (!this.initialized)
		{
			logger.info("Initializing the main window.");
			
			if (AssuranceUtils.getPlatform() == Platform.MAC)
			{

				System.setProperty("apple.laf.useScreenMenuBar", "true");
				com.apple.eawt.Application macApplication = com.apple.eawt.Application.getApplication();
				MacApplicationAdapter macAdapter = new MacApplicationAdapter(this);
				macApplication.addApplicationListener(macAdapter);
				macApplication.setEnabledPreferencesMenu(true);
			}

			this.setTitle(Application.applicationShortName);

			this.setDefaultCloseOperation(EXIT_ON_CLOSE);

			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);

			this.topArea = new JTabbedPane();

			this.scanLaunchPanel.setPreferredSize(new Dimension(600, 150));

			this.scanHistoryPanel.setPreferredSize(new Dimension(600, 150));

			this.topArea.addTab("Scan", this.scanLaunchPanel);

			this.topArea.addTab("History", this.scanHistoryPanel);

			this.resultsPanel.setPreferredSize(new Dimension(600, 400));

			this.topArea.addChangeListener(new ChangeListener()
			{
				public void stateChanged(ChangeEvent e)
				{
					resultsPanel.resetPanel();
					// NOTE:  This isn't ideal.  It feels brittle.
					if (topArea.getSelectedIndex() == viewHistoryMenuItemIndex)
					{
						viewHistoryMenuItem.setSelected(true);
					}
					else
					{
						viewScanMenuItem.setSelected(true);
					}
				}
			});

			GridBagConstraints topPanelConstraints = new GridBagConstraints();
			topPanelConstraints.anchor = GridBagConstraints.NORTH;
			topPanelConstraints.fill = GridBagConstraints.BOTH;
			topPanelConstraints.gridx = 0;
			topPanelConstraints.gridy = 0;
			topPanelConstraints.weightx = 1.0;
			topPanelConstraints.weighty = 0.33;
			topPanelConstraints.gridheight = 1;
			topPanelConstraints.gridwidth = 1;
			topPanelConstraints.insets = new Insets(0, 0, 0, 0);

			this.getContentPane().add(this.topArea, topPanelConstraints);

			GridBagConstraints resultsPanelConstraints = new GridBagConstraints();
			resultsPanelConstraints.anchor = GridBagConstraints.SOUTH;
			resultsPanelConstraints.fill = GridBagConstraints.BOTH;
			resultsPanelConstraints.gridx = 0;
			resultsPanelConstraints.gridy = 1;
			resultsPanelConstraints.weightx = 1.0;
			resultsPanelConstraints.weighty = 0.67;
			resultsPanelConstraints.gridheight = 1;
			resultsPanelConstraints.gridwidth = 1;
			resultsPanelConstraints.insets = new Insets(0, 0, 0, 0);

			this.getContentPane().add(this.resultsPanel, resultsPanelConstraints);

			this.applicationDelegate.addEventObserver(ScanStartedEvent.class, this);
			this.applicationDelegate.addEventObserver(ScanCompletedEvent.class, this);
			this.applicationDelegate.addEventObserver(SetScanDefinitionMenuStateEvent.class, this);
			this.applicationDelegate.addEventObserver(SetScanResultsMenuStateEvent.class, this);
			this.applicationDelegate.addEventObserver(ApplicationConfigurationLoadedEvent.class, this);

			JMenu menu;
			JMenuItem menuItem;

			menuBar = new JMenuBar();

			StringBuilder accessiblityLabel = new StringBuilder(128);
			if (AssuranceUtils.getPlatform() != Platform.MAC)
			{
				menu = new JMenu(Application.applicationShortName);
				menu.getAccessibleContext().setAccessibleDescription(accessiblityLabel.append("Actions for ").append(Application.applicationShortName).append(" application").toString());
				accessiblityLabel.setLength(0);
				menuBar.add(menu);

				menuItem = new JMenuItem(MainWindow.quitApplicationMenuLabel, KeyEvent.VK_Q);
				menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
				menuItem.getAccessibleContext().setAccessibleDescription(accessiblityLabel.append("Close the ").append(Application.applicationShortName).append(" application").toString());
				accessiblityLabel.setLength(0);
				menuItem.addActionListener(this);
				menuItem.setActionCommand(AssuranceActions.quitApplicationAction);
				menu.add(menuItem);

				menu.addSeparator();

				menuItem = new JMenuItem(MainWindow.aboutApplicationMenuLabel);
				menuItem.getAccessibleContext().setAccessibleDescription(accessiblityLabel.append("Display information about this version of ").append(Application.applicationShortName).append(".").toString());
				accessiblityLabel.setLength(0);
				menuItem.addActionListener(this);
				menuItem.setActionCommand(AssuranceActions.aboutApplicationAction);
				menu.add(menuItem);
			}

			menu = new JMenu("Scan");
			menu.setMnemonic(KeyEvent.VK_S);
			menu.getAccessibleContext().setAccessibleDescription("Actions for file scans");
			menuBar.add(menu);

			menuItem = new JMenuItem(MainWindow.newScanDefinitonMenuLabel, KeyEvent.VK_N);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
			menuItem.getAccessibleContext().setAccessibleDescription("Create a new scan definition");
			menuItem.addActionListener(this.scanLaunchPanel);
			menuItem.setActionCommand(AssuranceActions.newScanDefinitonAction);
			menu.add(menuItem);

			menuItem = new JMenuItem(MainWindow.deleteScanDefinitonMenuLabel, KeyEvent.VK_D);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
			menuItem.getAccessibleContext().setAccessibleDescription("Delete the selected scan definition");
			menuItem.addActionListener(this.scanLaunchPanel);
			menuItem.setActionCommand(AssuranceActions.deleteScanDefinitonAction);
			menu.add(menuItem);

			menu.addSeparator();

			menuItem = new JMenuItem(MainWindow.scanMenuLabel, KeyEvent.VK_S);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
			menuItem.getAccessibleContext().setAccessibleDescription("Launch a scan using the selected scan definition");
			menuItem.addActionListener(this.scanLaunchPanel);
			menuItem.setActionCommand(AssuranceActions.scanAction);
			menu.add(menuItem);

			menuItem = new JMenuItem(MainWindow.scanAndMergeMenuLabel, KeyEvent.VK_M);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.CTRL_MASK));
			menuItem.getAccessibleContext().setAccessibleDescription("Launch a scan using the selected scan definition and merge the results");
			menuItem.addActionListener(this.scanLaunchPanel);
			menuItem.setActionCommand(AssuranceActions.scanAndMergeAction);
			menu.add(menuItem);

			menu = new JMenu("Results");
			menu.setMnemonic(KeyEvent.VK_R);
			menu.getAccessibleContext().setAccessibleDescription("Actions for scan results");
			menuBar.add(menu);

			menuItem = new JMenuItem(MainWindow.replaceSourceMenuLabel, KeyEvent.VK_O);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
			menuItem.getAccessibleContext().setAccessibleDescription("Replace the source file with the target file");
			menuItem.addActionListener(this.resultsPanel.getResultMenuListener());
			menuItem.setActionCommand(AssuranceActions.replaceSourceAction);
			menu.add(menuItem);

			menuItem = new JMenuItem(MainWindow.replaceTargetMenuLabel, KeyEvent.VK_T);
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));
			menuItem.getAccessibleContext().setAccessibleDescription("Replace the target file with the source file");
			menuItem.addActionListener(this.resultsPanel.getResultMenuListener());
			menuItem.setActionCommand(AssuranceActions.replaceTargetAction);
			menu.add(menuItem);

			menu.addSeparator();

			menuItem = new JMenuItem(MainWindow.sourceAttributesMenuLabel);
			menuItem.getAccessibleContext().setAccessibleDescription("View the source file attributes");
			menuItem.addActionListener(this.resultsPanel.getResultMenuListener());
			menuItem.setActionCommand(AssuranceActions.sourceAttributesAction);
			menu.add(menuItem);

			menuItem = new JMenuItem(MainWindow.targetAttributesMenuLabel);
			menuItem.getAccessibleContext().setAccessibleDescription("View the target file attributes");
			menuItem.addActionListener(this.resultsPanel.getResultMenuListener());
			menuItem.setActionCommand(AssuranceActions.targetAttributesAction);
			menu.add(menuItem);

			menu = new JMenu("View");
			menu.setMnemonic(KeyEvent.VK_V);
			menu.getAccessibleContext().setAccessibleDescription(accessiblityLabel.append("Views within ").append(Application.applicationShortName).toString());
			accessiblityLabel.setLength(0);
			menuBar.add(menu);

			ButtonGroup group = new ButtonGroup();

			this.viewScanMenuItem = new JRadioButtonMenuItem(MainWindow.viewScanMenuLabel);
			this.viewScanMenuItem.addActionListener(this);
			this.viewScanMenuItem.setActionCommand(AssuranceActions.viewScanAction);
			this.viewScanMenuItem.setSelected(true);
			group.add(this.viewScanMenuItem);
			menu.add(this.viewScanMenuItem);

			this.viewHistoryMenuItem = new JRadioButtonMenuItem(MainWindow.viewHistoryMenuLabel);
			this.viewHistoryMenuItem.addActionListener(this);
			this.viewHistoryMenuItem.setActionCommand(AssuranceActions.viewHistoryAction);
			this.viewHistoryMenuItem.setSelected(true);
			group.add(this.viewHistoryMenuItem);
			menu.add(this.viewHistoryMenuItem);
			
			if (AssuranceUtils.getPlatform() != Platform.MAC)
			{
				menu = new JMenu("Tools");
				menu.getAccessibleContext().setAccessibleDescription(accessiblityLabel.append("Additional actions for ").append(Application.applicationShortName).append(" application").toString());
				accessiblityLabel.setLength(0);
				menuBar.add(menu);

				menuItem = new JMenuItem(MainWindow.settingsMenuLabel, KeyEvent.VK_COMMA);
				menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, ActionEvent.CTRL_MASK));
				menuItem.getAccessibleContext().setAccessibleDescription(accessiblityLabel.append("Change settings for the ").append(Application.applicationShortName).append(" application").toString());
				accessiblityLabel.setLength(0);
				menuItem.addActionListener(this);
				menuItem.setActionCommand(AssuranceActions.displaySettingsAction);
				menu.add(menuItem);
			}

			this.setJMenuBar(menuBar);

			this.initialized = true;
		}
	}

	public void display()
	{
		this.initializeComponent();

		this.pack();
		this.setVisible(true);

		this.setMenuState(this.scanMenuIndex, false);
		this.setMenuState(this.resultsMenuIndex, false);
		
		this.applicationDelegate.loadApplicationInitializationState();
	}

	public void notify(IAssuranceEvent event)
	{
		if (event instanceof ScanStartedEvent)
		{
			this.topArea.setSelectedIndex(0);
			
			this.topArea.setEnabled(false);
			
			this.setMenuState(this.scanMenuIndex, false, true);
			this.setMenuState(this.resultsMenuIndex, false, true);
			this.setMenuState(this.viewMenuIndex, false, true);
			this.setMenuState(this.toolsMenuIndex, false, true);
		}
		
		if (event instanceof ApplicationConfigurationLoadedEvent)
		{
			ApplicationConfiguration config = ((ApplicationConfigurationLoadedEvent) event).getApplicationConfiguration();
			this.setApplicationCongifuration(config);
		}
		
		if (event instanceof ApplicationConfigurationSavedEvent)
		{
			this.setApplicationCongifuration((ApplicationConfiguration)event.getSource());
		}
		
		if (event instanceof ScanCompletedEvent)
		{
			this.topArea.setEnabled(true);
			
			// NOTE:  I don't like how Swing manages menus.  This feels like
			// it could be done better.
			this.setMenuState(this.scanMenuIndex, true, true);
			this.setMenuState(this.viewMenuIndex, true, true);
			this.setMenuState(this.toolsMenuIndex, true, true);
			
			this.applicationDelegate.loadScanDefinitions();
		}

		if ((event instanceof SetScanDefinitionMenuStateEvent) || (event instanceof SetScanResultsMenuStateEvent))
		{
			Boolean enabled = (Boolean) event.getSource();
			int menuIndex = this.scanMenuIndex;
			if (event instanceof SetScanResultsMenuStateEvent)
			{
				menuIndex = this.resultsMenuIndex;
			}
			this.setMenuState(menuIndex, enabled);
			enabled = null;
		}
	}

	private void setMenuState(int menuIndex, Boolean enabled)
	{
		this.setMenuState(menuIndex, enabled, false);
	}
	
	private void setMenuState(int menuIndex, Boolean enabled, Boolean ignoreExceptions)
	{
		JMenu relevantMenu = this.menuBar.getMenu(menuIndex);
		
		if (relevantMenu != null)
		{
			for (int i = 0; i < relevantMenu.getItemCount(); i++)
			{
				JMenuItem item = relevantMenu.getItem(i);
				// NOTE: I don't like how Swing manages menus.
				if (item != null)
				{
					if (ignoreExceptions || (!AssuranceActions.newScanDefinitonAction.equals(item.getActionCommand())))
					{
						item.setEnabled(enabled);
					}
				}
			}
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		if (AssuranceActions.viewScanAction.equals(e.getActionCommand()))
		{
			this.topArea.setSelectedIndex(0);
			this.setMenuState(this.resultsMenuIndex, false);
		}
		if (AssuranceActions.viewHistoryAction.equals(e.getActionCommand()))
		{
			this.topArea.setSelectedIndex(1);
			this.setMenuState(this.scanMenuIndex, false);
		}
		if (AssuranceActions.quitApplicationAction.equals(e.getActionCommand()))
		{
			this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		}
		if (AssuranceActions.aboutApplicationAction.equals(e.getActionCommand()))
		{
			this.displayAboutDialog();
		}
		if (AssuranceActions.displaySettingsAction.equals(e.getActionCommand()))
		{
			this.displayPreferencesDialog();
		}
	}
	
	public void displayAboutDialog() 
	{
		JDialog aboutApplicationDialog = dialogFactory.createAboutDialogInstance(this, ModalityType.APPLICATION_MODAL);
		aboutApplicationDialog.setVisible(true);
	}
	
	public void displayPreferencesDialog() 
	{
		JDialog settingsDialog = dialogFactory.createSettingsDialogInstance(this, ModalityType.APPLICATION_MODAL, this.applicationConfiguration);
		settingsDialog.setVisible(true);
	}
	
	private void setApplicationCongifuration(ApplicationConfiguration config)
	{
		this.applicationConfiguration = config;
	}
	
	public ApplicationConfiguration getApplicationCongifuration()
	{
		return this.applicationConfiguration;
	}
}
