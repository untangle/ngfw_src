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

package com.untangle.gui.login;

import java.awt.*;
import javax.swing.*;

import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.uvm.MailSender;
import com.untangle.uvm.networking.AddressSettings;
import com.untangle.uvm.networking.BasicNetworkSettings;
import com.untangle.uvm.networking.NetworkUtil;
import com.untangle.uvm.networking.PPPoEConnectionRule;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;

public class InitialSetupNetworkJPanel extends MWizardPageJPanel {

    private static final String EXCEPTION_DHCP_IP_ADDRESS = "You have manually specified an invalid \"IP Address\".  Please correct this before proceeding.";
    private static final String EXCEPTION_DHCP_NETMASK = "You have manually specified an invalid \"Netmask\".  Please correct this before proceeding.";
    private static final String EXCEPTION_DHCP_GATEWAY = "You have manually specified an invalid \"Default Route\".  Please correct this before proceeding.";
    private static final String EXCEPTION_DHCP_DNS_1 = "You have manually specified an invalid \"Primary DNS\".  Please correct this before proceeding.";
    private static final String EXCEPTION_DHCP_DNS_2 = "You have manually specified an invalid \"Secondary DNS\".  Please correct this before proceeding.";
    private static final String EXCEPTION_HOSTNAME = "You must specify a dotted hostname for your Untangle Server.  Please correct this before proceeding.";
    private static final String EXCEPTION_PPPOE_NAME     = "You must specify a PPPoE Name.";
    private static final String EXCEPTION_PPPOE_PASSWORD = "You must specify a PPPoE Password.";


    public InitialSetupNetworkJPanel() {
        initComponents();
        setDhcpEnabledDependency(dhcpEnabledRadioButton.isSelected());
        setPPPOEEnabledDependency(pppoeEnabledJRadioButton.isSelected());
        hostnameJTextField.setText( "untangle" + "." + MailSender.DEFAULT_LOCAL_DOMAIN );

        Util.addFocusHighlight(hostnameJTextField);
        Util.addFocusHighlight(dhcpIPaddrJTextField);
        Util.addFocusHighlight(dhcpNetmaskJTextField);
        Util.addFocusHighlight(dhcpRouteJTextField);
        Util.addFocusHighlight(dnsPrimaryJTextField);
        Util.addFocusHighlight(dnsSecondaryJTextField);
        Util.addFocusHighlight(nameJTextField);
        Util.addFocusHighlight(passwordJPasswordField);
    }

    public void initialFocus(){
        hostnameJTextField.requestFocus();
    }


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
    String hostnameString;
    HostName hostname;
    Exception exception;

    boolean isPPPOEEnabled;
    String pppoeName;
    String pppoePassword;

    JProgressBar jProgressBar;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
            dhcpIPaddrJTextField.setBackground( Color.WHITE );
            dhcpNetmaskJTextField.setBackground( Color.WHITE );
            dhcpRouteJTextField.setBackground( Color.WHITE );
            dnsPrimaryJTextField.setBackground( Color.WHITE );
            dnsSecondaryJTextField.setBackground( Color.WHITE );
            hostnameJTextField.setBackground( Color.WHITE );
            nameJTextField.setBackground( Color.WHITE );
            passwordJPasswordField.setBackground( Color.WHITE );

            isDhcpEnabled = dhcpEnabledRadioButton.isSelected();
            hostString = dhcpIPaddrJTextField.getText();
            netmaskString = dhcpNetmaskJTextField.getText();
            gatewayString = dhcpRouteJTextField.getText();
            dns1String = dnsPrimaryJTextField.getText();
            dns2String = dnsSecondaryJTextField.getText();
            hostnameString = hostnameJTextField.getText();
            isPPPOEEnabled = pppoeEnabledJRadioButton.isSelected();
            pppoeName = nameJTextField.getText();
            pppoePassword = new String(passwordJPasswordField.getPassword());

            exception = null;

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

            try{
                if( hostnameString.length() == 0 )
                    throw new Exception();

                hostname = HostName.parseStrict( hostnameString );
            }
            catch(Exception e){
                hostnameJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_HOSTNAME);
                return;
            }

            if( isPPPOEEnabled ){
                if( pppoeName.length() == 0 ){
                    exception = new Exception(EXCEPTION_PPPOE_NAME);
                    return;
                }
                if( pppoePassword.length() == 0){
                    exception = new Exception(EXCEPTION_PPPOE_PASSWORD);
                    return;
                }
            }


        }});

        if( exception != null )
            throw exception;

        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            InitialSetupWizard.getInfiniteProgressJComponent().startLater("Saving Network Settings...");
            try{
                AddressSettings addressSettings = Util.getNetworkManager().getAddressSettings();
                BasicNetworkSettings basicSettings = Util.getNetworkManager().getBasicSettings();
                basicSettings.isDhcpEnabled( isDhcpEnabled );
                if( !isDhcpEnabled ){
                    basicSettings.host( host );
                    basicSettings.netmask( netmask );
                    basicSettings.gateway( gateway );
                    basicSettings.dns1( dns1 );
                    basicSettings.dns2( dns2 );
                }
                addressSettings.setHostName( hostname );
                PPPoEConnectionRule connectionRule = basicSettings.getPPPoESettings();
                connectionRule.setLive( isPPPOEEnabled );
                if( isPPPOEEnabled ){
                    connectionRule.setUsername( pppoeName );
                    connectionRule.setPassword( pppoePassword );
                }
                basicSettings.setPPPoESettings( connectionRule );

                boolean isPublic = NetworkUtil.getInstance().isHostnameLikelyPublic( hostname.toString() );
                addressSettings.setIsHostNamePublic(isPublic);

                InitialSetupWizard.setSharedData(hostname.toString());
                Util.getNetworkManager().setSetupSettings(addressSettings,basicSettings);
                InitialSetupWizard.getInfiniteProgressJComponent().stopLater(1500l);
            }
            catch(Exception e){
                InitialSetupWizard.getInfiniteProgressJComponent().stopLater(-1l);
                Util.handleExceptionNoRestart("Error sending data", e);
                throw new Exception("A network communication error occurred.  Please retry.");
            }
        }

    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        dhcpButtonGroup = new javax.swing.ButtonGroup();
        pppoeButtonGroup = new javax.swing.ButtonGroup();
        contentJPanel = new javax.swing.JPanel();
        hostJPanel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        hostnameJPanel = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        hostnameJTextField = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        dhcpJPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
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
        pppoeJPanel = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        pppoeDisabledJRadioButton = new javax.swing.JRadioButton();
        pppoeEnabledJRadioButton = new javax.swing.JRadioButton();
        staticIPJPanel1 = new javax.swing.JPanel();
        nameJLabel = new javax.swing.JLabel();
        nameJTextField = new javax.swing.JTextField();
        passwordJLabel = new javax.swing.JLabel();
        passwordJPasswordField = new javax.swing.JPasswordField();
        backgroundJPabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setOpaque(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        hostJPanel.setLayout(new java.awt.GridBagLayout());

        hostJPanel.setOpaque(false);
        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("<html>Please specify the Untangle Server's hostname on your network.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        hostJPanel.add(jLabel4, gridBagConstraints);

        hostnameJPanel.setLayout(new java.awt.GridBagLayout());

        hostnameJPanel.setMinimumSize(new java.awt.Dimension(300, 23));
        hostnameJPanel.setOpaque(false);
        hostnameJPanel.setPreferredSize(new java.awt.Dimension(300, 23));
        jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel10.setText("Hostname:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        hostnameJPanel.add(jLabel10, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        hostnameJPanel.add(hostnameJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 15);
        hostJPanel.add(hostnameJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contentJPanel.add(hostJPanel, gridBagConstraints);

        jSeparator1.setForeground(new java.awt.Color(156, 156, 156));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 15);
        contentJPanel.add(jSeparator1, gridBagConstraints);

        dhcpJPanel.setLayout(new java.awt.GridBagLayout());

        dhcpJPanel.setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>How should the Untangle Server get its external network settings?</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 15, 0, 15);
        dhcpJPanel.add(jLabel2, gridBagConstraints);

        dhcpButtonGroup.add(dhcpEnabledRadioButton);
        dhcpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dhcpEnabledRadioButton.setSelected(true);
        dhcpEnabledRadioButton.setText("<html><b>Automatically</b> through DHCP. (Use this if unsure)</html>");
        dhcpEnabledRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set Untangle's IP address from the network's DHCP server.</html>");
        dhcpEnabledRadioButton.setOpaque(false);
        dhcpEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    dhcpEnabledRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 15);
        dhcpJPanel.add(dhcpEnabledRadioButton, gridBagConstraints);

        dhcpButtonGroup.add(dhcpDisabledRadioButton);
        dhcpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dhcpDisabledRadioButton.setText("<html><b>Manually</b> through the fields below.</html>");
        dhcpDisabledRadioButton.setOpaque(false);
        dhcpDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    dhcpDisabledRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        dhcpJPanel.add(dhcpDisabledRadioButton, gridBagConstraints);

        staticIPJPanel.setLayout(new java.awt.GridBagLayout());

        staticIPJPanel.setMaximumSize(new java.awt.Dimension(400, 125));
        staticIPJPanel.setMinimumSize(new java.awt.Dimension(400, 125));
        staticIPJPanel.setOpaque(false);
        staticIPJPanel.setPreferredSize(new java.awt.Dimension(400, 125));
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        dhcpJPanel.add(staticIPJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        contentJPanel.add(dhcpJPanel, gridBagConstraints);

        jSeparator2.setForeground(new java.awt.Color(156, 156, 156));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 15);
        contentJPanel.add(jSeparator2, gridBagConstraints);

        pppoeJPanel.setLayout(new java.awt.GridBagLayout());

        pppoeJPanel.setOpaque(false);
        jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel11.setText("<html>You may need PPPoE to be enabled if your Internet Service Providers (ISPs) requires it.  In most cases, you should leave it disabled.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 15, 5, 15);
        pppoeJPanel.add(jLabel11, gridBagConstraints);

        pppoeButtonGroup.add(pppoeDisabledJRadioButton);
        pppoeDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        pppoeDisabledJRadioButton.setSelected(true);
        pppoeDisabledJRadioButton.setText("<html><b>Disable</b>  PPPoE.  (This is the default setting)</html>");
        pppoeDisabledJRadioButton.setActionCommand("<html><b>Use DHCP</b> to automatically set Untangle's IP address from the network's DHCP server.</html>");
        pppoeDisabledJRadioButton.setOpaque(false);
        pppoeDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    pppoeDisabledJRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
        pppoeJPanel.add(pppoeDisabledJRadioButton, gridBagConstraints);

        pppoeButtonGroup.add(pppoeEnabledJRadioButton);
        pppoeEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        pppoeEnabledJRadioButton.setText("<html><b>Enable</b> PPPoE.</html>");
        pppoeEnabledJRadioButton.setOpaque(false);
        pppoeEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    pppoeEnabledJRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
        pppoeJPanel.add(pppoeEnabledJRadioButton, gridBagConstraints);

        staticIPJPanel1.setLayout(new java.awt.GridBagLayout());

        staticIPJPanel1.setOpaque(false);
        nameJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        nameJLabel.setText("Name: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel1.add(nameJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        staticIPJPanel1.add(nameJTextField, gridBagConstraints);

        passwordJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        passwordJLabel.setText("Password: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        staticIPJPanel1.add(passwordJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        staticIPJPanel1.add(passwordJPasswordField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 10, 0);
        pppoeJPanel.add(staticIPJPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 1.0;
        contentJPanel.add(pppoeJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(contentJPanel, gridBagConstraints);

        backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/login/ProductShot.png")));
        backgroundJPabel.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        add(backgroundJPabel, gridBagConstraints);

    }//GEN-END:initComponents

    private void pppoeEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pppoeEnabledJRadioButtonActionPerformed
        setPPPOEEnabledDependency( true );
    }//GEN-LAST:event_pppoeEnabledJRadioButtonActionPerformed

    private void pppoeDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pppoeDisabledJRadioButtonActionPerformed
        setPPPOEEnabledDependency( false );
    }//GEN-LAST:event_pppoeDisabledJRadioButtonActionPerformed

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
        dhcpIPaddrJLabel.setEnabled( !enabled );
        dhcpNetmaskJLabel.setEnabled( !enabled );
        dhcpRouteJLabel.setEnabled( !enabled );
        dnsPrimaryJLabel.setEnabled( !enabled );
        dnsSecondaryJLabel.setEnabled( !enabled );
        optionalJLabel.setEnabled( !enabled );
    }
    private void setPPPOEEnabledDependency(boolean enabled){
        nameJLabel.setEnabled( enabled );
        nameJTextField.setEnabled( enabled );
        passwordJLabel.setEnabled( enabled );
        passwordJPasswordField.setEnabled( enabled );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJPabel;
    private javax.swing.JPanel contentJPanel;
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
    private javax.swing.JPanel hostJPanel;
    private javax.swing.JPanel hostnameJPanel;
    public javax.swing.JTextField hostnameJTextField;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JLabel nameJLabel;
    public javax.swing.JTextField nameJTextField;
    private javax.swing.JLabel optionalJLabel;
    private javax.swing.JLabel passwordJLabel;
    private javax.swing.JPasswordField passwordJPasswordField;
    private javax.swing.ButtonGroup pppoeButtonGroup;
    public javax.swing.JRadioButton pppoeDisabledJRadioButton;
    public javax.swing.JRadioButton pppoeEnabledJRadioButton;
    private javax.swing.JPanel pppoeJPanel;
    private javax.swing.JPanel staticIPJPanel;
    private javax.swing.JPanel staticIPJPanel1;
    // End of variables declaration//GEN-END:variables

}
