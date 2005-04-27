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
public class DnsJPanel extends javax.swing.JPanel {
    
    private Color INVALID_COLOR = Color.PINK;
    private Color BACKGROUND_COLOR = new Color(224, 224, 224);
    

    public DnsJPanel() {
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
        boolean dnsEnabled;
        
        // ENABLED ///////////
        try{
            dnsEnabled = natSettings.getDnsEnabled();
            if( dnsEnabled )
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
        boolean dnsEnabled;
        
        // ENABLED ///////////
        dnsEnabled = dnsMasqEnabledJRadioButton.isSelected();
        if( dnsMasqEnabledJRadioButton.isSelected() ^ dnsMasqDisabledJRadioButton.isSelected() ){
            dnsMasqEnabledJRadioButton.setBackground( BACKGROUND_COLOR );
            dnsMasqDisabledJRadioButton.setBackground( BACKGROUND_COLOR );
        }
        else{
            dnsMasqEnabledJRadioButton.setBackground( INVALID_COLOR );
            dnsMasqDisabledJRadioButton.setBackground( INVALID_COLOR );
            isValid = false;
        }

                
        // SAVE THE VALUES ////////////////////////////////////
        if(isValid){
            natSettings.setDnsEnabled( dnsEnabled );
        }
        else
            throw new Exception();
        
    }
    
    
    

    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        enabledButtonGroup = new javax.swing.ButtonGroup();
        nameserversJPanel = new javax.swing.JPanel();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        dnsMasqEnabledJRadioButton = new javax.swing.JRadioButton();
        dnsMasqDisabledJRadioButton = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        nameserversJPanel.setLayout(new java.awt.GridBagLayout());

        nameserversJPanel.setBorder(new javax.swing.border.TitledBorder(null, "DNS Forwarding", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setText("DNS Forwarding allows EdgeGuard to act as a DNS server for the internal network, by forwarding DNS requests to your external DNS server.  Your external DNS server is specified in the \"Network Settings\" Config Panel.");
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

        enabledButtonGroup.add(dnsMasqEnabledJRadioButton);
        dnsMasqEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dnsMasqEnabledJRadioButton.setText("Enabled");
        dnsMasqEnabledJRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(dnsMasqEnabledJRadioButton, gridBagConstraints);

        enabledButtonGroup.add(dnsMasqDisabledJRadioButton);
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
        jLabel3.setText("DNS Forwarding");
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

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(nameserversJPanel, gridBagConstraints);

    }//GEN-END:initComponents
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton dnsMasqDisabledJRadioButton;
    private javax.swing.JRadioButton dnsMasqEnabledJRadioButton;
    private javax.swing.ButtonGroup enabledButtonGroup;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JPanel nameserversJPanel;
    // End of variables declaration//GEN-END:variables
    
}
