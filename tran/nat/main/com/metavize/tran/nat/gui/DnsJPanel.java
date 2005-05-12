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


import com.metavize.gui.transform.*;
import com.metavize.gui.util.Util;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.tran.nat.*;

import java.awt.*;

/**
 *
 * @author  inieves
 */
public class DnsJPanel extends javax.swing.JPanel implements Savable, Refreshable {
    
    public DnsJPanel() {
        initComponents();
    }
    
    
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
        // ENABLED ///////////
        boolean dnsEnabled = dnsMasqEnabledJRadioButton.isSelected();
                
        // SAVE SETTINGS  ////////////////////////////////////
	if( !validateOnly ){
	    NatSettings natSettings = (NatSettings) settings;
	    natSettings.setDnsEnabled( dnsEnabled );
	}
        
    }
    
    
    public void doRefresh(Object settings) {
        NatSettings natSettings = (NatSettings) settings;

        // ENABLED ///////////
        boolean dnsEnabled;
	dnsEnabled = natSettings.getDnsEnabled();
	if( dnsEnabled )
	    dnsMasqEnabledJRadioButton.setSelected(true);
	else
	    dnsMasqDisabledJRadioButton.setSelected(true);
        
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

        setMinimumSize(new java.awt.Dimension(530, 175));
        setPreferredSize(new java.awt.Dimension(530, 175));
        nameserversJPanel.setLayout(new java.awt.GridBagLayout());

        nameserversJPanel.setBorder(new javax.swing.border.TitledBorder(null, "DNS Forwarding", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setText("DNS Forwarding allows EdgeGuard to act as a DNS server for the internal network, by forwarding DNS requests to your external DNS server.  Your external DNS server is specified in the \"Network Settings\" Config Panel.  (Note: If DHCP is enabled, this will also return hostnames served by DHCP.)");
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
    public javax.swing.JRadioButton dnsMasqDisabledJRadioButton;
    public javax.swing.JRadioButton dnsMasqEnabledJRadioButton;
    private javax.swing.ButtonGroup enabledButtonGroup;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JPanel nameserversJPanel;
    // End of variables declaration//GEN-END:variables
    
}
