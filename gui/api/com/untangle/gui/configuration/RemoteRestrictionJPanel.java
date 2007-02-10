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

package com.untangle.gui.configuration;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.security.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;

import com.untangle.mvvm.networking.AccessSettings;
import com.untangle.mvvm.networking.AddressSettings;

import javax.swing.SpinnerNumberModel;
import java.awt.*;
import javax.swing.JSpinner;

public class RemoteRestrictionJPanel extends javax.swing.JPanel
    implements Savable<RemoteCompoundSettings>, Refreshable<RemoteCompoundSettings> {

    private static final String EXCEPTION_OUTSIDE_ACCESS_NETWORK = "Invalid External Remote Administration \"IP Address\" specified.";
    private static final String EXCEPTION_OUTSIDE_ACCESS_NETMASK = "Invalid External Remote Administration \"Netmask\" specified.";

    
    public RemoteRestrictionJPanel() {
        initComponents();
	Util.setPortView(externalAccessPortJSpinner, 443);
    }

    public void doSave(RemoteCompoundSettings remoteCompoundSettings, boolean validateOnly) throws Exception {

	// OUTSIDE ACCESS ENABLED ////////
	boolean isOutsideAccessEnabled = externalAccessEnabledRadioButton.isSelected();
	
	// OUTSIDE ACCESS PORT //////
	((JSpinner.DefaultEditor)externalAccessPortJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
	int httpsPort = 0;
	if( isOutsideAccessEnabled ){
	    try{ externalAccessPortJSpinner.commitEdit(); }
	    catch(Exception e){ 
		((JSpinner.DefaultEditor)externalAccessPortJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
		throw new Exception(Util.EXCEPTION_PORT_RANGE);
	    }
	    httpsPort = (Integer) externalAccessPortJSpinner.getValue();
	}

	// OUTSIDE ACCESS RESTRICTIONS /////
	boolean isOutsideAdministrationEnabled = !restrictAdminJCheckBox.isSelected();
	boolean isOutsideReportingEnabled = !restrictReportingJCheckBox.isSelected();
	boolean isOutsideQuarantineEnabled = !restrictQuarantineJCheckBox.isSelected();

	// OUTSIDE ACCESS IP RESTRICTION ///////
	boolean isOutsideAccessRestricted = externalAdminRestrictEnabledRadioButton.isSelected();

	// OUTSIDE ACCESS IP RESTRICTION ADDRESS /////////
        restrictIPaddrJTextField.setBackground( Color.WHITE );
	IPaddr outsideNetwork = null;
	if( isOutsideAccessEnabled && isOutsideAccessRestricted ){
	    try{
		outsideNetwork = IPaddr.parse( restrictIPaddrJTextField.getText() );
		if( outsideNetwork.isEmpty() )
		    throw new Exception();
	    }
	    catch(Exception e){
                restrictIPaddrJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_OUTSIDE_ACCESS_NETWORK);
            }
        }

	// OUTSIDE ACCESS IP RESTRICTION NETMASK /////////
        restrictNetmaskJTextField.setBackground( Color.WHITE );
	IPaddr outsideNetmask = null;
	if( isOutsideAccessEnabled && isOutsideAccessRestricted ){	    
	    try{
		outsideNetmask = IPaddr.parse( restrictNetmaskJTextField.getText() );
	    }
	    catch ( Exception e ) {
		restrictNetmaskJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		throw new Exception(EXCEPTION_OUTSIDE_ACCESS_NETMASK);
            }
	}

	// INSIDE INSECURE ENABLED //////
	boolean isInsideInsecureEnabled = internalAdminEnabledRadioButton.isSelected();
        
	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    AccessSettings accessSettings = remoteCompoundSettings.getAccessSettings();
            AddressSettings addressSettings = remoteCompoundSettings.getAddressSettings();
            
	    accessSettings.setIsOutsideAccessEnabled( isOutsideAccessEnabled );
	    if( isOutsideAccessEnabled ){
		addressSettings.setHttpsPort( httpsPort );
		accessSettings.setIsOutsideAdministrationEnabled(isOutsideAdministrationEnabled);
		accessSettings.setIsOutsideReportingEnabled(isOutsideReportingEnabled);
		accessSettings.setIsOutsideQuarantineEnabled(isOutsideQuarantineEnabled);
		accessSettings.setIsOutsideAccessRestricted( isOutsideAccessRestricted );
		if( isOutsideAccessRestricted ){
		    accessSettings.setOutsideNetwork( outsideNetwork );
		    accessSettings.setOutsideNetmask( outsideNetmask );
		}
	    }
	    accessSettings.setIsInsideInsecureEnabled( isInsideInsecureEnabled );
        }
    }

    public void doRefresh(RemoteCompoundSettings remoteCompoundSettings){
        AccessSettings accessSettings = remoteCompoundSettings.getAccessSettings();
        AddressSettings addressSettings = remoteCompoundSettings.getAddressSettings();
        
	// OUTSIDE ACCESS ENABLED //////
	boolean isOutsideAccessEnabled = accessSettings.getIsOutsideAccessEnabled();
	setOutsideAccessEnabledDependency( isOutsideAccessEnabled );
	if( isOutsideAccessEnabled )
            externalAccessEnabledRadioButton.setSelected(true);
        else
            externalAccessDisabledRadioButton.setSelected(true);

	// PORT ///
	int httpsPort = addressSettings.getHttpsPort();
	externalAccessPortJSpinner.setValue( httpsPort );
	((JSpinner.DefaultEditor)externalAccessPortJSpinner.getEditor()).getTextField().setText(Integer.toString(httpsPort));
	((JSpinner.DefaultEditor)externalAccessPortJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
	
	// OUTSIDE ACCESS RESTRICTIONS ////
	boolean isOutsideAdministrationEnabled = accessSettings.getIsOutsideAdministrationEnabled();
	restrictAdminJCheckBox.setSelected(!isOutsideAdministrationEnabled);
	boolean isOutsideReportingEnabled = accessSettings.getIsOutsideReportingEnabled();
	restrictReportingJCheckBox.setSelected(!isOutsideReportingEnabled);
	boolean isOutsideQuarantineEnabled = accessSettings.getIsOutsideQuarantineEnabled();
	restrictQuarantineJCheckBox.setSelected(!isOutsideQuarantineEnabled);

	// OUTSIDE ACCESS IP RESTRICTED /////
	boolean isOutsideAccessRestricted = accessSettings.getIsOutsideAccessRestricted();
	if( isOutsideAccessRestricted )
            externalAdminRestrictEnabledRadioButton.setSelected(true);
        else
            externalAdminRestrictDisabledRadioButton.setSelected(true);
        
	// OUTSIDE ACCESS IP RESTRICTED NETWORK //////
        restrictIPaddrJTextField.setText( accessSettings.getOutsideNetwork().toString() );
	restrictIPaddrJTextField.setBackground( Color.WHITE );

	// OUTSIDE ACCESS IP RESTRICTED NETMASK /////
        restrictNetmaskJTextField.setText( accessSettings.getOutsideNetmask().toString() );
	restrictNetmaskJTextField.setBackground( Color.WHITE );
        
	// INSIDE INSECURE ENABLED ///////
	boolean isInsideInsecureEnabled = accessSettings.getIsInsideInsecureEnabled();
	if( isInsideInsecureEnabled )
            internalAdminEnabledRadioButton.setSelected(true);
        else
            internalAdminDisabledRadioButton.setSelected(true);
        	
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                dhcpButtonGroup = new javax.swing.ButtonGroup();
                tcpWindowButtonGroup = new javax.swing.ButtonGroup();
                externalAdminButtonGroup = new javax.swing.ButtonGroup();
                internalAdminButtonGroup = new javax.swing.ButtonGroup();
                restrictAdminButtonGroup = new javax.swing.ButtonGroup();
                sshButtonGroup = new javax.swing.ButtonGroup();
                externalRemoteJPanel = new javax.swing.JPanel();
                externalAccessDisabledRadioButton = new javax.swing.JRadioButton();
                externalAccessEnabledRadioButton = new javax.swing.JRadioButton();
                externalAccessPortJLabel = new javax.swing.JLabel();
                externalAccessPortJSpinner = new javax.swing.JSpinner();
                jSeparator2 = new javax.swing.JSeparator();
                enableRemoteJPanel = new javax.swing.JPanel();
                externalAdminRestrictDisabledRadioButton = new javax.swing.JRadioButton();
                externalAdminRestrictEnabledRadioButton = new javax.swing.JRadioButton();
                restrictIPJPanel = new javax.swing.JPanel();
                restrictIPaddrJLabel = new javax.swing.JLabel();
                restrictIPaddrJTextField = new javax.swing.JTextField();
                restrictNetmaskJLabel = new javax.swing.JLabel();
                restrictNetmaskJTextField = new javax.swing.JTextField();
                jSeparator3 = new javax.swing.JSeparator();
                restrictJPanel = new javax.swing.JPanel();
                restrictIPJPanel1 = new javax.swing.JPanel();
                restrictAdminJLabel = new javax.swing.JLabel();
                restrictAdminJCheckBox = new javax.swing.JCheckBox();
                restrictReportingJLabel = new javax.swing.JLabel();
                restrictReportingJCheckBox = new javax.swing.JCheckBox();
                restrictQuarantineJLabel = new javax.swing.JLabel();
                restrictQuarantineJCheckBox = new javax.swing.JCheckBox();
                internalRemoteJPanel = new javax.swing.JPanel();
                internalAdminEnabledRadioButton = new javax.swing.JRadioButton();
                internalAdminDisabledRadioButton = new javax.swing.JRadioButton();
                jLabel6 = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(550, 494));
                setMinimumSize(new java.awt.Dimension(550, 494));
                setPreferredSize(new java.awt.Dimension(550, 494));
                externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Outside Access", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                externalAdminButtonGroup.add(externalAccessDisabledRadioButton);
                externalAccessDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                externalAccessDisabledRadioButton.setText("<html><b>Disable</b> access outside the local network.  This will prevent any outside communication such as remote administration, remote report viewing, remote quarantine access, etc.</html>");
                externalAccessDisabledRadioButton.setFocusPainted(false);
                externalAccessDisabledRadioButton.setFocusable(false);
                externalAccessDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                externalAccessDisabledRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                externalRemoteJPanel.add(externalAccessDisabledRadioButton, gridBagConstraints);

                externalAdminButtonGroup.add(externalAccessEnabledRadioButton);
                externalAccessEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                externalAccessEnabledRadioButton.setText("<html><b>Enable</b> access outside the local network, via secure http (https).(This is the default settings)</html>");
                externalAccessEnabledRadioButton.setFocusPainted(false);
                externalAccessEnabledRadioButton.setFocusable(false);
                externalAccessEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                externalAccessEnabledRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                externalRemoteJPanel.add(externalAccessEnabledRadioButton, gridBagConstraints);

                externalAccessPortJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                externalAccessPortJLabel.setText("Outside Https Port:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 10, 0);
                externalRemoteJPanel.add(externalAccessPortJLabel, gridBagConstraints);

                externalAccessPortJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
                externalAccessPortJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
                externalAccessPortJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
                externalAccessPortJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 163, 10, 0);
                externalRemoteJPanel.add(externalAccessPortJSpinner, gridBagConstraints);

                jSeparator2.setForeground(new java.awt.Color(200, 200, 200));
                jSeparator2.setPreferredSize(new java.awt.Dimension(0, 1));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                externalRemoteJPanel.add(jSeparator2, gridBagConstraints);

                enableRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                restrictAdminButtonGroup.add(externalAdminRestrictDisabledRadioButton);
                externalAdminRestrictDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                externalAdminRestrictDisabledRadioButton.setText("<html><b>Allow</b> access to any outside IP address.</html>");
                externalAdminRestrictDisabledRadioButton.setFocusPainted(false);
                externalAdminRestrictDisabledRadioButton.setFocusable(false);
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
                externalAdminRestrictEnabledRadioButton.setText("<html><b>Restrict</b> access to the IP address(es):</html>");
                externalAdminRestrictEnabledRadioButton.setFocusPainted(false);
                externalAdminRestrictEnabledRadioButton.setFocusable(false);
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

                restrictIPaddrJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                restrictIPaddrJLabel.setText("IP Address:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(restrictIPaddrJLabel, gridBagConstraints);

                restrictIPaddrJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                restrictIPaddrJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                restrictIPaddrJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                restrictIPJPanel.add(restrictIPaddrJTextField, gridBagConstraints);

                restrictNetmaskJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                restrictNetmaskJLabel.setText("Netmask:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(restrictNetmaskJLabel, gridBagConstraints);

                restrictNetmaskJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                restrictNetmaskJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                restrictNetmaskJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
                restrictIPJPanel.add(restrictNetmaskJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 10, 0);
                enableRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
                externalRemoteJPanel.add(enableRemoteJPanel, gridBagConstraints);

                jSeparator3.setForeground(new java.awt.Color(200, 200, 200));
                jSeparator3.setPreferredSize(new java.awt.Dimension(0, 1));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                externalRemoteJPanel.add(jSeparator3, gridBagConstraints);

                restrictJPanel.setLayout(new java.awt.GridBagLayout());

                restrictIPJPanel1.setLayout(new java.awt.GridBagLayout());

                restrictAdminJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                restrictAdminJLabel.setText("Disable Outside Administration:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel1.add(restrictAdminJLabel, gridBagConstraints);

                restrictAdminJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                restrictIPJPanel1.add(restrictAdminJCheckBox, gridBagConstraints);

                restrictReportingJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                restrictReportingJLabel.setText("Disable Outside Report Viewing:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel1.add(restrictReportingJLabel, gridBagConstraints);

                restrictReportingJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                restrictIPJPanel1.add(restrictReportingJCheckBox, gridBagConstraints);

                restrictQuarantineJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                restrictQuarantineJLabel.setText("Disable Outside Quarantine Access:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel1.add(restrictQuarantineJLabel, gridBagConstraints);

                restrictQuarantineJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                restrictIPJPanel1.add(restrictQuarantineJCheckBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                restrictJPanel.add(restrictIPJPanel1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
                externalRemoteJPanel.add(restrictJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(externalRemoteJPanel, gridBagConstraints);

                internalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                internalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Internal Remote Administration", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                internalAdminButtonGroup.add(internalAdminEnabledRadioButton);
                internalAdminEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                internalAdminEnabledRadioButton.setText("<html><b>Allow</b> Remote Administration inside the local network, via http.<br>(This is the default setting.)</html>");
                internalAdminEnabledRadioButton.setFocusPainted(false);
                internalAdminEnabledRadioButton.setFocusable(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                internalRemoteJPanel.add(internalAdminEnabledRadioButton, gridBagConstraints);

                internalAdminButtonGroup.add(internalAdminDisabledRadioButton);
                internalAdminDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                internalAdminDisabledRadioButton.setText("<html><b>Disallow</b> Remote Administration inside the local network, via http.</html>");
                internalAdminDisabledRadioButton.setFocusPainted(false);
                internalAdminDisabledRadioButton.setFocusable(false);
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                internalRemoteJPanel.add(internalAdminDisabledRadioButton, gridBagConstraints);

                jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel6.setText("Note:  Internal Remote Administration via secure http (https) is always enabled");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 5, 0);
                internalRemoteJPanel.add(jLabel6, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(internalRemoteJPanel, gridBagConstraints);

        }//GEN-END:initComponents
    
    private void externalAccessDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAccessDisabledRadioButtonActionPerformed
        setOutsideAccessEnabledDependency( false );
    }//GEN-LAST:event_externalAccessDisabledRadioButtonActionPerformed
    
    private void externalAccessEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAccessEnabledRadioButtonActionPerformed
        setOutsideAccessEnabledDependency( true );
    }//GEN-LAST:event_externalAccessEnabledRadioButtonActionPerformed
    
    private void externalAdminRestrictEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminRestrictEnabledRadioButtonActionPerformed
	setOutsideAccessRestrictedDependency( true );
    }//GEN-LAST:event_externalAdminRestrictEnabledRadioButtonActionPerformed
    
    private void externalAdminRestrictDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminRestrictDisabledRadioButtonActionPerformed
	setOutsideAccessRestrictedDependency( false );
    }//GEN-LAST:event_externalAdminRestrictDisabledRadioButtonActionPerformed
    
    
    private void setOutsideAccessEnabledDependency(boolean enabled){
	externalAccessPortJSpinner.setEnabled( enabled );
	externalAccessPortJLabel.setEnabled( enabled );
	restrictAdminJCheckBox.setEnabled( enabled );
	restrictAdminJLabel.setEnabled( enabled );
	restrictReportingJCheckBox.setEnabled( enabled );
	restrictReportingJLabel.setEnabled( enabled );
	restrictQuarantineJCheckBox.setEnabled( enabled );
	restrictQuarantineJLabel.setEnabled( enabled );
	externalAdminRestrictDisabledRadioButton.setEnabled( enabled );
	externalAdminRestrictEnabledRadioButton.setEnabled( enabled );
	if( enabled )
	    setOutsideAccessRestrictedDependency( externalAdminRestrictEnabledRadioButton.isSelected() );
	else
	    setOutsideAccessRestrictedDependency( false );
    }
 
    private void setOutsideAccessRestrictedDependency(boolean enabled){
	restrictIPaddrJTextField.setEnabled( enabled );
	restrictIPaddrJLabel.setEnabled( enabled );
	restrictNetmaskJTextField.setEnabled( enabled );
	restrictNetmaskJLabel.setEnabled( enabled );
    }

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.ButtonGroup dhcpButtonGroup;
        private javax.swing.JPanel enableRemoteJPanel;
        public javax.swing.JRadioButton externalAccessDisabledRadioButton;
        public javax.swing.JRadioButton externalAccessEnabledRadioButton;
        private javax.swing.JLabel externalAccessPortJLabel;
        private javax.swing.JSpinner externalAccessPortJSpinner;
        private javax.swing.ButtonGroup externalAdminButtonGroup;
        public javax.swing.JRadioButton externalAdminRestrictDisabledRadioButton;
        public javax.swing.JRadioButton externalAdminRestrictEnabledRadioButton;
        private javax.swing.JPanel externalRemoteJPanel;
        private javax.swing.ButtonGroup internalAdminButtonGroup;
        public javax.swing.JRadioButton internalAdminDisabledRadioButton;
        public javax.swing.JRadioButton internalAdminEnabledRadioButton;
        private javax.swing.JPanel internalRemoteJPanel;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JSeparator jSeparator2;
        private javax.swing.JSeparator jSeparator3;
        private javax.swing.ButtonGroup restrictAdminButtonGroup;
        private javax.swing.JCheckBox restrictAdminJCheckBox;
        private javax.swing.JLabel restrictAdminJLabel;
        private javax.swing.JPanel restrictIPJPanel;
        private javax.swing.JPanel restrictIPJPanel1;
        private javax.swing.JLabel restrictIPaddrJLabel;
        public javax.swing.JTextField restrictIPaddrJTextField;
        private javax.swing.JPanel restrictJPanel;
        private javax.swing.JLabel restrictNetmaskJLabel;
        public javax.swing.JTextField restrictNetmaskJTextField;
        private javax.swing.JCheckBox restrictQuarantineJCheckBox;
        private javax.swing.JLabel restrictQuarantineJLabel;
        private javax.swing.JCheckBox restrictReportingJCheckBox;
        private javax.swing.JLabel restrictReportingJLabel;
        private javax.swing.ButtonGroup sshButtonGroup;
        private javax.swing.ButtonGroup tcpWindowButtonGroup;
        // End of variables declaration//GEN-END:variables
    

}
