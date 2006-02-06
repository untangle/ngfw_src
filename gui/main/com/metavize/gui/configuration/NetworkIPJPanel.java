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
import com.metavize.mvvm.networking.BasicNetworkSettings;
import com.metavize.mvvm.networking.NetworkSpace;
import com.metavize.mvvm.networking.IPNetwork;
import com.metavize.mvvm.networking.IPNetworkRule;
import com.metavize.mvvm.networking.DhcpStatus;


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
	    BasicNetworkSettings networkSettings = (BasicNetworkSettings) settings;
            
            networkSettings.setIsDhcpEnabled( isDhcpEnabled );
            
            if ( isDhcpEnabled ) {
                networkSettings.setPrimaryAddress( IPNetwork.getEmptyNetwork());
                networkSettings.setDefaultRoute( null );
                networkSettings.setDns1( null );
                networkSettings.setDns2( null );
            } else {
                networkSettings.setPrimaryAddress( IPNetwork.makeIPNetwork( host, netmask ));
                networkSettings.setDefaultRoute( gateway );
                networkSettings.setDns1( dns1 );
                networkSettings.setDns2( dns2 );
            }
        }

    }


    boolean isDhcpEnabledCurrent;
    String dhcpIPaddrCurrent;
    String dhcpNetmaskCurrent;
    String dhcpRouteCurrent;
    String dnsPrimaryCurrent;
    String dnsSecondaryCurrent;
    String hostnameCurrent;
    public void doRefresh(Object settings){
        BasicNetworkSettings networkSettings = (BasicNetworkSettings) settings;

        DhcpStatus dhcpStatus = networkSettings.getDhcpStatus();
        
	// DHCP ENABLED /////
	isDhcpEnabledCurrent = networkSettings.getIsDhcpEnabled();
	setDhcpEnabledDependency( isDhcpEnabledCurrent );
        renewDhcpLeaseJButton.setEnabled( isDhcpEnabledCurrent );
	if( isDhcpEnabledCurrent ) {
            dhcpEnabledRadioButton.setSelected(true);
            dhcpIPaddrCurrent  = dhcpStatus.getAddress().toString();
            dhcpNetmaskCurrent = dhcpStatus.getNetmask().toString();
            dhcpRouteCurrent   = dhcpStatus.getDefaultRoute().toString();
            dnsPrimaryCurrent  = dhcpStatus.getDns1().toString();
            if ( dhcpStatus.hasDns2()) dnsSecondaryCurrent = dhcpStatus.getDns2().toString();
            else dnsSecondaryCurrent = EMPTY_DNS2;    
        } else {
            IPNetwork network = networkSettings.getPrimaryAddress();

            dhcpDisabledRadioButton.setSelected(true);
            dhcpIPaddrCurrent  = network.getNetwork().toString();
            dhcpNetmaskCurrent = network.getNetmask().toString();
            dhcpRouteCurrent   = networkSettings.getDefaultRoute().toString();
            dnsPrimaryCurrent  = networkSettings.getDns1().toString();
            if ( networkSettings.hasDns2()) dnsSecondaryCurrent = networkSettings.getDns2().toString();
            else dnsSecondaryCurrent = EMPTY_DNS2;    
        }
        
	// DHCP HOST ////
	dhcpIPaddrJTextField.setText( dhcpIPaddrCurrent );
	dhcpIPaddrJTextField.setBackground( Color.WHITE );
	
	// DHCP NETMASK /////
        dhcpNetmaskJTextField.setText( dhcpNetmaskCurrent );
	dhcpNetmaskJTextField.setBackground( Color.WHITE );

	// DHCP DEFAULT ROUTE ////////
        dhcpRouteJTextField.setText( dhcpRouteCurrent );
	dhcpRouteJTextField.setBackground( Color.WHITE );

	// DNS1 ///////////
        dnsPrimaryJTextField.setText( dnsPrimaryCurrent );
	dnsPrimaryJTextField.setBackground( Color.WHITE );

	// DNS2 //////////
        dnsSecondaryJTextField.setText( dnsSecondaryCurrent );
        dnsSecondaryJTextField.setText( EMPTY_DNS2 );
	dnsSecondaryJTextField.setBackground( Color.WHITE );

	// HOSTNAME /////////
	hostnameCurrent = networkSettings.getHostname();
	hostnameJTextField.setText( hostnameCurrent );
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

                dhcpIPaddrJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                dhcpIPaddrJTextFieldCaretUpdate(evt);
                        }
                });

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

                dhcpNetmaskJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                dhcpNetmaskJTextFieldCaretUpdate(evt);
                        }
                });

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

                dhcpRouteJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                dhcpRouteJTextFieldCaretUpdate(evt);
                        }
                });

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

                dnsPrimaryJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                dnsPrimaryJTextFieldCaretUpdate(evt);
                        }
                });

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

                dnsSecondaryJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                dnsSecondaryJTextFieldCaretUpdate(evt);
                        }
                });

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

                hostnameJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                hostnameJTextFieldCaretUpdate(evt);
                        }
                });

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

    private void hostnameJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_hostnameJTextFieldCaretUpdate
	if( !hostnameJTextField.getText().trim().equals(hostnameCurrent) )
	    connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_hostnameJTextFieldCaretUpdate
    
    private void dnsSecondaryJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_dnsSecondaryJTextFieldCaretUpdate
	if( !dnsSecondaryJTextField.getText().trim().equals(dnsSecondaryCurrent) )
	    connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_dnsSecondaryJTextFieldCaretUpdate
    
    private void dnsPrimaryJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_dnsPrimaryJTextFieldCaretUpdate
	if( !dnsPrimaryJTextField.getText().trim().equals(dnsPrimaryCurrent) )
	    connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_dnsPrimaryJTextFieldCaretUpdate
    
    private void dhcpRouteJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_dhcpRouteJTextFieldCaretUpdate
	if( !dhcpRouteJTextField.getText().trim().equals(dhcpRouteCurrent) )
	    connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_dhcpRouteJTextFieldCaretUpdate
    
    private void dhcpNetmaskJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_dhcpNetmaskJTextFieldCaretUpdate
	if( !dhcpNetmaskJTextField.getText().trim().equals(dhcpNetmaskCurrent) )
	    connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_dhcpNetmaskJTextFieldCaretUpdate
    
    private void dhcpIPaddrJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_dhcpIPaddrJTextFieldCaretUpdate
	if( !dhcpIPaddrJTextField.getText().trim().equals(dhcpIPaddrCurrent) )
	    connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_dhcpIPaddrJTextFieldCaretUpdate
    
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
        BasicNetworkSettings newNetworkSettings = dhcpLeaseRenewDialog.getNetworkSettings();
        if( newNetworkSettings != null)
            doRefresh( newNetworkSettings );
    }//GEN-LAST:event_renewDhcpLeaseJButtonActionPerformed

    private void dhcpDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpDisabledRadioButtonActionPerformed
        setDhcpEnabledDependency( false );
	connectivityTestJButton.setEnabled(false);
    }//GEN-LAST:event_dhcpDisabledRadioButtonActionPerformed
    
    private void dhcpEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpEnabledRadioButtonActionPerformed
        setDhcpEnabledDependency( true );
	connectivityTestJButton.setEnabled(false);
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
