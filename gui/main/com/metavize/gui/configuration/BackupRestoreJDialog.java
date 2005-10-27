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

import com.metavize.gui.main.MMainJFrame;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.coloredTable.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.widgets.dialogs.*;

import com.metavize.mvvm.*;


public class BackupRestoreJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {


    public BackupRestoreJDialog() {
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
        hardDiskJPanel = new javax.swing.JPanel();
        contentJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        actionJPanel = new javax.swing.JPanel();
        backupHardDiskJButton = new javax.swing.JButton();
        backupHardDiskJProgressBar = new javax.swing.JProgressBar();
        usbKeyJPanel = new javax.swing.JPanel();
        contentJPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        actionJPanel1 = new javax.swing.JPanel();
        backupUSBKeyJButton = new javax.swing.JButton();
        backupUSBKeyJProgressBar = new javax.swing.JProgressBar();
        restoreJPanel = new javax.swing.JPanel();
        contentJPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        backgroundJLabel = new com.metavize.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Backup & Restore");
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
        hardDiskJPanel.setLayout(new java.awt.GridBagLayout());

        hardDiskJPanel.setFocusable(false);
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
        hardDiskJPanel.add(contentJPanel, gridBagConstraints);

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
        hardDiskJPanel.add(actionJPanel, gridBagConstraints);

        jTabbedPane.addTab("Backup to Hard Disk", hardDiskJPanel);

        usbKeyJPanel.setLayout(new java.awt.GridBagLayout());

        usbKeyJPanel.setFocusable(false);
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
        usbKeyJPanel.add(contentJPanel1, gridBagConstraints);

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
        usbKeyJPanel.add(actionJPanel1, gridBagConstraints);

        jTabbedPane.addTab("Backup to USB Key", usbKeyJPanel);

        restoreJPanel.setLayout(new java.awt.GridBagLayout());

        restoreJPanel.setFocusable(false);
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
        restoreJPanel.add(contentJPanel2, gridBagConstraints);

        jTabbedPane.addTab("Restore", restoreJPanel);

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

    private void backupUSBKeyJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupUSBKeyJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
        new BackupThread(false);
    }//GEN-LAST:event_backupUSBKeyJButtonActionPerformed

    private void backupHardDiskJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupHardDiskJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
        new BackupThread(true);
    }//GEN-LAST:event_backupHardDiskJButtonActionPerformed


    private class BackupThread extends Thread {
        private boolean isLocal;
	private JProgressBar activeJProgressBar;
	private String successString;
	private String failedString;
        public BackupThread(boolean isLocal){
	    super("MVCLIENT-BackupThread");
            this.isLocal = isLocal;
	    backupUSBKeyJButton.setEnabled(false);
	    backupHardDiskJButton.setEnabled(false);
	    if(isLocal){
		activeJProgressBar = backupHardDiskJProgressBar;
		successString = "Success:  The Hard Disk backup procedure completed.";
		failedString = "Error:  The Hard Disk backup procedure failed.  Contact support for further direction.";
	    }
	    else{
		activeJProgressBar = backupUSBKeyJProgressBar;
		successString = "Success:  The USB Key backup procedure completed.";
		failedString = "Error:  The USB Key backup procedure failed.  Contact support for further direction.";
	    }
	    this.start();
        }
        public void run() {
            try{
                SwingUtilities.invokeAndWait( new Runnable() {
			public void run() {
			    activeJProgressBar.setValue(0);
			    activeJProgressBar.setString("");
			    activeJProgressBar.setIndeterminate(true);
			} } );
                
                if(isLocal)
                    Util.getMvvmContext().localBackup();
                else
                    Util.getMvvmContext().usbBackup();
                
                SwingUtilities.invokeAndWait( new Runnable() {
			public void run() {
			    activeJProgressBar.setString( successString );
			    activeJProgressBar.setIndeterminate(false);
			    activeJProgressBar.setValue(100);
			} } );
	    }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error backing up", e);
                try{
		    SwingUtilities.invokeAndWait( new Runnable() {
			    public void run() {
				activeJProgressBar.setString( failedString );
			    } } );
                }
                catch(Exception f){}
            }
            finally{
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
			    activeJProgressBar.setIndeterminate(false);
			    backupUSBKeyJButton.setEnabled(true);
			    backupHardDiskJButton.setEnabled(true);
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








    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel actionJPanel;
    private javax.swing.JPanel actionJPanel1;
    private javax.swing.JLabel backgroundJLabel;
    protected javax.swing.JButton backupHardDiskJButton;
    protected javax.swing.JProgressBar backupHardDiskJProgressBar;
    protected javax.swing.JButton backupUSBKeyJButton;
    protected javax.swing.JProgressBar backupUSBKeyJProgressBar;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton closeJButton;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JPanel contentJPanel1;
    private javax.swing.JPanel contentJPanel2;
    private javax.swing.JPanel hardDiskJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JPanel restoreJPanel;
    private javax.swing.JPanel usbKeyJPanel;
    // End of variables declaration//GEN-END:variables

}


