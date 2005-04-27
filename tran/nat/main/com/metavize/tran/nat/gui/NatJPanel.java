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
public class NatJPanel extends javax.swing.JPanel {
    
    private Color INVALID_COLOR = Color.PINK;
    private Color BACKGROUND_COLOR = new Color(224, 224, 224);
    

    public NatJPanel() {
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
        boolean natEnabled;
        String natInternalAddress;
        String natInternalSubnet;
        String natInternalNetwork;
        String natExternalAddress;
        boolean isDhcpEnabled;
        
        // ENABLED ///////////
        try{
            natEnabled = natSettings.getNatEnabled();
            this.setNatEnabledDependency(natEnabled);
            if( natEnabled )
                natEnabledJRadioButton.setSelected(true);
            else
                natDisabledJRadioButton.setSelected(true);
            natEnabledJRadioButton.setBackground( BACKGROUND_COLOR );
            natDisabledJRadioButton.setBackground( BACKGROUND_COLOR );
        }
        catch(Exception e){
            natEnabledJRadioButton.setBackground( INVALID_COLOR );
            natDisabledJRadioButton.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // INTERNAL ADDRESS //////
        try{
            natInternalAddress = natSettings.getNatInternalAddress().toString();
            internalAddressIPaddrJTextField.setText( natInternalAddress );
            internalAddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            internalAddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // INTERNAL SUBNET ///////
        try{
            natInternalSubnet  = natSettings.getNatInternalSubnet().toString();
            internalSubnetIPaddrJTextField.setText( natInternalSubnet );
            internalSubnetIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            internalSubnetIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // INTERNAL NETWORK ///////
        try{
            natInternalNetwork = IPaddr.and(natSettings.getNatInternalAddress(), natSettings.getNatInternalSubnet()).toString();
            internalNetworkJLabel.setText( natInternalNetwork );
            internalNetworkJLabel.setBackground( BACKGROUND_COLOR );
        }
        catch(Exception e){
            internalNetworkJLabel.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // EXTERNAL ADDRESS ///////
        try{
            natExternalAddress = Util.getMvvmContext().networkingManager().get().host().toString();
            externalAddressJLabel.setText( natExternalAddress );
            externalAddressJLabel.setBackground( BACKGROUND_COLOR );
        }
        catch(Exception e){
            externalAddressJLabel.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // DHCP ///////
        try{
            isDhcpEnabled = Util.getMvvmContext().networkingManager().get().isDhcpEnabled();
            if( isDhcpEnabled )
                externalMethodJLabel.setText("Dynamic via DHCP");
            else
                externalMethodJLabel.setText("Manually specified");
            externalMethodJLabel.setBackground( BACKGROUND_COLOR );
        }
        catch(Exception e){
            externalMethodJLabel.setBackground( INVALID_COLOR );
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
        boolean natEnabled;
        IPaddr natInternalAddress = null;
        IPaddr natInternalSubnet = null;
        
        // ENABLED ///////////
        natEnabled = natEnabledJRadioButton.isSelected();
        if( natEnabledJRadioButton.isSelected() ^ natDisabledJRadioButton.isSelected() ){
            natEnabledJRadioButton.setBackground( BACKGROUND_COLOR );
            natDisabledJRadioButton.setBackground( BACKGROUND_COLOR );
        }
        else{
            natEnabledJRadioButton.setBackground( INVALID_COLOR );
            natDisabledJRadioButton.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // INTERNAL ADDRESS //////
        if(natEnabled){
            try{
                natInternalAddress = IPaddr.parse( internalAddressIPaddrJTextField.getText() );
                internalAddressIPaddrJTextField.setBackground( Color.WHITE );
            }
            catch(Exception e){
                internalAddressIPaddrJTextField.setBackground( INVALID_COLOR );
                isValid = false;
            }
        }
        
        // INTERNAL SUBNET ///////
        if(natEnabled){
            try{
                natInternalSubnet = IPaddr.parse( internalSubnetIPaddrJTextField.getText() );
                internalSubnetIPaddrJTextField.setBackground( Color.WHITE );
            }
            catch(Exception e){
                internalSubnetIPaddrJTextField.setBackground( INVALID_COLOR );
                isValid = false;
            }
        }        
        
        // SAVE THE VALUES ////////////////////////////////////
        if(isValid){
            natSettings.setNatEnabled( natEnabled );
            natSettings.setNatInternalAddress( natInternalAddress );
            natSettings.setNatInternalSubnet( natInternalSubnet );
        }
        else
            throw new Exception();
        
    }
    
    
    

    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        enabledButtonGroup = new javax.swing.ButtonGroup();
        explanationJPanel = new javax.swing.JPanel();
        jTextArea2 = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        natEnabledJRadioButton = new javax.swing.JRadioButton();
        natDisabledJRadioButton = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        externalRemoteJPanel = new javax.swing.JPanel();
        jTextArea3 = new javax.swing.JTextArea();
        restrictIPJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        internalAddressIPaddrJTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        internalSubnetIPaddrJTextField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        internalNetworkJLabel = new javax.swing.JLabel();
        internalRemoteJPanel = new javax.swing.JPanel();
        jTextArea1 = new javax.swing.JTextArea();
        restrictIPJPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        externalAddressJLabel = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        externalMethodJLabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        explanationJPanel.setLayout(new java.awt.GridBagLayout());

        explanationJPanel.setBorder(new javax.swing.border.TitledBorder(null, "NAT Usage", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea2.setEditable(false);
        jTextArea2.setLineWrap(true);
        jTextArea2.setText("NAT allows multiple computers in a private network to share internet access through a single shared public IP address.");
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

        enabledButtonGroup.add(natEnabledJRadioButton);
        natEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        natEnabledJRadioButton.setText("Enabled");
        natEnabledJRadioButton.setFocusPainted(false);
        natEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                natEnabledJRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(natEnabledJRadioButton, gridBagConstraints);

        enabledButtonGroup.add(natDisabledJRadioButton);
        natDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        natDisabledJRadioButton.setText("Disabled");
        natDisabledJRadioButton.setFocusPainted(false);
        natDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                natDisabledJRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(natDisabledJRadioButton, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("NAT ");
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

        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Internal Address (Gateway)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea3.setEditable(false);
        jTextArea3.setLineWrap(true);
        jTextArea3.setText("NAT requires that you assign an address on your internal network to EdgeGuard.  This internal address is the address that computers on the internal network will use to contact EdgeGuard, in an effort to access the Internet or some other external network.");
        jTextArea3.setWrapStyleWord(true);
        jTextArea3.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        externalRemoteJPanel.add(jTextArea3, gridBagConstraints);

        restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("Internal IP Address: ");
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
        restrictIPJPanel.add(internalAddressIPaddrJTextField, gridBagConstraints);

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel8.setText("Internal Subnet: ");
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
        restrictIPJPanel.add(internalSubnetIPaddrJTextField, gridBagConstraints);

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel9.setText("Network Address: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 2, 0);
        restrictIPJPanel.add(jLabel9, gridBagConstraints);

        internalNetworkJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        internalNetworkJLabel.setText("012.345.678.999");
        internalNetworkJLabel.setMinimumSize(null);
        internalNetworkJLabel.setPreferredSize(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 2, 0);
        restrictIPJPanel.add(internalNetworkJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 25;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        externalRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(externalRemoteJPanel, gridBagConstraints);

        internalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        internalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "External Address (Public Address)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setText("The external address is the address of EdgeGuard when viewed from some external network.  This is specified through the \"Network Settings\" Config Panel, either manually or automatically.  The external address is visible here only for your convenience.");
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        internalRemoteJPanel.add(jTextArea1, gridBagConstraints);

        restrictIPJPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("External IP Address: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel6, gridBagConstraints);

        externalAddressJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        externalAddressJLabel.setText("012.345.678.999");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel1.add(externalAddressJLabel, gridBagConstraints);

        jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel10.setText("Configuration Method: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel10, gridBagConstraints);

        externalMethodJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        externalMethodJLabel.setText("via XYZ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel1.add(externalMethodJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 25;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        internalRemoteJPanel.add(restrictIPJPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(internalRemoteJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void natDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_natDisabledJRadioButtonActionPerformed
        this.setNatEnabledDependency(false);
    }//GEN-LAST:event_natDisabledJRadioButtonActionPerformed

    private void natEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_natEnabledJRadioButtonActionPerformed
        this.setNatEnabledDependency(true);
    }//GEN-LAST:event_natEnabledJRadioButtonActionPerformed
    
    private void setNatEnabledDependency(boolean enabled){
        internalAddressIPaddrJTextField.setEnabled( enabled );
        internalSubnetIPaddrJTextField.setEnabled( enabled );
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup enabledButtonGroup;
    private javax.swing.JPanel explanationJPanel;
    private javax.swing.JLabel externalAddressJLabel;
    private javax.swing.JLabel externalMethodJLabel;
    private javax.swing.JPanel externalRemoteJPanel;
    public javax.swing.JTextField internalAddressIPaddrJTextField;
    private javax.swing.JLabel internalNetworkJLabel;
    private javax.swing.JPanel internalRemoteJPanel;
    public javax.swing.JTextField internalSubnetIPaddrJTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    public javax.swing.JRadioButton natDisabledJRadioButton;
    public javax.swing.JRadioButton natEnabledJRadioButton;
    private javax.swing.JPanel restrictIPJPanel;
    private javax.swing.JPanel restrictIPJPanel1;
    // End of variables declaration//GEN-END:variables
    
}
