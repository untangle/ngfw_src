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

import java.awt.*;
import javax.swing.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.util.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.snmp.*;
import com.untangle.mvvm.tran.*;

public class EmailOutgoingJPanel extends javax.swing.JPanel
    implements Savable<EmailCompoundSettings>, Refreshable<EmailCompoundSettings> {

    private static final String EXCEPTION_PASSWORD_MISSING = "A \"Password\" must be specified if a \"Login\" is specified.";
    private static final String EXCEPTION_LOGIN_MISSING = "A \"Login\" must be specified if a \"Password\" is specified.";
    private static final String EXCEPTION_HOSTNAME_MISSING = "A \"Hostanme\" must be specified if \"Login\" or \"Password\" are specified.";
    private static final String EXCEPTION_ADDRESS_MISSING = "A \"From Address\" must be specified.";

    public EmailOutgoingJPanel() {
        initComponents();
		Util.setPortView(portJSpinner, 25);
        MConfigJDialog.setInitialFocusComponent(smtpDisabledJRadioButton);
        Util.addPanelFocus(this, smtpDisabledJRadioButton);
        Util.addFocusHighlight(hostJTextField);
        Util.addFocusHighlight(portJSpinner);
        Util.addFocusHighlight(smtpLoginJTextField);
        Util.addFocusHighlight(smtpPasswordJPasswordField);
        Util.addFocusHighlight(addressJTextField);
    }

    public void doSave(EmailCompoundSettings emailCompoundSettings, boolean validateOnly) throws Exception {

    // ENABLED //
    boolean useMxRecords = smtpDisabledJRadioButton.isSelected();

    // HOSTNAME ///////
    String host = hostJTextField.getText();

    // PORT //////
    ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
    int port = 0;
    try{ portJSpinner.commitEdit(); }
    catch(Exception e){
        ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
        throw new Exception(Util.EXCEPTION_PORT_RANGE);
    }
        port = (Integer) portJSpinner.getValue();

    // SMTP LOGIN /////
    String login = smtpLoginJTextField.getText();

    // SMTP PASSWORD /////
    String password = new String(smtpPasswordJPasswordField.getPassword());

        // FROM ADDRESS /////
        String address = addressJTextField.getText();

    // CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED /////
    smtpPasswordJPasswordField.setBackground( Color.WHITE );
    smtpLoginJTextField.setBackground( Color.WHITE );
    if( (login.length() > 0) && (password.length() == 0) ){
        smtpPasswordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
        throw new Exception(EXCEPTION_PASSWORD_MISSING);
    }
    else if( (login.length() == 0) && (password.length() > 0) ){
        smtpLoginJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
        throw new Exception(EXCEPTION_LOGIN_MISSING);
    }

    // CHECK THAT IF EITHER LOGIN OR PASSWORD ARE FILLED, A HOSTNAME IS GIVEN
    hostJTextField.setBackground( Color.WHITE );
    if( (login.length() > 0) && (password.length() > 0) && (host.length() == 0) ){
        hostJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
        throw new Exception(EXCEPTION_HOSTNAME_MISSING);
    }

    // CHECK THAT A FROM ADDRESS IS SUPPLIED
    addressJTextField.setBackground( Color.WHITE );
    if( address.length() == 0 ){
        addressJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
        throw new Exception(EXCEPTION_ADDRESS_MISSING);
    }

    // SAVE SETTINGS ////////////
    if( !validateOnly ){
            MailSettings mailSettings = emailCompoundSettings.getMailSettings();
        mailSettings.setUseMxRecords( useMxRecords );
        if( !useMxRecords ){
        mailSettings.setSmtpHost( host );
        mailSettings.setSmtpPort( port );
        mailSettings.setAuthUser( login );
        mailSettings.setAuthPass( password );
        }
            mailSettings.setFromAddress( address );
        }

    }

    private boolean useMxRecordsCurrent;
    private String hostCurrent;
    private int portCurrent;
    private String loginCurrent;
    private String passwordCurrent;
    private String addressCurrent;

    public void doRefresh(EmailCompoundSettings emailCompoundSettings){
    MailSettings mailSettings = emailCompoundSettings.getMailSettings();

    // ENABLED //
    useMxRecordsCurrent = mailSettings.isUseMxRecords();
    setMxRecordsEnabledDependency( useMxRecordsCurrent );
    if( useMxRecordsCurrent )
        smtpDisabledJRadioButton.setSelected( true );
    else
        smtpEnabledJRadioButton.setSelected( true );

    // HOST /////
    hostCurrent = mailSettings.getSmtpHost();
    hostJTextField.setText( hostCurrent );
    hostJTextField.setBackground( Color.WHITE );

    // PORT /////
    portCurrent = mailSettings.getSmtpPort();
    portJSpinner.setValue( portCurrent );
    ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setText(Integer.toString(portCurrent));
    ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);

    // LOGIN //////
    loginCurrent = mailSettings.getAuthUser();
    smtpLoginJTextField.setText( loginCurrent );
    smtpLoginJTextField.setBackground( Color.WHITE );

    // PASSWORD /////
    passwordCurrent = mailSettings.getAuthPass();
    smtpPasswordJPasswordField.setText( passwordCurrent );
    smtpPasswordJPasswordField.setBackground( Color.WHITE );

    // FROM ADDRESS //////
    addressCurrent = mailSettings.getFromAddress();
    addressJTextField.setText( addressCurrent );
    addressJTextField.setBackground( Color.WHITE );

    // CONNECTIVITY TEST //
    connectivityTestJButton.setEnabled(true);
    }


        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                smtpButtonGroup = new javax.swing.ButtonGroup();
                externalRemoteJPanel = new javax.swing.JPanel();
                jLabel13 = new javax.swing.JLabel();
                smtpDisabledJRadioButton = new javax.swing.JRadioButton();
                smtpEnabledJRadioButton = new javax.swing.JRadioButton();
                restrictIPJPanel = new javax.swing.JPanel();
                hostJLabel = new javax.swing.JLabel();
                hostJTextField = new javax.swing.JTextField();
                portJLabel = new javax.swing.JLabel();
                portJSpinner = new javax.swing.JSpinner();
                smtpLoginJLabel = new javax.swing.JLabel();
                smtpLoginJTextField = new javax.swing.JTextField();
                smtpPasswordJLabel = new javax.swing.JLabel();
                smtpPasswordJPasswordField = new javax.swing.JPasswordField();
                enableRemoteJPanel = new javax.swing.JPanel();
                jSeparator2 = new javax.swing.JSeparator();
                restrictIPJPanel1 = new javax.swing.JPanel();
                jLabel6 = new javax.swing.JLabel();
                addressJTextField = new javax.swing.JTextField();
                jSeparator3 = new javax.swing.JSeparator();
                jLabel10 = new javax.swing.JLabel();
                connectivityTestJButton = new javax.swing.JButton();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(563, 400));
                setMinimumSize(new java.awt.Dimension(563, 400));
                setPreferredSize(new java.awt.Dimension(563, 400));
                externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Outgoing Email Server (SMTP)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel13.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel13.setText("<html>The Outgoing Email Server is used to send emails, such as email reports, etc.  In most cases, automatic settings should work, but if they do not, you should specify the settings manually.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 10);
                externalRemoteJPanel.add(jLabel13, gridBagConstraints);

                smtpButtonGroup.add(smtpDisabledJRadioButton);
                smtpDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                smtpDisabledJRadioButton.setText("<html>Send Email Directly</html>");
                smtpDisabledJRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set Untangle's IP address from the network's DHCP server.</html>");
                smtpDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                smtpDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                externalRemoteJPanel.add(smtpDisabledJRadioButton, gridBagConstraints);

                smtpButtonGroup.add(smtpEnabledJRadioButton);
                smtpEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                smtpEnabledJRadioButton.setText("<html>Send Email using the specified SMTP Server</html>");
                smtpEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                smtpEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                externalRemoteJPanel.add(smtpEnabledJRadioButton, gridBagConstraints);

                restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

                hostJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                hostJLabel.setText("SMTP Email Server:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(hostJLabel, gridBagConstraints);

                hostJTextField.setMaximumSize(new java.awt.Dimension(200, 19));
                hostJTextField.setMinimumSize(new java.awt.Dimension(200, 19));
                hostJTextField.setPreferredSize(new java.awt.Dimension(200, 19));
                hostJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                hostJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(hostJTextField, gridBagConstraints);

                portJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                portJLabel.setText("Port:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(portJLabel, gridBagConstraints);

                portJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
                portJSpinner.setMaximumSize(new java.awt.Dimension(75, 19));
                portJSpinner.setMinimumSize(new java.awt.Dimension(75, 19));
                portJSpinner.setPreferredSize(new java.awt.Dimension(75, 19));
                portJSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                        public void stateChanged(javax.swing.event.ChangeEvent evt) {
                                portJSpinnerStateChanged(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(portJSpinner, gridBagConstraints);

                smtpLoginJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                smtpLoginJLabel.setText("Login (optional):");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(smtpLoginJLabel, gridBagConstraints);

                smtpLoginJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                smtpLoginJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                smtpLoginJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                smtpLoginJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                smtpLoginJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(smtpLoginJTextField, gridBagConstraints);

                smtpPasswordJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                smtpPasswordJLabel.setText("Password (optional):");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(smtpPasswordJLabel, gridBagConstraints);

                smtpPasswordJPasswordField.setMaximumSize(new java.awt.Dimension(150, 19));
                smtpPasswordJPasswordField.setMinimumSize(new java.awt.Dimension(150, 19));
                smtpPasswordJPasswordField.setPreferredSize(new java.awt.Dimension(150, 19));
                smtpPasswordJPasswordField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                smtpPasswordJPasswordFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(smtpPasswordJPasswordField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
                externalRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

                enableRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                jSeparator2.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                enableRemoteJPanel.add(jSeparator2, gridBagConstraints);

                restrictIPJPanel1.setLayout(new java.awt.GridBagLayout());

                jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel6.setText("From Address:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel1.add(jLabel6, gridBagConstraints);

                addressJTextField.setMaximumSize(new java.awt.Dimension(200, 19));
                addressJTextField.setMinimumSize(new java.awt.Dimension(200, 19));
                addressJTextField.setPreferredSize(new java.awt.Dimension(200, 19));
                addressJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                addressJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                restrictIPJPanel1.add(addressJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 79, 5, 0);
                enableRemoteJPanel.add(restrictIPJPanel1, gridBagConstraints);

                jSeparator3.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                enableRemoteJPanel.add(jSeparator3, gridBagConstraints);

                jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel10.setText("<html>The <b>Email Test</b> will try to send an email to a specified recipient, using your currently saved SMTP Email Server settings.  If the email is not received, your settings may be incorrect.  If you have made changes to the above settings, you must save them before this button will be enabled.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                enableRemoteJPanel.add(jLabel10, gridBagConstraints);

                connectivityTestJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                connectivityTestJButton.setText("Run Email Test");
                connectivityTestJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                connectivityTestJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
                enableRemoteJPanel.add(connectivityTestJButton, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
                externalRemoteJPanel.add(enableRemoteJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(externalRemoteJPanel, gridBagConstraints);

        }//GEN-END:initComponents

    private void smtpEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smtpEnabledJRadioButtonActionPerformed
    setMxRecordsEnabledDependency(false);
    }//GEN-LAST:event_smtpEnabledJRadioButtonActionPerformed

    private void smtpDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smtpDisabledJRadioButtonActionPerformed
    setMxRecordsEnabledDependency(true);
    }//GEN-LAST:event_smtpDisabledJRadioButtonActionPerformed

    private void portJSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_portJSpinnerStateChanged
    connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_portJSpinnerStateChanged

    private void hostJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_hostJTextFieldCaretUpdate
    if( !hostJTextField.getText().equals(hostCurrent) )
        connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_hostJTextFieldCaretUpdate

    private void smtpLoginJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_smtpLoginJTextFieldCaretUpdate
    if( !smtpLoginJTextField.getText().equals(loginCurrent) )
        connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_smtpLoginJTextFieldCaretUpdate

    private void smtpPasswordJPasswordFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_smtpPasswordJPasswordFieldCaretUpdate
    if( !(new String(smtpPasswordJPasswordField.getPassword()).equals(passwordCurrent)) )
        connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_smtpPasswordJPasswordFieldCaretUpdate

    private void addressJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_addressJTextFieldCaretUpdate
    if( !addressJTextField.getText().equals(addressCurrent) )
        connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_addressJTextFieldCaretUpdate

    private void connectivityTestJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectivityTestJButtonActionPerformed
    if( Util.getIsDemo() )
        return;
    try{
        EmailConnectivityTestJDialog connectivityJDialog = new EmailConnectivityTestJDialog((JDialog)this.getTopLevelAncestor());
        connectivityJDialog.setVisible(true);
    }
    catch(Exception e){
        try{ Util.handleExceptionWithRestart("Error showing connectivity tester", e); }
        catch(Exception f){ Util.handleExceptionNoRestart("Error showing connectivity tester", f); }
    }
    }//GEN-LAST:event_connectivityTestJButtonActionPerformed

    private void setMxRecordsEnabledDependency(boolean enabled){
    hostJTextField.setEnabled( !enabled );
    hostJLabel.setEnabled( !enabled );
    portJSpinner.setEnabled( !enabled );
    portJLabel.setEnabled( !enabled );
    smtpLoginJTextField.setEnabled( !enabled );
    smtpLoginJLabel.setEnabled( !enabled );
    smtpPasswordJPasswordField.setEnabled( !enabled );
    smtpPasswordJLabel.setEnabled( !enabled );
    }

        // Variables declaration - do not modify//GEN-BEGIN:variables
        public javax.swing.JTextField addressJTextField;
        private javax.swing.JButton connectivityTestJButton;
        private javax.swing.JPanel enableRemoteJPanel;
        private javax.swing.JPanel externalRemoteJPanel;
        private javax.swing.JLabel hostJLabel;
        public javax.swing.JTextField hostJTextField;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel13;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JSeparator jSeparator2;
        private javax.swing.JSeparator jSeparator3;
        private javax.swing.JLabel portJLabel;
        private javax.swing.JSpinner portJSpinner;
        private javax.swing.JPanel restrictIPJPanel;
        private javax.swing.JPanel restrictIPJPanel1;
        private javax.swing.ButtonGroup smtpButtonGroup;
        public javax.swing.JRadioButton smtpDisabledJRadioButton;
        public javax.swing.JRadioButton smtpEnabledJRadioButton;
        private javax.swing.JLabel smtpLoginJLabel;
        public javax.swing.JTextField smtpLoginJTextField;
        private javax.swing.JLabel smtpPasswordJLabel;
        private javax.swing.JPasswordField smtpPasswordJPasswordField;
        // End of variables declaration//GEN-END:variables


}
