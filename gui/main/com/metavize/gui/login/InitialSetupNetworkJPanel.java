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

import com.metavize.gui.widgets.wizard.*;
import java.util.Arrays;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.gui.util.Util;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;
import javax.swing.SwingUtilities;
import java.awt.Color;

public class InitialSetupNetworkJPanel extends MWizardPageJPanel {

    private static final String EXCEPTION_HOSTNAME_MISSING = "You must fill out the hostname.";
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

    String hostname;
    boolean isDhcpEnabled;
    String hostString;
    IPaddr host;
    String netmaskString;
    IPaddr netmask;
    String gatewayString;
    IPaddr gateway;
    String dns1String;
    IPaddr dns1;
    String dns2String;
    IPaddr dns2;
    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
	    hostnameJTextField.setBackground( Color.WHITE );
	    dhcpIPaddrJTextField.setBackground( Color.WHITE );
	    dhcpNetmaskJTextField.setBackground( Color.WHITE );
	    dhcpRouteJTextField.setBackground( Color.WHITE );
	    dnsPrimaryJTextField.setBackground( Color.WHITE );
	    dnsSecondaryJTextField.setBackground( Color.WHITE );

	    hostname = hostnameJTextField.getText().trim();
	    isDhcpEnabled = dhcpEnabledRadioButton.isSelected();
	    hostString = dhcpIPaddrJTextField.getText();
	    netmaskString = dhcpNetmaskJTextField.getText();
	    gatewayString = dhcpRouteJTextField.getText();
	    dns1String = dnsPrimaryJTextField.getText();
	    dns1String = dnsSecondaryJTextField.getText();

	    exception = null;

	    if(hostname.length() == 0){
		hostnameJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		exception = new Exception(EXCEPTION_HOSTNAME_MISSING);   
		return;
	    }
	    
	    if( !isDhcpEnabled ){
		try{
		    host = IPaddr.parse( hostString );
		    if( host.isEmpty() )
			throw new Exception();
		}
		catch(Exception e){
		    dhcpIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_DHCP_IP_ADDRESS);
		    return;
		}
	    }
	    
	    if( !isDhcpEnabled ){
		try{
		    netmask = IPaddr.parse( netmaskString );
		    if( netmask.isEmpty() )
			throw new Exception();
		} 
		catch(Exception e){
		    dhcpNetmaskJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_DHCP_NETMASK);
		    return;
		}
	    }
	    
	    if( !isDhcpEnabled ){
		try{
		    gateway = IPaddr.parse( gatewayString );
		    if( gateway.isEmpty() )
			throw new Exception();
		}
		catch(Exception e){
		    dhcpRouteJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_DHCP_GATEWAY);
		    return;
		}
	    }
	    
	    if( !isDhcpEnabled ){
		try{
		    dns1 = IPaddr.parse( dns1String );
		    if( dns1.isEmpty() )
			throw new Exception();
		}
		catch(Exception e){
		    dnsPrimaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_DHCP_DNS_1);
		    return;
		}
	    }
	    
	    if( !isDhcpEnabled ){
		try{
		    if ( dns2String.length() > 0 ) {
			dns2 = IPaddr.parse( dns2String );
		    } else {
			/* Ignoring empty secondary DNS entry, dns2 = null is okay for network settings */
		    }
		}
		catch(Exception e){
		    dnsSecondaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		    exception = new Exception(EXCEPTION_DHCP_DNS_2);
		    return;
		}
	    }
	    
	}});

        if( exception != null )
	    throw exception;

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
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please choose a hostname for your EdgeGuard<br>and the domain name it will be operating in.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("Hostname:");
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 50, -1, -1));

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("Domain Name:");
        add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 70, -1, -1));

        hostnameJTextField.setColumns(15);
        hostnameJTextField.setText("mv-edgeguard");
        add(hostnameJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 50, -1, -1));

        domainnameJTextField.setColumns(15);
        add(domainnameJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 70, -1, -1));

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

        add(dhcpEnabledRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 120, -1, -1));

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

        add(dhcpDisabledRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 100, -1, -1));

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

        add(staticIPJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 140, 290, 130));

        jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel10.setText(" (Optional)");
        add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 70, -1, -1));

        jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel11.setText("(required)");
        add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 50, -1, -1));

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
    public javax.swing.JRadioButton dhcpDisabledRadioButton;
    public javax.swing.JRadioButton dhcpEnabledRadioButton;
    public javax.swing.JTextField dhcpIPaddrJTextField;
    public javax.swing.JTextField dhcpNetmaskJTextField;
    public javax.swing.JTextField dhcpRouteJTextField;
    public javax.swing.JTextField dnsPrimaryJTextField;
    public javax.swing.JTextField dnsSecondaryJTextField;
    private javax.swing.JTextField domainnameJTextField;
    private javax.swing.JTextField hostnameJTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
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
