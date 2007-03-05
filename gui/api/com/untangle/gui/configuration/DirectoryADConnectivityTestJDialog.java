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
import com.untangle.mvvm.addrbook.*;

import javax.swing.*;
import java.awt.*;

public class DirectoryADConnectivityTestJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    private boolean upgradesAvailable = true;
    
    public DirectoryADConnectivityTestJDialog(Dialog parentDialog){
        super(parentDialog, true);
        init(parentDialog);
    }
    
    public DirectoryADConnectivityTestJDialog() {
        super(Util.getMMainJFrame(), true);
        init(Util.getMMainJFrame());
    }

    private void init(Window window){
        initComponents();
        setTitle("Active Directory Test");
        this.addWindowListener(this);
        this.setBounds( Util.generateCenteredBounds(window.getBounds(), this.getWidth(), this.getHeight()) );
    }
   
    public boolean upgradesAvailable(){
        return upgradesAvailable;
    }
    
    public void setVisible(boolean v){
        if(v)
            new ConnectivityCheckThread();
        super.setVisible(v);
    }
    
        private void initComponents() {//GEN-BEGIN:initComponents
                labelJLabel = new javax.swing.JLabel();
                jProgressBar = new javax.swing.JProgressBar();
                closeJButton = new javax.swing.JButton();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Email Test");
                setModal(true);
                setResizable(false);
                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Active Directory Test:");
                labelJLabel.setDoubleBuffered(true);
                getContentPane().add(labelJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, 369, -1));

                jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 20));
                jProgressBar.setMinimumSize(new java.awt.Dimension(10, 20));
                jProgressBar.setPreferredSize(new java.awt.Dimension(148, 20));
                jProgressBar.setString("");
                jProgressBar.setStringPainted(true);
                getContentPane().add(jProgressBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, 309, -1));

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

                getContentPane().add(closeJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 162, -1, -1));

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
        private javax.swing.JProgressBar jProgressBar;
        private javax.swing.JLabel labelJLabel;
        // End of variables declaration//GEN-END:variables
    
    private class ConnectivityCheckThread extends Thread {
        public ConnectivityCheckThread(){
	    super("MVCLIENT-ADConnectivityCheckThread");
            this.setDaemon(true);
            this.setContextClassLoader(Util.getClassLoader());
            jProgressBar.setIndeterminate(true);
            jProgressBar.setValue(1);
            jProgressBar.setString("Testing...");
            start();
        }
        public void run() {
            String message;
            String output;
            try{
                message = (Util.getAddressBook().getStatus().isADWorking()?"Success!  Your settings work.":"Failure!  Your settings are not correct.");
                output = Util.getAddressBook().getStatus().adDetail();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error running connectivity tester", e);
                
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    DirectoryADConnectivityTestJDialog.this.jProgressBar.setIndeterminate(false);
                    DirectoryADConnectivityTestJDialog.this.jProgressBar.setValue(1);
                    DirectoryADConnectivityTestJDialog.this.jProgressBar.setString("Warning!  Test failed.  Check your settings.");
                }});
                return;
            }
            
            try{
                sleep(2000l);
            }
            catch(Exception e){}
            
            final String finalMessage = message;
            final String finalOutput = output;
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                DirectoryADConnectivityTestJDialog.this.jProgressBar.setIndeterminate(false);
                DirectoryADConnectivityTestJDialog.this.jProgressBar.setValue(100);
                DirectoryADConnectivityTestJDialog.this.jProgressBar.setString(finalMessage);
                //DirectoryADConnectivityTestJDialog.this.resultJEditorPane.setText(finalOutput);                
            }});
	    
            
            
	    
        }
    }
    
}

