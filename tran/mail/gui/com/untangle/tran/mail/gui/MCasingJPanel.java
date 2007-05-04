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


package com.untangle.tran.mail.gui;

import java.awt.*;
import javax.swing.*;

import com.untangle.gui.configuration.*;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.tran.*;
import com.untangle.tran.mail.papi.MailTransformSettings;

public class MCasingJPanel extends com.untangle.gui.transform.MCasingJPanel<MaintenanceCompoundSettings> {

    private static final String EXCEPTION_TIMEOUT_RANGE = "The timeout must be between "
        + MailTransformSettings.TIMEOUT_MIN/1000l
        + " and " + MailTransformSettings.TIMEOUT_MAX/1000l + ".";

    public MCasingJPanel() {
        initComponents();

        smtpInboundJSpinner.setModel( new SpinnerNumberModel((Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                             (Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                             (Long)(MailTransformSettings.TIMEOUT_MAX/1000l),
                                                             (Long)1l) );
        smtpOutboundJSpinner.setModel( new SpinnerNumberModel((Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                              (Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                              (Long)(MailTransformSettings.TIMEOUT_MAX/1000l),
                                                              (Long)1l) );
        popInboundJSpinner.setModel( new SpinnerNumberModel((Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                            (Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                            (Long)(MailTransformSettings.TIMEOUT_MAX/1000l),
                                                            (Long)1l) );
        popOutboundJSpinner.setModel( new SpinnerNumberModel((Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                             (Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                             (Long)(MailTransformSettings.TIMEOUT_MAX/1000l),
                                                             (Long)1l) );
        imapInboundJSpinner.setModel( new SpinnerNumberModel((Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                             (Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                             (Long)(MailTransformSettings.TIMEOUT_MAX/1000l),
                                                             (Long)1l) );
        imapOutboundJSpinner.setModel( new SpinnerNumberModel((Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                              (Long)(MailTransformSettings.TIMEOUT_MIN/1000l),
                                                              (Long)(MailTransformSettings.TIMEOUT_MAX/1000l),
                                                              (Long)1l) );
        timeoutLimitsJLabel.setText("(max=" + (MailTransformSettings.TIMEOUT_MAX/1000l) + " min=" +  (MailTransformSettings.TIMEOUT_MIN/1000l) + ")");

        Util.addPanelFocus(this, smtpEnabledRadioButton);
        Util.addFocusHighlight(smtpInboundJSpinner);
        Util.addFocusHighlight(smtpOutboundJSpinner);
        Util.addFocusHighlight(popInboundJSpinner);
        Util.addFocusHighlight(popOutboundJSpinner);
        Util.addFocusHighlight(imapInboundJSpinner);
        Util.addFocusHighlight(imapOutboundJSpinner);
    }

    public String getDisplayName(){ return "Mail Settings"; }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(MaintenanceCompoundSettings maintenanceCompoundSettings, boolean validateOnly) throws Exception {

        // EMAIL ENABLED ///////////
        boolean isSmtpEnabled = smtpEnabledRadioButton.isSelected();
        boolean isPopEnabled = popEnabledRadioButton.isSelected();
        boolean isImapEnabled = imapEnabledRadioButton.isSelected();

        // EMAIL TIMEOUTS //////////
        long smtpInboundTimeout;
        try{
            smtpInboundJSpinner.commitEdit();
            smtpInboundTimeout = ((Long) smtpInboundJSpinner.getValue()) * 1000l;
        }
        catch(Exception e){
            ((JSpinner.DefaultEditor)smtpInboundJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_TIMEOUT_RANGE);
        }

        long smtpOutboundTimeout;
        try{
            smtpOutboundJSpinner.commitEdit();
            smtpOutboundTimeout = ((Long) smtpOutboundJSpinner.getValue()) * 1000l;
        }
        catch(Exception e){
            ((JSpinner.DefaultEditor)smtpOutboundJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_TIMEOUT_RANGE);
        }

        long popInboundTimeout;
        try{
            popInboundJSpinner.commitEdit();
            popInboundTimeout = ((Long) popInboundJSpinner.getValue()) * 1000l;
        }
        catch(Exception e){
            ((JSpinner.DefaultEditor)popInboundJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_TIMEOUT_RANGE);
        }

        long popOutboundTimeout;
        try{
            popOutboundJSpinner.commitEdit();
            popOutboundTimeout = ((Long) popOutboundJSpinner.getValue()) * 1000l;
        }
        catch(Exception e){
            ((JSpinner.DefaultEditor)popOutboundJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_TIMEOUT_RANGE);
        }

        long imapInboundTimeout;
        try{
            imapInboundJSpinner.commitEdit();
            imapInboundTimeout = ((Long) imapInboundJSpinner.getValue()) * 1000l;
        }
        catch(Exception e){
            ((JSpinner.DefaultEditor)imapInboundJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_TIMEOUT_RANGE);
        }

        long imapOutboundTimeout;
        try{
            imapOutboundJSpinner.commitEdit();
            imapOutboundTimeout = ((Long) imapOutboundJSpinner.getValue()) * 1000l;
        }
        catch(Exception e){
            ((JSpinner.DefaultEditor)imapOutboundJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_TIMEOUT_RANGE);
        }



        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            MailTransformSettings mailTransformSettings =
                ((MailTransformCompoundSettings)maintenanceCompoundSettings.getMailTransformCompoundSettings()).getMailTransformSettings();
            mailTransformSettings.setSmtpEnabled(isSmtpEnabled);
            mailTransformSettings.setPopEnabled(isPopEnabled);
            mailTransformSettings.setImapEnabled(isImapEnabled);
            mailTransformSettings.setSmtpInboundTimeout(smtpInboundTimeout);
            mailTransformSettings.setSmtpOutboundTimeout(smtpOutboundTimeout);
            mailTransformSettings.setPopInboundTimeout(popInboundTimeout);
            mailTransformSettings.setPopOutboundTimeout(popOutboundTimeout);
            mailTransformSettings.setImapInboundTimeout(imapInboundTimeout);
            mailTransformSettings.setImapOutboundTimeout(imapOutboundTimeout);
        }

    }

    public void doRefresh(MaintenanceCompoundSettings maintenanceCompoundSettings){

        MailTransformSettings mailTransformSettings =
            ((MailTransformCompoundSettings)maintenanceCompoundSettings.getMailTransformCompoundSettings()).getMailTransformSettings();

        // EMAIL ENABLED /////////
        boolean isSmtpEnabled = mailTransformSettings.isSmtpEnabled();
        boolean isPopEnabled = mailTransformSettings.isPopEnabled();
        boolean isImapEnabled = mailTransformSettings.isImapEnabled();

        if( isSmtpEnabled )
            smtpEnabledRadioButton.setSelected(true);
        else
            smtpDisabledRadioButton.setSelected(true);
        Util.addSettingChangeListener(settingsChangedListener, this, smtpEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, smtpDisabledRadioButton);

        if( isPopEnabled )
            popEnabledRadioButton.setSelected(true);
        else
            popDisabledRadioButton.setSelected(true);
        Util.addSettingChangeListener(settingsChangedListener, this, popEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, popDisabledRadioButton);

        if( isImapEnabled )
            imapEnabledRadioButton.setSelected(true);
        else
            imapDisabledRadioButton.setSelected(true);
        Util.addSettingChangeListener(settingsChangedListener, this, imapEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, imapDisabledRadioButton);

        // EMAIL TIMEOUT /////////////
        smtpInboundJSpinner.setValue( (Long) (mailTransformSettings.getSmtpInboundTimeout()/1000l) );
        ((JSpinner.DefaultEditor)smtpInboundJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, smtpInboundJSpinner);

        smtpOutboundJSpinner.setValue( (Long) (mailTransformSettings.getSmtpOutboundTimeout()/1000l) );
        ((JSpinner.DefaultEditor)smtpOutboundJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, smtpOutboundJSpinner);

        popInboundJSpinner.setValue( (Long) (mailTransformSettings.getPopInboundTimeout()/1000l) );
        ((JSpinner.DefaultEditor)popInboundJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, popInboundJSpinner);

        popOutboundJSpinner.setValue( (Long) (mailTransformSettings.getPopOutboundTimeout()/1000l) );
        ((JSpinner.DefaultEditor)popOutboundJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, popOutboundJSpinner);

        imapInboundJSpinner.setValue( (Long) (mailTransformSettings.getImapInboundTimeout()/1000l) );
        ((JSpinner.DefaultEditor)imapInboundJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, imapInboundJSpinner);

        imapOutboundJSpinner.setValue( (Long) (mailTransformSettings.getImapOutboundTimeout()/1000l) );
        ((JSpinner.DefaultEditor)imapOutboundJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, imapOutboundJSpinner);
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        smtpButtonGroup = new javax.swing.ButtonGroup();
        popButtonGroup = new javax.swing.ButtonGroup();
        imapButtonGroup = new javax.swing.ButtonGroup();
        overrideJPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        smtpEnabledRadioButton = new javax.swing.JRadioButton();
        smtpDisabledRadioButton = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        popEnabledRadioButton = new javax.swing.JRadioButton();
        popDisabledRadioButton = new javax.swing.JRadioButton();
        jSeparator2 = new javax.swing.JSeparator();
        imapEnabledRadioButton = new javax.swing.JRadioButton();
        imapDisabledRadioButton = new javax.swing.JRadioButton();
        timeoutJPanel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        timeoutLimitsJLabel = new javax.swing.JLabel();
        smtpJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        smtpInboundJSpinner = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        smtpOutboundJSpinner = new javax.swing.JSpinner();
        jSeparator3 = new javax.swing.JSeparator();
        popJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        popInboundJSpinner = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        popOutboundJSpinner = new javax.swing.JSpinner();
        jSeparator4 = new javax.swing.JSeparator();
        imapJPanel = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        imapInboundJSpinner = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        imapOutboundJSpinner = new javax.swing.JSpinner();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(550, 520));
        setMinimumSize(new java.awt.Dimension(550, 520));
        setPreferredSize(new java.awt.Dimension(550, 520));
        overrideJPanel.setLayout(new java.awt.GridBagLayout());

        overrideJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Email Overrides", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("Warning:  These settings should not be changed unless instructed to do so by support.");
        overrideJPanel.add(jLabel3, new java.awt.GridBagConstraints());

        smtpButtonGroup.add(smtpEnabledRadioButton);
        smtpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        smtpEnabledRadioButton.setText("<html><b>Enable SMTP</b> email processing.  (This is the default setting)</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        overrideJPanel.add(smtpEnabledRadioButton, gridBagConstraints);

        smtpButtonGroup.add(smtpDisabledRadioButton);
        smtpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        smtpDisabledRadioButton.setText("<html><b>Disable SMTP</b> email processing.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        overrideJPanel.add(smtpDisabledRadioButton, gridBagConstraints);

        jSeparator1.setForeground(new java.awt.Color(180, 180, 180));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        overrideJPanel.add(jSeparator1, gridBagConstraints);

        popButtonGroup.add(popEnabledRadioButton);
        popEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        popEnabledRadioButton.setText("<html><b>Enable POP</b> email processing.  (This is the default setting)</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        overrideJPanel.add(popEnabledRadioButton, gridBagConstraints);

        popButtonGroup.add(popDisabledRadioButton);
        popDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        popDisabledRadioButton.setText("<html><b>Disable POP</b> email processing.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        overrideJPanel.add(popDisabledRadioButton, gridBagConstraints);

        jSeparator2.setForeground(new java.awt.Color(180, 180, 180));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        overrideJPanel.add(jSeparator2, gridBagConstraints);

        imapButtonGroup.add(imapEnabledRadioButton);
        imapEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        imapEnabledRadioButton.setText("<html><b>Enable IMAP</b> email processing.  (This is the default setting)</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        overrideJPanel.add(imapEnabledRadioButton, gridBagConstraints);

        imapButtonGroup.add(imapDisabledRadioButton);
        imapDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        imapDisabledRadioButton.setText("<html><b>Disable IMAP</b> email processing.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        overrideJPanel.add(imapDisabledRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(overrideJPanel, gridBagConstraints);

        timeoutJPanel.setLayout(new java.awt.GridBagLayout());

        timeoutJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Email Timeouts", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("Warning:  These settings should not be changed unless instructed to do so by support.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        timeoutJPanel.add(jLabel4, gridBagConstraints);

        timeoutLimitsJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        timeoutLimitsJLabel.setText("(max= min= )");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        timeoutJPanel.add(timeoutLimitsJLabel, gridBagConstraints);

        smtpJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("SMTP Inbound (seconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        smtpJPanel.add(jLabel1, gridBagConstraints);

        smtpInboundJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        smtpInboundJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        smtpInboundJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        smtpInboundJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 0);
        smtpJPanel.add(smtpInboundJSpinner, gridBagConstraints);

        jLabel2.setText("SMTP Outbound (seconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        smtpJPanel.add(jLabel2, gridBagConstraints);

        smtpOutboundJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        smtpOutboundJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        smtpOutboundJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        smtpOutboundJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        smtpJPanel.add(smtpOutboundJSpinner, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        timeoutJPanel.add(smtpJPanel, gridBagConstraints);

        jSeparator3.setForeground(new java.awt.Color(180, 180, 180));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        timeoutJPanel.add(jSeparator3, gridBagConstraints);

        popJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setText("POP Inbound (seconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        popJPanel.add(jLabel5, gridBagConstraints);

        popInboundJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        popInboundJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        popInboundJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        popInboundJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 19, 10, 0);
        popJPanel.add(popInboundJSpinner, gridBagConstraints);

        jLabel6.setText("POP Outbound (seconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        popJPanel.add(jLabel6, gridBagConstraints);

        popOutboundJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        popOutboundJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        popOutboundJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        popOutboundJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 19, 0, 0);
        popJPanel.add(popOutboundJSpinner, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        timeoutJPanel.add(popJPanel, gridBagConstraints);

        jSeparator4.setForeground(new java.awt.Color(180, 180, 180));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        timeoutJPanel.add(jSeparator4, gridBagConstraints);

        imapJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel7.setText("IMAP Inbound (seconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        imapJPanel.add(jLabel7, gridBagConstraints);

        imapInboundJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        imapInboundJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        imapInboundJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        imapInboundJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 10, 0);
        imapJPanel.add(imapInboundJSpinner, gridBagConstraints);

        jLabel8.setText("IMAP Outbound (seconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        imapJPanel.add(jLabel8, gridBagConstraints);

        imapOutboundJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        imapOutboundJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        imapOutboundJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        imapOutboundJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        imapJPanel.add(imapOutboundJSpinner, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        timeoutJPanel.add(imapJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(timeoutJPanel, gridBagConstraints);

    }//GEN-END:initComponents



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup imapButtonGroup;
    public javax.swing.JRadioButton imapDisabledRadioButton;
    public javax.swing.JRadioButton imapEnabledRadioButton;
    private javax.swing.JSpinner imapInboundJSpinner;
    private javax.swing.JPanel imapJPanel;
    private javax.swing.JSpinner imapOutboundJSpinner;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JPanel overrideJPanel;
    private javax.swing.ButtonGroup popButtonGroup;
    public javax.swing.JRadioButton popDisabledRadioButton;
    public javax.swing.JRadioButton popEnabledRadioButton;
    private javax.swing.JSpinner popInboundJSpinner;
    private javax.swing.JPanel popJPanel;
    private javax.swing.JSpinner popOutboundJSpinner;
    private javax.swing.ButtonGroup smtpButtonGroup;
    public javax.swing.JRadioButton smtpDisabledRadioButton;
    public javax.swing.JRadioButton smtpEnabledRadioButton;
    private javax.swing.JSpinner smtpInboundJSpinner;
    private javax.swing.JPanel smtpJPanel;
    private javax.swing.JSpinner smtpOutboundJSpinner;
    private javax.swing.JPanel timeoutJPanel;
    private javax.swing.JLabel timeoutLimitsJLabel;
    // End of variables declaration//GEN-END:variables


}
