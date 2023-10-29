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

import static javax.swing.SwingConstants.CENTER;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import com.markallenjohnson.assurance.Application;
import com.markallenjohnson.assurance.IApplicationDelegate;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.FileAttributes;
import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;
import com.markallenjohnson.assurance.model.merge.MergeEngine;
import com.markallenjohnson.assurance.notification.IEventObserver;
import com.markallenjohnson.assurance.notification.events.DeletedItemRestoreCompletedEvent;
import com.markallenjohnson.assurance.notification.events.DeletedItemRestoreProgressEvent;
import com.markallenjohnson.assurance.notification.events.DeletedItemRestoreStartedEvent;
import com.markallenjohnson.assurance.notification.events.IAssuranceEvent;
import com.markallenjohnson.assurance.notification.events.ResultMergeCompletedEvent;
import com.markallenjohnson.assurance.notification.events.ResultMergeProgressEvent;
import com.markallenjohnson.assurance.notification.events.ResultMergeStartedEvent;
import com.markallenjohnson.assurance.ui.factories.IDialogFactory;

@Component("ScanResultComponent")
public class ComparisonResultPanel extends JPanel implements ActionListener, IEventObserver
{
	private Logger logger = LogManager.getLogger(ComparisonResultPanel.class);

	@Autowired
	private IApplicationDelegate applicationDelegate;

	private boolean initialized = false;

	private JButton moreSourceAttributesButton = new JButton();
	private JButton moreTargetAttributesButton = new JButton();
	private JButton mergeTargetToSourceButton = new JButton("<-");
	private JButton mergeSourceToTargetButton = new JButton("->");
	private JButton mergeRestoreButton = new JButton("Restore");
	private JPanel standardPanel = new JPanel();
	private GridBagConstraints basePanelConstraints = new GridBagConstraints();
	private JPanel mergingPanel = new JPanel();
	private JLabel mergingLabel = new JLabel("Merging");
	private JProgressBar mergingProgressIndicator = new JProgressBar(0, 100);
	private JPanel mergePanel = new JPanel();

	private ComparisonResult result;

	private int numberOfLines = 0;

	private boolean editable = false;
	private boolean merging = false;

	private static final long serialVersionUID = 1L;

	public ComparisonResultPanel()
	{
		initializeComponent();
	}

	public ComparisonResultPanel(ComparisonResult result)
	{
		this(result, null, false);
	}

	public ComparisonResultPanel(ComparisonResult result, IApplicationDelegate delegate, boolean editable)
	{
		this.editable = editable;
		this.result = result;
		// NOTE: I shouldn't have to do this because of the auto-wired annotation in this class.
		// The fact that instances of this class are created by a prototype instance seems to
		// be effecting the auto-wire behavior.
		this.applicationDelegate = delegate;
		initializeComponent(this.result);
	}

	private void initializeComponent()
	{
		this.initializeComponent(null);
	}

	protected void initializeComponent(ComparisonResult result)
	{
		if (!this.initialized)
		{
			this.merging = false;
			this.numberOfLines = 0;

			this.setOpaque(true);

			GridBagLayout gridbag = new GridBagLayout();
			this.setLayout(gridbag);

			this.basePanelConstraints.anchor = GridBagConstraints.WEST;
			this.basePanelConstraints.fill = GridBagConstraints.BOTH;
			this.basePanelConstraints.gridx = 0;
			this.basePanelConstraints.gridy = 0;
			this.basePanelConstraints.weightx = 1.0;
			this.basePanelConstraints.weighty = 1.0;
			this.basePanelConstraints.gridheight = 1;
			this.basePanelConstraints.gridwidth = 1;
			this.basePanelConstraints.insets = new Insets(0, 0, 0, 0);

			this.standardPanel.setOpaque(false);
			GridBagLayout basePanelGridbag = new GridBagLayout();
			this.standardPanel.setLayout(basePanelGridbag);
			this.add(this.standardPanel, this.basePanelConstraints);

			GridBagConstraints sourcePanelConstraints = new GridBagConstraints();
			sourcePanelConstraints.anchor = GridBagConstraints.WEST;
			sourcePanelConstraints.fill = GridBagConstraints.BOTH;
			sourcePanelConstraints.gridx = 0;
			sourcePanelConstraints.gridy = 0;
			sourcePanelConstraints.weightx = 0.4;
			sourcePanelConstraints.weighty = 1.0;
			sourcePanelConstraints.gridheight = 1;
			sourcePanelConstraints.gridwidth = 1;
			sourcePanelConstraints.insets = new Insets(0, 0, 0, 0);

			JPanel sourcePanel = new JPanel();
			sourcePanel.setOpaque(false);
			GridBagLayout sourcePanelGridbag = new GridBagLayout();
			sourcePanel.setLayout(sourcePanelGridbag);
			this.standardPanel.add(sourcePanel, sourcePanelConstraints);

			GridBagConstraints sourceFileLabelConstraints = new GridBagConstraints();
			sourceFileLabelConstraints.anchor = GridBagConstraints.WEST;
			sourceFileLabelConstraints.gridx = 0;
			sourceFileLabelConstraints.gridy = 0;
			sourceFileLabelConstraints.weightx = 1.0;
			sourceFileLabelConstraints.weighty = 1.0;
			sourceFileLabelConstraints.gridheight = 1;
			sourceFileLabelConstraints.gridwidth = 1;
			sourceFileLabelConstraints.insets = new Insets(5, 5, 5, 5);

			String sourcePath = "No File Specified";
			if ((result != null) && (result.getSource() != null))
			{
				File sourceFile = result.getSource().getFile();
				if ((sourceFile == null) || (!sourceFile.exists()))
				{
					sourcePath = "File does not exist in source location.";
				}
				else
				{
					sourcePath = sourceFile.getPath();
				}
			}
			JLabel sourceFileLabel = new JLabel(sourcePath);
			sourcePanel.add(sourceFileLabel, sourceFileLabelConstraints);

			GridBagConstraints sourceAttributesPanelConstraints = new GridBagConstraints();
			sourceAttributesPanelConstraints.anchor = GridBagConstraints.WEST;
			sourceAttributesPanelConstraints.fill = GridBagConstraints.BOTH;
			sourceAttributesPanelConstraints.gridx = 0;
			sourceAttributesPanelConstraints.gridy = 1;
			sourceAttributesPanelConstraints.weightx = 1.0;
			sourceAttributesPanelConstraints.weighty = 1.0;
			sourceAttributesPanelConstraints.gridheight = 1;
			sourceAttributesPanelConstraints.gridwidth = 1;
			sourceAttributesPanelConstraints.insets = new Insets(0, 0, 0, 0);

			FileReference sourceReference = null;
			if (result != null)
			{
				sourceReference = result.getSource();
			}

			FileReference targetReference = null;
			if (result != null)
			{
				targetReference = result.getTarget();
			}

			JPanel sourceAttributesPanel = this.createFileAttributesPanel(sourceReference, targetReference, GridBagConstraints.WEST);
			sourcePanel.add(sourceAttributesPanel, sourceAttributesPanelConstraints);

			this.moreSourceAttributesButton.setHorizontalAlignment(SwingConstants.LEFT);
			this.moreSourceAttributesButton.setBorderPainted(false);
			this.moreSourceAttributesButton.setOpaque(false);
			this.moreSourceAttributesButton.setForeground(Color.blue);

			if (this.editable)
			{
				this.moreSourceAttributesButton.setText("All attributes...");
				this.moreSourceAttributesButton.addActionListener(this);
				this.moreSourceAttributesButton.setEnabled(true);
			}
			else
			{
				this.moreSourceAttributesButton.setText(" ");
				this.moreSourceAttributesButton.setEnabled(false);
			}
			this.moreSourceAttributesButton.setActionCommand(AssuranceActions.sourceAttributesAction);

			GridBagConstraints moreSourceAttributesButtonConstraints = new GridBagConstraints();
			moreSourceAttributesButtonConstraints.anchor = GridBagConstraints.WEST;
			moreSourceAttributesButtonConstraints.fill = GridBagConstraints.BOTH;
			moreSourceAttributesButtonConstraints.gridx = 0;
			moreSourceAttributesButtonConstraints.gridy = 2;
			moreSourceAttributesButtonConstraints.weightx = 1.0;
			moreSourceAttributesButtonConstraints.weighty = 1.0;
			moreSourceAttributesButtonConstraints.gridheight = 1;
			moreSourceAttributesButtonConstraints.gridwidth = 1;
			moreSourceAttributesButtonConstraints.insets = new Insets(0, 0, 0, 0);
			sourcePanel.add(this.moreSourceAttributesButton, moreSourceAttributesButtonConstraints);

			this.buildMergePanel(result);

			GridBagConstraints targetPanelConstraints = new GridBagConstraints();
			targetPanelConstraints.anchor = GridBagConstraints.EAST;
			targetPanelConstraints.fill = GridBagConstraints.BOTH;
			targetPanelConstraints.gridx = 2;
			targetPanelConstraints.gridy = 0;
			targetPanelConstraints.weightx = 0.4;
			targetPanelConstraints.weighty = 1.0;
			targetPanelConstraints.gridheight = 1;
			targetPanelConstraints.gridwidth = 1;
			targetPanelConstraints.insets = new Insets(0, 0, 0, 0);

			JPanel targetPanel = new JPanel();
			GridBagLayout targetPanelGridbag = new GridBagLayout();
			targetPanel.setOpaque(false);
			targetPanel.setLayout(targetPanelGridbag);
			this.standardPanel.add(targetPanel, targetPanelConstraints);

			GridBagConstraints targetFileLabelConstraints = new GridBagConstraints();
			targetFileLabelConstraints.anchor = GridBagConstraints.EAST;
			targetFileLabelConstraints.fill = GridBagConstraints.BOTH;
			targetFileLabelConstraints.gridx = 0;
			targetFileLabelConstraints.gridy = 0;
			targetFileLabelConstraints.weightx = 1.0;
			targetFileLabelConstraints.weighty = 1.0;
			targetFileLabelConstraints.gridheight = 1;
			targetFileLabelConstraints.gridwidth = 1;
			targetFileLabelConstraints.insets = new Insets(5, 5, 5, 5);

			// Create a label to put messages during an action event.
			String targetPath = "No File Specified";
			if ((result != null) && (result.getTarget() != null))
			{
				File targetFile = result.getTarget().getFile();
				if ((targetFile == null) || (!targetFile.exists()))
				{
					targetPath = "File does not exist in target location.";
				}
				else
				{
					targetPath = targetFile.getPath();
				}
			}
			JLabel targetFileLabel = new JLabel(targetPath);
			targetPanel.add(targetFileLabel, targetFileLabelConstraints);

			GridBagConstraints targetAttributesPanelConstraints = new GridBagConstraints();
			targetAttributesPanelConstraints.anchor = GridBagConstraints.EAST;
			targetAttributesPanelConstraints.fill = GridBagConstraints.BOTH;
			targetAttributesPanelConstraints.gridx = 0;
			targetAttributesPanelConstraints.gridy = 1;
			targetAttributesPanelConstraints.weightx = 1.0;
			targetAttributesPanelConstraints.weighty = 1.0;
			targetAttributesPanelConstraints.gridheight = 1;
			targetAttributesPanelConstraints.gridwidth = 1;
			targetAttributesPanelConstraints.insets = new Insets(0, 0, 0, 0);

			JPanel targetAttributesPanel = this.createFileAttributesPanel(targetReference, sourceReference, GridBagConstraints.EAST);
			targetPanel.add(targetAttributesPanel, targetAttributesPanelConstraints);
			
			this.moreTargetAttributesButton.setHorizontalAlignment(SwingConstants.RIGHT);
			this.moreTargetAttributesButton.setBorderPainted(false);
			this.moreTargetAttributesButton.setOpaque(false);
			this.moreTargetAttributesButton.setForeground(Color.blue);

			if (this.editable)
			{
				this.moreTargetAttributesButton.setText("All attributes...");
				this.moreTargetAttributesButton.addActionListener(this);
				this.moreTargetAttributesButton.setEnabled(true);
			}
			else
			{
				this.moreTargetAttributesButton.setText(" ");
				this.moreTargetAttributesButton.setEnabled(false);
			}
			this.moreTargetAttributesButton.setActionCommand(AssuranceActions.targetAttributesAction);

			GridBagConstraints moreTargetAttributesButtonConstraints = new GridBagConstraints();
			moreTargetAttributesButtonConstraints.anchor = GridBagConstraints.EAST;
			moreTargetAttributesButtonConstraints.fill = GridBagConstraints.BOTH;
			moreTargetAttributesButtonConstraints.gridx = 0;
			moreTargetAttributesButtonConstraints.gridy = 2;
			moreTargetAttributesButtonConstraints.weightx = 1.0;
			moreTargetAttributesButtonConstraints.weighty = 1.0;
			moreTargetAttributesButtonConstraints.gridheight = 1;
			moreTargetAttributesButtonConstraints.gridwidth = 1;
			moreTargetAttributesButtonConstraints.insets = new Insets(0, 0, 0, 0);
			targetPanel.add(this.moreTargetAttributesButton, moreTargetAttributesButtonConstraints);

			GridBagLayout mergingPanelGridbag = new GridBagLayout();
			this.mergingPanel.setLayout(mergingPanelGridbag);

			GridBagConstraints mergingLabelConstraints = new GridBagConstraints();
			mergingLabelConstraints.anchor = GridBagConstraints.NORTH;
			mergingLabelConstraints.fill = GridBagConstraints.BOTH;
			mergingLabelConstraints.gridx = 0;
			mergingLabelConstraints.gridy = 0;
			mergingLabelConstraints.weightx = 1.0;
			mergingLabelConstraints.weighty = 1.0;
			mergingLabelConstraints.gridheight = 1;
			mergingLabelConstraints.gridwidth = 1;
			mergingLabelConstraints.insets = new Insets(10, 10, 10, 10);

			this.mergingLabel.setHorizontalAlignment(CENTER);
			mergingPanel.add(this.mergingLabel, mergingLabelConstraints);

			GridBagConstraints mergingStatusIndicatorConstraints = new GridBagConstraints();
			mergingStatusIndicatorConstraints.anchor = GridBagConstraints.SOUTH;
			mergingStatusIndicatorConstraints.fill = GridBagConstraints.BOTH;
			mergingStatusIndicatorConstraints.gridx = 0;
			mergingStatusIndicatorConstraints.gridy = 1;
			mergingStatusIndicatorConstraints.weightx = 1.0;
			mergingStatusIndicatorConstraints.weighty = 1.0;
			mergingStatusIndicatorConstraints.gridheight = 1;
			mergingStatusIndicatorConstraints.gridwidth = 1;
			mergingStatusIndicatorConstraints.insets = new Insets(10, 10, 10, 10);

			this.mergingProgressIndicator.setIndeterminate(true);

			mergingPanel.add(this.mergingProgressIndicator, mergingStatusIndicatorConstraints);

			this.addAncestorListener(new AncestorListener()
			{
				public void ancestorAdded(AncestorEvent event)
				{
					if (applicationDelegate != null)
					{
						applicationDelegate.addEventObserver(ResultMergeStartedEvent.class, (IEventObserver) event.getSource());
						applicationDelegate.addEventObserver(ResultMergeProgressEvent.class, (IEventObserver) event.getSource());
						applicationDelegate.addEventObserver(ResultMergeCompletedEvent.class, (IEventObserver) event.getSource());
						applicationDelegate.addEventObserver(DeletedItemRestoreStartedEvent.class, (IEventObserver) event.getSource());
						applicationDelegate.addEventObserver(DeletedItemRestoreProgressEvent.class, (IEventObserver) event.getSource());
						applicationDelegate.addEventObserver(DeletedItemRestoreCompletedEvent.class, (IEventObserver) event.getSource());
					}
				}

				public void ancestorRemoved(AncestorEvent event)
				{
					if (applicationDelegate != null)
					{
						applicationDelegate.removeEventObserver(ResultMergeStartedEvent.class, (IEventObserver) event.getSource());
						applicationDelegate.removeEventObserver(ResultMergeProgressEvent.class, (IEventObserver) event.getSource());
						applicationDelegate.removeEventObserver(ResultMergeCompletedEvent.class, (IEventObserver) event.getSource());
						applicationDelegate.removeEventObserver(DeletedItemRestoreStartedEvent.class, (IEventObserver) event.getSource());
						applicationDelegate.removeEventObserver(DeletedItemRestoreProgressEvent.class, (IEventObserver) event.getSource());
						applicationDelegate.removeEventObserver(DeletedItemRestoreCompletedEvent.class, (IEventObserver) event.getSource());
					}
				}

				public void ancestorMoved(AncestorEvent event)
				{
					// This event does not to be handled in this application implementation.
				}
			});

			this.initialized = true;
		}
	}

	private void buildMergePanel(ComparisonResult result)
	{
		this.standardPanel.remove(this.mergePanel);

		GridBagConstraints mergePanelConstraints = new GridBagConstraints();
		mergePanelConstraints.anchor = GridBagConstraints.CENTER;
		mergePanelConstraints.fill = GridBagConstraints.BOTH;
		mergePanelConstraints.gridx = 1;
		mergePanelConstraints.gridy = 0;
		mergePanelConstraints.weightx = 0.2;
		mergePanelConstraints.weighty = 1.0;
		mergePanelConstraints.gridheight = 1;
		mergePanelConstraints.gridwidth = 1;
		mergePanelConstraints.insets = new Insets(0, 0, 0, 0);

		this.mergePanel = new JPanel();
		this.mergePanel.setOpaque(false);
		GridBagLayout mergePanelGridbag = new GridBagLayout();
		this.mergePanel.setLayout(mergePanelGridbag);
		this.standardPanel.add(this.mergePanel, mergePanelConstraints);

		AssuranceResultResolution resolution = AssuranceResultResolution.UNRESOLVED;
		if (result != null)
		{
			resolution = result.getResolution();

			if (resolution == AssuranceResultResolution.UNRESOLVED)
			{
				if (this.editable)
				{
					GridBagConstraints mergeTargetToSourceButtonConstraints = new GridBagConstraints();
					mergeTargetToSourceButtonConstraints.anchor = GridBagConstraints.CENTER;
					mergeTargetToSourceButtonConstraints.gridx = 0;
					mergeTargetToSourceButtonConstraints.gridy = 0;
					mergeTargetToSourceButtonConstraints.weightx = 1.0;
					mergeTargetToSourceButtonConstraints.weighty = 1.0;
					mergeTargetToSourceButtonConstraints.gridheight = 1;
					mergeTargetToSourceButtonConstraints.gridwidth = 1;
					mergeTargetToSourceButtonConstraints.insets = new Insets(5, 5, 5, 5);

					this.mergeTargetToSourceButton.addActionListener(this);
					Image icon;
					try
					{
						icon = ImageIO.read(Application.class.getClassLoader().getResource("replace-source.png"));
						icon = icon.getScaledInstance(25, 25, Image.SCALE_SMOOTH);
						this.mergeTargetToSourceButton.setIcon(new ImageIcon(icon));
						this.mergeTargetToSourceButton.setText("");
						this.mergeTargetToSourceButton.setToolTipText("Replace source with target.");
					}
					catch (IOException e)
					{
						logger.warn("Unable to load icon resources.", e);
					}
					this.mergeTargetToSourceButton.setActionCommand(AssuranceActions.replaceSourceAction);
					this.mergePanel.add(this.mergeTargetToSourceButton, mergeTargetToSourceButtonConstraints);

					GridBagConstraints mergeSourceToTargetButtonConstraints = new GridBagConstraints();
					mergeSourceToTargetButtonConstraints.anchor = GridBagConstraints.CENTER;
					mergeSourceToTargetButtonConstraints.gridx = 0;
					mergeSourceToTargetButtonConstraints.gridy = 1;
					mergeSourceToTargetButtonConstraints.weightx = 1.0;
					mergeSourceToTargetButtonConstraints.weighty = 1.0;
					mergeSourceToTargetButtonConstraints.gridheight = 1;
					mergeSourceToTargetButtonConstraints.gridwidth = 1;
					mergeSourceToTargetButtonConstraints.insets = new Insets(5, 5, 5, 5);

					this.mergeSourceToTargetButton.addActionListener(this);
					try
					{
						icon = ImageIO.read(Application.class.getClassLoader().getResource("replace-target.png"));
						icon = icon.getScaledInstance(25, 25, Image.SCALE_SMOOTH);
						this.mergeSourceToTargetButton.setIcon(new ImageIcon(icon));
						this.mergeSourceToTargetButton.setText("");
						this.mergeSourceToTargetButton.setToolTipText("Replace target with source.");
					}
					catch (IOException e)
					{
						logger.warn("Unable to load icon resources.", e);
					}
					this.mergeSourceToTargetButton.setActionCommand(AssuranceActions.replaceTargetAction);
					this.mergePanel.add(this.mergeSourceToTargetButton, mergeSourceToTargetButtonConstraints);
				}
			}
			else
			{
				Image resolutionLabelValue = null;
				try
				{
					switch (result.getResolution())
					{
						case REPLACE_SOURCE:
						case DELETE_SOURCE:
							resolutionLabelValue = ImageIO.read(Application.class.getClassLoader().getResource("replace-source.png"));
							resolutionLabelValue = resolutionLabelValue.getScaledInstance(25, 25, Image.SCALE_SMOOTH);
							break;
						case REPLACE_TARGET:
						case DELETE_TARGET:
							resolutionLabelValue = ImageIO.read(Application.class.getClassLoader().getResource("replace-target.png"));
							resolutionLabelValue = resolutionLabelValue.getScaledInstance(25, 25, Image.SCALE_SMOOTH);
							break;
						case PROCESSING_ERROR_ENCOUNTERED:
							resolutionLabelValue = ImageIO.read(Application.class.getClassLoader().getResource("resolution-error.png"));
							resolutionLabelValue = resolutionLabelValue.getScaledInstance(25, 25, Image.SCALE_SMOOTH);
							break;
						default:
							resolutionLabelValue = ImageIO.read(Application.class.getClassLoader().getResource("undetermined.png"));
							resolutionLabelValue = resolutionLabelValue.getScaledInstance(25, 25, Image.SCALE_SMOOTH);
							break;
					}
				}
				catch (IOException e)
				{
					logger.warn("Unable to load icon resources.", e);
				}

				GridBagConstraints mergeResolutionLabelConstraints = new GridBagConstraints();
				mergeResolutionLabelConstraints.anchor = GridBagConstraints.CENTER;
				mergeResolutionLabelConstraints.gridx = 0;
				mergeResolutionLabelConstraints.gridy = 0;
				mergeResolutionLabelConstraints.weightx = 1.0;
				mergeResolutionLabelConstraints.weighty = 1.0;
				mergeResolutionLabelConstraints.gridheight = 1;
				mergeResolutionLabelConstraints.gridwidth = 1;
				mergeResolutionLabelConstraints.insets = new Insets(5, 5, 5, 5);

				JLabel mergeResolutionLabel = new JLabel(new ImageIcon(resolutionLabelValue));
				this.mergePanel.add(mergeResolutionLabel, mergeResolutionLabelConstraints);

				if ((result.getResolution() == AssuranceResultResolution.DELETE_SOURCE) || (result.getResolution() == AssuranceResultResolution.DELETE_TARGET))
				{
					File deletedFile = null;
					if (result.getResolution() == AssuranceResultResolution.DELETE_SOURCE)
					{
						deletedFile = result.getSourceDeletedItemLocation(MergeEngine.getApplicationDeletedItemsLocation());
					}
					if (result.getResolution() == AssuranceResultResolution.DELETE_TARGET)
					{
						deletedFile = result.getTargetDeletedItemLocation(MergeEngine.getApplicationDeletedItemsLocation());
					}

					if ((deletedFile != null) && deletedFile.exists())
					{
						GridBagConstraints mergeRestoreButtonConstraints = new GridBagConstraints();
						mergeRestoreButtonConstraints.anchor = GridBagConstraints.CENTER;
						mergeRestoreButtonConstraints.gridx = 0;
						mergeRestoreButtonConstraints.gridy = 1;
						mergeRestoreButtonConstraints.weightx = 1.0;
						mergeRestoreButtonConstraints.weighty = 1.0;
						mergeRestoreButtonConstraints.gridheight = 1;
						mergeRestoreButtonConstraints.gridwidth = 1;
						mergeRestoreButtonConstraints.insets = new Insets(5, 5, 5, 5);

						Image icon = null;
						try
						{
							icon = ImageIO.read(Application.class.getClassLoader().getResource("restore.png"));
							icon = icon.getScaledInstance(25, 25, Image.SCALE_SMOOTH);
						}
						catch (IOException e)
						{
							logger.warn("Unable to load icon resources.", e);
						}
						this.mergeRestoreButton.setIcon(new ImageIcon(icon));
						this.mergeRestoreButton.setText("");
						this.mergeRestoreButton.setToolTipText("Restore");
						this.mergeRestoreButton.addActionListener(this);
						this.mergeRestoreButton.setActionCommand(AssuranceActions.restoreDeletedItemAction);
						this.mergePanel.add(this.mergeRestoreButton, mergeRestoreButtonConstraints);
					}
				}
				else if (result.getResolution() == AssuranceResultResolution.PROCESSING_ERROR_ENCOUNTERED)
				{
					mergeResolutionLabel.setToolTipText(result.getResolutionError());
				}
			}
		}
	}
	
	private JPanel createFileAttributesPanel(FileReference fileReference, FileReference comparedFileReference, int anchor)
	{
		// NOTE:  I'm very unhappy with this entire method implementation.  It needs to be refactored.
		
		JPanel attributePanel = new JPanel();
		GridBagLayout attributePanelGridbag = new GridBagLayout();
		attributePanel.setOpaque(false);
		attributePanel.setLayout(attributePanelGridbag);

		if (fileReference != null)
		{
			int index = 0;

			FileAttributes attributes = fileReference.getFileAttributes();
			FileAttributes comparisonAttributes = comparedFileReference.getFileAttributes();

			if (((attributes.getContentsHash() == null) && (comparisonAttributes.getContentsHash() != null)) || ((attributes.getContentsHash() != null) && (!attributes.getContentsHash().equals(comparisonAttributes.getContentsHash()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Contents Hash: ", (attributes.getContentsHash() != null) ? attributes.getContentsHash() : "");
				index++;
			}
			if (((attributes.getCreationTime() == null) && (comparisonAttributes.getCreationTime() != null)) || ((attributes.getCreationTime() != null) && (!attributes.getCreationTime().equals(comparisonAttributes.getCreationTime()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Creation Time: ", (attributes.getCreationTime() != null) ? attributes.getCreationTime().toString() : "");
				index++;
			}
			if (((attributes.getIsDirectory() == null) && (comparisonAttributes.getIsDirectory() != null)) || ((attributes.getIsDirectory() != null) && (!attributes.getIsDirectory().equals(comparisonAttributes.getIsDirectory()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Directory: ", (attributes.getIsDirectory() != null) ? attributes.getIsDirectory().toString() : "");
				index++;
			}
			if (((attributes.getIsOther() == null) && (comparisonAttributes.getIsOther() != null)) || ((attributes.getIsOther() != null) && (!attributes.getIsOther().equals(comparisonAttributes.getIsOther()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Other: ", (attributes.getIsOther() != null) ? attributes.getIsOther().toString() : "");
				index++;
			}
			if (((attributes.getIsRegularFile() == null) && (comparisonAttributes.getIsRegularFile() != null)) || ((attributes.getIsRegularFile() != null) && (!attributes.getIsRegularFile().equals(comparisonAttributes.getIsRegularFile()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Regular File: ", (attributes.getIsRegularFile() != null) ? attributes.getIsRegularFile().toString() : "");
				index++;
			}
			if (((attributes.getIsSymbolicLink() == null) && (comparisonAttributes.getIsSymbolicLink() != null)) || ((attributes.getIsSymbolicLink() != null) && (!attributes.getIsSymbolicLink().equals(comparisonAttributes.getIsSymbolicLink()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Symbolic Link: ", (attributes.getIsSymbolicLink() != null) ? attributes.getIsSymbolicLink().toString() : "");
				index++;
			}
			if (((attributes.getLastAccessTime() == null) && (comparisonAttributes.getLastAccessTime() != null)) || ((attributes.getLastAccessTime() != null) && (!attributes.getLastAccessTime().equals(comparisonAttributes.getLastAccessTime()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Last Access Time: ", (attributes.getLastAccessTime() != null) ? attributes.getLastAccessTime().toString() : "");
				index++;
			}
			if (((attributes.getLastModifiedTime() == null) && (comparisonAttributes.getLastModifiedTime() != null)) || ((attributes.getLastModifiedTime() != null) && (!attributes.getLastModifiedTime().equals(comparisonAttributes.getLastModifiedTime()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Last Modified Time: ", (attributes.getLastModifiedTime() != null) ? attributes.getLastModifiedTime().toString() : "");
				index++;
			}
			if (((attributes.getSize() == null) && (comparisonAttributes.getSize() != null)) || ((attributes.getSize() != null) && (!attributes.getSize().equals(comparisonAttributes.getSize()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "File Size: ", (attributes.getSize() != null) ? attributes.getSize().toString() : "");
				index++;
			}
			// DOS Attributes
			if (((attributes.getIsArchive() == null) && (comparisonAttributes.getIsArchive() != null)) || ((attributes.getIsArchive() != null) && (!attributes.getIsArchive().equals(comparisonAttributes.getIsArchive()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Archive: ", (attributes.getIsArchive() != null) ? attributes.getIsArchive().toString() : "");
				index++;
			}
			if (((attributes.getIsHidden() == null) && (comparisonAttributes.getIsHidden() != null)) || ((attributes.getIsHidden() != null) && (!attributes.getIsHidden().equals(comparisonAttributes.getIsHidden()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Hidden: ", (attributes.getIsHidden() != null) ? attributes.getIsHidden().toString() : "");
				index++;
			}
			if (((attributes.getIsReadOnly() == null) && (comparisonAttributes.getIsReadOnly() != null)) || ((attributes.getIsReadOnly() != null) && (!attributes.getIsReadOnly().equals(comparisonAttributes.getIsReadOnly()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Read Only: ", (attributes.getIsReadOnly() != null) ? attributes.getIsReadOnly().toString() : "");
				index++;
			}
			if (((attributes.getIsSystem() == null) && (comparisonAttributes.getIsSystem() != null)) || ((attributes.getIsSystem() != null) && (!attributes.getIsSystem().equals(comparisonAttributes.getIsSystem()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "System File: ", (attributes.getIsSystem() != null) ? attributes.getIsSystem().toString() : "");
				index++;
			}
			// POSIX Attributes
			if (((attributes.getGroupName() == null) && (comparisonAttributes.getGroupName() != null)) || ((attributes.getGroupName() != null) && (!attributes.getGroupName().equals(comparisonAttributes.getGroupName()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Group Name: ", (attributes.getGroupName() != null) ? attributes.getGroupName() : "");
				index++;
			}
			if (((attributes.getOwner() == null) && (comparisonAttributes.getOwner() != null)) || ((attributes.getOwner() != null) && (!attributes.getOwner().equals(comparisonAttributes.getOwner()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Owner: ", (attributes.getOwner() != null) ? attributes.getOwner() : "");
				index++;
			}
			if (((attributes.getPermissions() == null) && (comparisonAttributes.getPermissions() != null)) || ((attributes.getPermissions() != null) && (!attributes.getPermissions().equals(comparisonAttributes.getPermissions()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "Permissions: ", (attributes.getPermissions() != null) ? attributes.getPermissions() : "");
				index++;
			}
			// File-owner Attributes
			if (((attributes.getFileOwner() == null) && (comparisonAttributes.getFileOwner() != null)) || ((attributes.getFileOwner() != null) && (!attributes.getFileOwner().equals(comparisonAttributes.getFileOwner()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "File Owner: ", (attributes.getFileOwner() != null) ? attributes.getFileOwner() : "");
				index++;
			}
			// ACL Attributes
			if (((attributes.getAclDescription() == null) && (comparisonAttributes.getAclDescription() != null)) || ((attributes.getAclDescription() != null) && (!attributes.getAclDescription().equals(comparisonAttributes.getAclDescription()))))
			{
				this.addFileAttributeToPanel(attributePanel, anchor, index, "ACLs: ", (attributes.getAclDescription() != null) ? attributes.getAclDescription() : "");
				index++;
			}

			this.numberOfLines = index;
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

	private void displayAttributesDialog(FileReference file)
	{
		ClassPathXmlApplicationContext springContext = null;
		try
		{
			if (this.getParent() != null)
			{
				JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this.getParent());

				springContext = new ClassPathXmlApplicationContext("/META-INF/spring/app-context.xml");
				IDialogFactory dialogFactory = (IDialogFactory) springContext.getBean("DialogFactory");

				JDialog scanDefinitionDialog = dialogFactory.createFileAttributesDialogInstance(parent, ModalityType.APPLICATION_MODAL, file);
				scanDefinitionDialog.setPreferredSize(new Dimension(400, 600));

				scanDefinitionDialog.setVisible(true);
				dialogFactory = null;
			}
		}
		finally
		{
			if (springContext != null)
			{
				springContext.close();
				springContext = null;
			}
		}
	}

	private void mergeSourceToTarget()
	{
		this.applicationDelegate.mergeScanResult(this.result, AssuranceMergeStrategy.SOURCE);
	}

	private void mergeTargetToSource()
	{
		this.applicationDelegate.mergeScanResult(this.result, AssuranceMergeStrategy.TARGET);
	}

	private void restoreDeletedItem()
	{
		this.applicationDelegate.restoreDeletedItem(this.result);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (AssuranceActions.sourceAttributesAction.equals(e.getActionCommand()))
		{
			this.displayAttributesDialog(this.result.getSource());
		}
		if (AssuranceActions.targetAttributesAction.equals(e.getActionCommand()))
		{
			this.displayAttributesDialog(this.result.getTarget());
		}
		if (AssuranceActions.replaceTargetAction.equals(e.getActionCommand()))
		{
			this.mergeSourceToTarget();
		}
		if (AssuranceActions.replaceSourceAction.equals(e.getActionCommand()))
		{
			this.mergeTargetToSource();
		}
		if (AssuranceActions.restoreDeletedItemAction.equals(e.getActionCommand()))
		{
			this.restoreDeletedItem();
		}
	}

	public int getNumberOfLines()
	{
		return this.numberOfLines;
	}

	public boolean isMerging()
	{
		return this.merging;
	}

	private void conditionallyShowMergingPanel(boolean showMergingPanel, String message)
	{
		this.mergingLabel.setText("Merging");
		if (showMergingPanel)
		{
			this.mergingLabel.setText(message);
			this.remove(this.standardPanel);
			this.add(this.mergingPanel, this.basePanelConstraints);
			this.validate();
			this.mergingPanel.repaint();
		}
		else
		{
			this.add(this.standardPanel, this.basePanelConstraints);
			this.remove(this.mergingPanel);
			this.buildMergePanel(this.result);
			this.validate();
			this.standardPanel.repaint();
		}
	}

	public void notify(IAssuranceEvent event)
	{
		if ((event instanceof ResultMergeStartedEvent) || (event instanceof DeletedItemRestoreStartedEvent))
		{
			String actionLabel = "Merging ";
			if (event instanceof DeletedItemRestoreStartedEvent)
			{
				actionLabel = "Restoring ";
			}

			if (((ComparisonResult) event.getSource()).getId().equals(this.result.getId()))
			{
				this.result = (ComparisonResult) event.getSource();

				this.merging = true;
				StringBuilder message = new StringBuilder(128);
				this.conditionallyShowMergingPanel(this.merging, message.append(actionLabel).append(result.getSource().getFile().getName()).toString());
				message.setLength(0);
			}
		}

		if (event instanceof ResultMergeProgressEvent)
		{
			// TODO: Currently unhandled.
		}

		if (event instanceof DeletedItemRestoreProgressEvent)
		{
			// TODO: Currently unhandled.
		}

		if ((event instanceof ResultMergeCompletedEvent) || (event instanceof DeletedItemRestoreCompletedEvent))
		{
			if (((ComparisonResult) event.getSource()).getId() == this.result.getId())
			{
				this.result = (ComparisonResult) event.getSource();

				this.merging = false;
				this.conditionallyShowMergingPanel(this.merging, " ");
			}
		}
	}
}
