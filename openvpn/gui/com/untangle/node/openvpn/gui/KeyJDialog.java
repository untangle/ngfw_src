/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.openvpn.gui;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.node.openvpn.*;

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
        message = "<html><center>Please choose how you would like to distribute your digital key.<br>"
            + "Note: If you choose to send via email, you must supply an email<br>"
            + "address to send the email to.  If you choose to download to USB key,<br>"
            + "the data will be located on the key at:<br>"
            + "/untangle-data/openvpn/setup-"
            + vpnClient.getInternalName()
            + ".exe\n</html>";
        initComponents();
        Util.addFocusHighlight(emailJTextField);
        isUsbSelected = usbJRadioButton.isSelected();
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
        emailJRadioButton = new javax.swing.JRadioButton();
        emailJLabel = new javax.swing.JLabel();
        emailJTextField = new javax.swing.JTextField();
        usbJRadioButton = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        cancelJButton = new javax.swing.JButton();
        proceedJButton = new javax.swing.JButton();
        backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("OpenVPN Question...");
        setModal(true);
        setResizable(false);
        iconJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconDialogQuestion_96x96.png")));
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
        labelJLabel.setText("Question:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(labelJLabel, gridBagConstraints);

        messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        messageJLabel.setText(message);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(messageJLabel, gridBagConstraints);

        buttonGroup1.add(emailJRadioButton);
        emailJRadioButton.setSelected(true);
        emailJRadioButton.setText("Distribute via Email");
        emailJRadioButton.setOpaque(false);
        emailJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    emailJRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel1.add(emailJRadioButton, gridBagConstraints);

        emailJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        emailJLabel.setText("Email Address:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel1.add(emailJLabel, gridBagConstraints);

        emailJTextField.setMaximumSize(new java.awt.Dimension(250, 19));
        emailJTextField.setMinimumSize(new java.awt.Dimension(250, 19));
        emailJTextField.setPreferredSize(new java.awt.Dimension(250, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(emailJTextField, gridBagConstraints);

        buttonGroup1.add(usbJRadioButton);
        usbJRadioButton.setText("Distribute via USB Key");
        usbJRadioButton.setOpaque(false);
        usbJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    usbJRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(usbJRadioButton, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setOpaque(false);
        cancelJButton.setFont(new java.awt.Font("Default", 0, 12));
        cancelJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconCancel_16x16.png")));
        cancelJButton.setText("Cancel");
        cancelJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        cancelJButton.setMaximumSize(null);
        cancelJButton.setMinimumSize(null);
        cancelJButton.setPreferredSize(null);
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
        proceedJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconSave_23x16.png")));
        proceedJButton.setText("Proceed");
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
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel2.add(proceedJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(jPanel2, gridBagConstraints);

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
    private javax.swing.JPanel dividerJPanel;
    private javax.swing.JLabel emailJLabel;
    private javax.swing.JRadioButton emailJRadioButton;
    private javax.swing.JTextField emailJTextField;
    private javax.swing.JLabel iconJLabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel labelJLabel;
    protected javax.swing.JLabel messageJLabel;
    protected javax.swing.JButton proceedJButton;
    private javax.swing.JRadioButton usbJRadioButton;
    // End of variables declaration//GEN-END:variables

}
