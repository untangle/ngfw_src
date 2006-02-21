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

import com.metavize.gui.widgets.dialogs.MConfigJDialog;
import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.networking.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;
import javax.swing.JDialog;

public class NetworkDynamicDNSJPanel extends javax.swing.JPanel
    implements Savable<NetworkCompoundSettings>, Refreshable<NetworkCompoundSettings> {

    private static final String EXCEPTION_NO_LOGIN = "You must provide a login name.";
    private static final String EXCEPTION_NO_PASSWORD = "You must provide a password.";

    public NetworkDynamicDNSJPanel() {
        initComponents();
	for(String provider : DynamicDNSSettings.getProviderEnumeration()){
	    providerJComboBox.addItem(provider);
	}
    }

    public void doSave(NetworkCompoundSettings networkCompoundSettings, boolean validateOnly) throws Exception {

        // DYNAMIC DNS ENABLED //////////
	boolean isDynamicDNSEnabled = enabledJRadioButton.isSelected();

	String provider = null;
	String login = null;
	String password = null;
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
	    DynamicDNSSettings dynamicDNSSettings = networkCompoundSettings.getDynamicDNSSettings();
	    dynamicDNSSettings.setEnabled(isDynamicDNSEnabled);
	    if( isDynamicDNSEnabled ){
		dynamicDNSSettings.setProvider(provider);
		dynamicDNSSettings.setLogin(login);
		dynamicDNSSettings.setPassword(password);
	    }
        }
    }


    boolean isDynamicDNSEnabledCurrent;
    String providerCurrent;
    String loginCurrent;
    String passwordCurrent;

    public void doRefresh(NetworkCompoundSettings networkCompoundSettings){
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
                dhcpJPanel = new javax.swing.JPanel();
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

                setMaximumSize(new java.awt.Dimension(563, 195));
                setMinimumSize(new java.awt.Dimension(563, 195));
                setPreferredSize(new java.awt.Dimension(563, 195));
                dhcpJPanel.setLayout(new java.awt.GridBagLayout());

                dhcpJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Dynamic DNS", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel9.setText("<html> By using a Dynamic DNS service provider, EdgeGuard can have a specific hostname assigned to a dynamically changing public IP address.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                dhcpJPanel.add(jLabel9, gridBagConstraints);

                dnsButtonGroup.add(disabledJRadioButton);
                disabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                disabledJRadioButton.setText("<html><b>Disabled</b></html>");
                disabledJRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set EdgeGuard's IP address from the network's DHCP server.</html>");
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
                dhcpJPanel.add(disabledJRadioButton, gridBagConstraints);

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
                dhcpJPanel.add(enabledJRadioButton, gridBagConstraints);

                staticIPJPanel.setLayout(new java.awt.GridBagLayout());

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
                gridBagConstraints.ipadx = 150;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
                dhcpJPanel.add(staticIPJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(dhcpJPanel, gridBagConstraints);

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
    }

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel dhcpJPanel;
        public javax.swing.JRadioButton disabledJRadioButton;
        private javax.swing.ButtonGroup dnsButtonGroup;
        public javax.swing.JRadioButton enabledJRadioButton;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JLabel loginJLabel;
        private javax.swing.JTextField loginJTextField;
        private javax.swing.JLabel passwordJLabel;
        private javax.swing.JPasswordField passwordJPasswordField;
        private javax.swing.JComboBox providerJComboBox;
        private javax.swing.JLabel providerJLabel;
        private javax.swing.JPanel staticIPJPanel;
        // End of variables declaration//GEN-END:variables
    

}
