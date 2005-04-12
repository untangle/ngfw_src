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
    private Color BACKGROUND_COLOR = new Color(238, 238, 238);
    

    public NatJPanel() {
        initComponents();
    }
    
    
    
    public void refresh(Object settings) throws Exception {
        if(!(settings instanceof NatSettings))
            return;
        
        NatSettings natSettings = (NatSettings) settings;
        boolean natEnabled;
        String natInternalAddress;
        String natInternalSubnet;
        String natInternalNetwork;
        String natExternalAddress;
        boolean isDhcpEnabled;
        
        // PARSE THE SETTINGS INTO FORM VALUES
        natEnabled = natSettings.getNatEnabled();
        natInternalAddress = natSettings.getNatInternalAddress().toString();
        natInternalSubnet  = natSettings.getNatInternalSubnet().toString();
        natInternalNetwork = IPaddr.and(natSettings.getNatInternalAddress(), natSettings.getNatInternalSubnet()).toString();
        natExternalAddress = Util.getMvvmContext().networkingManager().get().host().toString();
        isDhcpEnabled = Util.getMvvmContext().networkingManager().get().isDhcpEnabled();
        
        // UPDATE THE FORM
        if( natEnabled )
            natEnabledJRadioButton.setSelected(true);
        else
            natDisabledJRadioButton.setSelected(true);
        internalAddressIPaddrJTextField.setText( natInternalAddress );
        internalSubnetIPaddrJTextField.setText( natInternalSubnet );
        internalNetworkJTextField.setText( natInternalNetwork );
        externalAddressJTextField.setText( natExternalAddress );
        if( isDhcpEnabled )
            externalMethodJTextField.setText("Dynamic via DHCP");
        else
            externalMethodJTextField.setText("Manually specified");
    }

    
    
    public void save(Object settings) throws Exception {
        if(!(settings instanceof NatSettings))
            return;
        
        NatSettings natSettings = (NatSettings) settings;
        boolean natEnabled;
        IPaddr natInternalAddress;
        IPaddr natInternalSubnet;
        
        // PARSE THE FORM INTO SAVABLE VALUES
        natEnabled = natEnabledJRadioButton.isSelected();
        natInternalAddress = IPaddr.parse(internalAddressIPaddrJTextField.getText() );
        natInternalSubnet  = IPaddr.parse(internalSubnetIPaddrJTextField.getText() );
        
        // SAVE THE VALUES
        natSettings.setNatEnabled( natEnabled );
        natSettings.setNatInternalAddress( natInternalAddress );
        natSettings.setNatInternalSubnet( natInternalSubnet );
    }
    
    
    
    public boolean isValid() {
        boolean isValid = true;

        // ENABLED ///////////
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
        try{
            IPaddr.parse( internalAddressIPaddrJTextField.getText() );
            internalAddressIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            internalAddressIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        // INTERNAL SUBNET ///////
        try{
            IPaddr.parse( internalSubnetIPaddrJTextField.getText() );
            internalSubnetIPaddrJTextField.setBackground( Color.WHITE );
        }
        catch(Exception e){
            internalSubnetIPaddrJTextField.setBackground( INVALID_COLOR );
            isValid = false;
        }
        
        return isValid;
    }
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        enabledButtonGroup = new javax.swing.ButtonGroup();
        explanationJPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jTextArea2 = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        natEnabledJRadioButton = new javax.swing.JRadioButton();
        natDisabledJRadioButton = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();
        externalRemoteJPanel = new javax.swing.JPanel();
        restrictIPJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        internalAddressIPaddrJTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        internalSubnetIPaddrJTextField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        internalNetworkJTextField = new javax.swing.JTextField();
        internalRemoteJPanel = new javax.swing.JPanel();
        jTextArea1 = new javax.swing.JTextArea();
        restrictIPJPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        externalAddressJTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        externalMethodJTextField = new javax.swing.JTextField();

        setLayout(new java.awt.GridBagLayout());

        setMinimumSize(new java.awt.Dimension(600, 730));
        setPreferredSize(new java.awt.Dimension(600, 730));
        explanationJPanel.setLayout(new java.awt.GridBagLayout());

        explanationJPanel.setBorder(new javax.swing.border.TitledBorder(null, "NAT Usage", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        explanationJPanel.add(jLabel2, gridBagConstraints);

        jTextArea2.setEditable(false);
        jTextArea2.setLineWrap(true);
        jTextArea2.setText("NAT allows multiple computers in a private network to share a single public IP address.");
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
        explanationJPanel.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(explanationJPanel, gridBagConstraints);

        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Internal Address", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
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
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        restrictIPJPanel.add(jLabel9, gridBagConstraints);

        internalNetworkJTextField.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 2, 0);
        restrictIPJPanel.add(internalNetworkJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 150;
        externalRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(externalRemoteJPanel, gridBagConstraints);

        internalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        internalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "External Address", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setText("The external address is specified through the \"Networ Settings\" Config Panel, either manually or automatically.  The external address is visible here only for convenience.");
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

        externalAddressJTextField.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel1.add(externalAddressJTextField, gridBagConstraints);

        jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel10.setText("Configuration Method: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel10, gridBagConstraints);

        externalMethodJTextField.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel1.add(externalMethodJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 150;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        internalRemoteJPanel.add(restrictIPJPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(internalRemoteJPanel, gridBagConstraints);

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup enabledButtonGroup;
    private javax.swing.JPanel explanationJPanel;
    public javax.swing.JTextField externalAddressJTextField;
    public javax.swing.JTextField externalMethodJTextField;
    private javax.swing.JPanel externalRemoteJPanel;
    public javax.swing.JTextField internalAddressIPaddrJTextField;
    public javax.swing.JTextField internalNetworkJTextField;
    private javax.swing.JPanel internalRemoteJPanel;
    public javax.swing.JTextField internalSubnetIPaddrJTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    public javax.swing.JRadioButton natDisabledJRadioButton;
    public javax.swing.JRadioButton natEnabledJRadioButton;
    private javax.swing.JPanel restrictIPJPanel;
    private javax.swing.JPanel restrictIPJPanel1;
    // End of variables declaration//GEN-END:variables
    
}
