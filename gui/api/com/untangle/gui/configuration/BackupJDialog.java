/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.configuration;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.text.*;
import java.util.*;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;
import javax.swing.text.*;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.coloredTable.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.*;
import com.untangle.node.util.IOUtil;

public class BackupJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    private static final String BACKUP_EXTENSION = ".backup";
    private static final String OLD_BACKUP_EXTENSION = ".egbackup";

    private InfiniteProgressJComponent infiniteProgressJComponent = new InfiniteProgressJComponent();
    private static final long MIN_PROGRESS_MILLIS = 1000l;

    public BackupJDialog(Frame parentFrame) {
        super(parentFrame, true);

        // BUILD GENERAL GUI
        initComponents();
        setBounds( Util.generateCenteredBounds( parentFrame, this.getWidth(), this.getHeight()) );

        addWindowListener(this);
        setGlassPane(infiniteProgressJComponent);

        if( Util.isLocal() ){
            backupLocalFileJButton.setEnabled(false);
            restoreLocalFileJButton.setEnabled(false);
        }
        else{
            localBackupDisabledJLabel.setVisible(false);
            localRestoreDisabledJLabel.setVisible(false);
        }
        MConfigJDialog.setInitialFocusComponent(backupLocalFileJButton);
        Util.addPanelFocus(backupLocalFileJPanel, backupLocalFileJButton);
        Util.addPanelFocus(backupUsbKeyJPanel, backupUsbKeyJButton);
        Util.addPanelFocus(backupHardDiskJPanel, backupHardDiskJButton);
        Util.addPanelFocus(restoreLocalFileJPanel, restoreLocalFileJButton);
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        closeJButton = new javax.swing.JButton();
        helpJButton = new javax.swing.JButton();
        jTabbedPane = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        backupJTabbedPane = new javax.swing.JTabbedPane();
        backupLocalFileJPanel = new javax.swing.JPanel();
        contentJPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        localBackupDisabledJLabel = new javax.swing.JLabel();
        actionJPanel2 = new javax.swing.JPanel();
        backupLocalFileJButton = new javax.swing.JButton();
        backupUsbKeyJPanel = new javax.swing.JPanel();
        contentJPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        actionJPanel1 = new javax.swing.JPanel();
        backupUsbKeyJButton = new javax.swing.JButton();
        backupHardDiskJPanel = new javax.swing.JPanel();
        contentJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        actionJPanel = new javax.swing.JPanel();
        backupHardDiskJButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        restoreJTabbedPane = new javax.swing.JTabbedPane();
        restoreLocalFileJPanel = new javax.swing.JPanel();
        contentJPanel4 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        localRestoreDisabledJLabel = new javax.swing.JLabel();
        actionJPanel3 = new javax.swing.JPanel();
        restoreLocalFileJButton = new javax.swing.JButton();
        restoreHDAndUSBJPanel = new javax.swing.JPanel();
        contentJPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Backup/Restore Config");
        setModal(true);
        setResizable(false);
        closeJButton.setFont(new java.awt.Font("Default", 0, 12));
        closeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconClose_16x16.png")));
        closeJButton.setText("Cancel");
        closeJButton.setIconTextGap(6);
        closeJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        closeJButton.setOpaque(false);
        closeJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    closeJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
        getContentPane().add(closeJButton, gridBagConstraints);

        helpJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        helpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconHelp_18x16.png")));
        helpJButton.setText("Help");
        helpJButton.setIconTextGap(6);
        helpJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        helpJButton.setOpaque(false);
        helpJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    helpJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 0);
        getContentPane().add(helpJButton, gridBagConstraints);

        jTabbedPane.setFont(new java.awt.Font("Default", 0, 12));
        jPanel1.setLayout(new java.awt.BorderLayout());

        backupJTabbedPane.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(7, 13, 13, 13)));
        backupJTabbedPane.setFocusable(false);
        backupJTabbedPane.setFont(new java.awt.Font("Dialog", 0, 12));
        backupLocalFileJPanel.setLayout(new java.awt.GridBagLayout());

        backupLocalFileJPanel.setFocusable(false);
        contentJPanel3.setLayout(new java.awt.GridBagLayout());

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("<html> You can backup your current system configuration to a file on your local computer for later restoration, in the event that you would like to replace new settings with your current settings.  The file name will end with \".egbackup\"<br> <br> After backing up your current system configuration to a file, you can then restore that configuration through this dialog by going to \"Restore\" -> \"From Local File\".</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        contentJPanel3.add(jLabel4, gridBagConstraints);

        localBackupDisabledJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        localBackupDisabledJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        localBackupDisabledJLabel.setText("<html><center><b>This feature is only enabled when running the client remotely.</b></center></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        contentJPanel3.add(localBackupDisabledJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        backupLocalFileJPanel.add(contentJPanel3, gridBagConstraints);

        actionJPanel2.setLayout(new java.awt.GridBagLayout());

        backupLocalFileJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        backupLocalFileJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconBackup_32x32.png")));
        backupLocalFileJButton.setText("Backup to File");
        backupLocalFileJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        backupLocalFileJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    backupLocalFileJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        actionJPanel2.add(backupLocalFileJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
        backupLocalFileJPanel.add(actionJPanel2, gridBagConstraints);

        backupJTabbedPane.addTab("To File", backupLocalFileJPanel);

        backupUsbKeyJPanel.setLayout(new java.awt.GridBagLayout());

        backupUsbKeyJPanel.setFocusable(false);
        contentJPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>\nYou can backup your current system configuration to USB Key for later restoration, in the event that you would like to replace new settings with your current settings.<br>\n<br>\nAfter backing up your current system configuration to USB Key, you can then restore that configuration through the <b>Backup and Restore Utilities</b>.  To access the Backup and Restore Utilities, you must have a monitor and keyboard physically plugged into your server when it is turned on, and then select \"Backup and Restore Utilities\" from the boot prompt.<br>\n<br>\n<b>Note: You must insert your USB Key into a valid USB port on the back of your server before pressing the button.  You must not remove the USB Key from the USB port until after the process is complete.  The progress bar will inform you when the process is complete.</b> \n</html>");
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

        backupUsbKeyJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        backupUsbKeyJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconBackup_32x32.png")));
        backupUsbKeyJButton.setText("Backup to USB Key");
        backupUsbKeyJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        backupUsbKeyJButton.setMaximumSize(null);
        backupUsbKeyJButton.setMinimumSize(null);
        backupUsbKeyJButton.setPreferredSize(null);
        backupUsbKeyJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    backupUsbKeyJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        actionJPanel1.add(backupUsbKeyJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
        backupUsbKeyJPanel.add(actionJPanel1, gridBagConstraints);

        backupJTabbedPane.addTab("To USB Key", backupUsbKeyJPanel);

        backupHardDiskJPanel.setLayout(new java.awt.GridBagLayout());

        backupHardDiskJPanel.setFocusable(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("<html>\nYou can backup your current system configuration to Hard Disk for later restoration, in the event that you would like to replace new settings with your current settings.<br>\n<br>\nAfter backing up your current system configuration to Hard Disk, you can then restore that configuration through the <b>Backup and Restore Utilities</b>.  To access the Backup and Restore Utilities, you must have a monitor and keyboard physically plugged into your server when it is turned on, and then select \"Backup and Restore Utilities\" from the boot prompt. \n</html>");
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

        backupHardDiskJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        backupHardDiskJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconBackup_32x32.png")));
        backupHardDiskJButton.setText("Backup to Hard Disk");
        backupHardDiskJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        backupHardDiskJButton.setMaximumSize(null);
        backupHardDiskJButton.setMinimumSize(null);
        backupHardDiskJButton.setPreferredSize(null);
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

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
        backupHardDiskJPanel.add(actionJPanel, gridBagConstraints);

        backupJTabbedPane.addTab("To Hard Disk", backupHardDiskJPanel);

        jPanel1.add(backupJTabbedPane, java.awt.BorderLayout.CENTER);

        jTabbedPane.addTab("Backup", jPanel1);

        jPanel2.setLayout(new java.awt.BorderLayout());

        restoreJTabbedPane.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(7, 13, 13, 13)));
        restoreJTabbedPane.setFocusable(false);
        restoreJTabbedPane.setFont(new java.awt.Font("Dialog", 0, 12));
        restoreLocalFileJPanel.setLayout(new java.awt.GridBagLayout());

        restoreLocalFileJPanel.setFocusable(false);
        contentJPanel4.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("<html> You can restore a previous system configuration from a backup file on your local computer.  The backup file name ends with \".egbackup\"</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        contentJPanel4.add(jLabel5, gridBagConstraints);

        localRestoreDisabledJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        localRestoreDisabledJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        localRestoreDisabledJLabel.setText("<html><center><b>This feature is only enabled when running the client remotely.</b></center></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        contentJPanel4.add(localRestoreDisabledJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        restoreLocalFileJPanel.add(contentJPanel4, gridBagConstraints);

        actionJPanel3.setLayout(new java.awt.GridBagLayout());

        restoreLocalFileJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        restoreLocalFileJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconRestore_32x32.png")));
        restoreLocalFileJButton.setText("Restore from File");
        restoreLocalFileJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        restoreLocalFileJButton.setMaximumSize(null);
        restoreLocalFileJButton.setMinimumSize(null);
        restoreLocalFileJButton.setPreferredSize(null);
        restoreLocalFileJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    restoreLocalFileJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
        actionJPanel3.add(restoreLocalFileJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
        restoreLocalFileJPanel.add(actionJPanel3, gridBagConstraints);

        restoreJTabbedPane.addTab("From File", restoreLocalFileJPanel);

        restoreHDAndUSBJPanel.setLayout(new java.awt.GridBagLayout());

        restoreHDAndUSBJPanel.setFocusable(false);
        contentJPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("<html>\nAfter backing up your system configuration, you can restore that configuration through the <b>Recovery Utilities</b> on your server once it is done booting.\n<br>\n<br>To access the <b>Recovery Utilities</b>, you must have a monitor and keyboard physically plugged into your server, and then click on the Recovery Utilities toolbar button when it is done booting.\n</html>");
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

        jPanel2.add(restoreJTabbedPane, java.awt.BorderLayout.CENTER);

        jTabbedPane.addTab("Restore", jPanel2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        getContentPane().add(jTabbedPane, gridBagConstraints);

        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/DarkGreyBackground1600x100.png")));
        backgroundJLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(backgroundJLabel, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-630)/2, (screenSize.height-500)/2, 630, 500);
    }//GEN-END:initComponents

    private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
        try{
            String focus = Util.getSelectedTabTitle(backupJTabbedPane).toLowerCase().replace(" ", "_");
            URL newURL = new URL( "http://www.untangle.com/docs/get.php?"
                                  + "version=" + Version.getVersion()
                                  + "&source=" + "backup_restore_config"
                                  + "&focus=" + focus);
            System.out.println(newURL);
            ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
        }
        catch(Exception f){
            Util.handleExceptionNoRestart("Error showing help for backup_config", f);
        }
    }//GEN-LAST:event_helpJButtonActionPerformed

    private void restoreLocalFileJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restoreLocalFileJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
        new RestoreThread();
    }//GEN-LAST:event_restoreLocalFileJButtonActionPerformed

    private void backupLocalFileJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupLocalFileJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
        new BackupThread(2);
    }//GEN-LAST:event_backupLocalFileJButtonActionPerformed

    private void backupUsbKeyJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupUsbKeyJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
        new BackupThread(1);
    }//GEN-LAST:event_backupUsbKeyJButtonActionPerformed

    private void backupHardDiskJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupHardDiskJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
        new BackupThread(0);
    }//GEN-LAST:event_backupHardDiskJButtonActionPerformed


    private class BackupThread extends Thread {
        private int type;
        private String successString;
        private String failedString;
        public BackupThread(int type){
            super("MVCLIENT-BackupThread");
            setDaemon(true);
            this.type = type;
            infiniteProgressJComponent.start("Backing Up...");
            if(type==0){
                successString = "Success:  The Hard Disk backup procedure completed.";
                failedString = "Error:  The Hard Disk backup procedure failed.  Contact support for further direction.";
            }
            else if(type==1){
                successString = "Success:  The USB Key backup procedure completed.";
                failedString = "Error:  The USB Key backup procedure failed.  Contact support for further direction.";
            }
            else{
                successString = "Success:  The local file backup procedure completed.";
                failedString = "Error:  The local file backup procedure failed.  Please try again.";
            }
            this.start();
        }
        public void run() {
            try{
                // PERFORM THE BACKUP
                if(type==0)
                    Util.getUvmContext().localBackup();
                else if(type==1)
                    Util.getUvmContext().usbBackup();
                else{
                    // XXX DO SOMETHING HERE
                    JFileChooser chooser = new JFileChooser();
                    chooser.addChoosableFileFilter(new BackupFileFilter());
                    int retVal = chooser.showSaveDialog(BackupJDialog.this);
                    if(retVal == JFileChooser.APPROVE_OPTION){
                        File file = chooser.getSelectedFile();
                        if(!file.getName().endsWith(BACKUP_EXTENSION) &&
                           !file.getName().endsWith(OLD_BACKUP_EXTENSION))
                            file = new File(file.getPath() + BACKUP_EXTENSION);

                        if(file.exists()){
                            // ASK IF YOU WANT TO REALLY OVER-WRITE!!
                            BackupSaveFileJDialog backupSaveFileJDialog = new BackupSaveFileJDialog(BackupJDialog.this);
                            boolean isProceeding = backupSaveFileJDialog.isProceeding();

                            if(isProceeding){
                                boolean deleteResult = file.delete();
                                if(!deleteResult)
                                    throw new Exception();
                            }
                            else{
                                return;
                            }

                        }

                        // GENERATE THE BACKUP DATA
                        byte[] backup = Util.getUvmContext().createBackup();

                        if(!file.createNewFile()){
                            // TELL HIM HE CANT WRITE HERE
                            MOneButtonJDialog.factory(BackupJDialog.this, "",
                                                      "The file cannot be written in that location.",
                                                      "Save Backup File Warning", "");
                            throw new Exception();
                        }

                        // WRITE THE SUCKER OUT
                        IOUtil.bytesToFile(backup, file);
                    }
                    else{
                        return; // user cancelled operation
                    }

                }

                infiniteProgressJComponent.setTextLater("Backup Success");
                MOneButtonJDialog.factory(BackupJDialog.this, "",
                                          successString,
                                          "Backup Success", "");
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error backing up", e);
                infiniteProgressJComponent.setTextLater("Restore Failure");
                MOneButtonJDialog.factory(BackupJDialog.this, "",
                                          failedString,
                                          "Backup Failure Warning", "");
            }
            finally{
                infiniteProgressJComponent.stopLater(MIN_PROGRESS_MILLIS);
            }
        }
    }


    private class RestoreThread extends Thread {
        private String successString;
        private String failedString;
        public RestoreThread(){
            super("MVCLIENT-RestoreThread");
            setDaemon(true);
            successString = "Success:  The Local File restore procedure completed.";
            failedString = "Error:  The Local File restore procedure failed.  The reason reported by the Untangle Server was:\n";
            infiniteProgressJComponent.start("Restoring...");
            this.start();
        }
        public void run() {
            try{
                // PERFORM THE RESTORE
                JFileChooser chooser = new JFileChooser();
                chooser.addChoosableFileFilter(new BackupFileFilter());
                int retVal = chooser.showOpenDialog(BackupJDialog.this);
                if(retVal == JFileChooser.APPROVE_OPTION){
                    File file = chooser.getSelectedFile();

                    if(!file.canRead()){
                        MOneButtonJDialog.factory(BackupJDialog.this, "",
                                                  "The file in that location cannot be read.",
                                                  "Restore Backup File Warning", "");
                        throw new Exception();
                    }

                    // READ THE SUCKER IN
                    byte[] backup = IOUtil.fileToBytes(file);

                    // RESTORE THE BACKUP DATA
                    Util.getUvmContext().restoreBackup(backup);
                }
                else{
                    return; // user cancelled operation
                }

                infiniteProgressJComponent.setTextLater("Restore Success");
                MOneButtonJDialog.factory(BackupJDialog.this, "",
                                          successString,
                                          "Restore Success", "");
                RestartDialog.factory(BackupJDialog.this);
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error restoring", e);
                infiniteProgressJComponent.setTextLater("Restore Failure");
                MOneButtonJDialog.factory(BackupJDialog.this, "",
                                          failedString + e.getMessage(),
                                          "Restore Failure Warning", "");
            }
            finally{
                infiniteProgressJComponent.stopLater(MIN_PROGRESS_MILLIS);
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
            else if(f.getName().endsWith(OLD_BACKUP_EXTENSION))
                return true;
            else
                return false;
        }
        public String getDescription(){
            return "Untangle Backup Files (*" + BACKUP_EXTENSION + ")";
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel actionJPanel;
    private javax.swing.JPanel actionJPanel1;
    private javax.swing.JPanel actionJPanel2;
    private javax.swing.JPanel actionJPanel3;
    private javax.swing.JLabel backgroundJLabel;
    protected javax.swing.JButton backupHardDiskJButton;
    private javax.swing.JPanel backupHardDiskJPanel;
    private javax.swing.JTabbedPane backupJTabbedPane;
    protected javax.swing.JButton backupLocalFileJButton;
    private javax.swing.JPanel backupLocalFileJPanel;
    protected javax.swing.JButton backupUsbKeyJButton;
    private javax.swing.JPanel backupUsbKeyJPanel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton closeJButton;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JPanel contentJPanel1;
    private javax.swing.JPanel contentJPanel2;
    private javax.swing.JPanel contentJPanel3;
    private javax.swing.JPanel contentJPanel4;
    private javax.swing.JButton helpJButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JLabel localBackupDisabledJLabel;
    private javax.swing.JLabel localRestoreDisabledJLabel;
    private javax.swing.JPanel restoreHDAndUSBJPanel;
    private javax.swing.JTabbedPane restoreJTabbedPane;
    protected javax.swing.JButton restoreLocalFileJButton;
    private javax.swing.JPanel restoreLocalFileJPanel;
    // End of variables declaration//GEN-END:variables

}


