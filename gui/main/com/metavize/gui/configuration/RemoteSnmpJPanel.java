/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.configuration;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.mvvm.snmp.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;
import javax.swing.*;

public class RemoteSnmpJPanel extends javax.swing.JPanel implements Savable, Refreshable {

    private static final String EXCEPTION_SNMP_COMMUNITY_MISSING = "An SNMP \"Community\" must be specified.";
    private static final String EXCEPTION_TRAP_COMMUNITY_MISSING = "A Trap \"Community\" must be specified.";
    private static final String EXCEPTION_TRAP_HOST_MISSING = "A Trap \"Host\" must be specified.";

    
    public RemoteSnmpJPanel() {
        initComponents();
	trapPortJSpinner.setModel(new SpinnerNumberModel(0,0,65535,1));
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

	// SNMP ENABLED ////////
	boolean isSnmpEnabled = snmpEnabledRadioButton.isSelected();

	// SNMP COMMUNITY ///////
	String snmpCommunity = snmpCommunityJTextField.getText();
	snmpCommunityJTextField.setBackground( Color.WHITE );
	if( isSnmpEnabled && (snmpCommunity.length() == 0) ){
	    snmpCommunityJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_SNMP_COMMUNITY_MISSING);
	}

	// SNMP SYSTEM CONTACT /////
	String snmpContact = snmpContactJTextField.getText();

	// SNMP SYSTEM LOCATION /////
	String snmpLocation = snmpLocationJTextField.getText();

	// TRAP ENABLED /////
	boolean isTrapEnabled = trapEnabledRadioButton.isSelected();
	
	// TRAP COMMUNITY /////
	String trapCommunity = trapCommunityJTextField.getText();
	trapCommunityJTextField.setBackground( Color.WHITE );
	if( isSnmpEnabled && isTrapEnabled && (trapCommunity.length() == 0) ){
	    trapCommunityJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_TRAP_COMMUNITY_MISSING);
	}

	// TRAP HOST /////
	String trapHost = trapHostJTextField.getText();
	trapHostJTextField.setBackground( Color.WHITE );
	if( isSnmpEnabled && isTrapEnabled && (trapHost.length() == 0) ){
	    trapHostJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_TRAP_HOST_MISSING);
	}

	// TRAP PORT /////
	int trapPort = (Integer) trapPortJSpinner.getValue();

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    SnmpManager snmpManager = Util.getAdminManager().getSnmpManager();
	    SnmpSettings snmpSettings = snmpManager.getSnmpSettings();
	    
	    snmpSettings.setEnabled( isSnmpEnabled );
	    if( isSnmpEnabled ){
		snmpSettings.setCommunityString( snmpCommunity );
		snmpSettings.setSysContact( snmpContact );
		snmpSettings.setSysLocation( snmpLocation );
		snmpSettings.setSendTraps( isTrapEnabled );
		if( isTrapEnabled ){
		    snmpSettings.setTrapCommunity( trapCommunity );
		    snmpSettings.setTrapHost( trapHost );
		    snmpSettings.setTrapPort( trapPort );
		}
	    }

	    snmpManager.setSnmpSettings( snmpSettings );
        }

    }

    public void doRefresh(Object settings){
	SnmpSettings snmpSettings = Util.getAdminManager().getSnmpManager().getSnmpSettings();

	// SNMP ENABLED //////
	boolean isSnmpEnabled = snmpSettings.isEnabled();
	setSnmpEnabledDependency( isSnmpEnabled );
	if( isSnmpEnabled )
            snmpEnabledRadioButton.setSelected(true);
        else
            snmpDisabledRadioButton.setSelected(true);

	// SNMP COMMUNITY /////
	String snmpCommunity = snmpSettings.getCommunityString();
	snmpCommunityJTextField.setText( snmpCommunity );
	snmpCommunityJTextField.setBackground( Color.WHITE );

	// SNMP CONTACT //////
	String snmpContact = snmpSettings.getSysContact();
	snmpContactJTextField.setText( snmpContact );

	// SNMP LOCATION /////
	String snmpLocation = snmpSettings.getSysLocation();
	snmpLocationJTextField.setText( snmpLocation );

	// TRAP ENABLED /////
	boolean isTrapEnabled = snmpSettings.isSendTraps();
	setTrapEnabledDependency( isTrapEnabled );
	if( isTrapEnabled )
            trapEnabledRadioButton.setSelected(true);
        else
            trapEnabledRadioButton.setSelected(true);
        
	// TRAP COMMUNITY //////
	String trapCommunity = snmpSettings.getTrapCommunity();
	trapCommunityJTextField.setText( trapCommunity );
	trapCommunityJTextField.setBackground( Color.WHITE );

	// TRAP HOST /////
	String trapHost = snmpSettings.getTrapHost();
	trapHostJTextField.setText( trapHost );
	trapHostJTextField.setBackground( Color.WHITE );

        // TRAP PORT //////
	int trapPort = snmpSettings.getTrapPort();
	trapPortJSpinner.setValue( trapPort );
        	
    }
    
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        snmpButtonGroup = new javax.swing.ButtonGroup();
        trapButtonGroup = new javax.swing.ButtonGroup();
        externalRemoteJPanel = new javax.swing.JPanel();
        snmpDisabledRadioButton = new javax.swing.JRadioButton();
        snmpEnabledRadioButton = new javax.swing.JRadioButton();
        enableRemoteJPanel = new javax.swing.JPanel();
        restrictIPJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        snmpCommunityJTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        snmpContactJTextField = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        snmpLocationJTextField = new javax.swing.JTextField();
        jSeparator3 = new javax.swing.JSeparator();
        enableRemoteJPanel1 = new javax.swing.JPanel();
        trapDisabledRadioButton = new javax.swing.JRadioButton();
        trapEnabledRadioButton = new javax.swing.JRadioButton();
        restrictIPJPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        trapCommunityJTextField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        trapHostJTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        trapPortJSpinner = new javax.swing.JSpinner();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 343));
        setMinimumSize(new java.awt.Dimension(563, 343));
        setPreferredSize(new java.awt.Dimension(563, 343));
        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "SNMP Monitoring", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        snmpButtonGroup.add(snmpDisabledRadioButton);
        snmpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        snmpDisabledRadioButton.setText("<html><b>Disable</b> SNMP Monitoring. (This is the default setting.)</html>");
        snmpDisabledRadioButton.setFocusPainted(false);
        snmpDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                snmpDisabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        externalRemoteJPanel.add(snmpDisabledRadioButton, gridBagConstraints);

        snmpButtonGroup.add(snmpEnabledRadioButton);
        snmpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        snmpEnabledRadioButton.setText("<html><b>Enable</b> SNMP Monitoring.</html>");
        snmpEnabledRadioButton.setFocusPainted(false);
        snmpEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                snmpEnabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        externalRemoteJPanel.add(snmpEnabledRadioButton, gridBagConstraints);

        enableRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("Community:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel5, gridBagConstraints);

        snmpCommunityJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        snmpCommunityJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        snmpCommunityJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel.add(snmpCommunityJTextField, gridBagConstraints);

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel8.setText("System Contact:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel8, gridBagConstraints);

        snmpContactJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        snmpContactJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        snmpContactJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel.add(snmpContactJTextField, gridBagConstraints);

        jLabel12.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel12.setText("System Location:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel12, gridBagConstraints);

        snmpLocationJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        snmpLocationJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        snmpLocationJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel.add(snmpLocationJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 5, 0);
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

        enableRemoteJPanel1.setLayout(new java.awt.GridBagLayout());

        trapButtonGroup.add(trapDisabledRadioButton);
        trapDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        trapDisabledRadioButton.setText("<html><b>Disable Traps</b> so no trap events are generated. (This is the default setting.)</html>");
        trapDisabledRadioButton.setFocusPainted(false);
        trapDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trapDisabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        enableRemoteJPanel1.add(trapDisabledRadioButton, gridBagConstraints);

        trapButtonGroup.add(trapEnabledRadioButton);
        trapEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        trapEnabledRadioButton.setText("<html><b>Enable Traps</b> so trap events are sent when they are generated.</html>");
        trapEnabledRadioButton.setFocusPainted(false);
        trapEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trapEnabledRadioButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        enableRemoteJPanel1.add(trapEnabledRadioButton, gridBagConstraints);

        restrictIPJPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("Community:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel6, gridBagConstraints);

        trapCommunityJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        trapCommunityJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        trapCommunityJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel1.add(trapCommunityJTextField, gridBagConstraints);

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel9.setText("Host:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel9, gridBagConstraints);

        trapHostJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        trapHostJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        trapHostJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel1.add(trapHostJTextField, gridBagConstraints);

        jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel10.setText("Port:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel10, gridBagConstraints);

        trapPortJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        trapPortJSpinner.setFocusable(false);
        trapPortJSpinner.setMaximumSize(new java.awt.Dimension(75, 19));
        trapPortJSpinner.setMinimumSize(new java.awt.Dimension(75, 19));
        trapPortJSpinner.setPreferredSize(new java.awt.Dimension(75, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel1.add(trapPortJSpinner, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 5, 0);
        enableRemoteJPanel1.add(restrictIPJPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        externalRemoteJPanel.add(enableRemoteJPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(externalRemoteJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void trapDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trapDisabledRadioButtonActionPerformed
	setTrapEnabledDependency( false );
    }//GEN-LAST:event_trapDisabledRadioButtonActionPerformed

    private void trapEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trapEnabledRadioButtonActionPerformed
	setTrapEnabledDependency( true );
    }//GEN-LAST:event_trapEnabledRadioButtonActionPerformed

    private void snmpDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_snmpDisabledRadioButtonActionPerformed
        setSnmpEnabledDependency( false );
	setTrapEnabledDependency( false );
    }//GEN-LAST:event_snmpDisabledRadioButtonActionPerformed

    private void snmpEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_snmpEnabledRadioButtonActionPerformed
        setSnmpEnabledDependency( true );
	setTrapEnabledDependency( trapEnabledRadioButton.isSelected() );
    }//GEN-LAST:event_snmpEnabledRadioButtonActionPerformed
    

    private void setSnmpEnabledDependency(boolean enabled){
	snmpCommunityJTextField.setEnabled( enabled );
	snmpContactJTextField.setEnabled( enabled );
	snmpLocationJTextField.setEnabled( enabled );
	trapEnabledRadioButton.setEnabled( enabled );
	trapDisabledRadioButton.setEnabled( enabled );
    }

    private void setTrapEnabledDependency(boolean enabled){
	trapCommunityJTextField.setEnabled( enabled );
	trapHostJTextField.setEnabled( enabled );
	trapPortJSpinner.setEnabled( enabled );
    }

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel enableRemoteJPanel;
    private javax.swing.JPanel enableRemoteJPanel1;
    private javax.swing.JPanel externalRemoteJPanel;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JPanel restrictIPJPanel;
    private javax.swing.JPanel restrictIPJPanel1;
    private javax.swing.ButtonGroup snmpButtonGroup;
    public javax.swing.JTextField snmpCommunityJTextField;
    public javax.swing.JTextField snmpContactJTextField;
    public javax.swing.JRadioButton snmpDisabledRadioButton;
    public javax.swing.JRadioButton snmpEnabledRadioButton;
    public javax.swing.JTextField snmpLocationJTextField;
    private javax.swing.ButtonGroup trapButtonGroup;
    public javax.swing.JTextField trapCommunityJTextField;
    public javax.swing.JRadioButton trapDisabledRadioButton;
    public javax.swing.JRadioButton trapEnabledRadioButton;
    public javax.swing.JTextField trapHostJTextField;
    private javax.swing.JSpinner trapPortJSpinner;
    // End of variables declaration//GEN-END:variables
    

}
