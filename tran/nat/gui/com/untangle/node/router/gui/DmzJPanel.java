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

package com.untangle.node.nat.gui;


import java.awt.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.Util;
import com.untangle.uvm.node.IPaddr;
import com.untangle.node.nat.*;


public class DmzJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {

    private static final String EXCEPTION_DMZ_TARGET = "The Target IP Address must be a valid IP address.";

    public DmzJPanel() {
        initComponents();
        Util.addPanelFocus(this, dmzEnabledJRadioButton);
        Util.addFocusHighlight(targetAddressIPaddrJTextField);

    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////


    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // ENABLED ///////////
        boolean dmzEnabled = dmzEnabledJRadioButton.isSelected();

        // INTERNAL ADDRESS //////
        IPaddr dmzTargetAddress = null;
        targetAddressIPaddrJTextField.setBackground( Color.WHITE );
        if(dmzEnabled){
            try{
                dmzTargetAddress = IPaddr.parse( targetAddressIPaddrJTextField.getText() );
            }
            catch(Exception e){
                targetAddressIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                throw new Exception(EXCEPTION_DMZ_TARGET);
            }
        }

        // LOGGING /////////
        boolean dmzLoggingEnabled = dmzLogEnabledJRadioButton.isSelected();

        // SAVE THE VALUES ////////////////////////////////////
        if( !validateOnly ){
            NatBasicSettings natSettings = (NatBasicSettings) settings;
            natSettings.setDmzEnabled( dmzEnabled );
            if(dmzEnabled){
                natSettings.setDmzAddress( dmzTargetAddress );
                natSettings.setDmzLoggingEnabled( dmzLoggingEnabled );
            }
        }

    }

    boolean dmzEnabledCurrent;
    String dmzTargetAddressCurrent;
    boolean dmzLoggingEnabledCurrent;

    public void doRefresh(Object settings) {

        NatBasicSettings natSettings = (NatBasicSettings) settings;

        // ENABLED ///////////
        dmzEnabledCurrent = natSettings.getDmzEnabled();
        this.setDmzEnabledDependency(dmzEnabledCurrent);
        if( dmzEnabledCurrent )
            dmzEnabledJRadioButton.setSelected(true);
        else
            dmzDisabledJRadioButton.setSelected(true);

        // TARGET ADDRESS //////
        dmzTargetAddressCurrent = natSettings.getDmzAddress().toString();
        targetAddressIPaddrJTextField.setText( dmzTargetAddressCurrent );
        targetAddressIPaddrJTextField.setBackground( Color.WHITE );

        // LOGGING ///////////
        dmzLoggingEnabledCurrent = natSettings.getDmzLoggingEnabled();
        if( dmzLoggingEnabledCurrent )
            dmzLogEnabledJRadioButton.setSelected(true);
        else
            dmzLogDisabledJRadioButton.setSelected(true);
    }




    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        enabledButtonGroup = new javax.swing.ButtonGroup();
        logButtonGroup = new javax.swing.ButtonGroup();
        explanationJPanel = new javax.swing.JPanel();
        jTextArea2 = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        dmzEnabledJRadioButton = new javax.swing.JRadioButton();
        dmzDisabledJRadioButton = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        externalRemoteJPanel = new javax.swing.JPanel();
        restrictIPJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        targetAddressIPaddrJTextField = new javax.swing.JTextField();
        jTextArea3 = new javax.swing.JTextArea();
        logRemoteJPanel = new javax.swing.JPanel();
        jTextArea4 = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        dmzLogEnabledJRadioButton = new javax.swing.JRadioButton();
        dmzLogDisabledJRadioButton = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setMinimumSize(new java.awt.Dimension(530, 430));
        setPreferredSize(new java.awt.Dimension(530, 430));
        explanationJPanel.setLayout(new java.awt.GridBagLayout());

        explanationJPanel.setBorder(new javax.swing.border.TitledBorder(null, "DMZ Host", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea2.setEditable(false);
        jTextArea2.setLineWrap(true);
        jTextArea2.setText("DMZ Host allows you to direct all incoming traffic to a specific computer on your internal network.  This is typically used for web servers or other servers which must be accessible from outside your secured internal network.  (Note:  Redirect takes precedence over DMZ Host)");
        jTextArea2.setWrapStyleWord(true);
        jTextArea2.setFocusable(false);
        jTextArea2.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        explanationJPanel.add(jTextArea2, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        enabledButtonGroup.add(dmzEnabledJRadioButton);
        dmzEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dmzEnabledJRadioButton.setText("Enabled");
        dmzEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    dmzEnabledJRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(dmzEnabledJRadioButton, gridBagConstraints);

        enabledButtonGroup.add(dmzDisabledJRadioButton);
        dmzDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dmzDisabledJRadioButton.setText("Disabled");
        dmzDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    dmzDisabledJRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(dmzDisabledJRadioButton, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("DMZ Host");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        explanationJPanel.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(explanationJPanel, gridBagConstraints);

        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Target Address", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("Target IP Address: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel5, gridBagConstraints);

        targetAddressIPaddrJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        targetAddressIPaddrJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        targetAddressIPaddrJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        targetAddressIPaddrJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                public void caretUpdate(javax.swing.event.CaretEvent evt) {
                    targetAddressIPaddrJTextFieldCaretUpdate(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel.add(targetAddressIPaddrJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 125;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        externalRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

        jTextArea3.setEditable(false);
        jTextArea3.setLineWrap(true);
        jTextArea3.setText("The target address is the address of the computer inside your network that will receive all incoming traffic when DMZ Host is enabled.");
        jTextArea3.setWrapStyleWord(true);
        jTextArea3.setFocusable(false);
        jTextArea3.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        externalRemoteJPanel.add(jTextArea3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(externalRemoteJPanel, gridBagConstraints);

        logRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        logRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "DMZ Host Logging", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea4.setEditable(false);
        jTextArea4.setLineWrap(true);
        jTextArea4.setText("If DMZ Host Logging is enabled, then all traffic inbound to the DMZ Host will be logged.  This information will then appear in the Event Log as well as in Untangle Reports.");
        jTextArea4.setWrapStyleWord(true);
        jTextArea4.setFocusable(false);
        jTextArea4.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        logRemoteJPanel.add(jTextArea4, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        logButtonGroup.add(dmzLogEnabledJRadioButton);
        dmzLogEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dmzLogEnabledJRadioButton.setText("Enabled");
        dmzLogEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    dmzLogEnabledJRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(dmzLogEnabledJRadioButton, gridBagConstraints);

        logButtonGroup.add(dmzLogDisabledJRadioButton);
        dmzLogDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dmzLogDisabledJRadioButton.setText("Disabled");
        dmzLogDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    dmzLogDisabledJRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(dmzLogDisabledJRadioButton, gridBagConstraints);

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("DMZ Host Logging");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        logRemoteJPanel.add(jPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(logRemoteJPanel, gridBagConstraints);

    }//GEN-END:initComponents


    private void dmzLogDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dmzLogDisabledJRadioButtonActionPerformed
        if( dmzLoggingEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_dmzLogDisabledJRadioButtonActionPerformed

    private void dmzLogEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dmzLogEnabledJRadioButtonActionPerformed
        if( !dmzLoggingEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_dmzLogEnabledJRadioButtonActionPerformed

    private void targetAddressIPaddrJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_targetAddressIPaddrJTextFieldCaretUpdate
        if( !targetAddressIPaddrJTextField.getText().trim().equals(dmzTargetAddressCurrent) && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_targetAddressIPaddrJTextFieldCaretUpdate

    private void dmzDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dmzDisabledJRadioButtonActionPerformed
        this.setDmzEnabledDependency(false);
        if( dmzEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_dmzDisabledJRadioButtonActionPerformed

    private void dmzEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dmzEnabledJRadioButtonActionPerformed
        this.setDmzEnabledDependency(true);
        if( !dmzEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_dmzEnabledJRadioButtonActionPerformed

    private void setDmzEnabledDependency(boolean enabled){
        targetAddressIPaddrJTextField.setEnabled(enabled);
        dmzLogEnabledJRadioButton.setEnabled(enabled);
        dmzLogDisabledJRadioButton.setEnabled(enabled);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JRadioButton dmzDisabledJRadioButton;
    public javax.swing.JRadioButton dmzEnabledJRadioButton;
    public javax.swing.JRadioButton dmzLogDisabledJRadioButton;
    public javax.swing.JRadioButton dmzLogEnabledJRadioButton;
    private javax.swing.ButtonGroup enabledButtonGroup;
    private javax.swing.JPanel explanationJPanel;
    private javax.swing.JPanel externalRemoteJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextArea jTextArea4;
    private javax.swing.ButtonGroup logButtonGroup;
    private javax.swing.JPanel logRemoteJPanel;
    private javax.swing.JPanel restrictIPJPanel;
    public javax.swing.JTextField targetAddressIPaddrJTextField;
    // End of variables declaration//GEN-END:variables

}
