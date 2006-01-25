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

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;


public class NetworkIPJPanel extends javax.swing.JPanel implements Savable, Refreshable {

    private static final String EXCEPTION_DHCP_IP_ADDRESS = "Invalid \"IP Address\" manually specified.";
    private static final String EXCEPTION_DHCP_NETMASK = "Invalid \"Netmask\" manually specified.";
    private static final String EXCEPTION_DHCP_GATEWAY = "Invalid \"Default Route\" manually specified.";
    private static final String EXCEPTION_DHCP_DNS_1 = "Invalid \"Primary DNS\" maually specified.";
    private static final String EXCEPTION_DHCP_DNS_2 = "Invalid \"Secondary DNS\" manually specified.";
	private static final String EXCEPTION_HOSTNAME = "Invalid \"Hostname\" specified.";
    private static final String EMPTY_DNS2 = "";

    
    public NetworkIPJPanel() {
        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

	// DISABLE BUTTONS
	renewDhcpLeaseJButton.setEnabled(false);
	connectivityTestJButton.setEnabled(false);

        // DHCP ENABLED //////////
	boolean isDhcpEnabled = dhcpEnabledRadioButton.isSelected();

	// DHCP HOST ////////////
	IPaddr host = null;
        dhcpIPaddrJTextField.setBackground( Color.WHITE );
        if( !isDhcpEnabled ){
            try{
                host = IPaddr.parse( dhcpIPaddrJTextField.getText() );
                if( host.isEmpty() )
                    throw new Exception();
            }
            catch(Exception e){
                dhcpIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_IP_ADDRESS);
            }
	}

	// DHCP NETMASK /////////
	IPaddr netmask = null;
        dhcpNetmaskJTextField.setBackground( Color.WHITE );
	if( !isDhcpEnabled ){
            try{
                netmask = IPaddr.parse( dhcpNetmaskJTextField.getText() );
                if( netmask.isEmpty() )
                    throw new Exception();
            } 
            catch(Exception e){
                dhcpNetmaskJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_NETMASK);
            }
	}

	// DHCP GATEWAY /////////
	IPaddr gateway = null;
        dhcpRouteJTextField.setBackground( Color.WHITE );
	if( !isDhcpEnabled ){
            try{
                gateway = IPaddr.parse( dhcpRouteJTextField.getText() );
                if( gateway.isEmpty() )
                    throw new Exception();
            }
            catch(Exception e){
                dhcpRouteJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_GATEWAY);
            }
	}

	// DHCP DNS1 ///////////
	IPaddr dns1 = null;
        dnsPrimaryJTextField.setBackground( Color.WHITE );
	if( !isDhcpEnabled ){
            try{
                dns1 = IPaddr.parse( dnsPrimaryJTextField.getText() );
                if( dns1.isEmpty() )
                    throw new Exception();
            }
            catch(Exception e){
                dnsPrimaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_DNS_1);
            }
	}

	// DHCP DNS2 ///////
	IPaddr dns2 = null;
        dnsSecondaryJTextField.setBackground( Color.WHITE );
	if( !isDhcpEnabled ){
            try{
                String value = dnsSecondaryJTextField.getText().trim();
                if ( value.length() > 0 ) {
                    dns2 = IPaddr.parse( value );
                } else {
                    /* Ignoring empty secondary DNS entry, dns2 = null is okay for network settings */
                }
            }
            catch(Exception e){
                dnsSecondaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_DNS_2);
            }
        }

	// HOSTNAME ///////
	String hostname = null;
        hostnameJTextField.setBackground( Color.WHITE );
	try{
	    hostname = hostnameJTextField.getText().trim();
	}
	catch(Exception e){
	    hostnameJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_HOSTNAME);
	}
	
	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    NetworkingConfiguration networkingConfiguration = (NetworkingConfiguration) settings;
	    networkingConfiguration.isDhcpEnabled( isDhcpEnabled );
	    if( !isDhcpEnabled ){
		networkingConfiguration.host( host );
		networkingConfiguration.netmask( netmask );
		networkingConfiguration.gateway( gateway );
		networkingConfiguration.dns1( dns1 );
		networkingConfiguration.dns2( dns2 );
		networkingConfiguration.hostname( hostname );
	    }
        }

    }

    public void doRefresh(Object settings){
        NetworkingConfiguration networkingConfiguration = (NetworkingConfiguration) settings;
        
	// DHCP ENABLED /////
	boolean isDhcpEnabled = networkingConfiguration.isDhcpEnabled();
	setDhcpEnabledDependency( isDhcpEnabled );
        renewDhcpLeaseJButton.setEnabled( isDhcpEnabled );
	if( isDhcpEnabled )
            dhcpEnabledRadioButton.setSelected(true);
        else
            dhcpDisabledRadioButton.setSelected(true);
        
	// DHCP HOST ////
	dhcpIPaddrJTextField.setText( networkingConfiguration.host().toString() );
	dhcpIPaddrJTextField.setBackground( Color.WHITE );
	
	// DHCP NETMASK /////
        dhcpNetmaskJTextField.setText( networkingConfiguration.netmask().toString() );
	dhcpNetmaskJTextField.setBackground( Color.WHITE );

	// DHCP DEFAULT ROUTE ////////
        dhcpRouteJTextField.setText( networkingConfiguration.gateway().toString() );
	dhcpRouteJTextField.setBackground( Color.WHITE );

	// DNS1 ///////////
        dnsPrimaryJTextField.setText( networkingConfiguration.dns1().toString() );
	dnsPrimaryJTextField.setBackground( Color.WHITE );

	// DNS2 //////////
        if ( networkingConfiguration.hasDns2()) {
            dnsSecondaryJTextField.setText( networkingConfiguration.dns2().toString() );
        } else {
            dnsSecondaryJTextField.setText( EMPTY_DNS2 );
        }
	dnsSecondaryJTextField.setBackground( Color.WHITE );

	// HOSTNAME /////////
	hostnameJTextField.setText( networkingConfiguration.hostname() );
	hostnameJTextField.setBackground( Color.WHITE );

	// ENABLE BUTTONS
	connectivityTestJButton.setEnabled(true); // dhcp lease is take care of above

    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                dhcpButtonGroup = new javax.swing.ButtonGroup();
                tcpWindowButtonGroup = new javax.swing.ButtonGroup();
                externalAdminButtonGroup = new javax.swing.ButtonGroup();
                internalAdminButtonGroup = new javax.swing.ButtonGroup();
                restrictAdminButtonGroup = new javax.swing.ButtonGroup();
                sshButtonGroup = new javax.swing.ButtonGroup();
                dhcpJPanel = new javax.swing.JPanel();
                dhcpEnabledRadioButton = new javax.swing.JRadioButton();
                dhcpDisabledRadioButton = new javax.swing.JRadioButton();
                staticIPJPanel = new javax.swing.JPanel();
                dhcpIPaddrJLabel = new javax.swing.JLabel();
                dhcpIPaddrJTextField = new javax.swing.JTextField();
                dhcpNetmaskJLabel = new javax.swing.JLabel();
                dhcpNetmaskJTextField = new javax.swing.JTextField();
                dhcpRouteJLabel = new javax.swing.JLabel();
                dhcpRouteJTextField = new javax.swing.JTextField();
                dnsPrimaryJLabel = new javax.swing.JLabel();
                dnsPrimaryJTextField = new javax.swing.JTextField();
                dnsSecondaryJLabel = new javax.swing.JLabel();
                dnsSecondaryJTextField = new javax.swing.JTextField();
                optionalJLabel = new javax.swing.JLabel();
                jSeparator2 = new javax.swing.JSeparator();
                hostnameJPanel = new javax.swing.JPanel();
                hostnameJLabel = new javax.swing.JLabel();
                hostnameJTextField = new javax.swing.JTextField();
                jSeparator4 = new javax.swing.JSeparator();
                jLabel9 = new javax.swing.JLabel();
                renewDhcpLeaseJButton = new javax.swing.JButton();
                jSeparator3 = new javax.swing.JSeparator();
                jLabel10 = new javax.swing.JLabel();
                connectivityTestJButton = new javax.swing.JButton();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(563, 482));
                setMinimumSize(new java.awt.Dimension(563, 482));
                setPreferredSize(new java.awt.Dimension(563, 482));
                dhcpJPanel.setLayout(new java.awt.GridBagLayout());

                dhcpJPanel.setBorder(new javax.swing.border.TitledBorder(null, "External IP Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                dhcpButtonGroup.add(dhcpEnabledRadioButton);
                dhcpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                dhcpEnabledRadioButton.setText("<html><b>Automatically Set</b>  EdgeGuard's external IP settings from the network's DHCP server.  The settings are shown in the fields below.</html>");
                dhcpEnabledRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set EdgeGuard's IP address from the network's DHCP server.</html>");
                dhcpEnabledRadioButton.setFocusPainted(false);
                dhcpEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                dhcpEnabledRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                dhcpJPanel.add(dhcpEnabledRadioButton, gridBagConstraints);

                dhcpButtonGroup.add(dhcpDisabledRadioButton);
                dhcpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                dhcpDisabledRadioButton.setText("<html><b>Manually Set</b> EdgeGuard's external IP settings through the fields below.</html>");
                dhcpDisabledRadioButton.setFocusPainted(false);
                dhcpDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                dhcpDisabledRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                dhcpJPanel.add(dhcpDisabledRadioButton, gridBagConstraints);

                staticIPJPanel.setLayout(new java.awt.GridBagLayout());

                dhcpIPaddrJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                dhcpIPaddrJLabel.setText("IP Address:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(dhcpIPaddrJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(dhcpIPaddrJTextField, gridBagConstraints);

                dhcpNetmaskJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                dhcpNetmaskJLabel.setText("Netmask:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(dhcpNetmaskJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(dhcpNetmaskJTextField, gridBagConstraints);

                dhcpRouteJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                dhcpRouteJLabel.setText("Default Route / Gateway:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(dhcpRouteJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(dhcpRouteJTextField, gridBagConstraints);

                dnsPrimaryJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                dnsPrimaryJLabel.setText("Primary DNS:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(dnsPrimaryJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(dnsPrimaryJTextField, gridBagConstraints);

                dnsSecondaryJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                dnsSecondaryJLabel.setText("Secondary DNS:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(dnsSecondaryJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                staticIPJPanel.add(dnsSecondaryJTextField, gridBagConstraints);

                optionalJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                optionalJLabel.setText(" (Optional)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(optionalJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.ipadx = 150;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
                dhcpJPanel.add(staticIPJPanel, gridBagConstraints);

                jSeparator2.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                dhcpJPanel.add(jSeparator2, gridBagConstraints);

                hostnameJPanel.setLayout(new java.awt.GridBagLayout());

                hostnameJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                hostnameJLabel.setText("Hostname:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                hostnameJPanel.add(hostnameJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                hostnameJPanel.add(hostnameJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.ipadx = 150;
                gridBagConstraints.insets = new java.awt.Insets(10, 23, 10, 0);
                dhcpJPanel.add(hostnameJPanel, gridBagConstraints);

                jSeparator4.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                dhcpJPanel.add(jSeparator4, gridBagConstraints);

                jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel9.setText("<html><b>Renew DHCP Lease</b> tells EdgeGuard to request new IP settings from the DHCP server.  This button is enabled only if your saved settings specify that External IP Settings are being Automatically Set from the network's DHCP server.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                dhcpJPanel.add(jLabel9, gridBagConstraints);

                renewDhcpLeaseJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                renewDhcpLeaseJButton.setText("Renew DHCP Lease");
                renewDhcpLeaseJButton.setFocusPainted(false);
                renewDhcpLeaseJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                renewDhcpLeaseJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
                dhcpJPanel.add(renewDhcpLeaseJButton, gridBagConstraints);

                jSeparator3.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                dhcpJPanel.add(jSeparator3, gridBagConstraints);

                jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel10.setText("<html><b>Connectivity Test</b> tells you if EdgeGuard can contact DNS and the internet, using your currently saved settings.  If you have made changes to the above settings, you must save them before this button will be enabled.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                dhcpJPanel.add(jLabel10, gridBagConstraints);

                connectivityTestJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                connectivityTestJButton.setText("Run Connectivity Test");
                connectivityTestJButton.setFocusPainted(false);
                connectivityTestJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                connectivityTestJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
                dhcpJPanel.add(connectivityTestJButton, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(dhcpJPanel, gridBagConstraints);

        }//GEN-END:initComponents

    private void connectivityTestJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectivityTestJButtonActionPerformed
        try{
	    NetworkConnectivityTestJDialog connectivityJDialog = new NetworkConnectivityTestJDialog();
	    connectivityJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error showing connectivity tester", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error showing connectivity tester", f); }
	}
    }//GEN-LAST:event_connectivityTestJButtonActionPerformed

    private void renewDhcpLeaseJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renewDhcpLeaseJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
        NetworkDhcpRenewDialog dhcpLeaseRenewDialog = new NetworkDhcpRenewDialog();
        NetworkingConfiguration newNetworkingConfiguration = dhcpLeaseRenewDialog.getNetworkingConfiguration();
        if( newNetworkingConfiguration != null)
            doRefresh( newNetworkingConfiguration );
    }//GEN-LAST:event_renewDhcpLeaseJButtonActionPerformed

    private void dhcpDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpDisabledRadioButtonActionPerformed
        setDhcpEnabledDependency( false );
    }//GEN-LAST:event_dhcpDisabledRadioButtonActionPerformed

    private void dhcpEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpEnabledRadioButtonActionPerformed
        setDhcpEnabledDependency( true );
    }//GEN-LAST:event_dhcpEnabledRadioButtonActionPerformed
    
    private void setDhcpEnabledDependency(boolean enabled){
        dhcpIPaddrJTextField.setEnabled( !enabled );
		dhcpIPaddrJLabel.setEnabled( !enabled );
        dhcpNetmaskJTextField.setEnabled( !enabled );
		dhcpNetmaskJLabel.setEnabled( !enabled );
        dhcpRouteJTextField.setEnabled( !enabled );
		dhcpRouteJLabel.setEnabled( !enabled );
        dnsPrimaryJTextField.setEnabled( !enabled );
		dnsPrimaryJLabel.setEnabled( !enabled );
        dnsSecondaryJTextField.setEnabled( !enabled );
		dnsSecondaryJLabel.setEnabled( !enabled );
		optionalJLabel.setEnabled( !enabled );
    }

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton connectivityTestJButton;
        private javax.swing.ButtonGroup dhcpButtonGroup;
        public javax.swing.JRadioButton dhcpDisabledRadioButton;
        public javax.swing.JRadioButton dhcpEnabledRadioButton;
        private javax.swing.JLabel dhcpIPaddrJLabel;
        public javax.swing.JTextField dhcpIPaddrJTextField;
        private javax.swing.JPanel dhcpJPanel;
        private javax.swing.JLabel dhcpNetmaskJLabel;
        public javax.swing.JTextField dhcpNetmaskJTextField;
        private javax.swing.JLabel dhcpRouteJLabel;
        public javax.swing.JTextField dhcpRouteJTextField;
        private javax.swing.JLabel dnsPrimaryJLabel;
        public javax.swing.JTextField dnsPrimaryJTextField;
        private javax.swing.JLabel dnsSecondaryJLabel;
        public javax.swing.JTextField dnsSecondaryJTextField;
        private javax.swing.ButtonGroup externalAdminButtonGroup;
        private javax.swing.JLabel hostnameJLabel;
        private javax.swing.JPanel hostnameJPanel;
        public javax.swing.JTextField hostnameJTextField;
        private javax.swing.ButtonGroup internalAdminButtonGroup;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JSeparator jSeparator2;
        private javax.swing.JSeparator jSeparator3;
        private javax.swing.JSeparator jSeparator4;
        private javax.swing.JLabel optionalJLabel;
        private javax.swing.JButton renewDhcpLeaseJButton;
        private javax.swing.ButtonGroup restrictAdminButtonGroup;
        private javax.swing.ButtonGroup sshButtonGroup;
        private javax.swing.JPanel staticIPJPanel;
        private javax.swing.ButtonGroup tcpWindowButtonGroup;
        // End of variables declaration//GEN-END:variables
    

}
