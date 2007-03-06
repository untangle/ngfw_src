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

import com.untangle.mvvm.security.RFC2253Name;
import com.untangle.gui.widgets.dialogs.MConfigJDialog;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.Color;
import javax.swing.*;

public class RemoteCertImportTrustedJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    MConfigJDialog mConfigJDialog;
    
    public static RemoteCertImportTrustedJDialog factory(Container topLevelContainer, MConfigJDialog mConfigJDialog){
	RemoteCertImportTrustedJDialog remoteCertImportTrustedJDialog;
	if(topLevelContainer instanceof Frame)
	    remoteCertImportTrustedJDialog = new RemoteCertImportTrustedJDialog((Frame)topLevelContainer, mConfigJDialog);
	else
	    remoteCertImportTrustedJDialog = new RemoteCertImportTrustedJDialog((Dialog)topLevelContainer, mConfigJDialog);
	return remoteCertImportTrustedJDialog;
    }
    
    public RemoteCertImportTrustedJDialog(Dialog topLevelDialog, MConfigJDialog mConfigJDialog) {
        super(topLevelDialog, true);
	init(topLevelDialog, mConfigJDialog);	
    }
    
    public RemoteCertImportTrustedJDialog(Frame topLevelFrame, MConfigJDialog mConfigJDialog) {
        super(topLevelFrame, true);
	init(topLevelFrame, mConfigJDialog);
    }
    
    private void init(Window topLevelWindow, MConfigJDialog mConfigJDialog) {
	this.mConfigJDialog = mConfigJDialog;
        initComponents();
		MConfigJDialog.setInitialFocusComponent(keyJTextArea);
		Util.addFocusHighlight(keyJTextArea);
		Util.addFocusHighlight(intermediateJTextArea);
        this.addWindowListener(this);
        this.setBounds( Util.generateCenteredBounds(topLevelWindow.getBounds(), this.getWidth(), this.getHeight()) );
    }
    
        private void initComponents() {//GEN-BEGIN:initComponents
                buttonGroup1 = new javax.swing.ButtonGroup();
                cancelJButton = new javax.swing.JButton();
                proceedJButton = new javax.swing.JButton();
                labelJLabel = new javax.swing.JLabel();
                messageJLabel = new javax.swing.JLabel();
                jProgressBar = new javax.swing.JProgressBar();
                keyJScrollPane = new javax.swing.JScrollPane();
                keyJTextArea = new javax.swing.JTextArea();
                intermediateJScrollPane = new javax.swing.JScrollPane();
                intermediateJTextArea = new javax.swing.JTextArea();
                message2JLabel = new javax.swing.JLabel();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Certificate Generation");
                setModal(true);
                setResizable(false);
                cancelJButton.setFont(new java.awt.Font("Default", 0, 12));
                cancelJButton.setText("<html><b>Close</b></html>");
                cancelJButton.setDoubleBuffered(true);
                cancelJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                cancelJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                cancelJButton.setMaximumSize(new java.awt.Dimension(130, 25));
                cancelJButton.setMinimumSize(new java.awt.Dimension(130, 25));
                cancelJButton.setPreferredSize(new java.awt.Dimension(130, 25));
                cancelJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                cancelJButtonActionPerformed(evt);
                        }
                });

                getContentPane().add(cancelJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 500, -1, -1));

                proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
                proceedJButton.setText("<html><b>Proceed</b></html>");
                proceedJButton.setDoubleBuffered(true);
                proceedJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                proceedJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                proceedJButton.setMaximumSize(new java.awt.Dimension(150, 25));
                proceedJButton.setMinimumSize(new java.awt.Dimension(150, 25));
                proceedJButton.setPreferredSize(new java.awt.Dimension(150, 25));
                proceedJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                proceedJButtonActionPerformed(evt);
                        }
                });

                getContentPane().add(proceedJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(240, 500, -1, -1));

                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Import Signed Certificate");
                labelJLabel.setDoubleBuffered(true);
                getContentPane().add(labelJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, 456, -1));

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                messageJLabel.setText("<html>When your Certificate Authority (Verisign, Thawte, etc.) has sent your Signed Certificate, copy and paste it below (Control-V), then press the Proceed button.</html>");
                messageJLabel.setDoubleBuffered(true);
                getContentPane().add(messageJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 40, 320, -1));

                jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 20));
                jProgressBar.setMinimumSize(new java.awt.Dimension(10, 20));
                jProgressBar.setPreferredSize(new java.awt.Dimension(148, 20));
                jProgressBar.setString("");
                jProgressBar.setStringPainted(true);
                getContentPane().add(jProgressBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 460, 320, -1));

                keyJScrollPane.setViewportView(keyJTextArea);

                getContentPane().add(keyJScrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 100, 320, 140));

                intermediateJScrollPane.setViewportView(intermediateJTextArea);

                getContentPane().add(intermediateJScrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 300, 320, 140));

                message2JLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                message2JLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                message2JLabel.setText("<html>If your Certificate Authority (Verisign, Thawte, etc.) also send you an Intermediate Certificate, paste it below.  Otherwise, do not paste anything below.</html>");
                message2JLabel.setDoubleBuffered(true);
                getContentPane().add(message2JLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 250, 320, -1));

                backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
                backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
                backgroundJLabel.setDoubleBuffered(true);
                backgroundJLabel.setFocusable(false);
                backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setOpaque(true);
                getContentPane().add(backgroundJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 456, 540));

                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                setBounds((screenSize.width-456)/2, (screenSize.height-562)/2, 456, 562);
        }//GEN-END:initComponents

		
    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
		new ProceedThread();
    }//GEN-LAST:event_proceedJButtonActionPerformed

    String certificateString = null;
    String intermediateString = null;

    private class ProceedThread extends Thread {
	public ProceedThread(){
	    super("MVCLIENT-CertImportThread");
	    setDaemon(true);
	    jProgressBar.setIndeterminate(true);
	    jProgressBar.setString("Importing Certificate");
	    jProgressBar.setValue(0);
	    proceedJButton.setEnabled(false);
	    cancelJButton.setEnabled(false);
	    start();
	}
	public void run(){
	    try{
				
		SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		    certificateString = keyJTextArea.getText();	
		    intermediateString = intermediateJTextArea.getText();
		}});
		
		Thread.sleep(1000);
		boolean success = Util.getAppServerManager().importServerCert(certificateString.getBytes(),
									      (intermediateString.length()==0?null:intermediateString.getBytes()));	
		if( !success )
		    throw new Exception();
		
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    jProgressBar.setIndeterminate(false);
		    jProgressBar.setValue(100);
		    jProgressBar.setString("Certificate Successfully Imported");
		}});
		Thread.sleep(1500);
		RemoteCertImportTrustedJDialog.this.mConfigJDialog.refreshGui();
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    RemoteCertImportTrustedJDialog.this.setVisible(false);
		}});
	    }
	    catch(Exception e){
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    jProgressBar.setIndeterminate(false);
		    jProgressBar.setValue(100);
		    jProgressBar.setString("Error. Please try again.");
		    proceedJButton.setEnabled(true);
		    cancelJButton.setEnabled(true);
		}});
		Util.handleExceptionNoRestart("Error generating self-signed certificate", e);
	    }

	}
	
    }
    

    
    private void cancelJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelJButtonActionPerformed
        windowClosing(null);
    }//GEN-LAST:event_cancelJButtonActionPerformed
    
    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.setVisible(false);
        dispose();
    }    
    
    
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}    
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}    
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}
    
	
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJLabel;
        private javax.swing.ButtonGroup buttonGroup1;
        protected javax.swing.JButton cancelJButton;
        private javax.swing.JScrollPane intermediateJScrollPane;
        private javax.swing.JTextArea intermediateJTextArea;
        private javax.swing.JProgressBar jProgressBar;
        private javax.swing.JScrollPane keyJScrollPane;
        private javax.swing.JTextArea keyJTextArea;
        private javax.swing.JLabel labelJLabel;
        protected javax.swing.JLabel message2JLabel;
        protected javax.swing.JLabel messageJLabel;
        protected javax.swing.JButton proceedJButton;
        // End of variables declaration//GEN-END:variables
    
}
