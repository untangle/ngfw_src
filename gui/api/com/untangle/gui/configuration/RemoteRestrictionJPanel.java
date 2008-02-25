/*
 * $HeadURL$
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
import javax.swing.JSpinner;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.uvm.*;
import com.untangle.uvm.networking.AccessSettings;
import com.untangle.uvm.networking.AddressSettings;
import com.untangle.uvm.node.*;
import com.untangle.uvm.security.*;

public class RemoteRestrictionJPanel extends javax.swing.JPanel
    implements Savable<RemoteCompoundSettings>, Refreshable<RemoteCompoundSettings> {

    private static final String EXCEPTION_OUTSIDE_ACCESS_NETWORK = "Invalid External Remote Administration \"IP Address\" specified.";
    private static final String EXCEPTION_OUTSIDE_ACCESS_NETMASK = "Invalid External Remote Administration \"Netmask\" specified.";


    public RemoteRestrictionJPanel() {
        initComponents();
        Util.setPortView(externalAccessPortJSpinner, 443);
        Util.addPanelFocus(this, restrictAdminJCheckBox);
        Util.addFocusHighlight(externalAccessPortJSpinner);
        Util.addFocusHighlight(restrictIPaddrJTextField);
        Util.addFocusHighlight(restrictNetmaskJTextField);
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(RemoteCompoundSettings remoteCompoundSettings, boolean validateOnly) throws Exception {

        // OUTSIDE ACCESS RESTRICTIONS /////
        boolean isOutsideAdministrationEnabled = restrictAdminJCheckBox.isSelected();
        boolean isOutsideReportingEnabled = restrictReportingJCheckBox.isSelected();
        boolean isOutsideQuarantineEnabled = restrictQuarantineJCheckBox.isSelected();
        boolean isOutsideAccessEnabled = isOutsideAdministrationEnabled || isOutsideReportingEnabled || isOutsideQuarantineEnabled;

        // OUTSIDE ACCESS PORT //////
        ((JSpinner.DefaultEditor)externalAccessPortJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        int httpsPort = 0;
        try{ externalAccessPortJSpinner.commitEdit(); }
        catch(Exception e){
            ((JSpinner.DefaultEditor)externalAccessPortJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(Util.EXCEPTION_PORT_RANGE);
        }
        httpsPort = (Integer) externalAccessPortJSpinner.getValue();


        // OUTSIDE ACCESS IP RESTRICTION ///////
        boolean isOutsideAccessRestricted = externalAdminRestrictEnabledRadioButton.isSelected();

        // OUTSIDE ACCESS IP RESTRICTION ADDRESS /////////
        restrictIPaddrJTextField.setBackground( Color.WHITE );
        IPaddr outsideNetwork = null;
        if( isOutsideAccessRestricted ){
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
        if( isOutsideAccessRestricted ){
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

            accessSettings.setIsOutsideAdministrationEnabled(isOutsideAdministrationEnabled);
            accessSettings.setIsOutsideReportingEnabled(isOutsideReportingEnabled);
            accessSettings.setIsOutsideQuarantineEnabled(isOutsideQuarantineEnabled);
            addressSettings.setHttpsPort( httpsPort );
            addressSettings.setIsHostNamePublic(resolvesPubliclyJCheckBox.isSelected());
            accessSettings.setIsOutsideAccessRestricted( isOutsideAccessRestricted );
            if( isOutsideAccessRestricted ){
                accessSettings.setOutsideNetwork( outsideNetwork );
                accessSettings.setOutsideNetmask( outsideNetmask );
            }
            accessSettings.setIsInsideInsecureEnabled( isInsideInsecureEnabled );
        }
    }

    public void doRefresh(RemoteCompoundSettings remoteCompoundSettings){
        AccessSettings accessSettings = remoteCompoundSettings.getAccessSettings();
        AddressSettings addressSettings = remoteCompoundSettings.getAddressSettings();

        // OUTSIDE ACCESS RESTRICTIONS ////
        boolean isOutsideAdministrationEnabled = accessSettings.getIsOutsideAdministrationEnabled();
        restrictAdminJCheckBox.setSelected(isOutsideAdministrationEnabled);
        Util.addSettingChangeListener(settingsChangedListener, this, restrictAdminJCheckBox);
        boolean isOutsideReportingEnabled = accessSettings.getIsOutsideReportingEnabled();
        restrictReportingJCheckBox.setSelected(isOutsideReportingEnabled);
        Util.addSettingChangeListener(settingsChangedListener, this, restrictReportingJCheckBox);
        boolean isOutsideQuarantineEnabled = accessSettings.getIsOutsideQuarantineEnabled();
        restrictQuarantineJCheckBox.setSelected(isOutsideQuarantineEnabled);
        Util.addSettingChangeListener(settingsChangedListener, this, restrictQuarantineJCheckBox);

        // OUTSIDE ACCESS ENABLED //////
        boolean isOutsideAccessEnabled = isOutsideAdministrationEnabled || isOutsideReportingEnabled || isOutsideQuarantineEnabled;

        // PORT ///
        int httpsPort = addressSettings.getHttpsPort();
        externalAccessPortJSpinner.setValue( httpsPort );
        ((JSpinner.DefaultEditor)externalAccessPortJSpinner.getEditor()).getTextField().setText(Integer.toString(httpsPort));
        ((JSpinner.DefaultEditor)externalAccessPortJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, externalAccessPortJSpinner);

        // Hostname resolves publicly
        resolvesPubliclyJCheckBox.setSelected(addressSettings.getIsHostNamePublic());
        Util.addSettingChangeListener(settingsChangedListener, this, resolvesPubliclyJCheckBox);

        // OUTSIDE ACCESS IP RESTRICTED /////
        boolean isOutsideAccessRestricted = accessSettings.getIsOutsideAccessRestricted();
        if( isOutsideAccessRestricted )
            externalAdminRestrictEnabledRadioButton.setSelected(true);
        else
            externalAdminRestrictDisabledRadioButton.setSelected(true);
        setOutsideAccessRestrictedDependency(isOutsideAccessRestricted);
        Util.addSettingChangeListener(settingsChangedListener, this, externalAdminRestrictEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, externalAdminRestrictDisabledRadioButton);

        // OUTSIDE ACCESS IP RESTRICTED NETWORK //////
        restrictIPaddrJTextField.setText( accessSettings.getOutsideNetwork().toString() );
        restrictIPaddrJTextField.setBackground( Color.WHITE );
        Util.addSettingChangeListener(settingsChangedListener, this, restrictIPaddrJTextField);

        // OUTSIDE ACCESS IP RESTRICTED NETMASK /////
        restrictNetmaskJTextField.setText( accessSettings.getOutsideNetmask().toString() );
        restrictNetmaskJTextField.setBackground( Color.WHITE );
        Util.addSettingChangeListener(settingsChangedListener, this, restrictNetmaskJTextField);

        // INSIDE INSECURE ENABLED ///////
        boolean isInsideInsecureEnabled = accessSettings.getIsInsideInsecureEnabled();
        if( isInsideInsecureEnabled )
            internalAdminEnabledRadioButton.setSelected(true);
        else
            internalAdminDisabledRadioButton.setSelected(true);
        Util.addSettingChangeListener(settingsChangedListener, this, internalAdminEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, internalAdminDisabledRadioButton);

    }


    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        dhcpButtonGroup = new javax.swing.ButtonGroup();
        tcpWindowButtonGroup = new javax.swing.ButtonGroup();
        externalAdminButtonGroup = new javax.swing.ButtonGroup();
        internalAdminButtonGroup = new javax.swing.ButtonGroup();
        restrictAdminButtonGroup = new javax.swing.ButtonGroup();
        sshButtonGroup = new javax.swing.ButtonGroup();
        externalRemoteJPanel = new javax.swing.JPanel();
        restrictJPanel1 = new javax.swing.JPanel();
        restrictIPJPanel2 = new javax.swing.JPanel();
        resolvesPubliclyJCheckBox = new javax.swing.JCheckBox();
        jSeparator5 = new javax.swing.JSeparator();
        restrictJPanel = new javax.swing.JPanel();
        restrictIPJPanel1 = new javax.swing.JPanel();
        restrictAdminJCheckBox = new javax.swing.JCheckBox();
        restrictReportingJCheckBox = new javax.swing.JCheckBox();
        restrictQuarantineJCheckBox = new javax.swing.JCheckBox();
        jSeparator4 = new javax.swing.JSeparator();
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
        internalRemoteJPanel = new javax.swing.JPanel();
        internalAdminEnabledRadioButton = new javax.swing.JRadioButton();
        internalAdminDisabledRadioButton = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(550, 450));
        setMinimumSize(new java.awt.Dimension(550, 450));
        setPreferredSize(new java.awt.Dimension(550, 450));
        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Outside Access", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        restrictJPanel1.setLayout(new java.awt.GridBagLayout());

        restrictIPJPanel2.setLayout(new java.awt.GridBagLayout());

        resolvesPubliclyJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        resolvesPubliclyJCheckBox.setText("Untangle Server hostname resolves publicly");
        resolvesPubliclyJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resolvesPubliclyJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        restrictIPJPanel2.add(resolvesPubliclyJCheckBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
        restrictJPanel1.add(restrictIPJPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        externalRemoteJPanel.add(restrictJPanel1, gridBagConstraints);

        jSeparator5.setForeground(new java.awt.Color(200, 200, 200));
        jSeparator5.setPreferredSize(new java.awt.Dimension(0, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        externalRemoteJPanel.add(jSeparator5, gridBagConstraints);

        restrictJPanel.setLayout(new java.awt.GridBagLayout());

        restrictIPJPanel1.setLayout(new java.awt.GridBagLayout());

        restrictAdminJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        restrictAdminJCheckBox.setText("Enable Outside Administration");
        restrictAdminJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restrictAdminJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        restrictIPJPanel1.add(restrictAdminJCheckBox, gridBagConstraints);

        restrictReportingJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        restrictReportingJCheckBox.setText("Enable Outside Report Viewing");
        restrictReportingJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restrictReportingJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        restrictIPJPanel1.add(restrictReportingJCheckBox, gridBagConstraints);

        restrictQuarantineJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        restrictQuarantineJCheckBox.setText("Enable Outside Quarantine Access");
        restrictQuarantineJCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restrictQuarantineJCheckBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
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
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        externalRemoteJPanel.add(restrictJPanel, gridBagConstraints);

        jSeparator4.setForeground(new java.awt.Color(200, 200, 200));
        jSeparator4.setPreferredSize(new java.awt.Dimension(0, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        externalRemoteJPanel.add(jSeparator4, gridBagConstraints);

        externalAccessPortJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        externalAccessPortJLabel.setText("Outside Https Port:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 50, 10, 0);
        externalRemoteJPanel.add(externalAccessPortJLabel, gridBagConstraints);

        externalAccessPortJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        externalAccessPortJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        externalAccessPortJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        externalAccessPortJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 163, 10, 0);
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

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(externalRemoteJPanel, gridBagConstraints);

        internalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        internalRemoteJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Internal Remote Administration", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        internalAdminButtonGroup.add(internalAdminEnabledRadioButton);
        internalAdminEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        internalAdminEnabledRadioButton.setText("<html><b>Allow</b> Remote Administration inside the local network, via http.<br>(This is the default setting.)</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        internalRemoteJPanel.add(internalAdminEnabledRadioButton, gridBagConstraints);

        internalAdminButtonGroup.add(internalAdminDisabledRadioButton);
        internalAdminDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        internalAdminDisabledRadioButton.setText("<html><b>Disallow</b> Remote Administration inside the local network, via http.</html>");
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

    }// </editor-fold>//GEN-END:initComponents

    private void resolvesPubliclyJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resolvesPubliclyJCheckBoxActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_resolvesPubliclyJCheckBoxActionPerformed

    private void restrictAdminJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restrictAdminJCheckBoxActionPerformed

    }//GEN-LAST:event_restrictAdminJCheckBoxActionPerformed

    private void restrictReportingJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restrictReportingJCheckBoxActionPerformed

    }//GEN-LAST:event_restrictReportingJCheckBoxActionPerformed

    private void restrictQuarantineJCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restrictQuarantineJCheckBoxActionPerformed

    }//GEN-LAST:event_restrictQuarantineJCheckBoxActionPerformed

    private void externalAdminRestrictEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminRestrictEnabledRadioButtonActionPerformed
        setOutsideAccessRestrictedDependency( true );
    }//GEN-LAST:event_externalAdminRestrictEnabledRadioButtonActionPerformed

    private void externalAdminRestrictDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_externalAdminRestrictDisabledRadioButtonActionPerformed
        setOutsideAccessRestrictedDependency( false );
    }//GEN-LAST:event_externalAdminRestrictDisabledRadioButtonActionPerformed


    private void setOutsideAccessEnabledDependency(boolean enabled){

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
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JCheckBox resolvesPubliclyJCheckBox;
    private javax.swing.ButtonGroup restrictAdminButtonGroup;
    private javax.swing.JCheckBox restrictAdminJCheckBox;
    private javax.swing.JPanel restrictIPJPanel;
    private javax.swing.JPanel restrictIPJPanel1;
    private javax.swing.JPanel restrictIPJPanel2;
    private javax.swing.JLabel restrictIPaddrJLabel;
    public javax.swing.JTextField restrictIPaddrJTextField;
    private javax.swing.JPanel restrictJPanel;
    private javax.swing.JPanel restrictJPanel1;
    private javax.swing.JLabel restrictNetmaskJLabel;
    public javax.swing.JTextField restrictNetmaskJTextField;
    private javax.swing.JCheckBox restrictQuarantineJCheckBox;
    private javax.swing.JCheckBox restrictReportingJCheckBox;
    private javax.swing.ButtonGroup sshButtonGroup;
    private javax.swing.ButtonGroup tcpWindowButtonGroup;
    // End of variables declaration//GEN-END:variables


}
