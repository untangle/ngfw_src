/*
 * NetworkJPanel.java
 *
 * Created on February 22, 2005, 1:10 PM
 */

package com.metavize.gui.configuration;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;

/**
 *
 * @author  inieves
 */
public class NetworkJPanel extends javax.swing.JPanel implements Savable, Refreshable {

    private static final String EXCEPTION_DHCP_IP_ADDRESS = "Invalid DHCP \"IP Address\" specified.";
    private static final String EXCEPTION_DHCP_NETMASK = "Invalid DHCP \"Netmask\" specified.";
    private static final String EXCEPTION_DHCP_GATEWAY = "Invalid DHCP \"Default Route\" specified.";
    private static final String EXCEPTION_DHCP_DNS_1 = "Invalid DHCP \"Primary DNS\" specified.";
    private static final String EXCEPTION_DHCP_DNS_2 = "Invalid DHCP \"Secondary DNS\" specified.";
    private static final String EXCEPTION_OUTSIDE_ACCESS_NETWORK = "Invalid Remote Administration \"IP Address\" specified.";
    private static final String EXCEPTION_OUTSIDE_ACCESS_NETMASK = "Invalid Remote Administration \"Netmask\" specified.";

    
    public NetworkJPanel() {
        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // DHCP ENABLED //////////
	boolean isDhcpEnabled = dhcpEnabledRadioButton.isSelected();

	// DHCP HOST ////////////
	IPaddr host = null;
        if( !isDhcpEnabled ){
            try{
                host = IPaddr.parse( dhcpIPaddrJTextField.getText() );
                if( host.isEmpty() )
                    throw new Exception();
                dhcpIPaddrJTextField.setBackground( Color.WHITE );
            }
            catch(Exception e){
                dhcpIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_IP_ADDRESS);
            }
	}
	else{
	    dhcpIPaddrJTextField.setBackground( Color.WHITE );
	}

	// DHCP NETMASK /////////
	IPaddr netmask = null;
	if( !isDhcpEnabled ){
            try{
                netmask = IPaddr.parse( dhcpNetmaskJTextField.getText() );
                if( netmask.isEmpty() )
                    throw new Exception();
                dhcpNetmaskJTextField.setBackground( Color.WHITE );
            } 
            catch(Exception e){
                dhcpNetmaskJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_NETMASK);
            }
	}
	else{
	    dhcpNetmaskJTextField.setBackground( Color.WHITE );
	}

	// DHCP GATEWAY /////////
	IPaddr gateway = null;
	if( !isDhcpEnabled ){
            try{
                gateway = IPaddr.parse( dhcpRouteJTextField.getText() );
                if( gateway.isEmpty() )
                    throw new Exception();
                dhcpRouteJTextField.setBackground( Color.WHITE );
            }
            catch(Exception e){
                dhcpRouteJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_GATEWAY);
            }
	}
	else{
	    dhcpRouteJTextField.setBackground( Color.WHITE );
	}

	// DHCP DNS1 ///////////
	IPaddr dns1 = null;
	if( !isDhcpEnabled ){
            try{
                dns1 = IPaddr.parse( dnsPrimaryJTextField.getText() );
                if( dns1.isEmpty() )
                    throw new Exception();
                dnsPrimaryJTextField.setBackground( Color.WHITE );
            }
            catch(Exception e){
                dnsPrimaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_DNS_1);
            }
	}
	else{
	    dnsPrimaryJTextField.setBackground( Color.WHITE );
	}

	// DHCP DNS2 ///////
	IPaddr dns2 = null;
	if( !isDhcpEnabled ){
            try{
                dns2 = IPaddr.parse( dnsSecondaryJTextField.getText() );
                dnsSecondaryJTextField.setBackground( Color.WHITE );
            }
            catch(Exception e){
                dnsSecondaryJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_DHCP_DNS_2);
            }
        }
	else{
	    dnsSecondaryJTextField.setBackground( Color.WHITE );
	}

	// OUTSIDE ACCESS ENABLED ////////
	boolean isOutsideAccessEnabled = externalAdminEnabledRadioButton.isSelected();

	// OUTSIDE ACCESS RESTRICTED ///////
	boolean isOutsideAccessRestricted = externalAdminRestrictEnabledRadioButton.isSelected();

	// OUTSIDE ACCESS RESTRICTION ADDRESS /////////
	IPaddr outsideNetwork = null;
	if( isOutsideAccessEnabled && isOutsideAccessRestricted ){
	    try{
		outsideNetwork = IPaddr.parse( restrictIPaddrJTextField.getText() );
		if( outsideNetwork.isEmpty() )
		    throw new Exception();
	    }
	    catch(Exception e){
                restrictIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_OUTSIDE_ACCESS_NETWORK);
            }
        }
	else{
	    restrictIPaddrJTextField.setBackground( Color.WHITE );
	}

	// OUTSIDE ACCESS RESTRICTION NETMASK /////////
	IPaddr outsideNetmask = null;
	if( isOutsideAccessEnabled && isOutsideAccessRestricted ){	    
	    try{
		outsideNetmask = IPaddr.parse( restrictNetmaskJTextField.getText() );
	    }
	    catch ( Exception e ) {
		restrictNetmaskJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_OUTSIDE_ACCESS_NETMASK);
            }
	}
	else{
	    restrictNetmaskJTextField.setBackground( Color.WHITE );
	}
	
        // SSH ENABLED ////////
	boolean isSshEnabled = sshEnabledRadioButton.isSelected();

	// INSIDE INSECURE ENABLED //////
	boolean isInsideInsecureEnabled = internalAdminEnabledRadioButton.isSelected();

	// TCP WINDOW SCALING ENABLED //////
	boolean isTcpWindowScalingEnabled = tcpWindowEnabledRadioButton.isSelected();

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
	    }
	    networkingConfiguration.isOutsideAccessEnabled( isOutsideAccessEnabled );
	    if( isOutsideAccessEnabled ){
		networkingConfiguration.isOutsideAccessRestricted( isOutsideAccessRestricted );
		if( isOutsideAccessRestricted ){
		    networkingConfiguration.outsideNetwork( outsideNetwork );
		    networkingConfiguration.outsideNetmask( outsideNetmask );
		}
	    }
	    networkingConfiguration.isSshEnabled( isSshEnabled );
	    networkingConfiguration.isInsideInsecureEnabled( isInsideInsecureEnabled );
	    networkingConfiguration.isTcpWindowScalingEnabled( isTcpWindowScalingEnabled );
        }

    }

    public void doRefresh(Object settings){
        NetworkingConfiguration networkingConfiguration = (NetworkingConfiguration) settings;
                
	// DHCP ENABLED /////
	boolean isDhcpEnabled = networkingConfiguration.isDhcpEnabled();
	setDhcpEnabledDependency( isDhcpEnabled );
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
        dnsSecondaryJTextField.setText( networkingConfiguration.dns2().toString() );
	dnsSecondaryJTextField.setBackground( Color.WHITE );

	// OUTSIDE ACCESS ENABLED //////
	boolean isOutsideAccessEnabled = networkingConfiguration.isOutsideAccessEnabled();
	setOutsideAccessEnabledDependency( isOutsideAccessEnabled );
	if( isOutsideAccessEnabled )
            externalAdminEnabledRadioButton.setSelected(true);
        else
            externalAdminDisabledRadioButton.setSelected(true);

	// OUTSIDE ACCESS RESTRICTED /////
	boolean isOutsideAccessRestricted = networkingConfiguration.isOutsideAccessRestricted();
	setOutsideAccessRestrictedDependency( isOutsideAccessRestricted );
	if( isOutsideAccessRestricted )
            externalAdminRestrictEnabledRadioButton.setSelected(true);
        else
            externalAdminRestrictDisabledRadioButton.setSelected(true);
        
	// OUTSIDE ACCESS RESTRICTED NETWORK //////
        restrictIPaddrJTextField.setText( networkingConfiguration.outsideNetwork().toString() );
	restrictIPaddrJTextField.setBackground( Color.WHITE );

	// OUTSIDE ACCESS RESTRICTED NETMASK /////
        restrictIPaddrJTextField.setText( networkingConfiguration.outsideNetwork().toString() );
	restrictIPaddrJTextField.setBackground( Color.white );

        // SSH ENABLED ///////
	boolean isSshEnabled = networkingConfiguration.isSshEnabled();
	if( isSshEnabled )
            sshEnabledRadioButton.setSelected(true);
        else
            sshDisabledRadioButton.setSelected(true);
        
	// INSIDE INSECURE ENABLED ///////
	boolean isInsideInsecureEnabled = networkingConfiguration.isInsideInsecureEnabled();
	if( isInsideInsecureEnabled )
            internalAdminEnabledRadioButton.setSelected(true);
        else
            internalAdminDisabledRadioButton.setSelected(true);

        // TCP WINDOW SCALING /////
	boolean isTcpWindowScalingEnabled = networkingConfiguration.isTcpWindowScalingEnabled();
	if( isTcpWindowScalingEnabled )
            tcpWindowEnabledRadioButton.setSelected(true);
        else
            tcpWindowDisabledRadioButton.setSelected(true);
        	
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
        jLabel1 = new javax.swing.JLabel();
        dhcpIPaddrJTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        dhcpNetmaskJTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        dhcpRouteJTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        dnsPrimaryJTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        dnsSecondaryJTextField = new javax.swing.JTextField();
        externalRemoteJPanel = new javax.swing.JPanel();
        externalAdminEnabledRadioButton = new javax.swing.JRadioButton();
        enableRemoteJPanel = new javax.swing.JPanel();
        externalAdminRestrictDisabledRadioButton = new javax.swing.JRadioButton();
        externalAdminRestrictEnabledRadioButton = new javax.swing.JRadioButton();
        restrictIPJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        restrictIPaddrJTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        restrictNetmaskJTextField = new javax.swing.JTextField();
        externalAdminDisabledRadioButton = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        sshEnabledRadioButton = new javax.swing.JRadioButton();
        sshDisabledRadioButton = new javax.swing.JRadioButton();
        internalRemoteJPanel = new javax.swing.JPanel();
        internalAdminEnabledRadioButton = new javax.swing.JRadioButton();
        internalAdminDisabledRadioButton = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();
        tcpWindowJPanel = new javax.swing.JPanel();
        tcpWindowEnabledRadioButton = new javax.swing.JRadioButton();
        tcpWindowDisabledRadioButton = new javax.swing.JRadioButton();

        setLayout(new java.awt.GridBagLayout());

        setMinimumSize(new java.awt.Dimension(563, 751));
        setPreferredSize(new java.awt.Dimension(563, 751));
        dhcpJPanel.setLayout(new java.awt.GridBagLayout());

        dhcpJPanel.setBorder(new javax.swing.border.TitledBorder(null, "IP Address Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        dhcpButtonGroup.add(dhcpEnabledRadioButton);
        dhcpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dhcpEnabledRadioButton.setText("<html><b>Enable</b> DHCP to automatically set EdgeGuard's IP address when powered on.</html>");
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
        dhcpDisabledRadioButton.setText("<html><b>Disable</b> DHCP and set EdgeGuard's IP address manually:</html>");
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

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("Default Route:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dhcpRouteJTextField, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("Primary DNS:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel3, gridBagConstraints);

        dnsPrimaryJTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dnsPrimaryJTextFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dnsPrimaryJTextField, gridBagConstraints);

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("Secondary DNS:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel.add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel.add(dnsSecondaryJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 5, 0);
        dhcpJPanel.add(staticIPJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(dhcpJPanel, gridBagConstraints);

        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "External Remote Administration Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        externalAdminButtonGroup.add(externalAdminEnabledRadioButton);
        externalAdminEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        externalAdminEnabledRadioButton.setText("<html><b>Enable</b> Remote Administration by authorized users outside of the local network.</html>");
        externalAdminEnabledRadioButton.setFocusPainted(false);
        externalAdminEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                externalAdminEnabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        externalRemoteJPanel.add(externalAdminEnabledRadioButton, gridBagConstraints);

        enableRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        restrictAdminButtonGroup.add(externalAdminRestrictDisabledRadioButton);
        externalAdminRestrictDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        externalAdminRestrictDisabledRadioButton.setText("<html><b>Allow Any</b> IP address to connect for remote administration.</html>");
        externalAdminRestrictDisabledRadioButton.setFocusPainted(false);
        externalAdminRestrictDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                externalAdminRestrictDisabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        enableRemoteJPanel.add(externalAdminRestrictDisabledRadioButton, gridBagConstraints);

        restrictAdminButtonGroup.add(externalAdminRestrictEnabledRadioButton);
        externalAdminRestrictEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        externalAdminRestrictEnabledRadioButton.setText("<html><b>Restrict</b> the set of IP addresses that can connect for remote administration to the following:</html>");
        externalAdminRestrictEnabledRadioButton.setFocusPainted(false);
        externalAdminRestrictEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                externalAdminRestrictEnabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        enableRemoteJPanel.add(externalAdminRestrictEnabledRadioButton, gridBagConstraints);

        restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("IP Address:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel.add(restrictIPaddrJTextField, gridBagConstraints);

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel8.setText("Netmask:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel8, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel.add(restrictNetmaskJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
        enableRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
        externalRemoteJPanel.add(enableRemoteJPanel, gridBagConstraints);

        externalAdminButtonGroup.add(externalAdminDisabledRadioButton);
        externalAdminDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        externalAdminDisabledRadioButton.setText("<html><b>Disable</b> Remote Administration by authorized users outside of the local network.  This is the default setting.</html>");
        externalAdminDisabledRadioButton.setFocusPainted(false);
        externalAdminDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                externalAdminDisabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        externalRemoteJPanel.add(externalAdminDisabledRadioButton, gridBagConstraints);

        jSeparator1.setForeground(new java.awt.Color(200, 200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        externalRemoteJPanel.add(jSeparator1, gridBagConstraints);

        sshButtonGroup.add(sshEnabledRadioButton);
        sshEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        sshEnabledRadioButton.setText("<html><b>Enable</b> secure remote maintenance.  This is for remote troubleshooting purposes.</html>");
        sshEnabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        externalRemoteJPanel.add(sshEnabledRadioButton, gridBagConstraints);

        sshButtonGroup.add(sshDisabledRadioButton);
        sshDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        sshDisabledRadioButton.setText("<html><b>Disable</b> secure remote maintenance.  This is the default setting.</html>");
        sshDisabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        externalRemoteJPanel.add(sshDisabledRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(externalRemoteJPanel, gridBagConstraints);

        internalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        internalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Internal Remote Administration Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        internalAdminButtonGroup.add(internalAdminEnabledRadioButton);
        internalAdminEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        internalAdminEnabledRadioButton.setText("<html><b>Enable</b> insecure Remote Administration inside the local network via http.  This is the default setting.</html>");
        internalAdminEnabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        internalRemoteJPanel.add(internalAdminEnabledRadioButton, gridBagConstraints);

        internalAdminButtonGroup.add(internalAdminDisabledRadioButton);
        internalAdminDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        internalAdminDisabledRadioButton.setText("<html><b>Disable</b> insecure Remote Administration inside the local network via http.</html>");
        internalAdminDisabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        internalRemoteJPanel.add(internalAdminDisabledRadioButton, gridBagConstraints);

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("Note:  internal Remote Administration via secure http (https) is always enabled");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(25, 0, 0, 0);
        internalRemoteJPanel.add(jLabel6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(internalRemoteJPanel, gridBagConstraints);

        tcpWindowJPanel.setLayout(new java.awt.GridBagLayout());

        tcpWindowJPanel.setBorder(new javax.swing.border.TitledBorder(null, "TCP Window Scaling Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        tcpWindowButtonGroup.add(tcpWindowEnabledRadioButton);
        tcpWindowEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        tcpWindowEnabledRadioButton.setText("<html><b>Enable</b> TCP Window Scaling to possibly increase network performance.  This may not be compatible with some networks.</html>");
        tcpWindowEnabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        tcpWindowJPanel.add(tcpWindowEnabledRadioButton, gridBagConstraints);

        tcpWindowButtonGroup.add(tcpWindowDisabledRadioButton);
        tcpWindowDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        tcpWindowDisabledRadioButton.setText("<html><b>Disable</b> TCP Window Scaling to avoid any possible incompatibilities with networking equipment.  This is the default setting.</html>");
        tcpWindowDisabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        tcpWindowJPanel.add(tcpWindowDisabledRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(tcpWindowJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void dnsPrimaryJTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dnsPrimaryJTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_dnsPrimaryJTextFieldActionPerformed

    private void externalAdminDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminDisabledRadioButtonActionPerformed
        setOutsideAccessEnabledDependency( false );
	setOutsideAccessRestrictedDependency( false );
    }//GEN-LAST:event_externalAdminDisabledRadioButtonActionPerformed

    private void externalAdminEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminEnabledRadioButtonActionPerformed
        setOutsideAccessEnabledDependency( true );
	setOutsideAccessRestrictedDependency( externalAdminRestrictEnabledRadioButton.isSelected() );
    }//GEN-LAST:event_externalAdminEnabledRadioButtonActionPerformed

    private void externalAdminRestrictEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminRestrictEnabledRadioButtonActionPerformed
        if( externalAdminEnabledRadioButton.isSelected() )
	    setOutsideAccessRestrictedDependency( true );
    }//GEN-LAST:event_externalAdminRestrictEnabledRadioButtonActionPerformed

    private void externalAdminRestrictDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminRestrictDisabledRadioButtonActionPerformed
	setOutsideAccessRestrictedDependency( false );
    }//GEN-LAST:event_externalAdminRestrictDisabledRadioButtonActionPerformed

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

    private void setOutsideAccessEnabledDependency(boolean enabled){
	externalAdminRestrictEnabledRadioButton.setEnabled( enabled );
	externalAdminRestrictDisabledRadioButton.setEnabled( enabled );
    }

    private void setOutsideAccessRestrictedDependency(boolean enabled){
	restrictIPaddrJTextField.setEnabled( enabled );
	restrictNetmaskJTextField.setEnabled( enabled );
    }

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup dhcpButtonGroup;
    public javax.swing.JRadioButton dhcpDisabledRadioButton;
    public javax.swing.JRadioButton dhcpEnabledRadioButton;
    public javax.swing.JTextField dhcpIPaddrJTextField;
    private javax.swing.JPanel dhcpJPanel;
    public javax.swing.JTextField dhcpNetmaskJTextField;
    public javax.swing.JTextField dhcpRouteJTextField;
    public javax.swing.JTextField dnsPrimaryJTextField;
    public javax.swing.JTextField dnsSecondaryJTextField;
    private javax.swing.JPanel enableRemoteJPanel;
    private javax.swing.ButtonGroup externalAdminButtonGroup;
    public javax.swing.JRadioButton externalAdminDisabledRadioButton;
    public javax.swing.JRadioButton externalAdminEnabledRadioButton;
    public javax.swing.JRadioButton externalAdminRestrictDisabledRadioButton;
    public javax.swing.JRadioButton externalAdminRestrictEnabledRadioButton;
    private javax.swing.JPanel externalRemoteJPanel;
    private javax.swing.ButtonGroup internalAdminButtonGroup;
    public javax.swing.JRadioButton internalAdminDisabledRadioButton;
    public javax.swing.JRadioButton internalAdminEnabledRadioButton;
    private javax.swing.JPanel internalRemoteJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.ButtonGroup restrictAdminButtonGroup;
    private javax.swing.JPanel restrictIPJPanel;
    public javax.swing.JTextField restrictIPaddrJTextField;
    public javax.swing.JTextField restrictNetmaskJTextField;
    private javax.swing.ButtonGroup sshButtonGroup;
    public javax.swing.JRadioButton sshDisabledRadioButton;
    public javax.swing.JRadioButton sshEnabledRadioButton;
    private javax.swing.JPanel staticIPJPanel;
    private javax.swing.ButtonGroup tcpWindowButtonGroup;
    public javax.swing.JRadioButton tcpWindowDisabledRadioButton;
    public javax.swing.JRadioButton tcpWindowEnabledRadioButton;
    private javax.swing.JPanel tcpWindowJPanel;
    // End of variables declaration//GEN-END:variables
    
}
