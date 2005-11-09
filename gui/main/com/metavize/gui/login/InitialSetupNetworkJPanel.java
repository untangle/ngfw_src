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

package com.metavize.gui.login;

import com.metavize.gui.transform.Savable;
import java.util.Arrays;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.gui.util.Util;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;

public class InitialSetupNetworkJPanel extends javax.swing.JPanel implements Savable {
    
    private static final String EXCEPTION_DHCP_IP_ADDRESS = "Invalid \"IP Address\" manually specified.";
    private static final String EXCEPTION_DHCP_NETMASK = "Invalid \"Netmask\" manually specified.";
    private static final String EXCEPTION_DHCP_GATEWAY = "Invalid \"Default Route\" manually specified.";
    private static final String EXCEPTION_DHCP_DNS_1 = "Invalid \"Primary DNS\" maually specified.";
    private static final String EXCEPTION_DHCP_DNS_2 = "Invalid \"Secondary DNS\" manually specified.";
    private static final String EMPTY_DNS2 = "";

    public InitialSetupNetworkJPanel() {
        initComponents();
        setDhcpEnabledDependency(dhcpEnabledRadioButton.isSelected());
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
        String hostname = hostnameJTextField.getText().trim();
        if(hostname.length() == 0)
            throw new Exception("You must fill out the hostname.");
        
        // DHCP ENABLED //////////
	boolean isDhcpEnabled = dhcpEnabledRadioButton.isSelected();

	// DHCP HOST ////////////
	IPaddr host = null;
        if( !isDhcpEnabled ){
            try{
                host = IPaddr.parse( dhcpIPaddrJTextField.getText() );
                if( host.isEmpty() )
                    throw new Exception();
            }
            catch(Exception e){
		throw new Exception(EXCEPTION_DHCP_IP_ADDRESS);
            }
	}

	// DHCP NETMASK /////////
	IPaddr netmask = null;
	if( !isDhcpEnabled ){
            try{
                netmask = IPaddr.parse( dhcpNetmaskJTextField.getText() );
                if( netmask.isEmpty() )
                    throw new Exception();
            } 
            catch(Exception e){
		throw new Exception(EXCEPTION_DHCP_NETMASK);
            }
	}

	// DHCP GATEWAY /////////
	IPaddr gateway = null;
	if( !isDhcpEnabled ){
            try{
                gateway = IPaddr.parse( dhcpRouteJTextField.getText() );
                if( gateway.isEmpty() )
                    throw new Exception();
            }
            catch(Exception e){
		throw new Exception(EXCEPTION_DHCP_GATEWAY);
            }
	}

	// DHCP DNS1 ///////////
	IPaddr dns1 = null;
	if( !isDhcpEnabled ){
            try{
                dns1 = IPaddr.parse( dnsPrimaryJTextField.getText() );
                if( dns1.isEmpty() )
                    throw new Exception();
            }
            catch(Exception e){
		throw new Exception(EXCEPTION_DHCP_DNS_1);
            }
	}

	// DHCP DNS2 ///////
	IPaddr dns2 = null;
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
		throw new Exception(EXCEPTION_DHCP_DNS_2);
            }
        }

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    NetworkingConfiguration networkingConfiguration = Util.getNetworkingManager().get();
	    networkingConfiguration.isDhcpEnabled( isDhcpEnabled );
	    if( !isDhcpEnabled ){
		networkingConfiguration.host( host );
		networkingConfiguration.netmask( netmask );
		networkingConfiguration.gateway( gateway );
		networkingConfiguration.dns1( dns1 );
		networkingConfiguration.dns2( dns2 );
	    }
            Util.getNetworkingManager().set(networkingConfiguration);
        }
        
    }
    

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        hostnameJTextField = new javax.swing.JTextField();
        domainnameJTextField = new javax.swing.JTextField();
        dhcpEnabledRadioButton = new javax.swing.JRadioButton();
        dhcpDisabledRadioButton = new javax.swing.JRadioButton();
        staticIPJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        dhcpIPaddrJTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        dhcpNetmaskJTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        dhcpRouteJTextField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        dnsPrimaryJTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        dnsSecondaryJTextField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please choose a hostname for your EdgeGuard<br>and the domain name it will be operating in.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("Hostname:");
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 90, -1, -1));

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("Domain Name:");
        add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 110, -1, -1));

        hostnameJTextField.setColumns(15);
        hostnameJTextField.setText("mv-edgeguard");
        add(hostnameJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 90, -1, -1));

        domainnameJTextField.setColumns(15);
        add(domainnameJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 110, -1, -1));

        buttonGroup1.add(dhcpEnabledRadioButton);
        dhcpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dhcpEnabledRadioButton.setSelected(true);
        dhcpEnabledRadioButton.setText("<html><b>Automatically Set</b>  EdgeGuard's network settings.</html>");
        dhcpEnabledRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set EdgeGuard's IP address from the network's DHCP server.</html>");
        dhcpEnabledRadioButton.setFocusPainted(false);
        dhcpEnabledRadioButton.setOpaque(false);
        dhcpEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dhcpEnabledRadioButtonActionPerformed(evt);
            }
        });

        add(dhcpEnabledRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 180, -1, -1));

        buttonGroup1.add(dhcpDisabledRadioButton);
        dhcpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dhcpDisabledRadioButton.setText("<html><b>Manually Set</b> EdgeGuard's network settings.</html>");
        dhcpDisabledRadioButton.setFocusPainted(false);
        dhcpDisabledRadioButton.setOpaque(false);
        dhcpDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dhcpDisabledRadioButtonActionPerformed(evt);
            }
        });

        add(dhcpDisabledRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 160, -1, -1));

        staticIPJPanel.setLayout(new java.awt.GridBagLayout());

        staticIPJPanel.setOpaque(false);
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("IP Address:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dhcpIPaddrJTextField, gridBagConstraints);

        jLabel7.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel7.setText("Netmask:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel7, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dhcpNetmaskJTextField, gridBagConstraints);

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("Default Route:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dhcpRouteJTextField, gridBagConstraints);

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("Primary DNS:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dnsPrimaryJTextField, gridBagConstraints);

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel8.setText("Secondary DNS:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel8, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dnsSecondaryJTextField, gridBagConstraints);

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel9.setText(" (Optional)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel9, gridBagConstraints);

        add(staticIPJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 205, 290, 130));

    }//GEN-END:initComponents

    private void dhcpDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpDisabledRadioButtonActionPerformed
        setDhcpEnabledDependency( false );
    }//GEN-LAST:event_dhcpDisabledRadioButtonActionPerformed

    private void dhcpEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpEnabledRadioButtonActionPerformed
        setDhcpEnabledDependency( true );
    }//GEN-LAST:event_dhcpEnabledRadioButtonActionPerformed
    private void setDhcpEnabledDependency(boolean enabled){
        dhcpIPaddrJTextField.setEnabled( !enabled );
        dhcpNetmaskJTextField.setEnabled( !enabled );
        dhcpRouteJTextField.setEnabled( !enabled );
        dnsPrimaryJTextField.setEnabled( !enabled );
        dnsSecondaryJTextField.setEnabled( !enabled );
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JRadioButton dhcpDisabledRadioButton;
    private javax.swing.JRadioButton dhcpEnabledRadioButton;
    private javax.swing.JTextField dhcpIPaddrJTextField;
    private javax.swing.JTextField dhcpNetmaskJTextField;
    private javax.swing.JTextField dhcpRouteJTextField;
    private javax.swing.JTextField dnsPrimaryJTextField;
    private javax.swing.JTextField dnsSecondaryJTextField;
    private javax.swing.JTextField domainnameJTextField;
    private javax.swing.JTextField hostnameJTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel staticIPJPanel;
    // End of variables declaration//GEN-END:variables
    
}
