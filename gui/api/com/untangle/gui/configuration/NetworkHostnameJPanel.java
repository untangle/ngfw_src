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

import com.untangle.gui.widgets.dialogs.MConfigJDialog;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.security.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.networking.AddressSettings;
import com.untangle.mvvm.networking.DynamicDNSSettings;
import com.untangle.mvvm.tran.*;

import java.awt.*;
import javax.swing.JDialog;

public class NetworkHostnameJPanel extends javax.swing.JPanel
    implements Savable<NetworkCompoundSettings>, Refreshable<NetworkCompoundSettings> {
    
    private static final String EXCEPTION_HOSTNAME = "Invalid \"Hostname\" specified.";
    private static final String EXCEPTION_NO_LOGIN = "You must provide a login name.";
    private static final String EXCEPTION_NO_PASSWORD = "You must provide a password.";

    public NetworkHostnameJPanel() {
        initComponents();
	for(String provider : DynamicDNSSettings.getProviderEnumeration()){
	    providerJComboBox.addItem(provider);
	}
    }

    public void doSave(NetworkCompoundSettings networkCompoundSettings, boolean validateOnly) throws Exception {

	// HOSTNAME ///////
	hostnameJTextField.setBackground( Color.WHITE );
	HostName hostname = null;
	try{
	    hostname = HostName.parse( hostnameJTextField.getText().trim());
	}
	catch(Exception e){
	    hostnameJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_HOSTNAME);
	}

	// HOSTNAME IS PUBLIC //
	boolean hostnameIsPublic;
	hostnameIsPublic = publicJCheckBox.isSelected();
	
        // DYNAMIC DNS ENABLED //////////
	boolean isDynamicDNSEnabled = enabledJRadioButton.isSelected();

	String provider = null;
	String login = null;
	String password = null;
	loginJTextField.setBackground( Color.WHITE );
	passwordJPasswordField.setBackground( Color.WHITE );
	if( isDynamicDNSEnabled ){
	    // PROVIDER //
	    provider = (String) providerJComboBox.getSelectedItem();
	    
	    // LOGIN //
	    login = loginJTextField.getText();
	    if( login.length() == 0 ){
		loginJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_NO_LOGIN);
	    }

	    // PASSWORD //
	    password = new String(passwordJPasswordField.getPassword());
	    if( password.length() == 0 ){
		passwordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_NO_PASSWORD);
	    }
	}
		
	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    AddressSettings addressSettings = networkCompoundSettings.getAddressSettings();
	    addressSettings.setIsHostNamePublic(hostnameIsPublic);
            addressSettings.setHostName(hostname);
	    
	    DynamicDNSSettings dynamicDNSSettings = networkCompoundSettings.getDynamicDNSSettings();
	    dynamicDNSSettings.setEnabled(isDynamicDNSEnabled);
	    if( isDynamicDNSEnabled ){
		dynamicDNSSettings.setProvider(provider);
		dynamicDNSSettings.setLogin(login);
		dynamicDNSSettings.setPassword(password);
	    }
        }
    }

    
    String hostnameCurrent;
    boolean isHostnamePublicCurrent;
    boolean isDynamicDNSEnabledCurrent;
    String providerCurrent;
    String loginCurrent;
    String passwordCurrent;

    public void doRefresh(NetworkCompoundSettings networkCompoundSettings){

	AddressSettings addressSettings = networkCompoundSettings.getAddressSettings();
			
	// HOSTNAME /////////
	hostnameCurrent = addressSettings.getHostName().toString();
	hostnameJTextField.setText( hostnameCurrent );
	hostnameJTextField.setBackground( Color.WHITE );

	// IS HOSTNAME PUBLIC
	isHostnamePublicCurrent = addressSettings.getIsHostNamePublic();
	publicJCheckBox.setSelected(isHostnamePublicCurrent);
			
        DynamicDNSSettings dynamicDNSSettings = networkCompoundSettings.getDynamicDNSSettings();
        
	// DYNAMIC DNS ENABLED /////
	isDynamicDNSEnabledCurrent = dynamicDNSSettings.isEnabled();
	setEnabledDependency( isDynamicDNSEnabledCurrent );
	if( isDynamicDNSEnabledCurrent )
            enabledJRadioButton.setSelected(true);
        else
            disabledJRadioButton.setSelected(true);

	// PROVIDER //
	providerCurrent = dynamicDNSSettings.getProvider();
	providerJComboBox.setSelectedItem(providerCurrent);

	// LOGIN //
	loginCurrent = dynamicDNSSettings.getLogin();
	loginJTextField.setText(loginCurrent);
	loginJTextField.setBackground( Color.WHITE );

	// PASSWORD //
	passwordCurrent = dynamicDNSSettings.getPassword();
	passwordJPasswordField.setText(passwordCurrent);
	passwordJPasswordField.setBackground( Color.WHITE );
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                dnsButtonGroup = new javax.swing.ButtonGroup();
                hostnameJPanel = new javax.swing.JPanel();
                jLabel10 = new javax.swing.JLabel();
                hostnameJPanel1 = new javax.swing.JPanel();
                hostnameJLabel = new javax.swing.JLabel();
                hostnameJTextField = new javax.swing.JTextField();
                jSeparator4 = new javax.swing.JSeparator();
                jLabel11 = new javax.swing.JLabel();
                publicJCheckBox = new javax.swing.JCheckBox();
                dynamicDNSJPanel = new javax.swing.JPanel();
                jLabel9 = new javax.swing.JLabel();
                disabledJRadioButton = new javax.swing.JRadioButton();
                enabledJRadioButton = new javax.swing.JRadioButton();
                staticIPJPanel = new javax.swing.JPanel();
                providerJLabel = new javax.swing.JLabel();
                providerJComboBox = new javax.swing.JComboBox();
                loginJLabel = new javax.swing.JLabel();
                loginJTextField = new javax.swing.JTextField();
                passwordJLabel = new javax.swing.JLabel();
                passwordJPasswordField = new javax.swing.JPasswordField();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(563, 435));
                setMinimumSize(new java.awt.Dimension(563, 435));
                setPreferredSize(new java.awt.Dimension(563, 435));
                hostnameJPanel.setLayout(new java.awt.GridBagLayout());

                hostnameJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Hostname", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel10.setText("<html>The Hostname is the name that Untangle Server will be known as on your network.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                hostnameJPanel.add(jLabel10, gridBagConstraints);

                hostnameJPanel1.setLayout(new java.awt.GridBagLayout());

                hostnameJPanel1.setMaximumSize(new java.awt.Dimension(350, 23));
                hostnameJPanel1.setMinimumSize(new java.awt.Dimension(350, 23));
                hostnameJPanel1.setPreferredSize(new java.awt.Dimension(350, 23));
                hostnameJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                hostnameJLabel.setText("Hostname:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                hostnameJPanel1.add(hostnameJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                hostnameJPanel1.add(hostnameJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
                hostnameJPanel.add(hostnameJPanel1, gridBagConstraints);

                jSeparator4.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                hostnameJPanel.add(jSeparator4, gridBagConstraints);

                jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel11.setText("<html>If your hostname resolves publicly (the name resolves to an IP address from anywhere on the Internet), then the Untangle Server can make use of its hostname when generating emails, etc.  This will make contacting Untangle easier.  (If you enable Dynamic DNS, then your hostname automatically resolves publicly.)</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                hostnameJPanel.add(jLabel11, gridBagConstraints);

                publicJCheckBox.setText("Hostname resolves publicly");
                publicJCheckBox.setFocusPainted(false);
                publicJCheckBox.setFocusable(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(10, 50, 10, 10);
                hostnameJPanel.add(publicJCheckBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(hostnameJPanel, gridBagConstraints);

                dynamicDNSJPanel.setLayout(new java.awt.GridBagLayout());

                dynamicDNSJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Dynamic DNS", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel9.setText("<html> By using a Dynamic DNS service provider, the Untangle Server can have a specific hostname assigned to a dynamically changing public IP address.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                dynamicDNSJPanel.add(jLabel9, gridBagConstraints);

                dnsButtonGroup.add(disabledJRadioButton);
                disabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                disabledJRadioButton.setText("<html><b>Disabled</b></html>");
                disabledJRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set Untangle's IP address from the network's DHCP server.</html>");
                disabledJRadioButton.setFocusPainted(false);
                disabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                disabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                dynamicDNSJPanel.add(disabledJRadioButton, gridBagConstraints);

                dnsButtonGroup.add(enabledJRadioButton);
                enabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                enabledJRadioButton.setText("<html><b>Enabled</b></html>");
                enabledJRadioButton.setFocusPainted(false);
                enabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                enabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                dynamicDNSJPanel.add(enabledJRadioButton, gridBagConstraints);

                staticIPJPanel.setLayout(new java.awt.GridBagLayout());

                staticIPJPanel.setMaximumSize(new java.awt.Dimension(250, 76));
                staticIPJPanel.setMinimumSize(new java.awt.Dimension(250, 76));
                staticIPJPanel.setPreferredSize(new java.awt.Dimension(250, 76));
                providerJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                providerJLabel.setText("Provider: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(providerJLabel, gridBagConstraints);

                providerJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
                providerJComboBox.setFocusable(false);
                providerJComboBox.setMaximumSize(null);
                providerJComboBox.setMinimumSize(null);
                providerJComboBox.setPreferredSize(null);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                staticIPJPanel.add(providerJComboBox, gridBagConstraints);

                loginJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                loginJLabel.setText("Login: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                staticIPJPanel.add(loginJLabel, gridBagConstraints);

                loginJTextField.setMaximumSize(null);
                loginJTextField.setMinimumSize(null);
                loginJTextField.setPreferredSize(null);
                loginJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                loginJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                staticIPJPanel.add(loginJTextField, gridBagConstraints);

                passwordJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                passwordJLabel.setText("Password: ");
                passwordJLabel.setMaximumSize(null);
                passwordJLabel.setMinimumSize(null);
                passwordJLabel.setPreferredSize(null);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(passwordJLabel, gridBagConstraints);

                passwordJPasswordField.setMaximumSize(null);
                passwordJPasswordField.setMinimumSize(null);
                passwordJPasswordField.setPreferredSize(null);
                passwordJPasswordField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                passwordJPasswordFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                staticIPJPanel.add(passwordJPasswordField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
                dynamicDNSJPanel.add(staticIPJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(dynamicDNSJPanel, gridBagConstraints);

        }//GEN-END:initComponents

		private void passwordJPasswordFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_passwordJPasswordFieldCaretUpdate
				// TODO add your handling code here:
		}//GEN-LAST:event_passwordJPasswordFieldCaretUpdate

		private void loginJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_loginJTextFieldCaretUpdate
				// TODO add your handling code here:
		}//GEN-LAST:event_loginJTextFieldCaretUpdate
                        
	private void enabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enabledJRadioButtonActionPerformed
	    setEnabledDependency( true );
	}//GEN-LAST:event_enabledJRadioButtonActionPerformed
    
	private void disabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disabledJRadioButtonActionPerformed
	    setEnabledDependency( false );
	}//GEN-LAST:event_disabledJRadioButtonActionPerformed
    
    private void setEnabledDependency(boolean enabled){
	providerJLabel.setEnabled(enabled);
	loginJLabel.setEnabled(enabled);
	passwordJLabel.setEnabled(enabled);
	providerJComboBox.setEnabled(enabled);
	loginJTextField.setEnabled(enabled);
	passwordJPasswordField.setEnabled(enabled);
	if(enabled){
	    publicJCheckBox.setSelected(true);
	}
	publicJCheckBox.setEnabled(!enabled);
    }

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        public javax.swing.JRadioButton disabledJRadioButton;
        private javax.swing.ButtonGroup dnsButtonGroup;
        private javax.swing.JPanel dynamicDNSJPanel;
        public javax.swing.JRadioButton enabledJRadioButton;
        private javax.swing.JLabel hostnameJLabel;
        private javax.swing.JPanel hostnameJPanel;
        private javax.swing.JPanel hostnameJPanel1;
        public javax.swing.JTextField hostnameJTextField;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JSeparator jSeparator4;
        private javax.swing.JLabel loginJLabel;
        public javax.swing.JTextField loginJTextField;
        private javax.swing.JLabel passwordJLabel;
        private javax.swing.JPasswordField passwordJPasswordField;
        private javax.swing.JComboBox providerJComboBox;
        private javax.swing.JLabel providerJLabel;
        private javax.swing.JCheckBox publicJCheckBox;
        private javax.swing.JPanel staticIPJPanel;
        // End of variables declaration//GEN-END:variables
    

}
