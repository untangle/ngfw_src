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

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.security.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;

import com.untangle.mvvm.networking.BasicNetworkSettings;

import java.awt.*;
import javax.swing.JDialog;

public class NetworkIPJPanel extends javax.swing.JPanel
    implements Savable<NetworkCompoundSettings>, Refreshable<NetworkCompoundSettings> {

    private static final String EXCEPTION_DHCP_IP_ADDRESS = "Invalid \"IP Address\" manually specified.";
    private static final String EXCEPTION_DHCP_NETMASK = "Invalid \"Netmask\" manually specified.";
    private static final String EXCEPTION_DHCP_GATEWAY = "Invalid \"Default Route\" manually specified.";
    private static final String EXCEPTION_DHCP_DNS_1 = "Invalid \"Primary DNS\" maually specified.";
    private static final String EXCEPTION_DHCP_DNS_2 = "Invalid \"Secondary DNS\" manually specified.";
    private static final String EMPTY_DNS2 = "";

    private MConfigJDialog mConfigJDialog;

    public NetworkIPJPanel(MConfigJDialog mConfigJDialog) {
        initComponents();
        MConfigJDialog.setInitialFocusComponent(dhcpEnabledRadioButton);
        Util.addPanelFocus(this, dhcpEnabledRadioButton);
        Util.addFocusHighlight(dhcpIPaddrJTextField);
        Util.addFocusHighlight(dhcpNetmaskJTextField);
        Util.addFocusHighlight(dhcpRouteJTextField);
        Util.addFocusHighlight(dnsPrimaryJTextField);
        Util.addFocusHighlight(dnsSecondaryJTextField);
        this.mConfigJDialog = mConfigJDialog;
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
	this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////
	
    public void doSave(NetworkCompoundSettings networkCompoundSettings, boolean validateOnly) throws Exception {

        // DHCP ENABLED //////////
	boolean isDhcpEnabled = dhcpEnabledRadioButton.isSelected();

	// DHCP HOST ////////////
        dhcpIPaddrJTextField.setBackground( Color.WHITE );
	IPaddr host = null;
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
        dhcpNetmaskJTextField.setBackground( Color.WHITE );
	IPaddr netmask = null;
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
        dhcpRouteJTextField.setBackground( Color.WHITE );
	IPaddr gateway = null;
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
        dnsPrimaryJTextField.setBackground( Color.WHITE );
	IPaddr dns1 = null;
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
        dnsSecondaryJTextField.setBackground( Color.WHITE );	
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
                dnsSecondaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_DNS_2);
            }
        }
	
	// SAVE SETTINGS ////////////
	if( !validateOnly ){	    
	    BasicNetworkSettings basicSettings = networkCompoundSettings.getBasicSettings();
	    basicSettings.isDhcpEnabled( isDhcpEnabled );
	    if( !isDhcpEnabled ){
		basicSettings.host( host );
		basicSettings.netmask( netmask );
		basicSettings.gateway( gateway );
		basicSettings.dns1( dns1 );
		basicSettings.dns2( dns2 );
	    }
        }
    }


    boolean isDhcpEnabledCurrent;
    String dhcpIPaddrCurrent;
    String dhcpNetmaskCurrent;
    String dhcpRouteCurrent;
    String dnsPrimaryCurrent;
    String dnsSecondaryCurrent;

    public void doRefresh(NetworkCompoundSettings networkCompoundSettings){
        BasicNetworkSettings basicSettings = networkCompoundSettings.getBasicSettings();
        
	// DHCP ENABLED /////
	isDhcpEnabledCurrent = basicSettings.isDhcpEnabled();
	setDhcpEnabledDependency( isDhcpEnabledCurrent );

        /* rbs: only enable the dhcp renew button if dhcp is enabled and pppoe is disabled */
        renewDhcpLeaseJButton.setEnabled( isDhcpEnabledCurrent && !basicSettings.getPPPoESettings().isLive());
	if( isDhcpEnabledCurrent )
            dhcpEnabledRadioButton.setSelected(true);
        else
            dhcpDisabledRadioButton.setSelected(true);
		Util.addSettingChangeListener(settingsChangedListener, this, dhcpEnabledRadioButton);
		Util.addSettingChangeListener(settingsChangedListener, this, dhcpDisabledRadioButton);
        
	// DHCP HOST ////
	dhcpIPaddrCurrent = basicSettings.host().toString();
	dhcpIPaddrJTextField.setText( dhcpIPaddrCurrent );
	dhcpIPaddrJTextField.setBackground( Color.WHITE );
	Util.addSettingChangeListener(settingsChangedListener, this, dhcpIPaddrJTextField);
	
	// DHCP NETMASK /////
	dhcpNetmaskCurrent = basicSettings.netmask().toString();
        dhcpNetmaskJTextField.setText( dhcpNetmaskCurrent );
	dhcpNetmaskJTextField.setBackground( Color.WHITE );
	Util.addSettingChangeListener(settingsChangedListener, this, dhcpNetmaskJTextField);

	// DHCP DEFAULT ROUTE ////////
	dhcpRouteCurrent = basicSettings.gateway().toString();
        dhcpRouteJTextField.setText( dhcpRouteCurrent );
	dhcpRouteJTextField.setBackground( Color.WHITE );
	Util.addSettingChangeListener(settingsChangedListener, this, dhcpRouteJTextField);

	// DNS1 ///////////
	dnsPrimaryCurrent = basicSettings.dns1().toString();
        dnsPrimaryJTextField.setText( dnsPrimaryCurrent );
	dnsPrimaryJTextField.setBackground( Color.WHITE );
	Util.addSettingChangeListener(settingsChangedListener, this, dnsPrimaryJTextField);

	// DNS2 //////////
        if ( basicSettings.hasDns2()) {
	    dnsSecondaryCurrent = basicSettings.dns2().toString();
            dnsSecondaryJTextField.setText( dnsSecondaryCurrent );
        } else {
	    dnsSecondaryCurrent = "";
            dnsSecondaryJTextField.setText( EMPTY_DNS2 );
        }
	dnsSecondaryJTextField.setBackground( Color.WHITE );
	Util.addSettingChangeListener(settingsChangedListener, this, dnsSecondaryJTextField);
	
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
                jLabel11 = new javax.swing.JLabel();
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
                jSeparator4 = new javax.swing.JSeparator();
                jLabel9 = new javax.swing.JLabel();
                renewDhcpLeaseJButton = new javax.swing.JButton();
                jSeparator3 = new javax.swing.JSeparator();
                jLabel10 = new javax.swing.JLabel();
                connectivityTestJButton = new javax.swing.JButton();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(563, 470));
                setMinimumSize(new java.awt.Dimension(563, 470));
                setPreferredSize(new java.awt.Dimension(563, 470));
                dhcpJPanel.setLayout(new java.awt.GridBagLayout());

                dhcpJPanel.setBorder(new javax.swing.border.TitledBorder(null, "External IP Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel11.setText("<html>The External IP Settings are used to configure the \"External\" network interface to communicate with the Internet or some other external network.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 10);
                dhcpJPanel.add(jLabel11, gridBagConstraints);

                dhcpButtonGroup.add(dhcpEnabledRadioButton);
                dhcpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                dhcpEnabledRadioButton.setText("<html><b>Automatically Set</b>  using the network's DHCP server.</html>");
                dhcpEnabledRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set Untangle's IP address from the network's DHCP server.</html>");
                dhcpEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                dhcpEnabledRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                dhcpJPanel.add(dhcpEnabledRadioButton, gridBagConstraints);

                dhcpButtonGroup.add(dhcpDisabledRadioButton);
                dhcpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                dhcpDisabledRadioButton.setText("<html><b>Manually Set</b> using  the fields below.</html>");
                dhcpDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                dhcpDisabledRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
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

                jSeparator4.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                dhcpJPanel.add(jSeparator4, gridBagConstraints);

                jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel9.setText("<html><b>Renew DHCP Lease</b> requests a new IP settings from the DHCP server.  This button is enabled only if your saved settings specify that External IP Settings are being Automatically Set from the network's DHCP server.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                dhcpJPanel.add(jLabel9, gridBagConstraints);

                renewDhcpLeaseJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                renewDhcpLeaseJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconAction_32x32.png")));
                renewDhcpLeaseJButton.setText("Renew DHCP Lease");
                renewDhcpLeaseJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
                renewDhcpLeaseJButton.setMaximumSize(null);
                renewDhcpLeaseJButton.setMinimumSize(null);
                renewDhcpLeaseJButton.setPreferredSize(null);
                renewDhcpLeaseJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                renewDhcpLeaseJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
                dhcpJPanel.add(renewDhcpLeaseJButton, gridBagConstraints);

                jSeparator3.setForeground(new java.awt.Color(200, 200, 200));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                dhcpJPanel.add(jSeparator3, gridBagConstraints);

                jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel10.setText("<html>The <b>Connectivity Test</b> tells you if the Untangle Server can contact DNS and the internet.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                dhcpJPanel.add(jLabel10, gridBagConstraints);

                connectivityTestJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                connectivityTestJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconTest_16x16.png")));
                connectivityTestJButton.setText("Connectivity Test");
                connectivityTestJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
                connectivityTestJButton.setMaximumSize(null);
                connectivityTestJButton.setMinimumSize(null);
                connectivityTestJButton.setPreferredSize(null);
                connectivityTestJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                connectivityTestJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
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
    
    private void dnsSecondaryJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_dnsSecondaryJTextFieldCaretUpdate
	if( !dnsSecondaryJTextField.getText().trim().equals(dnsSecondaryCurrent) );
    }//GEN-LAST:event_dnsSecondaryJTextFieldCaretUpdate
    
    private void dnsPrimaryJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_dnsPrimaryJTextFieldCaretUpdate
	if( !dnsPrimaryJTextField.getText().trim().equals(dnsPrimaryCurrent) );
    }//GEN-LAST:event_dnsPrimaryJTextFieldCaretUpdate
    
    private void dhcpRouteJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_dhcpRouteJTextFieldCaretUpdate
	if( !dhcpRouteJTextField.getText().trim().equals(dhcpRouteCurrent) );
    }//GEN-LAST:event_dhcpRouteJTextFieldCaretUpdate
    
    private void dhcpNetmaskJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_dhcpNetmaskJTextFieldCaretUpdate
	if( !dhcpNetmaskJTextField.getText().trim().equals(dhcpNetmaskCurrent) );
    }//GEN-LAST:event_dhcpNetmaskJTextFieldCaretUpdate
    
    private void dhcpIPaddrJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_dhcpIPaddrJTextFieldCaretUpdate
	if( !dhcpIPaddrJTextField.getText().trim().equals(dhcpIPaddrCurrent) );
    }//GEN-LAST:event_dhcpIPaddrJTextFieldCaretUpdate
    
	private class TestThread extends Thread {
				public TestThread(){
						setName("MVCLIENT-TestThread");
						setDaemon(true);
						start();
				}
				public void run(){
						if( ((MConfigJDialog)NetworkIPJPanel.this.getTopLevelAncestor()).getSettingsChanged() ){
							TestSaveSettingsJDialog dialog = new TestSaveSettingsJDialog((JDialog)NetworkIPJPanel.this.getTopLevelAncestor());
							if(!dialog.isProceeding())
								return;

                            if( !((MConfigJDialog)NetworkIPJPanel.this.getTopLevelAncestor()).saveSettings() )
                                return;
						}						
					try{
						NetworkConnectivityTestJDialog connectivityJDialog = new NetworkConnectivityTestJDialog((JDialog)NetworkIPJPanel.this.getTopLevelAncestor());
						connectivityJDialog.setVisible(true);
					}
					catch(Exception e){
						try{ Util.handleExceptionWithRestart("Error showing connectivity tester", e); }
						catch(Exception f){ Util.handleExceptionNoRestart("Error showing connectivity tester", f); }
					}	
				}
		}
	
    private void connectivityTestJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectivityTestJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
	
	new TestThread();
    }//GEN-LAST:event_connectivityTestJButtonActionPerformed

    private void renewDhcpLeaseJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renewDhcpLeaseJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
        MTwoButtonJDialog warning = MTwoButtonJDialog.factory((Window)this.getTopLevelAncestor(), "Networking Config",
                                                              "Renewing your DHCP lease may cause you to get disconnected from Untangle.<br>"
                                                              + "You can login again if you get disconnected.",
                                                              "Networking Config Warning", "Warning");
        warning.setVisible(true);
        if( warning.isProceeding() ){
            NetworkDhcpRenewDialog dhcpLeaseRenewDialog = new NetworkDhcpRenewDialog((JDialog)this.getTopLevelAncestor());
            BasicNetworkSettings basicSettings = dhcpLeaseRenewDialog.getBasicSettings();
            if( basicSettings != null){
                mConfigJDialog.refreshGui();
                // UPDATE STORE
                Util.getPolicyStateMachine().updateStoreModel();
            }
        }
        else
            return;
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
        private javax.swing.ButtonGroup internalAdminButtonGroup;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel9;
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
