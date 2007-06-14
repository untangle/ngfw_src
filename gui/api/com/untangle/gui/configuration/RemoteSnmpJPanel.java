/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.configuration;

import java.awt.*;
import javax.swing.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.uvm.*;
import com.untangle.uvm.security.*;
import com.untangle.uvm.snmp.*;
import com.untangle.uvm.node.*;

public class RemoteSnmpJPanel extends javax.swing.JPanel
    implements Savable<RemoteCompoundSettings>, Refreshable<RemoteCompoundSettings> {

    private static final String EXCEPTION_SNMP_COMMUNITY_MISSING = "An SNMP \"Community\" must be specified.";
    private static final String EXCEPTION_TRAP_COMMUNITY_MISSING = "A Trap \"Community\" must be specified.";
    private static final String EXCEPTION_TRAP_HOST_MISSING = "A Trap \"Host\" must be specified.";


    public RemoteSnmpJPanel() {
        initComponents();
        Util.setPortView(trapPortJSpinner, 162);
        Util.addPanelFocus(this, snmpDisabledRadioButton);
        Util.addFocusHighlight(snmpCommunityJTextField);
        Util.addFocusHighlight(snmpContactJTextField);
        Util.addFocusHighlight(snmpLocationJTextField);
        Util.addFocusHighlight(trapCommunityJTextField);
        Util.addFocusHighlight(trapHostJTextField);
        Util.addFocusHighlight(trapPortJSpinner);
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(RemoteCompoundSettings remoteCompoundSettings, boolean validateOnly) throws Exception {

        // SNMP ENABLED ////////
        boolean isSnmpEnabled = snmpEnabledRadioButton.isSelected();

        // SNMP COMMUNITY ///////
        snmpCommunityJTextField.setBackground( Color.WHITE );
        String snmpCommunity = snmpCommunityJTextField.getText();
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
        trapCommunityJTextField.setBackground( Color.WHITE );
        String trapCommunity = trapCommunityJTextField.getText();
        if( isSnmpEnabled && isTrapEnabled && (trapCommunity.length() == 0) ){
            trapCommunityJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
            throw new Exception(EXCEPTION_TRAP_COMMUNITY_MISSING);
        }

        // TRAP HOST /////
        trapHostJTextField.setBackground( Color.WHITE );
        String trapHost = trapHostJTextField.getText();
        if( isSnmpEnabled && isTrapEnabled && (trapHost.length() == 0) ){
            trapHostJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
            throw new Exception(EXCEPTION_TRAP_HOST_MISSING);
        }

        // TRAP PORT /////
        ((JSpinner.DefaultEditor)trapPortJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        int trapPort = 0;
        try{ trapPortJSpinner.commitEdit(); }
        catch(Exception e){
            throw new Exception(Util.EXCEPTION_PORT_RANGE);
        }
        trapPort = (Integer) trapPortJSpinner.getValue();

        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            SnmpSettings snmpSettings = remoteCompoundSettings.getSnmpSettings();

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
        }

    }

    public void doRefresh(RemoteCompoundSettings remoteCompoundSettings){
        SnmpSettings snmpSettings = remoteCompoundSettings.getSnmpSettings();

        // SNMP ENABLED //////
        boolean isSnmpEnabled = snmpSettings.isEnabled();
        if( isSnmpEnabled )
            snmpEnabledRadioButton.setSelected(true);
        else
            snmpDisabledRadioButton.setSelected(true);
        setSnmpEnabledDependency( isSnmpEnabled );
        Util.addSettingChangeListener(settingsChangedListener, this, snmpEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, snmpDisabledRadioButton);

        // SNMP COMMUNITY /////
        String snmpCommunity = snmpSettings.getCommunityString();
        snmpCommunityJTextField.setText( snmpCommunity );
        snmpCommunityJTextField.setBackground( Color.WHITE );
        Util.addSettingChangeListener(settingsChangedListener, this, snmpCommunityJTextField);

        // SNMP CONTACT //////
        String snmpContact = snmpSettings.getSysContact();
        snmpContactJTextField.setText( snmpContact );
        Util.addSettingChangeListener(settingsChangedListener, this, snmpContactJTextField);

        // SNMP LOCATION /////
        String snmpLocation = snmpSettings.getSysLocation();
        snmpLocationJTextField.setText( snmpLocation );
        Util.addSettingChangeListener(settingsChangedListener, this, snmpLocationJTextField);

        // TRAP ENABLED /////
        boolean isTrapEnabled = snmpSettings.isSendTraps();
        if( isTrapEnabled )
            trapEnabledRadioButton.setSelected(true);
        else
            trapDisabledRadioButton.setSelected(true);
        setTrapEnabledDependency( isSnmpEnabled && isTrapEnabled );
        Util.addSettingChangeListener(settingsChangedListener, this, trapEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, trapDisabledRadioButton);

        // TRAP COMMUNITY //////
        String trapCommunity = snmpSettings.getTrapCommunity();
        trapCommunityJTextField.setText( trapCommunity );
        trapCommunityJTextField.setBackground( Color.WHITE );
        Util.addSettingChangeListener(settingsChangedListener, this, trapCommunityJTextField);

        // TRAP HOST /////
        String trapHost = snmpSettings.getTrapHost();
        trapHostJTextField.setText( trapHost );
        trapHostJTextField.setBackground( Color.WHITE );
        Util.addSettingChangeListener(settingsChangedListener, this, trapHostJTextField);

        // TRAP PORT //////
        int trapPort = snmpSettings.getTrapPort();
        trapPortJSpinner.setValue( trapPort );
        ((JSpinner.DefaultEditor)trapPortJSpinner.getEditor()).getTextField().setText(Integer.toString(trapPort));
        ((JSpinner.DefaultEditor)trapPortJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, trapPortJSpinner);
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
        snmpCommunityJLabel = new javax.swing.JLabel();
        snmpCommunityJTextField = new javax.swing.JTextField();
        snmpContactJLabel = new javax.swing.JLabel();
        snmpContactJTextField = new javax.swing.JTextField();
        snmpLocationJLabel = new javax.swing.JLabel();
        snmpLocationJTextField = new javax.swing.JTextField();
        jSeparator3 = new javax.swing.JSeparator();
        enableRemoteJPanel1 = new javax.swing.JPanel();
        trapDisabledRadioButton = new javax.swing.JRadioButton();
        trapEnabledRadioButton = new javax.swing.JRadioButton();
        restrictIPJPanel1 = new javax.swing.JPanel();
        trapCommunityJLabel = new javax.swing.JLabel();
        trapCommunityJTextField = new javax.swing.JTextField();
        trapHostJLabel = new javax.swing.JLabel();
        trapHostJTextField = new javax.swing.JTextField();
        trapPortJLabel = new javax.swing.JLabel();
        trapPortJSpinner = new javax.swing.JSpinner();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(550, 293));
        setMinimumSize(new java.awt.Dimension(550, 293));
        setPreferredSize(new java.awt.Dimension(550, 293));
        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "SNMP Monitoring", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        snmpButtonGroup.add(snmpDisabledRadioButton);
        snmpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        snmpDisabledRadioButton.setText("<html><b>Disable</b> SNMP Monitoring. (This is the default setting.)</html>");
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

        snmpCommunityJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        snmpCommunityJLabel.setText("Community:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(snmpCommunityJLabel, gridBagConstraints);

        snmpCommunityJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        snmpCommunityJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        snmpCommunityJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel.add(snmpCommunityJTextField, gridBagConstraints);

        snmpContactJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        snmpContactJLabel.setText("System Contact:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(snmpContactJLabel, gridBagConstraints);

        snmpContactJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        snmpContactJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        snmpContactJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel.add(snmpContactJTextField, gridBagConstraints);

        snmpLocationJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        snmpLocationJLabel.setText("System Location:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(snmpLocationJLabel, gridBagConstraints);

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
        trapDisabledRadioButton.setText("<html><b>Disable Traps</b> so no trap events are generated.  (This is the default setting.)</html>");
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

        trapCommunityJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        trapCommunityJLabel.setText("Community:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(trapCommunityJLabel, gridBagConstraints);

        trapCommunityJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        trapCommunityJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        trapCommunityJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel1.add(trapCommunityJTextField, gridBagConstraints);

        trapHostJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        trapHostJLabel.setText("Host:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(trapHostJLabel, gridBagConstraints);

        trapHostJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        trapHostJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        trapHostJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel1.add(trapHostJTextField, gridBagConstraints);

        trapPortJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        trapPortJLabel.setText("Port:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(trapPortJLabel, gridBagConstraints);

        trapPortJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
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
    }//GEN-LAST:event_snmpDisabledRadioButtonActionPerformed

    private void snmpEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_snmpEnabledRadioButtonActionPerformed
        setSnmpEnabledDependency( true );
    }//GEN-LAST:event_snmpEnabledRadioButtonActionPerformed


    private void setSnmpEnabledDependency(boolean enabled){
        snmpCommunityJTextField.setEnabled( enabled );
        snmpCommunityJLabel.setEnabled( enabled );
        snmpContactJTextField.setEnabled( enabled );
        snmpContactJLabel.setEnabled( enabled );
        snmpLocationJTextField.setEnabled( enabled );
        snmpLocationJLabel.setEnabled( enabled );
        trapEnabledRadioButton.setEnabled( enabled );
        trapDisabledRadioButton.setEnabled( enabled );
        if(!enabled)
            setTrapEnabledDependency(false);
        else
            setTrapEnabledDependency( trapEnabledRadioButton.isSelected() );
    }

    private void setTrapEnabledDependency(boolean enabled){
        trapCommunityJTextField.setEnabled( enabled );
        trapCommunityJLabel.setEnabled( enabled );
        trapHostJTextField.setEnabled( enabled );
        trapHostJLabel.setEnabled( enabled );
        trapPortJSpinner.setEnabled( enabled );
        trapPortJLabel.setEnabled( enabled );
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel enableRemoteJPanel;
    private javax.swing.JPanel enableRemoteJPanel1;
    private javax.swing.JPanel externalRemoteJPanel;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JPanel restrictIPJPanel;
    private javax.swing.JPanel restrictIPJPanel1;
    private javax.swing.ButtonGroup snmpButtonGroup;
    private javax.swing.JLabel snmpCommunityJLabel;
    public javax.swing.JTextField snmpCommunityJTextField;
    private javax.swing.JLabel snmpContactJLabel;
    public javax.swing.JTextField snmpContactJTextField;
    public javax.swing.JRadioButton snmpDisabledRadioButton;
    public javax.swing.JRadioButton snmpEnabledRadioButton;
    private javax.swing.JLabel snmpLocationJLabel;
    public javax.swing.JTextField snmpLocationJTextField;
    private javax.swing.ButtonGroup trapButtonGroup;
    private javax.swing.JLabel trapCommunityJLabel;
    public javax.swing.JTextField trapCommunityJTextField;
    public javax.swing.JRadioButton trapDisabledRadioButton;
    public javax.swing.JRadioButton trapEnabledRadioButton;
    private javax.swing.JLabel trapHostJLabel;
    public javax.swing.JTextField trapHostJTextField;
    private javax.swing.JLabel trapPortJLabel;
    private javax.swing.JSpinner trapPortJSpinner;
    // End of variables declaration//GEN-END:variables


}
