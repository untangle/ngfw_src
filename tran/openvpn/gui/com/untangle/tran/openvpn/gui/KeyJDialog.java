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

package com.untangle.tran.openvpn.gui;

import com.untangle.tran.openvpn.*;

import com.untangle.gui.util.Util;
import com.untangle.mvvm.toolbox.ToolboxManager;
import com.untangle.gui.widgets.dialogs.*;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Window;

public class KeyJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    private VpnClientBase vpnClient;

    private boolean isProceeding = false;
    private boolean isUsbSelected;
    private String emailAddress;

    private String message;
    
    public static KeyJDialog factory(Container topLevelContainer, VpnClientBase vpnClient){
	KeyJDialog keyJDialog;
	if(topLevelContainer instanceof Frame)
	    keyJDialog = new KeyJDialog((Frame)topLevelContainer, vpnClient);
	else
	    keyJDialog = new KeyJDialog((Dialog)topLevelContainer, vpnClient);
	return keyJDialog;
    }
    
    public KeyJDialog(Dialog topLevelDialog, VpnClientBase vpnClient) {
        super( topLevelDialog, true);
	init( topLevelDialog, vpnClient );
	
    }
    
    public KeyJDialog(Frame topLevelFrame, VpnClientBase vpnClient) {
        super( topLevelFrame, true);
	init( topLevelFrame, vpnClient );
    }
    
    private void init(Window topLevelWindow, VpnClientBase vpnClient) {
	this.vpnClient = vpnClient;
	message = "<html>\nPlease choose how you would like to distribute your digital key.  "
	    + "Note: If you choose to send via email, you must supply an email address to send the email to.  "
	    + "If you choose to download to USB key, the data will be located on the key at: /untangle-data/openvpn/setup-"
	    + vpnClient.getInternalName()
	    + ".exe\n</html>";
        initComponents();
		Util.addFocusHighlight(emailJTextField);
	isUsbSelected = usbJRadioButton.isSelected();
        this.addWindowListener(this);
        this.setBounds( Util.generateCenteredBounds(topLevelWindow.getBounds(), this.getWidth(), this.getHeight()) );
    }
    
        private void initComponents() {//GEN-BEGIN:initComponents
                buttonGroup1 = new javax.swing.ButtonGroup();
                cancelJButton = new javax.swing.JButton();
                proceedJButton = new javax.swing.JButton();
                messageJLabel = new javax.swing.JLabel();
                labelJLabel = new javax.swing.JLabel();
                emailJRadioButton = new javax.swing.JRadioButton();
                emailJLabel = new javax.swing.JLabel();
                emailJTextField = new javax.swing.JTextField();
                usbJRadioButton = new javax.swing.JRadioButton();
                backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

                getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                setTitle("OpenVPN Question...");
                setModal(true);
                setResizable(false);
                cancelJButton.setFont(new java.awt.Font("Default", 0, 12));
                cancelJButton.setText("<html><b>Cancel</b></html>");
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

                getContentPane().add(cancelJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(73, 295, -1, -1));

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

                getContentPane().add(proceedJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(243, 295, -1, -1));

                messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                messageJLabel.setText(message);
                getContentPane().add(messageJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 40, 396, -1));

                labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
                labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                labelJLabel.setText("Question:");
                getContentPane().add(labelJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, 456, -1));

                buttonGroup1.add(emailJRadioButton);
                emailJRadioButton.setSelected(true);
                emailJRadioButton.setText("Distribute via Email");
                emailJRadioButton.setOpaque(false);
                emailJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                emailJRadioButtonActionPerformed(evt);
                        }
                });

                getContentPane().add(emailJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 140, -1, -1));

                emailJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                emailJLabel.setText("Email Address:");
                getContentPane().add(emailJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 170, -1, -1));

                getContentPane().add(emailJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 169, 150, -1));

                buttonGroup1.add(usbJRadioButton);
                usbJRadioButton.setText("Distribute via USB Key");
                usbJRadioButton.setOpaque(false);
                usbJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                usbJRadioButtonActionPerformed(evt);
                        }
                });

                getContentPane().add(usbJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 210, -1, -1));

                backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
                backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
                backgroundJLabel.setFocusable(false);
                backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                backgroundJLabel.setOpaque(true);
                getContentPane().add(backgroundJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 456, 333));

                java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                setBounds((screenSize.width-456)/2, (screenSize.height-355)/2, 456, 355);
        }//GEN-END:initComponents

		private void usbJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usbJRadioButtonActionPerformed
				emailJLabel.setEnabled(false);
				emailJTextField.setEnabled(false);
				isUsbSelected = true;
		}//GEN-LAST:event_usbJRadioButtonActionPerformed

		private void emailJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_emailJRadioButtonActionPerformed
				emailJLabel.setEnabled(true);
				emailJTextField.setEnabled(true);
				isUsbSelected = false;
		}//GEN-LAST:event_emailJRadioButtonActionPerformed

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
        if( !isUsbSelected && emailJTextField.getText().trim().length() == 0) {
				emailJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
				MOneButtonJDialog.factory(this, "OpenVPN", "You must specify an email address to send the key to.", "OpenVPN Warning", "Warning");
				return;
		}
		emailAddress = emailJTextField.getText();
		isProceeding = true;
        windowClosing(null);
    }//GEN-LAST:event_proceedJButtonActionPerformed

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
    
    public boolean isProceeding(){
        return isProceeding;
    }
	
	public boolean isUsbSelected(){ return isUsbSelected; }
	public boolean isEmailSelected(){ return !isUsbSelected; }
	public String getEmailAddress(){ return emailAddress; }
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJLabel;
        private javax.swing.ButtonGroup buttonGroup1;
        protected javax.swing.JButton cancelJButton;
        private javax.swing.JLabel emailJLabel;
        private javax.swing.JRadioButton emailJRadioButton;
        private javax.swing.JTextField emailJTextField;
        private javax.swing.JLabel labelJLabel;
        protected javax.swing.JLabel messageJLabel;
        protected javax.swing.JButton proceedJButton;
        private javax.swing.JRadioButton usbJRadioButton;
        // End of variables declaration//GEN-END:variables
    
}
