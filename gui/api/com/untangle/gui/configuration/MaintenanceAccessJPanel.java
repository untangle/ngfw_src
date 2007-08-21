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

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.MConfigJDialog;
import com.untangle.uvm.*;
import com.untangle.uvm.networking.AccessSettings;
import com.untangle.uvm.networking.MiscSettings;
import com.untangle.uvm.security.*;
import com.untangle.uvm.node.*;

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

        // BRANDING ////
        if(maintenanceCompoundSettings.isBrandingEnabled()){
            supportJCheckBox.setText("<html><b>Allow</b> us to securely access your server for support purposes.</html>");
            reportJCheckBox.setText("<html><b>Send</b> us data about your server. This will send us status updates and an email if any unexpected problems occur, but will not allow us to login to your server. No personal information about your network traffic will be transmitted.</html>");
        }
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
        supportJCheckBox.setText("<html><b>Allow</b> Untangle to securely access my Untangle Server. This will allow the Untangle Support team to monitor and change settings on your Untangle Server.</html>");
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
