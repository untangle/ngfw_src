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

package com.untangle.tran.nat.gui;


import java.awt.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.Util;
import com.untangle.mvvm.tran.*;
import com.untangle.tran.nat.*;


public class DnsJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {

    private static final String EXCEPTION_DNS_DOMAIN = "The Domain Name Suffix must be a valid domain name.";


    public DnsJPanel() {
        initComponents();
        Util.addPanelFocus(this, dnsMasqEnabledJRadioButton);
        Util.addFocusHighlight(suffixJTextField);
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // ENABLED ///////////
        boolean dnsEnabled = dnsMasqEnabledJRadioButton.isSelected();

        // LOCAL DOMAIN //////////
        HostName dnsLocalDomain = null;
        suffixJTextField.setBackground( Color.WHITE );
        if(dnsEnabled){
            try{
                dnsLocalDomain = HostName.parse( suffixJTextField.getText() );
            }
            catch(Exception e){
                suffixJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                throw new Exception(EXCEPTION_DNS_DOMAIN);
            }
        }

        // SAVE SETTINGS  ////////////////////////////////////
        if( !validateOnly ){
            NatCommonSettings natSettings = (NatCommonSettings) settings;
            natSettings.setDnsEnabled( dnsEnabled );
            if( dnsEnabled ){
                natSettings.setDnsLocalDomain( dnsLocalDomain );
            }
        }

    }

    boolean dnsEnabledCurrent;
    String dnsLocalDomainCurrent;

    public void doRefresh(Object settings) {
        NatCommonSettings natSettings = (NatCommonSettings) settings;

        // ENABLED ///////////
        dnsEnabledCurrent = natSettings.getDnsEnabled();
        setDnsEnabledDependency( dnsEnabledCurrent );
        if( dnsEnabledCurrent )
            dnsMasqEnabledJRadioButton.setSelected(true);
        else
            dnsMasqDisabledJRadioButton.setSelected(true);

        // LOCAL DOMAIN /////
        dnsLocalDomainCurrent = natSettings.getDnsLocalDomain().toString();
        suffixJTextField.setText( dnsLocalDomainCurrent );
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
        domainSuffixJPanel = new javax.swing.JPanel();
        jTextArea3 = new javax.swing.JTextArea();
        suffixJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        suffixJTextField = new javax.swing.JTextField();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(515, 340));
        setMinimumSize(new java.awt.Dimension(515, 340));
        setPreferredSize(new java.awt.Dimension(515, 340));
        nameserversJPanel.setLayout(new java.awt.GridBagLayout());

        nameserversJPanel.setBorder(new javax.swing.border.TitledBorder(null, "DNS Forwarding", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setText("DNS Forwarding allows your Untangle Server to act as a DNS server for the internal network.  (Note: This will also serve hostnames from the DHCP Address map and the DNS Forwarding Address Map, if they are enabled.)");
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setFocusable(false);
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
        dnsMasqEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    dnsMasqEnabledJRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add(dnsMasqEnabledJRadioButton, gridBagConstraints);

        enabledButtonGroup.add(dnsMasqDisabledJRadioButton);
        dnsMasqDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        dnsMasqDisabledJRadioButton.setText("Disabled");
        dnsMasqDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    dnsMasqDisabledJRadioButtonActionPerformed(evt);
                }
            });

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
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(nameserversJPanel, gridBagConstraints);

        domainSuffixJPanel.setLayout(new java.awt.GridBagLayout());

        domainSuffixJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Domain Name Suffix", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea3.setEditable(false);
        jTextArea3.setLineWrap(true);
        jTextArea3.setText("For example, \"acme.com\".  The Domain Name Suffix allows hosts on the internal network to be accessible by fully qualified hostname, such as \"host1.acme.com\".  The suffix is appended to unqualified hostnames in the DNS Forwarding Address Map and the DHCP Address Map.  This functionality is enabled only when DNS Forwarding is enabled.");
        jTextArea3.setWrapStyleWord(true);
        jTextArea3.setFocusable(false);
        jTextArea3.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        domainSuffixJPanel.add(jTextArea3, gridBagConstraints);

        suffixJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("Suffix:  ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        suffixJPanel.add(jLabel5, gridBagConstraints);

        suffixJTextField.setMaximumSize(new java.awt.Dimension(250, 19));
        suffixJTextField.setMinimumSize(new java.awt.Dimension(250, 19));
        suffixJTextField.setPreferredSize(new java.awt.Dimension(250, 19));
        suffixJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                public void caretUpdate(javax.swing.event.CaretEvent evt) {
                    suffixJTextFieldCaretUpdate(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        suffixJPanel.add(suffixJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 125;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        domainSuffixJPanel.add(suffixJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(domainSuffixJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void suffixJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_suffixJTextFieldCaretUpdate
        if( !suffixJTextField.getText().trim().equals(dnsLocalDomainCurrent) && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_suffixJTextFieldCaretUpdate

    private void dnsMasqDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dnsMasqDisabledJRadioButtonActionPerformed
        setDnsEnabledDependency(false);
        if( dnsEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_dnsMasqDisabledJRadioButtonActionPerformed

    private void dnsMasqEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dnsMasqEnabledJRadioButtonActionPerformed
        setDnsEnabledDependency(true);
        if( !dnsEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_dnsMasqEnabledJRadioButtonActionPerformed

    private void setDnsEnabledDependency(boolean dnsEnabled){
        suffixJTextField.setEnabled( dnsEnabled );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JRadioButton dnsMasqDisabledJRadioButton;
    public javax.swing.JRadioButton dnsMasqEnabledJRadioButton;
    private javax.swing.JPanel domainSuffixJPanel;
    private javax.swing.ButtonGroup enabledButtonGroup;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JPanel nameserversJPanel;
    private javax.swing.JPanel suffixJPanel;
    public javax.swing.JTextField suffixJTextField;
    // End of variables declaration//GEN-END:variables

}
