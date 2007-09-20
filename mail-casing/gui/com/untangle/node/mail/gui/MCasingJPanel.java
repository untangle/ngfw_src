/*
 * $HeadURL$
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


package com.untangle.node.mail.gui;

import java.awt.*;
import javax.swing.*;

import com.untangle.gui.configuration.*;
import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.node.mail.papi.MailNodeSettings;
import com.untangle.uvm.*;
import com.untangle.uvm.node.*;
import com.untangle.uvm.security.*;

public class MCasingJPanel extends com.untangle.gui.node.MCasingJPanel<MaintenanceCompoundSettings> {

    private static final String EXCEPTION_TIMEOUT_RANGE = "The timeout must be between "
        + MailNodeSettings.TIMEOUT_MIN/1000l
        + " and " + MailNodeSettings.TIMEOUT_MAX/1000l + ".";

    public MCasingJPanel() {
        initComponents();

        smtpJSpinner.setModel( new SpinnerNumberModel((Long)(MailNodeSettings.TIMEOUT_MIN/1000l),
                                                      (Long)(MailNodeSettings.TIMEOUT_MIN/1000l),
                                                      (Long)(MailNodeSettings.TIMEOUT_MAX/1000l),
                                                      (Long)1l) );
        popJSpinner.setModel( new SpinnerNumberModel((Long)(MailNodeSettings.TIMEOUT_MIN/1000l),
                                                     (Long)(MailNodeSettings.TIMEOUT_MIN/1000l),
                                                     (Long)(MailNodeSettings.TIMEOUT_MAX/1000l),
                                                     (Long)1l) );
        imapJSpinner.setModel( new SpinnerNumberModel((Long)(MailNodeSettings.TIMEOUT_MIN/1000l),
                                                      (Long)(MailNodeSettings.TIMEOUT_MIN/1000l),
                                                      (Long)(MailNodeSettings.TIMEOUT_MAX/1000l),
                                                      (Long)1l) );
        timeoutLimitsJLabel.setText("(max=" + (MailNodeSettings.TIMEOUT_MAX/1000l) + " min=" +  (MailNodeSettings.TIMEOUT_MIN/1000l) + ")");

        Util.addPanelFocus(this, smtpEnabledRadioButton);
        Util.addFocusHighlight(smtpJSpinner);
        Util.addFocusHighlight(popJSpinner);
        Util.addFocusHighlight(imapJSpinner);
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
        long smtpTimeout;
        try{
            smtpJSpinner.commitEdit();
            smtpTimeout = ((Long) smtpJSpinner.getValue()) * 1000l;
        }
        catch(Exception e){
            ((JSpinner.DefaultEditor)smtpJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_TIMEOUT_RANGE);
        }

        long popTimeout;
        try{
            popJSpinner.commitEdit();
            popTimeout = ((Long) popJSpinner.getValue()) * 1000l;
        }
        catch(Exception e){
            ((JSpinner.DefaultEditor)popJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_TIMEOUT_RANGE);
        }

        long imapTimeout;
        try{
            imapJSpinner.commitEdit();
            imapTimeout = ((Long) imapJSpinner.getValue()) * 1000l;
        }
        catch(Exception e){
            ((JSpinner.DefaultEditor)imapJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_TIMEOUT_RANGE);
        }


        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            MailNodeSettings mailNodeSettings =
                ((MailNodeCompoundSettings)maintenanceCompoundSettings.getMailNodeCompoundSettings()).getMailNodeSettings();
            mailNodeSettings.setSmtpEnabled(isSmtpEnabled);
            mailNodeSettings.setPopEnabled(isPopEnabled);
            mailNodeSettings.setImapEnabled(isImapEnabled);
            mailNodeSettings.setSmtpTimeout(smtpTimeout);
            mailNodeSettings.setPopTimeout(popTimeout);
            mailNodeSettings.setImapTimeout(imapTimeout);
        }

    }

    public void doRefresh(MaintenanceCompoundSettings maintenanceCompoundSettings){

        MailNodeSettings mailNodeSettings =
            ((MailNodeCompoundSettings)maintenanceCompoundSettings.getMailNodeCompoundSettings()).getMailNodeSettings();

        // EMAIL ENABLED /////////
        boolean isSmtpEnabled = mailNodeSettings.isSmtpEnabled();
        boolean isPopEnabled = mailNodeSettings.isPopEnabled();
        boolean isImapEnabled = mailNodeSettings.isImapEnabled();

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
        smtpJSpinner.setValue( (Long) (mailNodeSettings.getSmtpTimeout()/1000l) );
        ((JSpinner.DefaultEditor)smtpJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, smtpJSpinner);

        popJSpinner.setValue( (Long) (mailNodeSettings.getPopTimeout()/1000l) );
        ((JSpinner.DefaultEditor)popJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, popJSpinner);

        imapJSpinner.setValue( (Long) (mailNodeSettings.getImapTimeout()/1000l) );
        ((JSpinner.DefaultEditor)imapJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, imapJSpinner);
    }


    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
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
        jLabel2 = new javax.swing.JLabel();
        smtpJSpinner = new javax.swing.JSpinner();
        jSeparator3 = new javax.swing.JSeparator();
        popJPanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        popJSpinner = new javax.swing.JSpinner();
        jSeparator4 = new javax.swing.JSeparator();
        imapJPanel = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        imapJSpinner = new javax.swing.JSpinner();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(550, 520));
        setMinimumSize(new java.awt.Dimension(550, 520));
        setPreferredSize(new java.awt.Dimension(550, 520));
        overrideJPanel.setLayout(new java.awt.GridBagLayout());

        overrideJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Email Overrides", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
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

        timeoutJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Email Timeouts", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
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

        jLabel2.setText("SMTP (seconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        smtpJPanel.add(jLabel2, gridBagConstraints);

        smtpJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        smtpJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        smtpJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        smtpJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        smtpJPanel.add(smtpJSpinner, gridBagConstraints);

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

        jLabel6.setText("POP (seconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        popJPanel.add(jLabel6, gridBagConstraints);

        popJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        popJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        popJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        popJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 19, 0, 0);
        popJPanel.add(popJSpinner, gridBagConstraints);

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

        jLabel8.setText("IMAP (seconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        imapJPanel.add(jLabel8, gridBagConstraints);

        imapJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        imapJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        imapJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        imapJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        imapJPanel.add(imapJSpinner, gridBagConstraints);

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

    }// </editor-fold>//GEN-END:initComponents



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup imapButtonGroup;
    public javax.swing.JRadioButton imapDisabledRadioButton;
    public javax.swing.JRadioButton imapEnabledRadioButton;
    private javax.swing.JPanel imapJPanel;
    private javax.swing.JSpinner imapJSpinner;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JPanel overrideJPanel;
    private javax.swing.ButtonGroup popButtonGroup;
    public javax.swing.JRadioButton popDisabledRadioButton;
    public javax.swing.JRadioButton popEnabledRadioButton;
    private javax.swing.JPanel popJPanel;
    private javax.swing.JSpinner popJSpinner;
    private javax.swing.ButtonGroup smtpButtonGroup;
    public javax.swing.JRadioButton smtpDisabledRadioButton;
    public javax.swing.JRadioButton smtpEnabledRadioButton;
    private javax.swing.JPanel smtpJPanel;
    private javax.swing.JSpinner smtpJSpinner;
    private javax.swing.JPanel timeoutJPanel;
    private javax.swing.JLabel timeoutLimitsJLabel;
    // End of variables declaration//GEN-END:variables


}
