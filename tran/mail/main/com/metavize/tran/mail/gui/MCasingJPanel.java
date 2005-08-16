/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.mail.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.tran.mail.papi.MailTransformSettings;

import java.awt.*;
import javax.swing.*;

public class MCasingJPanel extends com.metavize.gui.transform.MCasingJPanel {

    public MCasingJPanel(TransformContext transformContext) {
        super(transformContext);
        initComponents();
        
        smtpInboundJSpinner.setModel( new SpinnerNumberModel((Long)MailTransformSettings.TIMEOUT_MIN,
							     (Long)MailTransformSettings.TIMEOUT_MIN,
							     (Long)MailTransformSettings.TIMEOUT_MAX,
							     (Long)1l) );
        smtpOutboundJSpinner.setModel( new SpinnerNumberModel((Long)MailTransformSettings.TIMEOUT_MIN,
							      (Long)MailTransformSettings.TIMEOUT_MIN,
							      (Long)MailTransformSettings.TIMEOUT_MAX,
							      (Long)1l) );
        popInboundJSpinner.setModel( new SpinnerNumberModel((Long)MailTransformSettings.TIMEOUT_MIN,
							    (Long)MailTransformSettings.TIMEOUT_MIN,
							    (Long)MailTransformSettings.TIMEOUT_MAX,
							    (Long)1l) );
        popOutboundJSpinner.setModel( new SpinnerNumberModel((Long)MailTransformSettings.TIMEOUT_MIN,
							     (Long)MailTransformSettings.TIMEOUT_MIN,
							     (Long)MailTransformSettings.TIMEOUT_MAX,
							     (Long)1l) );
        imapInboundJSpinner.setModel( new SpinnerNumberModel((Long)MailTransformSettings.TIMEOUT_MIN,
							     (Long)MailTransformSettings.TIMEOUT_MIN,
							     (Long)MailTransformSettings.TIMEOUT_MAX,
							     (Long)1l) );
        imapOutboundJSpinner.setModel( new SpinnerNumberModel((Long)MailTransformSettings.TIMEOUT_MIN,
							      (Long)MailTransformSettings.TIMEOUT_MIN,
							      (Long)MailTransformSettings.TIMEOUT_MAX,
							      (Long)1l) );
	timeoutLimitsJLabel.setText("(max=" + MailTransformSettings.TIMEOUT_MAX + " min=" +  MailTransformSettings.TIMEOUT_MIN + ")");
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // EMAIL ENABLED ///////////
        boolean isSmtpEnabled = smtpEnabledRadioButton.isSelected();
        boolean isPopEnabled = popEnabledRadioButton.isSelected();
        boolean isImapEnabled = imapEnabledRadioButton.isSelected();
        
        // EMAIL TIMEOUTS //////////
        long smtpInboundTimeout = (Long) smtpInboundJSpinner.getValue();
        long smtpOutboundTimeout = (Long) smtpOutboundJSpinner.getValue();
        long popInboundTimeout = (Long) popInboundJSpinner.getValue();
        long popOutboundTimeout = (Long) popOutboundJSpinner.getValue();
        long imapInboundTimeout = (Long) imapInboundJSpinner.getValue();
        long imapOutboundTimeout = (Long) imapOutboundJSpinner.getValue();
        
	// SAVE SETTINGS ////////////
	if( !validateOnly ){
            MailTransformSettings mailTransformSettings = (MailTransformSettings) transformContext.transform().getSettings();
            mailTransformSettings.setSmtpEnabled(isSmtpEnabled);
            mailTransformSettings.setPopEnabled(isPopEnabled);
            mailTransformSettings.setImapEnabled(isImapEnabled);
            mailTransformSettings.setSmtpInboundTimeout(smtpInboundTimeout);
            mailTransformSettings.setSmtpOutboundTimeout(smtpOutboundTimeout);
            mailTransformSettings.setPopInboundTimeout(popInboundTimeout);
            mailTransformSettings.setPopOutboundTimeout(popOutboundTimeout);
            mailTransformSettings.setImapInboundTimeout(imapInboundTimeout);
            mailTransformSettings.setImapOutboundTimeout(imapOutboundTimeout);
            transformContext.transform().setSettings(mailTransformSettings);
        }

    }

    public void doRefresh(Object settings){
        
        MailTransformSettings mailTransformSettings = (MailTransformSettings) transformContext.transform().getSettings();
        
        // EMAIL ENABLED /////////
        boolean isSmtpEnabled = mailTransformSettings.isSmtpEnabled();
        boolean isPopEnabled = mailTransformSettings.isPopEnabled();
        boolean isImapEnabled = mailTransformSettings.isImapEnabled();

        if( isSmtpEnabled )
            smtpEnabledRadioButton.setSelected(true);
        else
            smtpDisabledRadioButton.setSelected(true);

        if( isPopEnabled )
            popEnabledRadioButton.setSelected(true);
        else
            popDisabledRadioButton.setSelected(true);

        if( isImapEnabled )
            imapEnabledRadioButton.setSelected(true);
        else
            imapDisabledRadioButton.setSelected(true);
        
        // EMAIL TIMEOUT /////////////
        smtpInboundJSpinner.setValue( (Long) mailTransformSettings.getSmtpInboundTimeout() );
        smtpOutboundJSpinner.setValue( (Long) mailTransformSettings.getSmtpOutboundTimeout() );
        popInboundJSpinner.setValue( (Long) mailTransformSettings.getPopInboundTimeout() );
        popOutboundJSpinner.setValue( (Long) mailTransformSettings.getPopOutboundTimeout() );
        imapInboundJSpinner.setValue( (Long) mailTransformSettings.getImapInboundTimeout() );
        imapOutboundJSpinner.setValue( (Long) mailTransformSettings.getImapOutboundTimeout() );
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

        setMaximumSize(new java.awt.Dimension(563, 520));
        setMinimumSize(new java.awt.Dimension(563, 520));
        setPreferredSize(new java.awt.Dimension(563, 520));
        overrideJPanel.setLayout(new java.awt.GridBagLayout());

        overrideJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Email Overrides", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("Warning:  These settings should not be changed unless instructed to do so by support.");
        overrideJPanel.add(jLabel3, new java.awt.GridBagConstraints());

        smtpButtonGroup.add(smtpEnabledRadioButton);
        smtpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        smtpEnabledRadioButton.setText("<html><b>Enable SMTP</b> email processing.  (This is the default settings)</html>");
        smtpEnabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        overrideJPanel.add(smtpEnabledRadioButton, gridBagConstraints);

        smtpButtonGroup.add(smtpDisabledRadioButton);
        smtpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        smtpDisabledRadioButton.setText("<html><b>Disable SMTP</b> email processing.</html>");
        smtpDisabledRadioButton.setFocusPainted(false);
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
        popEnabledRadioButton.setText("<html><b>Enable POP</b> email processing.  (This is the default settings)</html>");
        popEnabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        overrideJPanel.add(popEnabledRadioButton, gridBagConstraints);

        popButtonGroup.add(popDisabledRadioButton);
        popDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        popDisabledRadioButton.setText("<html><b>Disable POP</b> email processing.</html>");
        popDisabledRadioButton.setFocusPainted(false);
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
        imapEnabledRadioButton.setText("<html><b>Enable IMAP</b> email processing.  (This is the default settings)</html>");
        imapEnabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        overrideJPanel.add(imapEnabledRadioButton, gridBagConstraints);

        imapButtonGroup.add(imapDisabledRadioButton);
        imapDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        imapDisabledRadioButton.setText("<html><b>Disable IMAP</b> email processing.</html>");
        imapDisabledRadioButton.setFocusPainted(false);
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

        jLabel1.setText("SMTP Inbound (milliseconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        smtpJPanel.add(jLabel1, gridBagConstraints);

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

        jLabel2.setText("SMTP Outbound (milliseconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        smtpJPanel.add(jLabel2, gridBagConstraints);

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
        timeoutJPanel.add(jSeparator3, gridBagConstraints);

        popJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setText("POP Inbound (milliseconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        popJPanel.add(jLabel5, gridBagConstraints);

        popInboundJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        popInboundJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        popInboundJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 0);
        popJPanel.add(popInboundJSpinner, gridBagConstraints);

        jLabel6.setText("POP Outbound (milliseconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        popJPanel.add(jLabel6, gridBagConstraints);

        popOutboundJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        popOutboundJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        popOutboundJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
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
        timeoutJPanel.add(jSeparator4, gridBagConstraints);

        imapJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel7.setText("IMAP Inbound (milliseconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        imapJPanel.add(jLabel7, gridBagConstraints);

        imapInboundJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        imapInboundJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        imapInboundJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 0);
        imapJPanel.add(imapInboundJSpinner, gridBagConstraints);

        jLabel8.setText("IMAP Outbound (milliseconds)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        imapJPanel.add(jLabel8, gridBagConstraints);

        imapOutboundJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        imapOutboundJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        imapOutboundJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
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
