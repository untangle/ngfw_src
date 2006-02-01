/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.configuration;

import java.awt.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.filechooser.*;

import java.io.*;

import com.metavize.gui.main.MMainJFrame;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.coloredTable.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.widgets.dialogs.*;

import com.metavize.mvvm.*;


public class BackupJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    private static final String BACKUP_EXTENSION = ".egbackup";
    

    public BackupJDialog() {
        super(Util.getMMainJFrame());

        // BUILD GENERAL GUI
        initComponents();
        this.setBounds( Util.generateCenteredBounds( Util.getMMainJFrame().getBounds(),
							this.getWidth(),
							this.getHeight()) );
        
        this.addWindowListener(this);


    }

    public void dataChangedInvalid(Object reference){}
    public void dataChangedValid(Object reference){}
    public void dataRefreshed(Object reference){}

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                buttonGroup1 = new javax.swing.ButtonGroup();
                closeJButton = new javax.swing.JButton();
                jTabbedPane = new javax.swing.JTabbedPane();
                backupJTabbedPane = new javax.swing.JTabbedPane();
                backupHardDiskJPanel = new javax.swing.JPanel();
                contentJPanel = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                actionJPanel = new javax.swing.JPanel();
                backupHardDiskJButton = new javax.swing.JButton();
                backupHardDiskJProgressBar = new javax.swing.JProgressBar();
                backupUsbKeyJPanel = new javax.swing.JPanel();
                contentJPanel1 = new javax.swing.JPanel();
                jLabel2 = new javax.swing.JLabel();
                actionJPanel1 = new javax.swing.JPanel();
                backupUSBKeyJButton = new javax.swing.JButton();
                backupUSBKeyJProgressBar = new javax.swing.JProgressBar();
                backupLocalFileJPanel = new javax.swing.JPanel();
                contentJPanel3 = new javax.swing.JPanel();
                jLabel4 = new javax.swing.JLabel();
                actionJPanel2 = new javax.swing.JPanel();
                backupFileJButton = new javax.swing.JButton();
                backupFileJProgressBar = new javax.swing.JProgressBar();
                restoreJTabbedPane = new javax.swing.JTabbedPane();
                restoreHDAndUSBJPanel = new javax.swing.JPanel();
                contentJPanel2 = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                backgroundJLabel = new com.metavize.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new java.awt.GridBagLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Backup/Restore Config");
                setModal(true);
                setResizable(false);
                closeJButton.setFont(new java.awt.Font("Default", 0, 12));
                closeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/Button_Close_Window_106x17.png")));
                closeJButton.setDoubleBuffered(true);
                closeJButton.setFocusPainted(false);
                closeJButton.setFocusable(false);
                closeJButton.setMaximumSize(new java.awt.Dimension(117, 25));
                closeJButton.setMinimumSize(new java.awt.Dimension(117, 25));
                closeJButton.setPreferredSize(new java.awt.Dimension(117, 25));
                closeJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                closeJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 0);
                getContentPane().add(closeJButton, gridBagConstraints);

                jTabbedPane.setDoubleBuffered(true);
                jTabbedPane.setFocusable(false);
                jTabbedPane.setFont(new java.awt.Font("Default", 0, 12));
                backupJTabbedPane.setFocusable(false);
                backupJTabbedPane.setFont(new java.awt.Font("Dialog", 0, 12));
                backupHardDiskJPanel.setLayout(new java.awt.GridBagLayout());

                backupHardDiskJPanel.setFocusable(false);
                contentJPanel.setLayout(new java.awt.GridBagLayout());

                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("<html>\nYou can backup your current system configuration to Hard Disk for later restoration, in the event that you would like to replace new settings with your current settings.<br>\n<br>\nAfter backing up your current system configuration to Hard Disk, you can then restore that configuration through the <b>Backup and Restore Utilities</b>.  To access the Backup and Restore Utilities, you must have a monitor and keyboard physically plugged into EdgeGuard when it is turned on, and then select \"Backup and Restore Utilities\" from the boot prompt. \n</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                contentJPanel.add(jLabel1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                backupHardDiskJPanel.add(contentJPanel, gridBagConstraints);

                actionJPanel.setLayout(new java.awt.GridBagLayout());

                backupHardDiskJButton.setIcon(Util.getButtonBackupToHardDisk());
                backupHardDiskJButton.setDoubleBuffered(true);
                backupHardDiskJButton.setFocusPainted(false);
                backupHardDiskJButton.setFocusable(false);
                backupHardDiskJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                backupHardDiskJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                backupHardDiskJButton.setMaximumSize(new java.awt.Dimension(155, 25));
                backupHardDiskJButton.setMinimumSize(new java.awt.Dimension(155, 25));
                backupHardDiskJButton.setPreferredSize(new java.awt.Dimension(155, 25));
                backupHardDiskJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                backupHardDiskJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
                actionJPanel.add(backupHardDiskJButton, gridBagConstraints);

                backupHardDiskJProgressBar.setFont(new java.awt.Font("Default", 0, 10));
                backupHardDiskJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
                backupHardDiskJProgressBar.setDoubleBuffered(true);
                backupHardDiskJProgressBar.setFocusable(false);
                backupHardDiskJProgressBar.setMinimumSize(new java.awt.Dimension(10, 15));
                backupHardDiskJProgressBar.setPreferredSize(new java.awt.Dimension(148, 15));
                backupHardDiskJProgressBar.setString("");
                backupHardDiskJProgressBar.setStringPainted(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                actionJPanel.add(backupHardDiskJProgressBar, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
                backupHardDiskJPanel.add(actionJPanel, gridBagConstraints);

                backupJTabbedPane.addTab("To Hard Disk", backupHardDiskJPanel);

                backupUsbKeyJPanel.setLayout(new java.awt.GridBagLayout());

                backupUsbKeyJPanel.setFocusable(false);
                contentJPanel1.setLayout(new java.awt.GridBagLayout());

                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>\nYou can backup your current system configuration to USB Key for later restoration, in the event that you would like to replace new settings with your current settings.<br>\n<br>\nAfter backing up your current system configuration to USB Key, you can then restore that configuration through the <b>Backup and Restore Utilities</b>.  To access the Backup and Restore Utilities, you must have a monitor and keyboard physically plugged into EdgeGuard when it is turned on, and then select \"Backup and Restore Utilities\" from the boot prompt.<br>\n<br>\n<b>Note: You must insert your USB Key into a valid USB port on the back of the EdgeGuard before pressing the button.  You must not remove the USB Key from the USB port until after the process is complete.  The progress bar will inform you when the process is complete.</b> \n</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                contentJPanel1.add(jLabel2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                backupUsbKeyJPanel.add(contentJPanel1, gridBagConstraints);

                actionJPanel1.setLayout(new java.awt.GridBagLayout());

                backupUSBKeyJButton.setIcon(Util.getButtonBackupToUsbKey());
                backupUSBKeyJButton.setDoubleBuffered(true);
                backupUSBKeyJButton.setFocusPainted(false);
                backupUSBKeyJButton.setFocusable(false);
                backupUSBKeyJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                backupUSBKeyJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                backupUSBKeyJButton.setMaximumSize(new java.awt.Dimension(155, 25));
                backupUSBKeyJButton.setMinimumSize(new java.awt.Dimension(155, 25));
                backupUSBKeyJButton.setPreferredSize(new java.awt.Dimension(155, 25));
                backupUSBKeyJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                backupUSBKeyJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
                actionJPanel1.add(backupUSBKeyJButton, gridBagConstraints);

                backupUSBKeyJProgressBar.setFont(new java.awt.Font("Default", 0, 10));
                backupUSBKeyJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
                backupUSBKeyJProgressBar.setDoubleBuffered(true);
                backupUSBKeyJProgressBar.setFocusable(false);
                backupUSBKeyJProgressBar.setMinimumSize(new java.awt.Dimension(10, 15));
                backupUSBKeyJProgressBar.setPreferredSize(new java.awt.Dimension(148, 15));
                backupUSBKeyJProgressBar.setString("");
                backupUSBKeyJProgressBar.setStringPainted(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                actionJPanel1.add(backupUSBKeyJProgressBar, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
                backupUsbKeyJPanel.add(actionJPanel1, gridBagConstraints);

                backupJTabbedPane.addTab("To USB Key", backupUsbKeyJPanel);

                backupLocalFileJPanel.setLayout(new java.awt.GridBagLayout());

                backupLocalFileJPanel.setFocusable(false);
                contentJPanel3.setLayout(new java.awt.GridBagLayout());

                jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel4.setText("<html> You can backup your current system configuration to a file on your local computer for later restoration, in the event that you would like to replace new settings with your current settings.<br> <br> After backing up your current system configuration to a file, you can then restore that configuration through this dialog by going to \"Restore\" -> \"From Local File\".</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                contentJPanel3.add(jLabel4, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                backupLocalFileJPanel.add(contentJPanel3, gridBagConstraints);

                actionJPanel2.setLayout(new java.awt.GridBagLayout());

                backupFileJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                backupFileJButton.setText("<html><b>Backup</b> to File</html>");
                backupFileJButton.setDoubleBuffered(true);
                backupFileJButton.setFocusPainted(false);
                backupFileJButton.setFocusable(false);
                backupFileJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                backupFileJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                backupFileJButton.setMaximumSize(new java.awt.Dimension(155, 25));
                backupFileJButton.setMinimumSize(new java.awt.Dimension(155, 25));
                backupFileJButton.setPreferredSize(new java.awt.Dimension(155, 25));
                backupFileJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                backupFileJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
                actionJPanel2.add(backupFileJButton, gridBagConstraints);

                backupFileJProgressBar.setFont(new java.awt.Font("Default", 0, 10));
                backupFileJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
                backupFileJProgressBar.setDoubleBuffered(true);
                backupFileJProgressBar.setFocusable(false);
                backupFileJProgressBar.setMinimumSize(new java.awt.Dimension(10, 15));
                backupFileJProgressBar.setPreferredSize(new java.awt.Dimension(148, 15));
                backupFileJProgressBar.setString("");
                backupFileJProgressBar.setStringPainted(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                actionJPanel2.add(backupFileJProgressBar, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
                backupLocalFileJPanel.add(actionJPanel2, gridBagConstraints);

                backupJTabbedPane.addTab("To Local File", backupLocalFileJPanel);

                jTabbedPane.addTab("Backup", backupJTabbedPane);

                restoreJTabbedPane.setFocusable(false);
                restoreJTabbedPane.setFont(new java.awt.Font("Dialog", 0, 12));
                restoreHDAndUSBJPanel.setLayout(new java.awt.GridBagLayout());

                restoreHDAndUSBJPanel.setFocusable(false);
                contentJPanel2.setLayout(new java.awt.GridBagLayout());

                jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel3.setText("<html>\nAfter backing up your system configuration, you can restore that configuration through the <b>Restore Utilities</b> on EdgeGuard once it is done booting.\n<br>\n<br>To access the <b>Restore Utilities</b>, you must have a monitor and keyboard physically plugged into the EdgeGuard, and then right click on the desktop when it is done booting... select \"Advanced\" from the menu... select \"EdgeGuard Recovery\", and proceed from there. \n</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                contentJPanel2.add(jLabel3, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
                restoreHDAndUSBJPanel.add(contentJPanel2, gridBagConstraints);

                restoreJTabbedPane.addTab("From Hard Disk and USB Key", restoreHDAndUSBJPanel);

                jTabbedPane.addTab("Restore", restoreJTabbedPane);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
                getContentPane().add(jTabbedPane, gridBagConstraints);

                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/DarkGreyBackground1600x100.png")));
                backgroundJLabel.setDoubleBuffered(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridheight = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                getContentPane().add(backgroundJLabel, gridBagConstraints);

                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                setBounds((screenSize.width-630)/2, (screenSize.height-402)/2, 630, 402);
        }//GEN-END:initComponents

    private void backupFileJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupFileJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
	new BackupThread(2);
    }//GEN-LAST:event_backupFileJButtonActionPerformed
    
    private void backupUSBKeyJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupUSBKeyJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
        new BackupThread(1);
    }//GEN-LAST:event_backupUSBKeyJButtonActionPerformed

    private void backupHardDiskJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupHardDiskJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
        new BackupThread(0);
    }//GEN-LAST:event_backupHardDiskJButtonActionPerformed


    private class BackupThread extends Thread {
        private int type;
		private JProgressBar activeJProgressBar;
	private String successString;
	private String failedString;
        public BackupThread(int type){
	    super("MVCLIENT-BackupThread");
            this.type = type;
	    backupUSBKeyJButton.setEnabled(false);
	    backupHardDiskJButton.setEnabled(false);
	    backupFileJButton.setEnabled(false);
	    if(type==0){
		activeJProgressBar = backupHardDiskJProgressBar;
		successString = "Success:  The Hard Disk backup procedure completed.";
		failedString = "Error:  The Hard Disk backup procedure failed.  Contact support for further direction.";
	    }
	    else if(type==1){
		activeJProgressBar = backupUSBKeyJProgressBar;
		successString = "Success:  The USB Key backup procedure completed.";
		failedString = "Error:  The USB Key backup procedure failed.  Contact support for further direction.";
	    }
	    else{
		activeJProgressBar = backupFileJProgressBar;	
		successString = "Success:  The local file backup procedure completed.";	
		failedString = "Error:  The local file backup procedure failed.  Please try again.";
	    }
	    this.start();
        }
        public void run() {
            try{
                SwingUtilities.invokeLater( new Runnable() { public void run() {
		    activeJProgressBar.setValue(0);
		    activeJProgressBar.setString("");
		    activeJProgressBar.setIndeterminate(true);
		} } );
		
		// PERFORM THE BACKUP
                if(type==0)
                    Util.getMvvmContext().localBackup();
                else if(type==1)
                    Util.getMvvmContext().usbBackup();
		else{
		    // XXX DO SOMETHING HERE
		    JFileChooser chooser = new JFileChooser();
			chooser.addChoosableFileFilter(new BackupFileFilter());
		    int retVal = chooser.showSaveDialog(BackupJDialog.this);
		    if(retVal == JFileChooser.APPROVE_OPTION){
			File file = chooser.getSelectedFile();
			if(file.exists()){
			    // ASK IF YOU WANT TO REALLY OVER-WRITE!!
			    BackupSaveFileJDialog backupSaveFileJDialog = new BackupSaveFileJDialog();
			    boolean isProceeding = backupSaveFileJDialog.isProceeding();
			    
			    if(isProceeding){
				boolean deleteResult = file.delete();
				if(!deleteResult)
				    throw new Exception();
			    }
			    else{
				return;
			    }
			    
			    if(!file.createNewFile()){
				// TELL HIM HE CANT WRITE HERE
				new MOneButtonJDialog("Save File", "The file cannot be written in that location.");
				throw new Exception();
			    }
			}
			else{
			    file.createNewFile();
			}


			// WRITE THE SUCKER OUT
			byte[] backup = Util.getMvvmContext().createBackup();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			fileOutputStream.write(backup);
			fileOutputStream.close();
		    }

		}		
		if(type!=2){
		    SwingUtilities.invokeLater( new Runnable() { public void run() {
			activeJProgressBar.setString( successString );
			activeJProgressBar.setIndeterminate(false);
			activeJProgressBar.setValue(100);
		    } } );
		}
	    }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error backing up", e);
                try{
		    SwingUtilities.invokeAndWait( new Runnable() { public void run() {
			activeJProgressBar.setString( failedString );
		    } } );
                }
                catch(Exception f){}
            }
            finally{
		SwingUtilities.invokeLater( new Runnable() {public void run() {
		    activeJProgressBar.setIndeterminate(false);
		    backupUSBKeyJButton.setEnabled(true);
		    backupHardDiskJButton.setEnabled(true);
		    backupFileJButton.setEnabled(true);
		} } );
	    }
	}       	
    }
    

    private void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
        windowClosing(null);
    }//GEN-LAST:event_closeJButtonActionPerformed


    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.dispose();
    }

    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}

	
    private class BackupFileFilter extends javax.swing.filechooser.FileFilter {
	
	public boolean accept(File f){
	    if(f.isDirectory())
		return true;
	    else if(f.getName().endsWith(BACKUP_EXTENSION))
		return true;
	    else
		return false;
	}
	public String getDescription(){
	    return "EdgeGuard Backup Files (*" + BACKUP_EXTENSION + ")";
	}
    }
	

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel actionJPanel;
        private javax.swing.JPanel actionJPanel1;
        private javax.swing.JPanel actionJPanel2;
        private javax.swing.JLabel backgroundJLabel;
        protected javax.swing.JButton backupFileJButton;
        protected javax.swing.JProgressBar backupFileJProgressBar;
        protected javax.swing.JButton backupHardDiskJButton;
        private javax.swing.JPanel backupHardDiskJPanel;
        protected javax.swing.JProgressBar backupHardDiskJProgressBar;
        private javax.swing.JTabbedPane backupJTabbedPane;
        private javax.swing.JPanel backupLocalFileJPanel;
        protected javax.swing.JButton backupUSBKeyJButton;
        protected javax.swing.JProgressBar backupUSBKeyJProgressBar;
        private javax.swing.JPanel backupUsbKeyJPanel;
        private javax.swing.ButtonGroup buttonGroup1;
        private javax.swing.JButton closeJButton;
        private javax.swing.JPanel contentJPanel;
        private javax.swing.JPanel contentJPanel1;
        private javax.swing.JPanel contentJPanel2;
        private javax.swing.JPanel contentJPanel3;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JTabbedPane jTabbedPane;
        private javax.swing.JPanel restoreHDAndUSBJPanel;
        private javax.swing.JTabbedPane restoreJTabbedPane;
        // End of variables declaration//GEN-END:variables

}


