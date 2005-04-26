/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: NatEventHandler.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.tran.nat.gui;

import java.awt.*;

import com.metavize.tran.nat.*;
import com.metavize.gui.util.Util;
import com.metavize.mvvm.tran.IPaddr;

/**
 *
 * @author  inieves
 */
public class DhcpJPanel extends javax.swing.JPanel {
    
    private Color INVALID_COLOR = Color.PINK;
    private Color BACKGROUND_COLOR = new Color(224, 224, 224);
    

    public DhcpJPanel() {
        initComponents();
    }
    
    
    
    public void refresh(Object settings) throws Exception {
        if(!(settings instanceof NatSettings)){
            this.setBackground(INVALID_COLOR);
            return;
        }
        else{
            this.setBackground(BACKGROUND_COLOR);
        }
        
        boolean isValid = true;
        
        NatSettings natSettings = (NatSettings) settings;
        boolean dhcpIsEnabled;
        String dhcpStartAddress;
        String dhcpEndAddress;
        String dhcpGateway;
        String dhcpSubnet;
        String dhcpNetwork;
        boolean dnsIsEnabled;
        String dhcpNameserver1;
        String dhcpNameserver2;

        
        // DHCP ENABLED ///////////
        try{
            dhcpIsEnabled = natSettings.getDhcpEnabled();
            this.setDhcpEnabledDependency(dhcpIsEnabled);
            if( dhcpIsEnabled )
                dhcpEnabledJRadioButton.setSelected(true);
            else
                dhcpDisabledJRadioButton.setSelected(true);
            dhcpEnabledJRadioButton.setBackground( BACKGROUND_COLOR );
            dhcpDisabledJRadioButton.setBackground( BACKGROUND_COLOR );
        }
        catch(Exception e){
            dhcpEnabledJRadioButton.setBackground( INVALID_COLOR );
            dhcpDisabledJRadioButton.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DYNAMIC RANGE START //////
        try{
            dhcpStartAddress = natSettings.getDhcpStartAddress().toString();
            startAddressIPaddrJTextField.setText( dhcpStartAddress );
            startAddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            startAddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DYNAMIC RANGE END //////
        try{
            dhcpEndAddress = natSettings.getDhcpEndAddress().toString();
            endAddressIPaddrJTextField.setText( dhcpEndAddress );
            endAddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            endAddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // INTERNAL ADDRESS //////
        try{
            dhcpGateway = natSettings.getDhcpGateway().toString();
            internalAddressIPaddrJTextField.setText( dhcpGateway );
            internalAddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            internalAddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // INTERNAL SUBNET ///////
        try{
            dhcpSubnet  = natSettings.getDhcpSubnet().toString();
            internalSubnetIPaddrJTextField.setText( dhcpSubnet );
            internalSubnetIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            internalSubnetIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // INTERNAL NETWORK ///////
        try{
            dhcpNetwork = IPaddr.and(natSettings.getDhcpGateway(), natSettings.getDhcpSubnet()).toString();
            internalNetworkJTextField.setText( dhcpNetwork );
            internalNetworkJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            internalNetworkJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DNS MASQ ENABLED ///////////
        try{
            dnsIsEnabled = natSettings.getDnsEnabled();
            if( dnsIsEnabled )
                dnsMasqEnabledJRadioButton.setSelected(true);
            else
                dnsMasqDisabledJRadioButton.setSelected(true);
            dnsMasqEnabledJRadioButton.setBackground( BACKGROUND_COLOR );
            dnsMasqDisabledJRadioButton.setBackground( BACKGROUND_COLOR );
        }
        catch(Exception e){
            dnsMasqEnabledJRadioButton.setBackground( INVALID_COLOR );
            dnsMasqDisabledJRadioButton.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DNS PRIMARY //////
        try{
            dhcpNameserver1 = natSettings.getDhcpNameserver1().toString();
            nameserver1AddressIPaddrJTextField.setText( dhcpNameserver1 );
            nameserver1AddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            nameserver1AddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DNS SECONDARY //////
        try{
            dhcpNameserver2 = natSettings.getDhcpNameserver2().toString();
            nameserver2AddressIPaddrJTextField.setText( dhcpNameserver2 );
            nameserver2AddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            nameserver2AddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        if(!isValid)
            throw new Exception();
        
    }

    
    
    public void save(Object settings) throws Exception {
        if(!(settings instanceof NatSettings)){
            this.setBackground(INVALID_COLOR);
            return;
        }
        else{
            this.setBackground(BACKGROUND_COLOR);
        }
        
        boolean isValid = true;
        
        NatSettings natSettings = (NatSettings) settings;
        boolean dhcpIsEnabled;
        IPaddr dhcpStartAddress = null;
        IPaddr dhcpEndAddress = null;
        IPaddr dhcpGateway = null;
        IPaddr dhcpSubnet = null;
        boolean dnsIsEnabled;
        IPaddr dhcpNameserver1 = null;
        IPaddr dhcpNameserver2 = null;
        
        // DHCP ENABLED ///////////
        dhcpIsEnabled = dhcpEnabledJRadioButton.isSelected();
        if( dhcpEnabledJRadioButton.isSelected() ^ dhcpDisabledJRadioButton.isSelected() ){
            dhcpEnabledJRadioButton.setBackground( BACKGROUND_COLOR );
            dhcpDisabledJRadioButton.setBackground( BACKGROUND_COLOR );
        }
        else{
            dhcpEnabledJRadioButton.setBackground( INVALID_COLOR );
            dhcpDisabledJRadioButton.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DYNAMIC RANGE START //////
        try{
            dhcpStartAddress = IPaddr.parse( startAddressIPaddrJTextField.getText() );
            startAddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            startAddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DYNAMIC RANGE END //////
        try{
            dhcpEndAddress = IPaddr.parse( endAddressIPaddrJTextField.getText() );
            endAddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            endAddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // INTERNAL ADDRESS //////
        try{
            dhcpGateway = IPaddr.parse( internalAddressIPaddrJTextField.getText() );
            internalAddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            internalAddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // INTERNAL SUBNET ///////
        try{
            dhcpSubnet  = IPaddr.parse( internalSubnetIPaddrJTextField.getText() );
            internalSubnetIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            internalSubnetIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DNS MASQ ENABLED ///////////
        dnsIsEnabled = dnsMasqEnabledJRadioButton.isSelected();
        if( dnsMasqEnabledJRadioButton.isSelected() ^ dnsMasqDisabledJRadioButton.isSelected() ){
            dnsMasqEnabledJRadioButton.setBackground( BACKGROUND_COLOR );
            dnsMasqDisabledJRadioButton.setBackground( BACKGROUND_COLOR );
        }
        else{
            dnsMasqEnabledJRadioButton.setBackground( INVALID_COLOR );
            dnsMasqDisabledJRadioButton.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DNS PRIMARY //////
        try{
            dhcpNameserver1 = IPaddr.parse( nameserver1AddressIPaddrJTextField.getText() );
            nameserver1AddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            nameserver1AddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DNS SECONDARY //////
        try{
            dhcpNameserver2 = IPaddr.parse( nameserver2AddressIPaddrJTextField.getText() );
            nameserver2AddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            nameserver2AddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        
        // SAVE THE VALUES ////////////////////////////////////
        if(isValid){
            natSettings.setDhcpEnabled( dhcpIsEnabled );
            natSettings.setDhcpStartAddress( dhcpStartAddress );
            natSettings.setDhcpEndAddress( dhcpEndAddress );
            natSettings.setDhcpGateway( dhcpGateway );
            natSettings.setDhcpSubnet( dhcpSubnet );
            natSettings.setDnsEnabled( dnsIsEnabled );
            natSettings.setDhcpNameserver1( dhcpNameserver1 );
            natSettings.setDhcpNameserver2( dhcpNameserver2 );
        }
        else
            throw new Exception();
        
    }
    
    
    

    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        enabledButtonGroup = new javax.swing.ButtonGroup();
        dnsButtonGroup = new javax.swing.ButtonGroup();
        explanationJPanel = new javax.swing.JPanel();
        jTextArea2 = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        dhcpEnabledJRadioButton = new javax.swing.JRadioButton();
        dhcpDisabledJRadioButton = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        dynamicRangeJPanel = new javax.swing.JPanel();
        jTextArea3 = new javax.swing.JTextArea();
        restrictIPJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        startAddressIPaddrJTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        endAddressIPaddrJTextField = new javax.swing.JTextField();
        internalAddressJPanel = new javax.swing.JPanel();
        jTextArea4 = new javax.swing.JTextArea();
        jTextArea5 = new javax.swing.JTextArea();
        restrictIPJPanel2 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        internalAddressIPaddrJTextField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        internalSubnetIPaddrJTextField = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        internalNetworkJTextField = new javax.swing.JTextField();
        nameserversJPanel = new javax.swing.JPanel();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        dnsMasqEnabledJRadioButton = new javax.swing.JRadioButton();
        dnsMasqDisabledJRadioButton = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        restrictIPJPanel3 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        nameserver1AddressIPaddrJTextField = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        nameserver2AddressIPaddrJTextField = new javax.swing.JTextField();

        setLayout(new java.awt.GridBagLayout());

        explanationJPanel.setLayout(new java.awt.GridBagLayout());

        explanationJPanel.setBorder(new javax.swing.border.TitledBorder(null, "DHCP Usage", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea2.setEditable(false);
        jTextArea2.setLineWrap(true);
        jTextArea2.setText("DHCP allows computers in a private network to be assigned an IP address (either dynamically or statically), based on their hardware MAC addresses.  The computers can then also register hostnames when they request an IP address via DHCP.  Through DNS Masquerading, computers on the internal network can then address each other by hostname (in addition to IP address).");
        jTextArea2.setWrapStyleWord(true);
        jTextArea2.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        explanationJPanel.add(jTextArea2, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        enabledButtonGroup.add(dhcpEnabledJRadioButton);
        dhcpEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dhcpEnabledJRadioButton.setText("Enabled");
        dhcpEnabledJRadioButton.setFocusPainted(false);
        dhcpEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dhcpEnabledJRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(dhcpEnabledJRadioButton, gridBagConstraints);

        enabledButtonGroup.add(dhcpDisabledJRadioButton);
        dhcpDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dhcpDisabledJRadioButton.setText("Disabled");
        dhcpDisabledJRadioButton.setFocusPainted(false);
        dhcpDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dhcpDisabledJRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(dhcpDisabledJRadioButton, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("DHCP");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        explanationJPanel.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(explanationJPanel, gridBagConstraints);

        dynamicRangeJPanel.setLayout(new java.awt.GridBagLayout());

        dynamicRangeJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Dynamic IP Address Range", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea3.setEditable(false);
        jTextArea3.setLineWrap(true);
        jTextArea3.setText("DHCP can assign addresses to computers dynamically, from a pool of dynamic IP addresses.  The pool of dynamic IP addresses must be specified as a range of addresses, with a beginning (start) and an end.  You can use the Client Address Table to specify that a computer on the internal network should be assigned an IP address dynamically.");
        jTextArea3.setWrapStyleWord(true);
        jTextArea3.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        dynamicRangeJPanel.add(jTextArea3, gridBagConstraints);

        restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("IP Address Range Start: ");
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
        restrictIPJPanel.add(startAddressIPaddrJTextField, gridBagConstraints);

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel8.setText("IP Address Range End: ");
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
        restrictIPJPanel.add(endAddressIPaddrJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        dynamicRangeJPanel.add(restrictIPJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(dynamicRangeJPanel, gridBagConstraints);

        internalAddressJPanel.setLayout(new java.awt.GridBagLayout());

        internalAddressJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Internal Address (Gateway)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea4.setEditable(false);
        jTextArea4.setFont(new java.awt.Font("Dialog", 1, 12));
        jTextArea4.setLineWrap(true);
        jTextArea4.setText("Note:  This is enabled only if NAT is disabled");
        jTextArea4.setWrapStyleWord(true);
        jTextArea4.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        internalAddressJPanel.add(jTextArea4, gridBagConstraints);

        jTextArea5.setEditable(false);
        jTextArea5.setLineWrap(true);
        jTextArea5.setText("DHCP requires that you assign an address on your internal network to EdgeGuard.  This internal address is the address that computers on the internal network will use to contact EdgeGuard, in an effort to access the Internet or some other external network.  This address will be supplied by NAT if NAT is enabled.");
        jTextArea5.setWrapStyleWord(true);
        jTextArea5.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        internalAddressJPanel.add(jTextArea5, gridBagConstraints);

        restrictIPJPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel7.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel7.setText("Internal IP Address: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel2.add(jLabel7, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel2.add(internalAddressIPaddrJTextField, gridBagConstraints);

        jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel11.setText("Internal Subnet: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel2.add(jLabel11, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel2.add(internalSubnetIPaddrJTextField, gridBagConstraints);

        jLabel12.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel12.setText("Network Address: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        restrictIPJPanel2.add(jLabel12, gridBagConstraints);

        internalNetworkJTextField.setEditable(false);
        internalNetworkJTextField.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 2, 0);
        restrictIPJPanel2.add(internalNetworkJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        internalAddressJPanel.add(restrictIPJPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(internalAddressJPanel, gridBagConstraints);

        nameserversJPanel.setLayout(new java.awt.GridBagLayout());

        nameserversJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Nameservers (DNS)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setText("DHCP can assign a DNS to computers dynamically.  You can enable DNS Masquerading which sets EdgeGuard as the main DNS and uses the primary and secondary DNS for failover.  Or you can disable DNS Masquerading which makes the primary and secondary DNS the only DNS sources.");
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        nameserversJPanel.add(jTextArea1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        dnsButtonGroup.add(dnsMasqEnabledJRadioButton);
        dnsMasqEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dnsMasqEnabledJRadioButton.setText("Enabled");
        dnsMasqEnabledJRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(dnsMasqEnabledJRadioButton, gridBagConstraints);

        dnsButtonGroup.add(dnsMasqDisabledJRadioButton);
        dnsMasqDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dnsMasqDisabledJRadioButton.setText("Disabled");
        dnsMasqDisabledJRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(dnsMasqDisabledJRadioButton, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("DNS Masquerading");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(jLabel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        nameserversJPanel.add(jPanel2, gridBagConstraints);

        restrictIPJPanel3.setLayout(new java.awt.GridBagLayout());

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel9.setText("Primary DNS IP Address: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel3.add(jLabel9, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel3.add(nameserver1AddressIPaddrJTextField, gridBagConstraints);

        jLabel13.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel13.setText("Secondary DNS IP Address: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel3.add(jLabel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel3.add(nameserver2AddressIPaddrJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        nameserversJPanel.add(restrictIPJPanel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(nameserversJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void dhcpDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpDisabledJRadioButtonActionPerformed
        this.setDhcpEnabledDependency(false);
    }//GEN-LAST:event_dhcpDisabledJRadioButtonActionPerformed

    private void dhcpEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpEnabledJRadioButtonActionPerformed
        this.setDhcpEnabledDependency(true);
    }//GEN-LAST:event_dhcpEnabledJRadioButtonActionPerformed
    
    private void setDhcpEnabledDependency(boolean enabled){
        startAddressIPaddrJTextField.setEnabled( enabled );
        endAddressIPaddrJTextField.setEnabled( enabled );
        internalAddressIPaddrJTextField.setEnabled( enabled );
        internalSubnetIPaddrJTextField.setEnabled( enabled );
        nameserver1AddressIPaddrJTextField.setEnabled( enabled );
        nameserver2AddressIPaddrJTextField.setEnabled( enabled );
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JRadioButton dhcpDisabledJRadioButton;
    public javax.swing.JRadioButton dhcpEnabledJRadioButton;
    private javax.swing.ButtonGroup dnsButtonGroup;
    public javax.swing.JRadioButton dnsMasqDisabledJRadioButton;
    public javax.swing.JRadioButton dnsMasqEnabledJRadioButton;
    private javax.swing.JPanel dynamicRangeJPanel;
    private javax.swing.ButtonGroup enabledButtonGroup;
    public javax.swing.JTextField endAddressIPaddrJTextField;
    private javax.swing.JPanel explanationJPanel;
    public javax.swing.JTextField internalAddressIPaddrJTextField;
    private javax.swing.JPanel internalAddressJPanel;
    public javax.swing.JTextField internalNetworkJTextField;
    public javax.swing.JTextField internalSubnetIPaddrJTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextArea jTextArea4;
    private javax.swing.JTextArea jTextArea5;
    public javax.swing.JTextField nameserver1AddressIPaddrJTextField;
    public javax.swing.JTextField nameserver2AddressIPaddrJTextField;
    private javax.swing.JPanel nameserversJPanel;
    private javax.swing.JPanel restrictIPJPanel;
    private javax.swing.JPanel restrictIPJPanel2;
    private javax.swing.JPanel restrictIPJPanel3;
    public javax.swing.JTextField startAddressIPaddrJTextField;
    // End of variables declaration//GEN-END:variables
    
}
