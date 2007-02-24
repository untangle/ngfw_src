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

package com.untangle.tran.openvpn.gui;

import com.untangle.gui.widgets.dialogs.MConfigJDialog;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.security.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.networking.*;
import com.untangle.mvvm.tran.*;

import com.untangle.tran.openvpn.*;

import java.awt.*;
import javax.swing.JDialog;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner;

public class ServerAdvancedJPanel extends javax.swing.JPanel
    implements Savable<Object>, Refreshable<Object> {

    private static final String EXCEPTION_INVALID_PRIMARY_DNS = "You must enter a valid IP address for the Primary Address";
    private static final String EXCEPTION_INVALID_SECONDARY_DNS = "You must enter a valid IP address for the Secondary Address";
    private static final String EXCEPTION_INVALID_SITE_NAME = "You must enter a site name.";

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
	this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////
  
    public ServerAdvancedJPanel() {
        initComponents();
	    Util.setPortView(portJSpinner, VpnSettings.DEFAULT_PUBLIC_PORT);
		defaultJLabel.setText("(default: " + VpnSettings.DEFAULT_PUBLIC_PORT + ")");
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // PORT //
        ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        int port = 0;
        try{ portJSpinner.commitEdit(); }
        catch(Exception e){ 
            ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(Util.EXCEPTION_PORT_RANGE);
        }
        port = (Integer) portJSpinner.getValue();

        // SITE NAME //
        nameJTextField.setBackground(Color.WHITE);
        String siteName = null;
        siteName = nameJTextField.getText();
        if( siteName.trim().length() == 0 ){
            nameJTextField.setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_INVALID_SITE_NAME);
        }

        // OVERRIDE //
        boolean overrideEnabled = overrideEnabledJRadioButton.isSelected();
        IPaddr primaryOverrideIPaddr = null;
        IPaddr secondaryOverrideIPaddr = null;
        primaryDNSIPaddrJTextField.setBackground( Color.WHITE );
        secondaryDNSIPaddrJTextField.setBackground( Color.WHITE );
        if( overrideEnabled ){
            try{
                primaryOverrideIPaddr = IPaddr.parse( primaryDNSIPaddrJTextField.getText() );
            }
            catch(Exception e){
                primaryDNSIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                throw new Exception(EXCEPTION_INVALID_PRIMARY_DNS);
            }
            if( secondaryDNSIPaddrJTextField.getText().trim().length() > 0 ){
                try{
                    secondaryOverrideIPaddr = IPaddr.parse( secondaryDNSIPaddrJTextField.getText() );
                }
                catch(Exception e){
                    secondaryDNSIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                    throw new Exception(EXCEPTION_INVALID_SECONDARY_DNS);
                }
            }
        }
		
        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            VpnSettings vpnSettings = (VpnSettings) settings;
            vpnSettings.setPublicPort( port );
            vpnSettings.setSiteName( siteName );
            vpnSettings.setIsDnsOverrideEnabled( overrideEnabled );
            if( overrideEnabled ){
                vpnSettings.setDns1( primaryOverrideIPaddr );
                vpnSettings.setDns2( secondaryOverrideIPaddr );
            }
        }
	}


    int portCurrent;
    String siteCurrent;
    boolean overrideEnabledCurrent;
    String overridePrimaryCurrent;
    String overrideSecondaryCurrent;

    public void doRefresh(Object settings){
		VpnSettings vpnSettings = (VpnSettings) settings;
	
        // PORT //
        portCurrent = vpnSettings.getPublicPort();
        portJSpinner.setValue(portCurrent);
        ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setText(Integer.toString(portCurrent));
        ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);

        // SITE //
        siteCurrent = vpnSettings.getSiteName();
        nameJTextField.setText(siteCurrent);
        nameJTextField.setBackground(Color.WHITE);

        // OVERRIDE //
        overrideEnabledCurrent = vpnSettings.getIsDnsOverrideEnabled();
        setOverrideEnabledDependency(overrideEnabledCurrent);
        if(overrideEnabledCurrent)
            overrideEnabledJRadioButton.setSelected(true);
        else
            overrideDisabledJRadioButton.setSelected(true);

        // OVERRIDE PRIMARY //
        IPaddr overridePrimaryCurrentIPaddr = vpnSettings.getDns1();
        overridePrimaryCurrent = overridePrimaryCurrentIPaddr!=null?overridePrimaryCurrentIPaddr.toString():"";
        primaryDNSIPaddrJTextField.setBackground( Color.WHITE );
        primaryDNSIPaddrJTextField.setText( overridePrimaryCurrent );
        

        // OVERRIDE SECONDARY //
        IPaddr overrideSecondaryCurrentIPaddr = vpnSettings.getDns2();
        overrideSecondaryCurrent = overrideSecondaryCurrentIPaddr!=null?overrideSecondaryCurrentIPaddr.toString():"";
        secondaryDNSIPaddrJTextField.setBackground( Color.WHITE );
        secondaryDNSIPaddrJTextField.setText( overrideSecondaryCurrent );
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                overrideButtonGroup = new javax.swing.ButtonGroup();
                portJPanel = new javax.swing.JPanel();
                jLabel9 = new javax.swing.JLabel();
                staticIPJPanel = new javax.swing.JPanel();
                portJLabel = new javax.swing.JLabel();
                portJSpinner = new javax.swing.JSpinner();
                defaultJLabel = new javax.swing.JLabel();
                siteJPanel = new javax.swing.JPanel();
                jLabel10 = new javax.swing.JLabel();
                staticIPJPanel1 = new javax.swing.JPanel();
                portJLabel1 = new javax.swing.JLabel();
                nameJTextField = new javax.swing.JTextField();
                dnsJPanel = new javax.swing.JPanel();
                jTextArea2 = new javax.swing.JTextArea();
                jPanel1 = new javax.swing.JPanel();
                overrideEnabledJRadioButton = new javax.swing.JRadioButton();
                overrideDisabledJRadioButton = new javax.swing.JRadioButton();
                jLabel1 = new javax.swing.JLabel();
                restrictIPJPanel = new javax.swing.JPanel();
                primaryDNSJLabel = new javax.swing.JLabel();
                primaryDNSIPaddrJTextField = new javax.swing.JTextField();
                secondaryDNSJLabel = new javax.swing.JLabel();
                secondaryDNSIPaddrJTextField = new javax.swing.JTextField();
                optionalJLabel = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(515, 534));
                setMinimumSize(new java.awt.Dimension(515, 534));
                setPreferredSize(new java.awt.Dimension(515, 534));
                portJPanel.setLayout(new java.awt.GridBagLayout());

                portJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Server Port", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel9.setText("<html> The Server Port is the port that the VPN Server will accept connections from.  These connections are UDP.  This setting is intended to allow you to run your VPN Server from a non-standard port in case you already have a VPN running, or you have a complex network setup.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                portJPanel.add(jLabel9, gridBagConstraints);

                staticIPJPanel.setLayout(new java.awt.GridBagLayout());

                portJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                portJLabel.setText("Port: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel.add(portJLabel, gridBagConstraints);

                portJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
                portJSpinner.setMaximumSize(new java.awt.Dimension(75, 19));
                portJSpinner.setMinimumSize(new java.awt.Dimension(75, 19));
                portJSpinner.setPreferredSize(new java.awt.Dimension(75, 19));
                portJSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                        public void stateChanged(javax.swing.event.ChangeEvent evt) {
                                portJSpinnerStateChanged(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                staticIPJPanel.add(portJSpinner, gridBagConstraints);

                defaultJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
                staticIPJPanel.add(defaultJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.ipadx = 150;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
                portJPanel.add(staticIPJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(portJPanel, gridBagConstraints);

                siteJPanel.setLayout(new java.awt.GridBagLayout());

                siteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Site Name", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel10.setText("<html> If you have multiple VPN sites, you may want to use this to change the name of this site.  Then cients can choose between multiple VPN sites to connect to, because each one will have a different name.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
                siteJPanel.add(jLabel10, gridBagConstraints);

                staticIPJPanel1.setLayout(new java.awt.GridBagLayout());

                portJLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                portJLabel1.setText("Name: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                staticIPJPanel1.add(portJLabel1, gridBagConstraints);

                nameJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                nameJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                nameJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                nameJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                nameJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                staticIPJPanel1.add(nameJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.ipadx = 150;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
                siteJPanel.add(staticIPJPanel1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(siteJPanel, gridBagConstraints);

                dnsJPanel.setLayout(new java.awt.GridBagLayout());

                dnsJPanel.setBorder(new javax.swing.border.TitledBorder(null, "DNS Override", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                jTextArea2.setEditable(false);
                jTextArea2.setLineWrap(true);
                jTextArea2.setText("DNS Override allows you to specify up to two IP addresses of DNS servers which you would like to export to your VPN clients.  These addresses will be used instead of the Untangle Server's own DNS settings.  This can be useful if you need to make an Active Directory server available to your VPN clients.");
                jTextArea2.setWrapStyleWord(true);
                jTextArea2.setOpaque(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
                dnsJPanel.add(jTextArea2, gridBagConstraints);

                jPanel1.setLayout(new java.awt.GridBagLayout());

                overrideButtonGroup.add(overrideEnabledJRadioButton);
                overrideEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                overrideEnabledJRadioButton.setText("Enabled");
                overrideEnabledJRadioButton.setFocusPainted(false);
                overrideEnabledJRadioButton.setFocusable(false);
                overrideEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                overrideEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel1.add(overrideEnabledJRadioButton, gridBagConstraints);

                overrideButtonGroup.add(overrideDisabledJRadioButton);
                overrideDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                overrideDisabledJRadioButton.setText("Disabled");
                overrideDisabledJRadioButton.setFocusPainted(false);
                overrideDisabledJRadioButton.setFocusable(false);
                overrideDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                overrideDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                jPanel1.add(overrideDisabledJRadioButton, gridBagConstraints);

                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("DNS Override");
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
                dnsJPanel.add(jPanel1, gridBagConstraints);

                restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

                primaryDNSJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                primaryDNSJLabel.setText("Primary IP Address: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(primaryDNSJLabel, gridBagConstraints);

                primaryDNSIPaddrJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                primaryDNSIPaddrJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                primaryDNSIPaddrJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                primaryDNSIPaddrJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                primaryDNSIPaddrJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                restrictIPJPanel.add(primaryDNSIPaddrJTextField, gridBagConstraints);

                secondaryDNSJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                secondaryDNSJLabel.setText("Secondary IP Address: ");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(secondaryDNSJLabel, gridBagConstraints);

                secondaryDNSIPaddrJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                secondaryDNSIPaddrJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                secondaryDNSIPaddrJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                secondaryDNSIPaddrJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                secondaryDNSIPaddrJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                restrictIPJPanel.add(secondaryDNSIPaddrJTextField, gridBagConstraints);

                optionalJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                optionalJLabel.setText(" (optional)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(optionalJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.ipadx = 25;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 5, 0);
                dnsJPanel.add(restrictIPJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
                add(dnsJPanel, gridBagConstraints);

        }//GEN-END:initComponents

		private void nameJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_nameJTextFieldCaretUpdate
            if( !nameJTextField.getText().trim().equals(siteCurrent) && (settingsChangedListener!=null) )
                settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_nameJTextFieldCaretUpdate

		private void secondaryDNSIPaddrJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_secondaryDNSIPaddrJTextFieldCaretUpdate
            if( !secondaryDNSIPaddrJTextField.getText().trim().equals(overrideSecondaryCurrent) && (settingsChangedListener != null) )
                settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_secondaryDNSIPaddrJTextFieldCaretUpdate

		private void primaryDNSIPaddrJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_primaryDNSIPaddrJTextFieldCaretUpdate
            if( !primaryDNSIPaddrJTextField.getText().trim().equals(overridePrimaryCurrent) && (settingsChangedListener != null) )
                settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_primaryDNSIPaddrJTextFieldCaretUpdate

		private void overrideDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_overrideDisabledJRadioButtonActionPerformed
            this.setOverrideEnabledDependency(true);
            if( overrideEnabledCurrent && (settingsChangedListener != null) )
                settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_overrideDisabledJRadioButtonActionPerformed

		private void overrideEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_overrideEnabledJRadioButtonActionPerformed
            this.setOverrideEnabledDependency(true);
            if( !overrideEnabledCurrent && (settingsChangedListener != null) )
                settingsChangedListener.settingsChanged(this);
		}//GEN-LAST:event_overrideEnabledJRadioButtonActionPerformed

		private void portJSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_portJSpinnerStateChanged
		if( !portJSpinner.getValue().equals(portCurrent) && (settingsChangedListener != null) )
		    settingsChangedListener.settingsChanged(this);				// TODO add your handling code here:
		}//GEN-LAST:event_portJSpinnerStateChanged
                                
    private void setOverrideEnabledDependency(boolean enabled){
        primaryDNSJLabel.setEnabled( enabled );
        primaryDNSIPaddrJTextField.setEnabled( enabled );
        secondaryDNSJLabel.setEnabled( enabled );
        secondaryDNSIPaddrJTextField.setEnabled( enabled );
    }
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel defaultJLabel;
        private javax.swing.JPanel dnsJPanel;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JTextArea jTextArea2;
        private javax.swing.JTextField nameJTextField;
        private javax.swing.JLabel optionalJLabel;
        private javax.swing.ButtonGroup overrideButtonGroup;
        public javax.swing.JRadioButton overrideDisabledJRadioButton;
        public javax.swing.JRadioButton overrideEnabledJRadioButton;
        private javax.swing.JLabel portJLabel;
        private javax.swing.JLabel portJLabel1;
        private javax.swing.JPanel portJPanel;
        private javax.swing.JSpinner portJSpinner;
        public javax.swing.JTextField primaryDNSIPaddrJTextField;
        private javax.swing.JLabel primaryDNSJLabel;
        private javax.swing.JPanel restrictIPJPanel;
        public javax.swing.JTextField secondaryDNSIPaddrJTextField;
        private javax.swing.JLabel secondaryDNSJLabel;
        private javax.swing.JPanel siteJPanel;
        private javax.swing.JPanel staticIPJPanel;
        private javax.swing.JPanel staticIPJPanel1;
        // End of variables declaration//GEN-END:variables
    

}
