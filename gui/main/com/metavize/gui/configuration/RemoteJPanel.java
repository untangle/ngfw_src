/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: RemoteJPanel.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.configuration;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;

/**
 *
 * @author  inieves
 */
public class RemoteJPanel extends javax.swing.JPanel implements Savable, Refreshable {

    private static final String EXCEPTION_OUTSIDE_ACCESS_NETWORK = "Invalid External Remote Administration \"IP Address\" specified.";
    private static final String EXCEPTION_OUTSIDE_ACCESS_NETMASK = "Invalid External Remote Administration \"Netmask\" specified.";

    
    public RemoteJPanel() {
        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

	// OUTSIDE ACCESS ENABLED ////////
	boolean isOutsideAccessEnabled = externalAdminEnabledRadioButton.isSelected();

	// OUTSIDE ACCESS RESTRICTED ///////
	boolean isOutsideAccessRestricted = externalAdminRestrictEnabledRadioButton.isSelected();

	// OUTSIDE ACCESS RESTRICTION ADDRESS /////////
	IPaddr outsideNetwork = null;
        restrictIPaddrJTextField.setBackground( Color.WHITE );
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

	// OUTSIDE ACCESS RESTRICTION NETMASK /////////
	IPaddr outsideNetmask = null;
        restrictNetmaskJTextField.setBackground( Color.WHITE );
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
	    NetworkingConfiguration networkingConfiguration = (NetworkingConfiguration) settings;
	    networkingConfiguration.isOutsideAccessEnabled( isOutsideAccessEnabled );
	    if( isOutsideAccessEnabled ){
		networkingConfiguration.isOutsideAccessRestricted( isOutsideAccessRestricted );
		if( isOutsideAccessRestricted ){
		    networkingConfiguration.outsideNetwork( outsideNetwork );
		    networkingConfiguration.outsideNetmask( outsideNetmask );
		}
	    }
	    networkingConfiguration.isInsideInsecureEnabled( isInsideInsecureEnabled );
        }

    }

    public void doRefresh(Object settings){
        NetworkingConfiguration networkingConfiguration = (NetworkingConfiguration) settings;
        
	// OUTSIDE ACCESS ENABLED //////
	boolean isOutsideAccessEnabled = networkingConfiguration.isOutsideAccessEnabled();
	setOutsideAccessEnabledDependency( isOutsideAccessEnabled );
	if( isOutsideAccessEnabled )
            externalAdminEnabledRadioButton.setSelected(true);
        else
            externalAdminDisabledRadioButton.setSelected(true);

	// OUTSIDE ACCESS RESTRICTED /////
	boolean isOutsideAccessRestricted = networkingConfiguration.isOutsideAccessRestricted();
	setOutsideAccessRestrictedDependency( isOutsideAccessRestricted );
	if( isOutsideAccessRestricted )
            externalAdminRestrictEnabledRadioButton.setSelected(true);
        else
            externalAdminRestrictDisabledRadioButton.setSelected(true);
        
	// OUTSIDE ACCESS RESTRICTED NETWORK //////
        restrictIPaddrJTextField.setText( networkingConfiguration.outsideNetwork().toString() );
	restrictIPaddrJTextField.setBackground( Color.WHITE );

	// OUTSIDE ACCESS RESTRICTED NETMASK /////
        restrictIPaddrJTextField.setText( networkingConfiguration.outsideNetwork().toString() );
	restrictIPaddrJTextField.setBackground( Color.white );
        
	// INSIDE INSECURE ENABLED ///////
	boolean isInsideInsecureEnabled = networkingConfiguration.isInsideInsecureEnabled();
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
        internalRemoteJPanel = new javax.swing.JPanel();
        internalAdminEnabledRadioButton = new javax.swing.JRadioButton();
        internalAdminDisabledRadioButton = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 343));
        setMinimumSize(new java.awt.Dimension(563, 343));
        setPreferredSize(new java.awt.Dimension(563, 343));
        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "External Remote Administration", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        externalAdminButtonGroup.add(externalAdminEnabledRadioButton);
        externalAdminEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        externalAdminEnabledRadioButton.setText("<html><b>Allow</b> Remote Administration by authorized users outside of the local network.</html>");
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
        externalAdminRestrictDisabledRadioButton.setText("<html><b>Unrestrict</b> so any IP address can connect for remote administration.</html>");
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
        externalAdminRestrictEnabledRadioButton.setText("<html><b>Restrict</b> the set of IP addresses that can remotely administer to the following:</html>");
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
        externalAdminDisabledRadioButton.setText("<html><b>Disallow</b> Remote Administration by any user outside of the local network.  (This is the default setting.)</html>");
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
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        externalRemoteJPanel.add(externalAdminDisabledRadioButton, gridBagConstraints);

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
        internalAdminEnabledRadioButton.setText("<html><b>Allow</b> insecure Remote Administration inside the local network via http.  (This is the default setting.)</html>");
        internalAdminEnabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        internalRemoteJPanel.add(internalAdminEnabledRadioButton, gridBagConstraints);

        internalAdminButtonGroup.add(internalAdminDisabledRadioButton);
        internalAdminDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        internalAdminDisabledRadioButton.setText("<html><b>Disallow</b> insecure Remote Administration inside the local network via http.</html>");
        internalAdminDisabledRadioButton.setFocusPainted(false);
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

    private void externalAdminDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminDisabledRadioButtonActionPerformed
        setOutsideAccessEnabledDependency( false );
	setOutsideAccessRestrictedDependency( false );
    }//GEN-LAST:event_externalAdminDisabledRadioButtonActionPerformed

    private void externalAdminEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminEnabledRadioButtonActionPerformed
        setOutsideAccessEnabledDependency( true );
	setOutsideAccessRestrictedDependency( externalAdminRestrictEnabledRadioButton.isSelected() );
    }//GEN-LAST:event_externalAdminEnabledRadioButtonActionPerformed

    private void externalAdminRestrictEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminRestrictEnabledRadioButtonActionPerformed
        if( externalAdminEnabledRadioButton.isSelected() )
	    setOutsideAccessRestrictedDependency( true );
    }//GEN-LAST:event_externalAdminRestrictEnabledRadioButtonActionPerformed

    private void externalAdminRestrictDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminRestrictDisabledRadioButtonActionPerformed
	setOutsideAccessRestrictedDependency( false );
    }//GEN-LAST:event_externalAdminRestrictDisabledRadioButtonActionPerformed
    

    private void setOutsideAccessEnabledDependency(boolean enabled){
	externalAdminRestrictEnabledRadioButton.setEnabled( enabled );
	externalAdminRestrictDisabledRadioButton.setEnabled( enabled );
    }

    private void setOutsideAccessRestrictedDependency(boolean enabled){
	restrictIPaddrJTextField.setEnabled( enabled );
	restrictNetmaskJTextField.setEnabled( enabled );
    }

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup dhcpButtonGroup;
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
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.ButtonGroup restrictAdminButtonGroup;
    private javax.swing.JPanel restrictIPJPanel;
    public javax.swing.JTextField restrictIPaddrJTextField;
    public javax.swing.JTextField restrictNetmaskJTextField;
    private javax.swing.ButtonGroup sshButtonGroup;
    private javax.swing.ButtonGroup tcpWindowButtonGroup;
    // End of variables declaration//GEN-END:variables
    

}
