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

/**
 *
 * @author  inieves
 */
public class DhcpJPanel extends javax.swing.JPanel {
    
    /** Creates new form NetworkJPanel */
    public DhcpJPanel() {
        initComponents();
    }
    
    public void refresh( Object settings ) throws Exception {
        throw new NullPointerException();
    }

    public void save( Object settings ) throws Exception {
        throw new NullPointerException();
    }

    public boolean isValid( ) {
        throw new NullPointerException();        
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

        setMinimumSize(new java.awt.Dimension(600, 730));
        setPreferredSize(new java.awt.Dimension(600, 730));
        dhcpJPanel.setLayout(new java.awt.GridBagLayout());

        dhcpJPanel.setBorder(new javax.swing.border.TitledBorder(null, "IP Address Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 16)));
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

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "External Remote Administration Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 16)));
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

        internalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Internal Remote Administration Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 16)));
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

        tcpWindowJPanel.setBorder(new javax.swing.border.TitledBorder(null, "TCP Window Scaling Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 16)));
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
        setAllEnabled( enableRemoteJPanel, !externalAdminDisabledRadioButton.isSelected() );
        setAllEnabled( restrictIPJPanel, false );
    }//GEN-LAST:event_externalAdminDisabledRadioButtonActionPerformed

    private void externalAdminEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminEnabledRadioButtonActionPerformed
        setAllEnabled( enableRemoteJPanel, externalAdminEnabledRadioButton.isSelected() );
        setAllEnabled( restrictIPJPanel, externalAdminRestrictEnabledRadioButton.isSelected() );
    }//GEN-LAST:event_externalAdminEnabledRadioButtonActionPerformed

    private void externalAdminRestrictEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminRestrictEnabledRadioButtonActionPerformed
        if( externalAdminEnabledRadioButton.isSelected() )
            setAllEnabled( restrictIPJPanel, externalAdminRestrictEnabledRadioButton.isSelected() );
    }//GEN-LAST:event_externalAdminRestrictEnabledRadioButtonActionPerformed

    private void externalAdminRestrictDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminRestrictDisabledRadioButtonActionPerformed
        setAllEnabled( restrictIPJPanel, !externalAdminRestrictDisabledRadioButton.isSelected() );
    }//GEN-LAST:event_externalAdminRestrictDisabledRadioButtonActionPerformed

    private void dhcpDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpDisabledRadioButtonActionPerformed
        setAllEnabled( staticIPJPanel, dhcpDisabledRadioButton.isSelected() );
    }//GEN-LAST:event_dhcpDisabledRadioButtonActionPerformed

    private void dhcpEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpEnabledRadioButtonActionPerformed
        setAllEnabled( staticIPJPanel, !dhcpEnabledRadioButton.isSelected() );
    }//GEN-LAST:event_dhcpEnabledRadioButtonActionPerformed
    
    private void setAllEnabled(Container container, boolean enabled){
        Component[] components = container.getComponents();
        for(int i=0; i < components.length; i++)
                components[i].setEnabled(enabled);
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
