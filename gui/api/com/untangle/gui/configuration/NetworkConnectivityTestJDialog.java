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

package com.untangle.gui.configuration;

import com.untangle.gui.util.Util;
import com.untangle.mvvm.*;
import com.untangle.mvvm.client.MvvmRemoteContextFactory;

import javax.swing.*;
import java.awt.*;

public class NetworkConnectivityTestJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    private boolean upgradesAvailable = true;
    
    public NetworkConnectivityTestJDialog(Dialog parentDialog){
        super(parentDialog, true);
        init(parentDialog);
    }
    
    public NetworkConnectivityTestJDialog() {
        super(Util.getMMainJFrame(), true);
        init(Util.getMMainJFrame());
    }
    
    private void init(Window window){
        initComponents();
        this.addWindowListener(this);
	this.setBounds( Util.generateCenteredBounds(window.getBounds(), this.getWidth(), this.getHeight()) );
    }
    
    public boolean upgradesAvailable(){
        return upgradesAvailable;
    }
    
    public void setVisible(boolean isVisible){
        if(isVisible){
            new ConnectivityCheckThread();
            super.setVisible(true);
        }
        else{
            super.setVisible(false);
	    dispose();
	}
    }
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                labelJLabel = new javax.swing.JLabel();
                messageJLabel = new javax.swing.JLabel();
                jProgressBar = new javax.swing.JProgressBar();
                proceedJButton = new javax.swing.JButton();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new java.awt.GridBagLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Connectivity Check");
                setModal(true);
                setResizable(false);
                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Checking Connectivity:");
                labelJLabel.setDoubleBuffered(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
                getContentPane().add(labelJLabel, gridBagConstraints);

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                messageJLabel.setText("<html><center> This test can take up to 40 seconds to complete.  This will<br> determine if DNS and TCP connectivity are working.<br> <br> </center></html>");
                messageJLabel.setDoubleBuffered(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.insets = new java.awt.Insets(60, 30, 0, 30);
                getContentPane().add(messageJLabel, gridBagConstraints);

                jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 20));
                jProgressBar.setMinimumSize(new java.awt.Dimension(10, 20));
                jProgressBar.setPreferredSize(new java.awt.Dimension(148, 20));
                jProgressBar.setString("");
                jProgressBar.setStringPainted(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.insets = new java.awt.Insets(0, 30, 50, 30);
                getContentPane().add(jProgressBar, gridBagConstraints);

                proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
                proceedJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/Button_Close_Dialog_106x17.png")));
                proceedJButton.setDoubleBuffered(true);
                proceedJButton.setFocusPainted(false);
                proceedJButton.setFocusable(false);
                proceedJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                proceedJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                proceedJButton.setMaximumSize(new java.awt.Dimension(125, 25));
                proceedJButton.setMinimumSize(new java.awt.Dimension(125, 25));
                proceedJButton.setOpaque(false);
                proceedJButton.setPreferredSize(new java.awt.Dimension(125, 25));
                proceedJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                proceedJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 13, 0);
                getContentPane().add(proceedJButton, gridBagConstraints);

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
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                getContentPane().add(backgroundJLabel, gridBagConstraints);

                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                setBounds((screenSize.width-456)/2, (screenSize.height-222)/2, 456, 222);
        }//GEN-END:initComponents

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
        this.setVisible(false);
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
        private javax.swing.JProgressBar jProgressBar;
        private javax.swing.JLabel labelJLabel;
        private javax.swing.JLabel messageJLabel;
        private javax.swing.JButton proceedJButton;
        // End of variables declaration//GEN-END:variables
    
    private class ConnectivityCheckThread extends Thread {
        public ConnectivityCheckThread(){
	    super("MVCLIENT-ConnectivityCheckThread");
            this.setDaemon(true);
	    this.setContextClassLoader(Util.getClassLoader());
            jProgressBar.setIndeterminate(true);
            jProgressBar.setString("Testing...");
            start();
        }
        public void run() {
            
            ConnectivityTester.Status status = null;
            try{
		int initialTimeout = MvvmRemoteContextFactory.factory().getTimeout();
		MvvmRemoteContextFactory.factory().setTimeout(Util.RECONFIGURE_NETWORK_TIMEOUT_MILLIS);
                status = Util.getMvvmContext().getConnectivityTester().getStatus();		
		MvvmRemoteContextFactory.factory().setTimeout(initialTimeout);
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error running connectivity tester", e);
                
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    NetworkConnectivityTestJDialog.this.jProgressBar.setIndeterminate(false);
                    NetworkConnectivityTestJDialog.this.jProgressBar.setValue(0);
                    NetworkConnectivityTestJDialog.this.jProgressBar.setString("Warning!  Test incomplete for an unknown reason.");
                }});
                return;
            }
            
            try{
                sleep(2000l);
            }
            catch(Exception e){}
            
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                NetworkConnectivityTestJDialog.this.jProgressBar.setIndeterminate(false);
                NetworkConnectivityTestJDialog.this.jProgressBar.setValue(100);
                NetworkConnectivityTestJDialog.this.jProgressBar.setString("Test complete.");
            }});
                
            try{
                sleep(2000l);
            }
            catch(Exception e){}
            
            final String result;
            if( status.isDnsWorking() ){
                if( status.isTcpWorking() ){
                    result = "Success!  Internet and DNS are both working.";
                }
                else{
                    result = "Warning!  DNS is contactable, but the Internet cannot be contacted.";
                }
            }
            else{
                if( status.isTcpWorking() ){
                    result = "Warning!  The Internet is contactable, but DNS cannot be contacted.";
                }
                else{
                    result = "Warning!  The Internet and DNS cannot be contacted.";
                }
            }
            
	    
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                NetworkConnectivityTestJDialog.this.jProgressBar.setValue(1);
                NetworkConnectivityTestJDialog.this.jProgressBar.setString(result);
            }});
        }
    }
    
}

