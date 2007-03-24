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

public class RemoteCertGenTrustedJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    
    public static RemoteCertGenTrustedJDialog factory(Container topLevelContainer){
	RemoteCertGenTrustedJDialog remoteCertGenTrustedJDialog;
	if(topLevelContainer instanceof Frame)
	    remoteCertGenTrustedJDialog = new RemoteCertGenTrustedJDialog((Frame)topLevelContainer);
	else
	    remoteCertGenTrustedJDialog = new RemoteCertGenTrustedJDialog((Dialog)topLevelContainer);
	return remoteCertGenTrustedJDialog;
    }
    
    public RemoteCertGenTrustedJDialog(Dialog topLevelDialog) {
        super(topLevelDialog, true);
	init(topLevelDialog);	
    }
    
    public RemoteCertGenTrustedJDialog(Frame topLevelFrame) {
        super(topLevelFrame, true);
	init(topLevelFrame);
    }
    
    private void init(Window topLevelWindow) {
        initComponents();
		MConfigJDialog.setInitialFocusComponent(keyJTextArea);
		Util.addFocusHighlight(keyJTextArea);
        this.addWindowListener(this);
		pack();
        this.setBounds( Util.generateCenteredBounds(topLevelWindow.getBounds(), this.getWidth(), this.getHeight()) );
		
    }
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                buttonGroup1 = new javax.swing.ButtonGroup();
                iconJLabel = new javax.swing.JLabel();
                dividerJPanel = new javax.swing.JPanel();
                jPanel1 = new javax.swing.JPanel();
                labelJLabel = new javax.swing.JLabel();
                messageJLabel = new javax.swing.JLabel();
                keyJScrollPane = new javax.swing.JScrollPane();
                keyJTextArea = new javax.swing.JTextArea();
                jProgressBar = new javax.swing.JProgressBar();
                jPanel2 = new javax.swing.JPanel();
                cancelJButton = new javax.swing.JButton();
                proceedJButton = new javax.swing.JButton();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new java.awt.GridBagLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("Certificate Generation");
                setModal(true);
                setResizable(false);
                iconJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconDialogQuestion_96x96.png")));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridheight = 2;
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
                gridBagConstraints.gridheight = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(20, 0, 20, 0);
                getContentPane().add(dividerJPanel, gridBagConstraints);

                jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                jPanel1.setOpaque(false);
                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Certificate Signature Request");
                jPanel1.add(labelJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, 456, -1));

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                messageJLabel.setText("<html>Click the Proceed button to generate a signature below.  Copy the signature (Control-C), and paste it into the necessary form from your Certificate Authority (Verisign, Thawte, etc.).</html>");
                jPanel1.add(messageJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 40, 320, -1));

                keyJScrollPane.setViewportView(keyJTextArea);

                jPanel1.add(keyJScrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 120, 320, 160));

                jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
                jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 20));
                jProgressBar.setMinimumSize(new java.awt.Dimension(10, 20));
                jProgressBar.setPreferredSize(new java.awt.Dimension(148, 20));
                jProgressBar.setString("");
                jProgressBar.setStringPainted(true);
                jPanel1.add(jProgressBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 300, 320, -1));

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(20, 20, 20, 20);
                getContentPane().add(jPanel1, gridBagConstraints);

                jPanel2.setLayout(new java.awt.GridBagLayout());

                jPanel2.setMaximumSize(null);
                jPanel2.setMinimumSize(null);
                jPanel2.setOpaque(false);
                jPanel2.setPreferredSize(null);
                cancelJButton.setFont(new java.awt.Font("Default", 0, 12));
                cancelJButton.setText("<html><b>Close</b></html>");
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

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                jPanel2.add(cancelJButton, gridBagConstraints);

                proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
                proceedJButton.setText("<html><b>Proceed</b></html>");
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

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
                jPanel2.add(proceedJButton, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
                getContentPane().add(jPanel2, gridBagConstraints);

                backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
                backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
                backgroundJLabel.setFocusable(false);
                backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setOpaque(true);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.gridwidth = 3;
                gridBagConstraints.gridheight = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                getContentPane().add(backgroundJLabel, gridBagConstraints);

        }//GEN-END:initComponents

		
    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
		new ProceedThread();
    }//GEN-LAST:event_proceedJButtonActionPerformed

    private class ProceedThread extends Thread {
	public ProceedThread(){
	    super("MVCLIENT-CertGenProceedThread");
	    setDaemon(true);
	    jProgressBar.setIndeterminate(true);
	    jProgressBar.setString("Generating Certificate");
	    jProgressBar.setValue(0);
	    proceedJButton.setEnabled(false);
	    cancelJButton.setEnabled(false);
	    start();
	}
	String csrString = null;
	public void run(){
	    try{
		byte[] csr = null;
		csr = Util.getAppServerManager().generateCSR();
		if(csr == null)
		    throw new Exception();
		csrString = new String(csr);
		
		Thread.sleep(1000);
		
		
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    jProgressBar.setIndeterminate(false);
		    jProgressBar.setValue(100);
		    keyJTextArea.setText(csrString);
		    jProgressBar.setString("Certificate Successfully Generated");
		    cancelJButton.setEnabled(true);
		}});
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    keyJScrollPane.getVerticalScrollBar().setValue(0);
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
        private javax.swing.JPanel dividerJPanel;
        private javax.swing.JLabel iconJLabel;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JProgressBar jProgressBar;
        private javax.swing.JScrollPane keyJScrollPane;
        private javax.swing.JTextArea keyJTextArea;
        private javax.swing.JLabel labelJLabel;
        protected javax.swing.JLabel messageJLabel;
        protected javax.swing.JButton proceedJButton;
        // End of variables declaration//GEN-END:variables
    
}
