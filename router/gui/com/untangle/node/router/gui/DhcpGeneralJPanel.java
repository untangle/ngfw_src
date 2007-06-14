/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.router.gui;

import java.awt.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.Util;
import com.untangle.uvm.node.IPaddr;
import com.untangle.node.router.*;


public class DhcpGeneralJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {

    private static final String EXCEPTION_DHCP_RANGE_START = "The DHCP Start Address must be a valid IP address.";
    private static final String EXCEPTION_DHCP_RANGE_END = "The DHCP End Address must be a valid IP address.";

    public DhcpGeneralJPanel() {
        initComponents();
        Util.addPanelFocus(this, dhcpEnabledJRadioButton);
        Util.addFocusHighlight(startAddressIPaddrJTextField);
        Util.addFocusHighlight(endAddressIPaddrJTextField);
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // DHCP ENABLED ///////////
        boolean dhcpIsEnabled = dhcpEnabledJRadioButton.isSelected();

        // DYNAMIC RANGE START //////
        IPaddr dhcpStartAddress = null;
        startAddressIPaddrJTextField.setBackground( Color.WHITE );
        if(dhcpIsEnabled){
            try{
                dhcpStartAddress = IPaddr.parse( startAddressIPaddrJTextField.getText() );
            }
            catch(Exception e){
                startAddressIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                throw new Exception(EXCEPTION_DHCP_RANGE_START);
            }
        }

        // DYNAMIC RANGE END //////
        IPaddr dhcpEndAddress = null;
        endAddressIPaddrJTextField.setBackground( Color.WHITE );
        if(dhcpIsEnabled){
            try{
                dhcpEndAddress = IPaddr.parse( endAddressIPaddrJTextField.getText() );
            }
            catch(Exception e){
                endAddressIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                throw new Exception(EXCEPTION_DHCP_RANGE_END);
            }
        }


        // SAVE SETTINGS ////////////////////////////////////
        if( !validateOnly ){
            RouterCommonSettings routerSettings = (RouterCommonSettings) settings;
            routerSettings.setDhcpEnabled( dhcpIsEnabled );
            if(dhcpIsEnabled){
                routerSettings.setDhcpStartAndEndAddress(dhcpStartAddress, dhcpEndAddress);
            }
        }

    }

    boolean dhcpIsEnabledCurrent;
    String dhcpStartAddressCurrent;
    String dhcpEndAddressCurrent;

    public void doRefresh(Object settings) {
        RouterCommonSettings routerSettings = (RouterCommonSettings) settings;

        // DHCP ENABLED ///////////
        dhcpIsEnabledCurrent = routerSettings.getDhcpEnabled();
        this.setDhcpEnabledDependency(dhcpIsEnabledCurrent);
        if( dhcpIsEnabledCurrent )
            dhcpEnabledJRadioButton.setSelected(true);
        else
            dhcpDisabledJRadioButton.setSelected(true);

        // DYNAMIC RANGE START //////
        dhcpStartAddressCurrent = routerSettings.getDhcpStartAddress().toString();
        startAddressIPaddrJTextField.setText( dhcpStartAddressCurrent );
        startAddressIPaddrJTextField.setBackground( Color.WHITE );

        // DYNAMIC RANGE END //////
        dhcpEndAddressCurrent = routerSettings.getDhcpEndAddress().toString();
        endAddressIPaddrJTextField.setText( dhcpEndAddressCurrent );
        endAddressIPaddrJTextField.setBackground( Color.WHITE );
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

        setMinimumSize(new java.awt.Dimension(515, 333));
        setPreferredSize(new java.awt.Dimension(515, 333));
        explanationJPanel.setLayout(new java.awt.GridBagLayout());

        explanationJPanel.setBorder(new javax.swing.border.TitledBorder(null, "DHCP", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea2.setEditable(false);
        jTextArea2.setLineWrap(true);
        jTextArea2.setText("DHCP dynamically assigns IP addresses to computers in the internal network.  The Address Map can be used in addition to this to statically assign IP addresses to computers in the internal network, based on the computers' MAC addresses.");
        jTextArea2.setWrapStyleWord(true);
        jTextArea2.setFocusable(false);
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
        jTextArea3.setText("DHCP assigns dynamic addresses from a pool.  The pool of IP addresses must be specified as a range, with a beginning (start) and an end.  You can use the Address Map to specify that a computer on the internal network should be assigned a static address.");
        jTextArea3.setWrapStyleWord(true);
        jTextArea3.setFocusable(false);
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

        startAddressIPaddrJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        startAddressIPaddrJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        startAddressIPaddrJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        startAddressIPaddrJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                public void caretUpdate(javax.swing.event.CaretEvent evt) {
                    startAddressIPaddrJTextFieldCaretUpdate(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel.add(startAddressIPaddrJTextField, gridBagConstraints);

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel8.setText("IP Address Range End: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel8, gridBagConstraints);

        endAddressIPaddrJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        endAddressIPaddrJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        endAddressIPaddrJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        endAddressIPaddrJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                public void caretUpdate(javax.swing.event.CaretEvent evt) {
                    endAddressIPaddrJTextFieldCaretUpdate(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
        restrictIPJPanel.add(endAddressIPaddrJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 125;
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

    private void endAddressIPaddrJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_endAddressIPaddrJTextFieldCaretUpdate
        if( !endAddressIPaddrJTextField.getText().trim().equals(dhcpEndAddressCurrent) && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_endAddressIPaddrJTextFieldCaretUpdate

    private void startAddressIPaddrJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_startAddressIPaddrJTextFieldCaretUpdate
        if( !startAddressIPaddrJTextField.getText().trim().equals(dhcpStartAddressCurrent) && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_startAddressIPaddrJTextFieldCaretUpdate

    private void dhcpDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpDisabledJRadioButtonActionPerformed
        this.setDhcpEnabledDependency(false);
        if( dhcpIsEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_dhcpDisabledJRadioButtonActionPerformed

    private void dhcpEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dhcpEnabledJRadioButtonActionPerformed
        this.setDhcpEnabledDependency(true);
        if( !dhcpIsEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
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
