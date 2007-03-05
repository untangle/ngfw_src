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

import javax.swing.*;
import java.awt.*;

public class EmailConnectivityTestJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    private boolean upgradesAvailable = true;
    
    public EmailConnectivityTestJDialog(Dialog parentDialog){
        super(parentDialog, true);
	init(parentDialog);
    }
    
    public EmailConnectivityTestJDialog() {
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
    

    
        private void initComponents() {//GEN-BEGIN:initComponents
                labelJLabel = new javax.swing.JLabel();
                messageJLabel = new javax.swing.JLabel();
                jProgressBar = new javax.swing.JProgressBar();
                proceedJButton = new javax.swing.JButton();
                jLabel1 = new javax.swing.JLabel();
                emailAddressJTextField = new javax.swing.JTextField();
                closeJButton = new javax.swing.JButton();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Email Test");
                setModal(true);
                setResizable(false);
                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Email Test:");
                labelJLabel.setDoubleBuffered(true);
                getContentPane().add(labelJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, 369, -1));

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                messageJLabel.setText("<html><center> Enter an email address which you would like to send a test message to, and then press \"Proceed\". </center></html>");
                messageJLabel.setDoubleBuffered(true);
                getContentPane().add(messageJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 60, 309, -1));

                jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 20));
                jProgressBar.setMinimumSize(new java.awt.Dimension(10, 20));
                jProgressBar.setPreferredSize(new java.awt.Dimension(148, 20));
                jProgressBar.setString("");
                jProgressBar.setStringPainted(true);
                getContentPane().add(jProgressBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 134, 309, -1));

                proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
                proceedJButton.setText("<html><b>Proceed</b></html>");
                proceedJButton.setDoubleBuffered(true);
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

                getContentPane().add(proceedJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 162, -1, -1));

                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("Email Address:");
                getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 110, -1, -1));

                getContentPane().add(emailAddressJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 109, 200, -1));

                closeJButton.setFont(new java.awt.Font("Default", 0, 12));
                closeJButton.setText("<html><b>Close</b></html>");
                closeJButton.setDoubleBuffered(true);
                closeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                closeJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                closeJButton.setMaximumSize(new java.awt.Dimension(125, 25));
                closeJButton.setMinimumSize(new java.awt.Dimension(125, 25));
                closeJButton.setOpaque(false);
                closeJButton.setPreferredSize(new java.awt.Dimension(125, 25));
                closeJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                closeJButtonActionPerformed(evt);
                        }
                });

                getContentPane().add(closeJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 162, -1, -1));

                backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
                backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
                backgroundJLabel.setDoubleBuffered(true);
                backgroundJLabel.setFocusable(false);
                backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setOpaque(true);
                getContentPane().add(backgroundJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 369, 200));

                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                setBounds((screenSize.width-369)/2, (screenSize.height-222)/2, 369, 222);
        }//GEN-END:initComponents

		private void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
			this.setVisible(false);
		}//GEN-LAST:event_closeJButtonActionPerformed

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
        new ConnectivityCheckThread();
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
        private javax.swing.JButton closeJButton;
        private javax.swing.JTextField emailAddressJTextField;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JProgressBar jProgressBar;
        private javax.swing.JLabel labelJLabel;
        private javax.swing.JLabel messageJLabel;
        private javax.swing.JButton proceedJButton;
        // End of variables declaration//GEN-END:variables
    
    private class ConnectivityCheckThread extends Thread {
        public ConnectivityCheckThread(){
	    super("MVCLIENT-EmailConnectivityCheckThread");
            this.setDaemon(true);
	    this.setContextClassLoader(Util.getClassLoader());
	    if( emailAddressJTextField.getText().length() == 0){
		jProgressBar.setIndeterminate(false);
		jProgressBar.setValue(0);
		jProgressBar.setString("Error: Please enter an email address");	
		return;
	    }
            jProgressBar.setIndeterminate(true);
	    jProgressBar.setValue(0);
            jProgressBar.setString("Sending...");
	    proceedJButton.setEnabled(false);
            start();
        }
        public void run() {
            
            
            try{
		String recipient = emailAddressJTextField.getText();
		boolean result = Util.getAdminManager().sendTestMessage(recipient);
		if(!result)
		    throw new Exception();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error running connectivity tester", e);
                
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    EmailConnectivityTestJDialog.this.jProgressBar.setIndeterminate(false);
                    EmailConnectivityTestJDialog.this.jProgressBar.setValue(0);
                    EmailConnectivityTestJDialog.this.jProgressBar.setString("Warning!  Test failed.  Check your settings.");
		    EmailConnectivityTestJDialog.this.proceedJButton.setEnabled(true);
                }});
                return;
            }
            
            try{
                sleep(2000l);
            }
            catch(Exception e){}
            
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                EmailConnectivityTestJDialog.this.jProgressBar.setIndeterminate(false);
                EmailConnectivityTestJDialog.this.jProgressBar.setValue(100);
                EmailConnectivityTestJDialog.this.jProgressBar.setString("Test email sent.");
		EmailConnectivityTestJDialog.this.proceedJButton.setEnabled(true);
            }});
	    
            
            
	    
        }
    }
    
}

