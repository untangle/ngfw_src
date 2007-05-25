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

import java.awt.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.MConfigJDialog;
import com.untangle.mvvm.*;
import com.untangle.mvvm.networking.AccessSettings;
import com.untangle.mvvm.networking.MiscSettings;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.tran.*;

public class MaintenanceAccessJPanel extends javax.swing.JPanel
    implements Savable<MaintenanceCompoundSettings>, Refreshable<MaintenanceCompoundSettings> {


    public MaintenanceAccessJPanel() {
        initComponents();
        MConfigJDialog.setInitialFocusComponent(supportJCheckBox);
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(MaintenanceCompoundSettings maintenanceCompoundSettings, boolean validateOnly) throws Exception {

        // SUPPORT ENABLED ////////
        boolean isSupportEnabled = supportJCheckBox.isSelected();

        // REPORTING ENABLED //////
        boolean isExceptionReportingEnabled = reportJCheckBox.isSelected();

        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            AccessSettings accessSettings = maintenanceCompoundSettings.getAccessSettings();
            MiscSettings miscSettings = maintenanceCompoundSettings.getMiscSettings();
            accessSettings.setIsSupportEnabled( isSupportEnabled );
            miscSettings.setIsExceptionReportingEnabled( isExceptionReportingEnabled );
        }
    }

    public void doRefresh(MaintenanceCompoundSettings maintenanceCompoundSettings){
        AccessSettings accessSettings = maintenanceCompoundSettings.getAccessSettings();
        MiscSettings miscSettings = maintenanceCompoundSettings.getMiscSettings();

        // SSH ENABLED ///////
        boolean isSupportEnabled = accessSettings.getIsSupportEnabled();
        supportJCheckBox.setSelected( isSupportEnabled );
        Util.addSettingChangeListener(settingsChangedListener, this, supportJCheckBox);
        Util.addSettingChangeListener(settingsChangedListener, this, supportJCheckBox);

        // REPORTING ENABLED ////
        boolean isExceptionReportingEnabled = miscSettings.getIsExceptionReportingEnabled();
        reportJCheckBox.setSelected( isExceptionReportingEnabled );
        Util.addSettingChangeListener(settingsChangedListener, this, reportJCheckBox);
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        maintainRemoteJPanel = new javax.swing.JPanel();
        jSeparator2 = new javax.swing.JSeparator();
        reportJCheckBox = new javax.swing.JCheckBox();
        supportJCheckBox = new javax.swing.JCheckBox();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 180));
        setMinimumSize(new java.awt.Dimension(563, 180));
        setPreferredSize(new java.awt.Dimension(563, 180));
        maintainRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        maintainRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Untangle Support", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));

        supportJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        supportJCheckBox.setText("<html><b>Allow</b> Untangle complete access to my Untangle Server. This will allow the Untangle Support team to monitor and change settings on your Untangle Server.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        maintainRemoteJPanel.add(supportJCheckBox, gridBagConstraints);

        jSeparator2.setForeground(new java.awt.Color(200, 200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        maintainRemoteJPanel.add(jSeparator2, gridBagConstraints);

        reportJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        reportJCheckBox.setText("<html><b>Send</b> Untangle data about my Untangle Server. This will send Untangle Support team status updates and an email if any unexpected problems occur, but will not allow Untangle to login to your Untangle Server. No personal information about your network traffic will be transmitted.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        maintainRemoteJPanel.add(reportJCheckBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(maintainRemoteJPanel, gridBagConstraints);

    }//GEN-END:initComponents



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPanel maintainRemoteJPanel;
    private javax.swing.JCheckBox reportJCheckBox;
    private javax.swing.JCheckBox supportJCheckBox;
    // End of variables declaration//GEN-END:variables


}
