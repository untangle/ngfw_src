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

package com.untangle.gui.pipeline;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.mvvm.snmp.*;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;

import com.untangle.mvvm.tran.firewall.ip.IPMatcherFactory;
import com.untangle.mvvm.tran.firewall.port.PortMatcherFactory;
import com.untangle.mvvm.tran.firewall.time.DayOfWeekMatcherConstants;
import java.text.SimpleDateFormat;

import java.awt.*;
import javax.swing.*;
import java.util.Vector;
import java.util.Date;

public class PolicyWizardJPanel extends javax.swing.JPanel
    implements Savable<CompoundVector> {

    Vector newRow;
    
    public PolicyWizardJPanel(Vector newRow) {
        initComponents();
		Util.addFocusHighlight(addressClientJTextField);
		Util.addFocusHighlight(addressServerJTextField);
		Util.addFocusHighlight(portServerJTextField);
		Util.addFocusHighlight(timeStartJTextField);
		Util.addFocusHighlight(timeEndJTextField);
		Util.addFocusHighlight(rackDescJTextField);
        this.newRow = newRow;
        doRefresh(newRow);
    }

    String newUser;

    public void doSave(CompoundVector compoundVector, boolean validateOnly) throws Exception {
        Vector newRow = compoundVector.getVector();

        newRow.setElementAt(protocolSettingJComboBox.getModel(), 7);
        newRow.setElementAt(interfaceServerJComboBox.getModel(), 6);
        newRow.setElementAt(interfaceClientJComboBox.getModel(), 5);

        try{ IPMatcherFactory.getInstance().parse( addressServerJTextField.getText() ); }
        catch(Exception e){ throw new Exception("You must specify a valid Server IP address."); }
        newRow.setElementAt( addressServerJTextField.getText(), 9);

        try{ IPMatcherFactory.getInstance().parse( addressClientJTextField.getText() ); }
        catch(Exception e){ throw new Exception("You must specify a valid Client IP address."); }
        newRow.setElementAt( addressClientJTextField.getText(), 8);

        try{ PortMatcherFactory.getInstance().parse( portServerJTextField.getText() ); }
        catch(Exception e){ throw new Exception("You must specify a valid Server port."); }
        newRow.setElementAt( portServerJTextField.getText(), 11);


        ((UidButtonRunnable) newRow.elementAt(12)).setUid(newUser);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date startDate = null;
        Date endDate = null;
        try{ startDate = sdf.parse( timeStartJTextField.getText() ); }
        catch(Exception e){ throw new Exception("You must specify a valid start time."); }
        newRow.setElementAt( timeStartJTextField.getText(), 14);

        try{ endDate = sdf.parse( timeEndJTextField.getText() ); }
        catch(Exception e){ throw new Exception("You must specify a valid end time."); }
        newRow.setElementAt( timeEndJTextField.getText(), 15);

        if( startDate.compareTo(endDate) > 0 )
            throw new Exception("Your start time must be earlier than your end time.");

        ComboBoxModel timeModel = (ComboBoxModel) newRow.elementAt(13);
        if( timeInvertJCheckBox.isSelected() )
            timeModel.setSelectedItem("Invert day/time");
        else
            timeModel.setSelectedItem("Normal day/time");
        newRow.setElementAt( timeModel, 13);

        boolean allDaysSelected = true;
        String dayString = new String();
        if( sundayJCheckBox.isSelected() )
            dayString += "Sunday, ";
        else
            allDaysSelected = false;
        if( mondayJCheckBox.isSelected() )
            dayString += "Monday, ";
        else
            allDaysSelected = false;
        if( tuesdayJCheckBox.isSelected() )
            dayString += "Tuesday, ";
        else
            allDaysSelected = false;
        if( wednesdayJCheckBox.isSelected() )
            dayString += "Wednesday, ";
        else
            allDaysSelected = false;
        if( thursdayJCheckBox.isSelected() )
            dayString += "Thursday, ";
        else
            allDaysSelected = false;
        if( fridayJCheckBox.isSelected() )
            dayString += "Friday, ";
        else
            allDaysSelected = false;
        if( saturdayJCheckBox.isSelected() )
            dayString += "Saturday, ";
        else
            allDaysSelected = false;

        if( dayString.length() == 0 )
            throw new Exception("You must select at least one day for this policy to be active.");
        dayString = dayString.substring(0, dayString.length()-2);
        if(allDaysSelected)
            newRow.setElementAt("any", 16);
        else
            newRow.setElementAt(dayString, 16);

        newRow.setElementAt(rackSelectJComboBox.getModel(), 4);
        newRow.setElementAt(rackDescJTextField.getText(), 17);
        newRow.setElementAt(rackEnableJCheckBox.isSelected(), 3);
    }

    public void doRefresh(Vector newRow){

        ComboBoxModel protocolModel = (ComboBoxModel) newRow.elementAt(7);
        protocolSettingJComboBox.setModel(protocolModel);

        ComboBoxModel serverInterfaceModel = (ComboBoxModel) newRow.elementAt(6);
        interfaceServerJComboBox.setModel(serverInterfaceModel);

        ComboBoxModel clientInterfaceModel = (ComboBoxModel) newRow.elementAt(5);
        interfaceClientJComboBox.setModel(clientInterfaceModel);

        String serverAddress = (String) newRow.elementAt(9);
        addressServerJTextField.setText(serverAddress);

        String clientAddress = (String) newRow.elementAt(8);
        addressClientJTextField.setText(clientAddress);

        String serverPort = (String) newRow.elementAt(11);
        portServerJTextField.setText(serverPort);

        newUser = ((UidButtonRunnable) newRow.elementAt(12)).getUid();
        userSettingsJLabel.setText("User: " + newUser);

        String startTime = (String) newRow.elementAt(14);
        timeStartJTextField.setText(startTime);

        String endTime = (String) newRow.elementAt(15);
        timeEndJTextField.setText(endTime);

        boolean invertTime = ((ComboBoxModel) newRow.elementAt(13)).getSelectedItem().equals("Invert day/time"); // comes from policy custom panel
        timeInvertJCheckBox.setSelected(invertTime);

        String dayString = (String) newRow.elementAt(16);

        if(dayString.contains("Sunday"))
            sundayJCheckBox.setSelected(true);
        else
            sundayJCheckBox.setSelected(false);
        if(dayString.contains("Monday"))
            mondayJCheckBox.setSelected(true);
        else
            mondayJCheckBox.setSelected(false);
        if(dayString.contains("Tuesday"))
            tuesdayJCheckBox.setSelected(true);
        else
            tuesdayJCheckBox.setSelected(false);
        if(dayString.contains("Wednesday"))
            wednesdayJCheckBox.setSelected(true);
        else
            wednesdayJCheckBox.setSelected(false);
        if(dayString.contains("Thursday"))
            thursdayJCheckBox.setSelected(true);
        else
            thursdayJCheckBox.setSelected(false);
        if(dayString.contains("Friday"))
            fridayJCheckBox.setSelected(true);
        else
            fridayJCheckBox.setSelected(false);
        if(dayString.contains("Saturday"))
            saturdayJCheckBox.setSelected(true);
        else
            saturdayJCheckBox.setSelected(false);

        if(dayString.contains("any")){
            sundayJCheckBox.setSelected(true);
            mondayJCheckBox.setSelected(true);
            tuesdayJCheckBox.setSelected(true);
            wednesdayJCheckBox.setSelected(true);
            thursdayJCheckBox.setSelected(true);
            fridayJCheckBox.setSelected(true);
            saturdayJCheckBox.setSelected(true);
        }
        
        ComboBoxModel policyModel = (ComboBoxModel) newRow.elementAt(4);
        rackSelectJComboBox.setModel(policyModel);

        String description = (String) newRow.elementAt(17);
        rackDescJTextField.setText(description);

        boolean enabled = (Boolean) newRow.elementAt(3);
        rackEnableJCheckBox.setSelected(enabled);        

    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                smtpButtonGroup = new javax.swing.ButtonGroup();
                protocolJPanel = new javax.swing.JPanel();
                protocolJLabel = new javax.swing.JLabel();
                protocolSettingsJPanel = new javax.swing.JPanel();
                protocolSettingJLabel = new javax.swing.JLabel();
                protocolSettingJComboBox = new javax.swing.JComboBox();
                interfaceJPanel = new javax.swing.JPanel();
                interfaceJLabel = new javax.swing.JLabel();
                interfaceSettingsJPanel = new javax.swing.JPanel();
                interfaceClientJLabel = new javax.swing.JLabel();
                interfaceClientJComboBox = new javax.swing.JComboBox();
                interfaceServerJLabel = new javax.swing.JLabel();
                interfaceServerJComboBox = new javax.swing.JComboBox();
                addressJPanel = new javax.swing.JPanel();
                addressJLabel = new javax.swing.JLabel();
                addressSettingsJPanel = new javax.swing.JPanel();
                addressClientJLabel = new javax.swing.JLabel();
                addressClientJTextField = new javax.swing.JTextField();
                addressServerJLabel = new javax.swing.JLabel();
                addressServerJTextField = new javax.swing.JTextField();
                portJPanel = new javax.swing.JPanel();
                portJLabel = new javax.swing.JLabel();
                portSettingsJPanel = new javax.swing.JPanel();
                portServerJLabel = new javax.swing.JLabel();
                portServerJTextField = new javax.swing.JTextField();
                userJPanel = new javax.swing.JPanel();
                userJLabel = new javax.swing.JLabel();
                userSettingsJPanel = new javax.swing.JPanel();
                userSettingsJLabel = new javax.swing.JLabel();
                userSettingsJButton = new javax.swing.JButton();
                timeJPanel = new javax.swing.JPanel();
                timeJLabel = new javax.swing.JLabel();
                timeSettingsJPanel = new javax.swing.JPanel();
                timeStartJLabel = new javax.swing.JLabel();
                timeStartJTextField = new javax.swing.JTextField();
                timeEndJLabel = new javax.swing.JLabel();
                timeEndJTextField = new javax.swing.JTextField();
                dayJPanel = new javax.swing.JPanel();
                dayJLabel = new javax.swing.JLabel();
                daySettingsJPanel = new javax.swing.JPanel();
                sundayJCheckBox = new javax.swing.JCheckBox();
                mondayJCheckBox = new javax.swing.JCheckBox();
                tuesdayJCheckBox = new javax.swing.JCheckBox();
                wednesdayJCheckBox = new javax.swing.JCheckBox();
                thursdayJCheckBox = new javax.swing.JCheckBox();
                fridayJCheckBox = new javax.swing.JCheckBox();
                saturdayJCheckBox = new javax.swing.JCheckBox();
                invertJPanel = new javax.swing.JPanel();
                invertJLabel = new javax.swing.JLabel();
                invertSettingsJPanel = new javax.swing.JPanel();
                timeInvertJCheckBox = new javax.swing.JCheckBox();
                rackJPanel = new javax.swing.JPanel();
                rackJLabel = new javax.swing.JLabel();
                rackSettingsJPanel = new javax.swing.JPanel();
                rackSelectJLabel = new javax.swing.JLabel();
                rackSelectJComboBox = new javax.swing.JComboBox();
                rackDescJLabel = new javax.swing.JLabel();
                rackDescJTextField = new javax.swing.JTextField();
                rackEnableJCheckBox = new javax.swing.JCheckBox();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(460, 1000));
                setMinimumSize(new java.awt.Dimension(460, 1000));
                setPreferredSize(new java.awt.Dimension(460, 1000));
                protocolJPanel.setLayout(new java.awt.GridBagLayout());

                protocolJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Protocol", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                protocolJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                protocolJLabel.setText("The protocol you would like this policy to handle.");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                protocolJPanel.add(protocolJLabel, gridBagConstraints);

                protocolSettingsJPanel.setLayout(new java.awt.GridBagLayout());

                protocolSettingJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                protocolSettingJLabel.setText("Protocol:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                protocolSettingsJPanel.add(protocolSettingJLabel, gridBagConstraints);

                protocolSettingJComboBox.setMaximumSize(new java.awt.Dimension(150, 24));
                protocolSettingJComboBox.setMinimumSize(new java.awt.Dimension(150, 24));
                protocolSettingJComboBox.setPreferredSize(new java.awt.Dimension(150, 24));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                protocolSettingsJPanel.add(protocolSettingJComboBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                protocolJPanel.add(protocolSettingsJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(protocolJPanel, gridBagConstraints);

                interfaceJPanel.setLayout(new java.awt.GridBagLayout());

                interfaceJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Interface", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                interfaceJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                interfaceJLabel.setText("The ethernet interface (NIC) you would like this policy to handle.");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                interfaceJPanel.add(interfaceJLabel, gridBagConstraints);

                interfaceSettingsJPanel.setLayout(new java.awt.GridBagLayout());

                interfaceClientJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                interfaceClientJLabel.setText("Client:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                interfaceSettingsJPanel.add(interfaceClientJLabel, gridBagConstraints);

                interfaceClientJComboBox.setMaximumSize(new java.awt.Dimension(150, 24));
                interfaceClientJComboBox.setMinimumSize(new java.awt.Dimension(150, 24));
                interfaceClientJComboBox.setPreferredSize(new java.awt.Dimension(150, 24));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                interfaceSettingsJPanel.add(interfaceClientJComboBox, gridBagConstraints);

                interfaceServerJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                interfaceServerJLabel.setText("Server:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
                interfaceSettingsJPanel.add(interfaceServerJLabel, gridBagConstraints);

                interfaceServerJComboBox.setMaximumSize(new java.awt.Dimension(150, 24));
                interfaceServerJComboBox.setMinimumSize(new java.awt.Dimension(150, 24));
                interfaceServerJComboBox.setPreferredSize(new java.awt.Dimension(150, 24));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
                interfaceSettingsJPanel.add(interfaceServerJComboBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                interfaceJPanel.add(interfaceSettingsJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(interfaceJPanel, gridBagConstraints);

                addressJPanel.setLayout(new java.awt.GridBagLayout());

                addressJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Address", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                addressJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                addressJLabel.setText("The IP address which you would like this policy to handle.");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                addressJPanel.add(addressJLabel, gridBagConstraints);

                addressSettingsJPanel.setLayout(new java.awt.GridBagLayout());

                addressClientJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                addressClientJLabel.setText("Client:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                addressSettingsJPanel.add(addressClientJLabel, gridBagConstraints);

                addressClientJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                addressClientJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                addressClientJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                addressSettingsJPanel.add(addressClientJTextField, gridBagConstraints);

                addressServerJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                addressServerJLabel.setText("Server:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
                addressSettingsJPanel.add(addressServerJLabel, gridBagConstraints);

                addressServerJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                addressServerJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                addressServerJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 2, 0);
                addressSettingsJPanel.add(addressServerJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                addressJPanel.add(addressSettingsJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(addressJPanel, gridBagConstraints);

                portJPanel.setLayout(new java.awt.GridBagLayout());

                portJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Port", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                portJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                portJLabel.setText("The port which you would like this policy to handle.");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                portJPanel.add(portJLabel, gridBagConstraints);

                portSettingsJPanel.setLayout(new java.awt.GridBagLayout());

                portServerJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                portServerJLabel.setText("Server:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                portSettingsJPanel.add(portServerJLabel, gridBagConstraints);

                portServerJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                portServerJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                portServerJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                portSettingsJPanel.add(portServerJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                portJPanel.add(portSettingsJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(portJPanel, gridBagConstraints);

                userJPanel.setLayout(new java.awt.GridBagLayout());

                userJPanel.setBorder(new javax.swing.border.TitledBorder(null, "User", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                userJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                userJLabel.setText("The user you would like to apply this policy to.");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                userJPanel.add(userJLabel, gridBagConstraints);

                userSettingsJPanel.setLayout(new java.awt.GridBagLayout());

                userSettingsJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                userSettingsJLabel.setText("User: any");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                userSettingsJPanel.add(userSettingsJLabel, gridBagConstraints);

                userSettingsJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                userSettingsJButton.setText("Change User");
                userSettingsJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                userSettingsJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
                userSettingsJPanel.add(userSettingsJButton, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                userJPanel.add(userSettingsJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(userJPanel, gridBagConstraints);

                timeJPanel.setLayout(new java.awt.GridBagLayout());

                timeJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Time of Day", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                timeJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                timeJLabel.setText("The time of day you would like this policy active.");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                timeJPanel.add(timeJLabel, gridBagConstraints);

                timeSettingsJPanel.setLayout(new java.awt.GridBagLayout());

                timeStartJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                timeStartJLabel.setText("Start Time:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                timeSettingsJPanel.add(timeStartJLabel, gridBagConstraints);

                timeStartJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                timeStartJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                timeStartJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                timeSettingsJPanel.add(timeStartJTextField, gridBagConstraints);

                timeEndJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                timeEndJLabel.setText("End Time:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
                timeSettingsJPanel.add(timeEndJLabel, gridBagConstraints);

                timeEndJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                timeEndJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                timeEndJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 2, 0);
                timeSettingsJPanel.add(timeEndJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                timeJPanel.add(timeSettingsJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(timeJPanel, gridBagConstraints);

                dayJPanel.setLayout(new java.awt.GridBagLayout());

                dayJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Days of Week", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                dayJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                dayJLabel.setText("The days of the week you would like this policy active.");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                dayJPanel.add(dayJLabel, gridBagConstraints);

                daySettingsJPanel.setLayout(new java.awt.GridBagLayout());

                sundayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                sundayJCheckBox.setText("Sun");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                daySettingsJPanel.add(sundayJCheckBox, gridBagConstraints);

                mondayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                mondayJCheckBox.setText("Mon");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                daySettingsJPanel.add(mondayJCheckBox, gridBagConstraints);

                tuesdayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                tuesdayJCheckBox.setText("Tue");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                daySettingsJPanel.add(tuesdayJCheckBox, gridBagConstraints);

                wednesdayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                wednesdayJCheckBox.setText("Wed");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                daySettingsJPanel.add(wednesdayJCheckBox, gridBagConstraints);

                thursdayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                thursdayJCheckBox.setText("Thu");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                daySettingsJPanel.add(thursdayJCheckBox, gridBagConstraints);

                fridayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                fridayJCheckBox.setText("Fri");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                daySettingsJPanel.add(fridayJCheckBox, gridBagConstraints);

                saturdayJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                saturdayJCheckBox.setText("Sat");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
                daySettingsJPanel.add(saturdayJCheckBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                dayJPanel.add(daySettingsJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(dayJPanel, gridBagConstraints);

                invertJPanel.setLayout(new java.awt.GridBagLayout());

                invertJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Invert Time", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                invertJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                invertJLabel.setText("<html>This allows you to activate a policy when it is normally off, and deactivate it when it is normally on.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                invertJPanel.add(invertJLabel, gridBagConstraints);

                invertSettingsJPanel.setLayout(new java.awt.GridBagLayout());

                timeInvertJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                timeInvertJCheckBox.setText("Invert day and time");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
                invertSettingsJPanel.add(timeInvertJCheckBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                invertJPanel.add(invertSettingsJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(invertJPanel, gridBagConstraints);

                rackJPanel.setLayout(new java.awt.GridBagLayout());

                rackJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Rack", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                rackJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                rackJLabel.setText("The rack you would like to use to handle this policy.");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
                rackJPanel.add(rackJLabel, gridBagConstraints);

                rackSettingsJPanel.setLayout(new java.awt.GridBagLayout());

                rackSelectJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                rackSelectJLabel.setText("Rack:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                rackSettingsJPanel.add(rackSelectJLabel, gridBagConstraints);

                rackSelectJComboBox.setMaximumSize(new java.awt.Dimension(200, 24));
                rackSelectJComboBox.setMinimumSize(new java.awt.Dimension(200, 24));
                rackSelectJComboBox.setPreferredSize(new java.awt.Dimension(200, 24));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                rackSettingsJPanel.add(rackSelectJComboBox, gridBagConstraints);

                rackDescJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                rackDescJLabel.setText("Description:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
                rackSettingsJPanel.add(rackDescJLabel, gridBagConstraints);

                rackDescJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                rackDescJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                rackDescJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 2, 0);
                rackSettingsJPanel.add(rackDescJTextField, gridBagConstraints);

                rackEnableJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
                rackEnableJCheckBox.setText("Enable this Policy");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
                rackSettingsJPanel.add(rackEnableJCheckBox, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
                rackJPanel.add(rackSettingsJPanel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
                add(rackJPanel, gridBagConstraints);

        }//GEN-END:initComponents

		private void userSettingsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userSettingsJButtonActionPerformed
            UidSelectJDialog uidSelectJDialog = UidSelectJDialog.factory((Window) getTopLevelAncestor());
            uidSelectJDialog.setVisible(true);
            newUser = uidSelectJDialog.getUid();
            userSettingsJLabel.setText("User: " + newUser);
		}//GEN-LAST:event_userSettingsJButtonActionPerformed
                
                
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel addressClientJLabel;
        public javax.swing.JTextField addressClientJTextField;
        private javax.swing.JLabel addressJLabel;
        private javax.swing.JPanel addressJPanel;
        private javax.swing.JLabel addressServerJLabel;
        public javax.swing.JTextField addressServerJTextField;
        private javax.swing.JPanel addressSettingsJPanel;
        private javax.swing.JLabel dayJLabel;
        private javax.swing.JPanel dayJPanel;
        private javax.swing.JPanel daySettingsJPanel;
        private javax.swing.JCheckBox fridayJCheckBox;
        private javax.swing.JComboBox interfaceClientJComboBox;
        private javax.swing.JLabel interfaceClientJLabel;
        private javax.swing.JLabel interfaceJLabel;
        private javax.swing.JPanel interfaceJPanel;
        private javax.swing.JComboBox interfaceServerJComboBox;
        private javax.swing.JLabel interfaceServerJLabel;
        private javax.swing.JPanel interfaceSettingsJPanel;
        private javax.swing.JLabel invertJLabel;
        private javax.swing.JPanel invertJPanel;
        private javax.swing.JPanel invertSettingsJPanel;
        private javax.swing.JCheckBox mondayJCheckBox;
        private javax.swing.JLabel portJLabel;
        private javax.swing.JPanel portJPanel;
        private javax.swing.JLabel portServerJLabel;
        public javax.swing.JTextField portServerJTextField;
        private javax.swing.JPanel portSettingsJPanel;
        private javax.swing.JLabel protocolJLabel;
        private javax.swing.JPanel protocolJPanel;
        private javax.swing.JComboBox protocolSettingJComboBox;
        private javax.swing.JLabel protocolSettingJLabel;
        private javax.swing.JPanel protocolSettingsJPanel;
        private javax.swing.JLabel rackDescJLabel;
        public javax.swing.JTextField rackDescJTextField;
        private javax.swing.JCheckBox rackEnableJCheckBox;
        private javax.swing.JLabel rackJLabel;
        private javax.swing.JPanel rackJPanel;
        private javax.swing.JComboBox rackSelectJComboBox;
        private javax.swing.JLabel rackSelectJLabel;
        private javax.swing.JPanel rackSettingsJPanel;
        private javax.swing.JCheckBox saturdayJCheckBox;
        private javax.swing.ButtonGroup smtpButtonGroup;
        private javax.swing.JCheckBox sundayJCheckBox;
        private javax.swing.JCheckBox thursdayJCheckBox;
        private javax.swing.JLabel timeEndJLabel;
        public javax.swing.JTextField timeEndJTextField;
        private javax.swing.JCheckBox timeInvertJCheckBox;
        private javax.swing.JLabel timeJLabel;
        private javax.swing.JPanel timeJPanel;
        private javax.swing.JPanel timeSettingsJPanel;
        private javax.swing.JLabel timeStartJLabel;
        public javax.swing.JTextField timeStartJTextField;
        private javax.swing.JCheckBox tuesdayJCheckBox;
        private javax.swing.JLabel userJLabel;
        private javax.swing.JPanel userJPanel;
        private javax.swing.JButton userSettingsJButton;
        private javax.swing.JLabel userSettingsJLabel;
        private javax.swing.JPanel userSettingsJPanel;
        private javax.swing.JCheckBox wednesdayJCheckBox;
        // End of variables declaration//GEN-END:variables
    

}
