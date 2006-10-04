/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.configuration;

import com.metavize.mvvm.addrbook.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.mvvm.snmp.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;
import javax.swing.*;

public class DirectoryRemoteADJPanel extends javax.swing.JPanel
    implements Savable<DirectoryCompoundSettings>, Refreshable<DirectoryCompoundSettings> {

    private static final String EXCEPTION_PASSWORD_MISSING = "A \"Password\" must be specified if a \"Login\" is specified.";
    private static final String EXCEPTION_LOGIN_MISSING = "A \"Login\" must be specified if a \"Password\" is specified.";
    private static final String EXCEPTION_HOSTNAME_MISSING = "A \"Hostname\" must be specified if \"Login\" or \"Password\" are specified.";
    private static final String EXCEPTION_DOMAIN_MISSING = "A \"Search Base\" must be specified.";
    
    public DirectoryRemoteADJPanel() {
        initComponents();
	Util.setPortView(portJSpinner, 25);
    }

    public void doSave(DirectoryCompoundSettings directoryCompoundSettings, boolean validateOnly) throws Exception {

	// ENABLED //
	boolean enabled = adEnabledJRadioButton.isSelected();

	// HOSTNAME ///////
	String host = hostJTextField.getText();

	// PORT //////
	int port = 0;
	if( enabled ){
	    ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
	    try{ portJSpinner.commitEdit(); }
	    catch(Exception e){ 
		((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
		throw new Exception(Util.EXCEPTION_PORT_RANGE);
	    }
	    port = (Integer) portJSpinner.getValue();
	}

	// LOGIN /////
	String login = loginJTextField.getText();

	// PASSWORD /////
	String password = new String(passwordJPasswordField.getPassword());

        // DOMAIN /////
        String domain = baseJTextField.getText();

	// ORG //
	String org = orgJTextField.getText();

	if( enabled ){
	    // CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED /////
	    passwordJPasswordField.setBackground( Color.WHITE );
	    loginJTextField.setBackground( Color.WHITE );
	    if( (login.length() > 0) && (password.length() == 0) ){
		passwordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_PASSWORD_MISSING);
	    }
	    else if( (login.length() == 0) && (password.length() > 0) ){
		loginJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_LOGIN_MISSING);
	    }
	    
	    // CHECK THAT IF EITHER LOGIN OR PASSWORD ARE FILLED, A HOSTNAME IS GIVEN
	    hostJTextField.setBackground( Color.WHITE );
	    if( (login.length() > 0) && (password.length() > 0) && (host.length() == 0) ){
		hostJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_HOSTNAME_MISSING);
	    }
	    
	    // CHECK THAT A DOMAIN IS SUPPLIED
	    baseJTextField.setBackground( Color.WHITE );
	    if( domain.length() == 0 ){
		baseJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DOMAIN_MISSING);
	    }
	}

	// SAVE SETTINGS ////////////
	if( !validateOnly ){	    
	    if( enabled ){
		directoryCompoundSettings.setAddressBookConfiguration( AddressBookConfiguration.AD_AND_LOCAL );
		RepositorySettings repositorySettings = directoryCompoundSettings.getAddressBookSettings().getADRepositorySettings();		
		repositorySettings.setSuperuser( login );
		repositorySettings.setSuperuserPass( password );
		repositorySettings.setLDAPHost( host );
		repositorySettings.setLDAPPort( port );
		repositorySettings.setDomain( domain );
		repositorySettings.setOUFilter( org );
	    }
	    else{
		directoryCompoundSettings.setAddressBookConfiguration( AddressBookConfiguration.LOCAL_ONLY );
	    }
        }

    }

    private boolean enabledCurrent;
    private String hostCurrent;
    private int portCurrent;
    private String loginCurrent;
    private String passwordCurrent;
    private String domainCurrent;
    private String orgCurrent;

    public void doRefresh(DirectoryCompoundSettings directoryCompoundSettings){
	RepositorySettings repositorySettings = directoryCompoundSettings.getAddressBookSettings().getADRepositorySettings();
	AddressBookConfiguration addressBookConfiguration = directoryCompoundSettings.getAddressBookConfiguration();

	// ENABLED //
	enabledCurrent = addressBookConfiguration.equals( AddressBookConfiguration.AD_AND_LOCAL );
	if( enabledCurrent )
	    adEnabledJRadioButton.setSelected( true );
	else
	    adDisabledJRadioButton.setSelected( true );
	adEnabledDependency( enabledCurrent );

	// HOST /////
	hostCurrent = repositorySettings.getLDAPHost();
	hostJTextField.setText( hostCurrent );
	hostJTextField.setBackground( Color.WHITE );

	// PORT /////
	portCurrent = repositorySettings.getLDAPPort();
	portJSpinner.setValue( portCurrent );
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setText(Integer.toString(portCurrent));
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);

	// LOGIN //////
	loginCurrent = repositorySettings.getSuperuser();
	loginJTextField.setText( loginCurrent );
	loginJTextField.setBackground( Color.WHITE );

	// PASSWORD /////
	passwordCurrent = repositorySettings.getSuperuserPass();
	passwordJPasswordField.setText( passwordCurrent );
	passwordJPasswordField.setBackground( Color.WHITE );

	// DOMAIN //////
	domainCurrent = repositorySettings.getDomain();
	baseJTextField.setText( domainCurrent );	
	baseJTextField.setBackground( Color.WHITE );

	// ORG //
	orgCurrent = repositorySettings.getOUFilter();
	orgJTextField.setText( orgCurrent );
	orgJTextField.setBackground( Color.WHITE );
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                adButtonGroup = new javax.swing.ButtonGroup();
                externalRemoteJPanel = new javax.swing.JPanel();
                enableRemoteJPanel = new javax.swing.JPanel();
                adDisabledJRadioButton = new javax.swing.JRadioButton();
                adEnabledJRadioButton = new javax.swing.JRadioButton();
                restrictIPJPanel = new javax.swing.JPanel();
                hostJLabel = new javax.swing.JLabel();
                hostJTextField = new javax.swing.JTextField();
                portJLabel = new javax.swing.JLabel();
                portJSpinner = new javax.swing.JSpinner();
                loginJLabel = new javax.swing.JLabel();
                loginJTextField = new javax.swing.JTextField();
                passwordJLabel = new javax.swing.JLabel();
                passwordJPasswordField = new javax.swing.JPasswordField();
                jSeparator2 = new javax.swing.JSeparator();
                restrictIPJPanel1 = new javax.swing.JPanel();
                baseJLabel = new javax.swing.JLabel();
                baseJTextField = new javax.swing.JTextField();
                orgJLabel = new javax.swing.JLabel();
                orgJTextField = new javax.swing.JTextField();
                orgOptionalJLabel = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(563, 243));
                setMinimumSize(new java.awt.Dimension(563, 243));
                setPreferredSize(new java.awt.Dimension(563, 243));
                externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Active Directory (AD) Server", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                enableRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                adButtonGroup.add(adDisabledJRadioButton);
                adDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                adDisabledJRadioButton.setText("<html><b>Disabled</b></html>");
                adDisabledJRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set EdgeGuard's IP address from the network's DHCP server.</html>");
                adDisabledJRadioButton.setFocusPainted(false);
                adDisabledJRadioButton.setFocusable(false);
                adDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                adDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                enableRemoteJPanel.add(adDisabledJRadioButton, gridBagConstraints);

                adButtonGroup.add(adEnabledJRadioButton);
                adEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                adEnabledJRadioButton.setText("<html><b>Enabled</b></html>");
                adEnabledJRadioButton.setFocusPainted(false);
                adEnabledJRadioButton.setFocusable(false);
                adEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                adEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                enableRemoteJPanel.add(adEnabledJRadioButton, gridBagConstraints);

                restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

                hostJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                hostJLabel.setText("Host:");
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

                loginJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                loginJLabel.setText("Authentication Login:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(loginJLabel, gridBagConstraints);

                loginJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                loginJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                loginJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                loginJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                loginJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(loginJTextField, gridBagConstraints);

                passwordJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                passwordJLabel.setText("Authentication Password:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(passwordJLabel, gridBagConstraints);

                passwordJPasswordField.setMaximumSize(new java.awt.Dimension(150, 19));
                passwordJPasswordField.setMinimumSize(new java.awt.Dimension(150, 19));
                passwordJPasswordField.setPreferredSize(new java.awt.Dimension(150, 19));
                passwordJPasswordField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                passwordJPasswordFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(passwordJPasswordField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 114, 5, 0);
                enableRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

                jSeparator2.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                enableRemoteJPanel.add(jSeparator2, gridBagConstraints);

                restrictIPJPanel1.setLayout(new java.awt.GridBagLayout());

                baseJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                baseJLabel.setText("Active Directory Domain:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel1.add(baseJLabel, gridBagConstraints);

                baseJTextField.setMaximumSize(new java.awt.Dimension(200, 19));
                baseJTextField.setMinimumSize(new java.awt.Dimension(200, 19));
                baseJTextField.setPreferredSize(new java.awt.Dimension(200, 19));
                baseJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                baseJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                restrictIPJPanel1.add(baseJTextField, gridBagConstraints);

                orgJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                orgJLabel.setText("Active Directory Organization:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
                restrictIPJPanel1.add(orgJLabel, gridBagConstraints);

                orgJTextField.setMaximumSize(new java.awt.Dimension(200, 19));
                orgJTextField.setMinimumSize(new java.awt.Dimension(200, 19));
                orgJTextField.setPreferredSize(new java.awt.Dimension(200, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
                restrictIPJPanel1.add(orgJTextField, gridBagConstraints);

                orgOptionalJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                orgOptionalJLabel.setText(" (Optional)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
                restrictIPJPanel1.add(orgOptionalJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 87, 5, 0);
                enableRemoteJPanel.add(restrictIPJPanel1, gridBagConstraints);

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

    private void adEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adEnabledJRadioButtonActionPerformed
	adEnabledDependency( true );
    }//GEN-LAST:event_adEnabledJRadioButtonActionPerformed
    
    private void adDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adDisabledJRadioButtonActionPerformed
	adEnabledDependency( false );
    }//GEN-LAST:event_adDisabledJRadioButtonActionPerformed
    
    private void portJSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_portJSpinnerStateChanged
	
    }//GEN-LAST:event_portJSpinnerStateChanged
    
    private void hostJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_hostJTextFieldCaretUpdate
	
    }//GEN-LAST:event_hostJTextFieldCaretUpdate
    
    private void loginJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_loginJTextFieldCaretUpdate
	
    }//GEN-LAST:event_loginJTextFieldCaretUpdate
    
    private void passwordJPasswordFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_passwordJPasswordFieldCaretUpdate
	
    }//GEN-LAST:event_passwordJPasswordFieldCaretUpdate
    
    private void baseJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_baseJTextFieldCaretUpdate

    }//GEN-LAST:event_baseJTextFieldCaretUpdate
    
    private void adEnabledDependency(boolean enabled){
	hostJTextField.setEnabled( enabled );
	hostJLabel.setEnabled( enabled );
	portJSpinner.setEnabled( enabled );
	portJLabel.setEnabled( enabled );
	loginJTextField.setEnabled( enabled );
	loginJLabel.setEnabled( enabled );
	passwordJPasswordField.setEnabled( enabled );
	passwordJLabel.setEnabled( enabled );
	baseJTextField.setEnabled( enabled );
	baseJLabel.setEnabled( enabled );
	orgJTextField.setEnabled( enabled );
	orgJLabel.setEnabled( enabled );
    }
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.ButtonGroup adButtonGroup;
        public javax.swing.JRadioButton adDisabledJRadioButton;
        public javax.swing.JRadioButton adEnabledJRadioButton;
        private javax.swing.JLabel baseJLabel;
        public javax.swing.JTextField baseJTextField;
        private javax.swing.JPanel enableRemoteJPanel;
        private javax.swing.JPanel externalRemoteJPanel;
        private javax.swing.JLabel hostJLabel;
        public javax.swing.JTextField hostJTextField;
        private javax.swing.JSeparator jSeparator2;
        private javax.swing.JLabel loginJLabel;
        public javax.swing.JTextField loginJTextField;
        private javax.swing.JLabel orgJLabel;
        public javax.swing.JTextField orgJTextField;
        private javax.swing.JLabel orgOptionalJLabel;
        private javax.swing.JLabel passwordJLabel;
        private javax.swing.JPasswordField passwordJPasswordField;
        private javax.swing.JLabel portJLabel;
        private javax.swing.JSpinner portJSpinner;
        private javax.swing.JPanel restrictIPJPanel;
        private javax.swing.JPanel restrictIPJPanel1;
        // End of variables declaration//GEN-END:variables
    

}
