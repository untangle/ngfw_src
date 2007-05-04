/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.store;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.*;

import com.untangle.gui.upgrade.UpgradeJDialog;
import com.untangle.gui.util.Util;
import com.untangle.mvvm.*;
import com.untangle.mvvm.toolbox.MackageDesc;

public class StoreCheckJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    private UpgradeCheckThread upgradeCheckThread;
    private Window window;
    public StoreCheckJDialog(Frame parentFrame) {
        super(parentFrame, true);
        initComponents();
        this.addWindowListener(this);
        this.window = Util.getMMainJFrame();

        setVisible(true);
    }

    public void setVisible(boolean isVisible){
        if(isVisible){
            pack();
            this.setBounds( Util.generateCenteredBounds(window, this.getWidth(), this.getHeight()) );
            upgradeCheckThread = new UpgradeCheckThread();
        }
        super.setVisible(isVisible);
        if(!isVisible){
            dispose();
        }
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        iconJLabel = new javax.swing.JLabel();
        dividerJPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        labelJLabel = new javax.swing.JLabel();
        messageJLabel = new javax.swing.JLabel();
        jProgressBar = new javax.swing.JProgressBar();
        proceedJButton = new javax.swing.JButton();
        backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Upgrade Availability Check");
        setModal(true);
        setResizable(false);
        iconJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconDialogWizard_96x96.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 20, 20, 20);
        getContentPane().add(iconJLabel, gridBagConstraints);

        dividerJPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(154, 154, 154)));
        dividerJPanel.setMaximumSize(new java.awt.Dimension(1, 1600));
        dividerJPanel.setMinimumSize(new java.awt.Dimension(1, 10));
        dividerJPanel.setOpaque(false);
        dividerJPanel.setPreferredSize(new java.awt.Dimension(1, 10));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 20, 0);
        getContentPane().add(dividerJPanel, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setOpaque(false);
        labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
        labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelJLabel.setText("Checking Upgrades:");
        labelJLabel.setDoubleBuffered(true);
        labelJLabel.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(labelJLabel, gridBagConstraints);

        messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        messageJLabel.setText("<html><center>\nYou must perform all possible upgrades before purchasing a new Product.<br>\n<br>\nNow checking for available upgrades.\n</center></html>");
        messageJLabel.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(messageJLabel, gridBagConstraints);

        jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        jProgressBar.setForeground(new java.awt.Color(68, 91, 255));
        jProgressBar.setFocusable(false);
        jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 16));
        jProgressBar.setMinimumSize(new java.awt.Dimension(10, 16));
        jProgressBar.setPreferredSize(new java.awt.Dimension(148, 16));
        jProgressBar.setString("");
        jProgressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(jProgressBar, gridBagConstraints);

        proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
        proceedJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconCancel_16x16.png")));
        proceedJButton.setText("Cancel");
        proceedJButton.setDoubleBuffered(true);
        proceedJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        proceedJButton.setMaximumSize(null);
        proceedJButton.setMinimumSize(null);
        proceedJButton.setPreferredSize(null);
        proceedJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    proceedJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        jPanel1.add(proceedJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 20, 20, 20);
        getContentPane().add(jPanel1, gridBagConstraints);

        backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
        backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
        backgroundJLabel.setDoubleBuffered(true);
        backgroundJLabel.setFocusable(false);
        backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        backgroundJLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(backgroundJLabel, gridBagConstraints);

    }//GEN-END:initComponents

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
        setVisible(false);
        if(upgradeCheckThread.upgradesAvailable()){
            try{
                UpgradeJDialog upgradeJDialog =  new UpgradeJDialog(Util.getMMainJFrame());
                upgradeJDialog.setVisible(true);
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error checking for upgrades on server", e); }
                catch(Exception f){ Util.handleExceptionNoRestart("Error checking for upgrades on server", f); }
            }
        }
    }//GEN-LAST:event_proceedJButtonActionPerformed


    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.setVisible(false);
    }


    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJLabel;
    private javax.swing.JPanel dividerJPanel;
    private javax.swing.JLabel iconJLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JProgressBar jProgressBar;
    private javax.swing.JLabel labelJLabel;
    private javax.swing.JLabel messageJLabel;
    private javax.swing.JButton proceedJButton;
    // End of variables declaration//GEN-END:variables

    private class UpgradeCheckThread extends Thread {
        private MackageDesc[] mackageDescs;
        public UpgradeCheckThread(){
            super("MVCLIENT-UpgradeCheckThread");
            this.setDaemon(true);
            this.setContextClassLoader(Util.getClassLoader());
            Util.setUpgradeCount(Util.UPGRADE_CHECKING);
            this.start();
        }
        public boolean upgradesAvailable(){ return !Util.isArrayEmpty(mackageDescs); }
        public void run() {
            try{
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    StoreCheckJDialog.this.jProgressBar.setIndeterminate(true);
                    StoreCheckJDialog.this.jProgressBar.setString("Checking for upgrades...");
                }});

                Thread.sleep(2000l);

                Util.getToolboxManager().update();
                mackageDescs = Util.getToolboxManager().upgradable();
                if( Util.isArrayEmpty(mackageDescs) ){
                    Util.getMMainJFrame().updateJButton(0);
                    Util.setUpgradeCount(0);
                    SwingUtilities.invokeLater( new Runnable(){ public void run(){
                        StoreCheckJDialog.this.jProgressBar.setIndeterminate(false);
                        StoreCheckJDialog.this.jProgressBar.setValue(1);
                        StoreCheckJDialog.this.jProgressBar.setString("No upgrades found.  Proceeding.");
                    }});
                    Thread.sleep(2000l);
                    StoreCheckJDialog.this.setVisible(false);
                }
                else{
                    Util.getMMainJFrame().updateJButton(mackageDescs.length);
                    Util.setUpgradeCount(mackageDescs.length);
                    SwingUtilities.invokeLater( new Runnable(){ public void run(){
                        StoreCheckJDialog.this.jProgressBar.setIndeterminate(false);
                        StoreCheckJDialog.this.jProgressBar.setValue(1);
                        StoreCheckJDialog.this.jProgressBar.setString("Upgrades found.  Please perform upgrades.");
                        proceedJButton.setText("Show Available Upgrades");
                    }});
                }
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error auto checking for upgrades on server", e);
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    StoreCheckJDialog.this.jProgressBar.setIndeterminate(false);
                    StoreCheckJDialog.this.jProgressBar.setString("Upgrades check problem.  Please try again later.");
                }});
                Util.setUpgradeCount(Util.UPGRADE_UNAVAILABLE);
            }

        }
    }

}

