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
        
        
        
        // SAVE THE VALUES ////////////////////////////////////
        if(isValid){
            natSettings.setDhcpEnabled( dhcpIsEnabled );
            natSettings.setDhcpStartAddress( dhcpStartAddress );
            natSettings.setDhcpEndAddress( dhcpEndAddress );
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(dynamicRangeJPanel, gridBagConstraints);

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
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JRadioButton dhcpDisabledJRadioButton;
    public javax.swing.JRadioButton dhcpEnabledJRadioButton;
    private javax.swing.ButtonGroup dnsButtonGroup;
    private javax.swing.JPanel dynamicRangeJPanel;
    private javax.swing.ButtonGroup enabledButtonGroup;
    public javax.swing.JTextField endAddressIPaddrJTextField;
    private javax.swing.JPanel explanationJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JPanel restrictIPJPanel;
    public javax.swing.JTextField startAddressIPaddrJTextField;
    // End of variables declaration//GEN-END:variables
    
}
