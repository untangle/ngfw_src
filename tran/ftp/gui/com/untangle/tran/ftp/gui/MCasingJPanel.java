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


package com.untangle.tran.ftp.gui;

import com.untangle.gui.configuration.*;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.security.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.tran.*;
import com.untangle.tran.ftp.FtpSettings;

import java.awt.*;


public class MCasingJPanel extends com.untangle.gui.transform.MCasingJPanel<MaintenanceCompoundSettings> {

    
    public MCasingJPanel() {
        initComponents();
    }

    public String getDisplayName(){ return "FTP Settings"; }

    public void doSave(MaintenanceCompoundSettings maintenanceCompoundSettings, boolean validateOnly) throws Exception {

        // FTP ENABLED ///////////
        boolean isFtpEnabled = ftpEnabledRadioButton.isSelected();
        
	// SAVE SETTINGS ////////////
	if( !validateOnly ){
            FtpSettings ftpSettings = ((FtpTransformCompoundSettings)maintenanceCompoundSettings.getFtpTransformCompoundSettings()).getFtpTransformSettings();
            ftpSettings.setEnabled(isFtpEnabled);
        }

    }

    public void doRefresh(MaintenanceCompoundSettings maintenanceCompoundSettings){
        
        // FTP ENABLED /////////
	FtpSettings ftpSettings = ((FtpTransformCompoundSettings)maintenanceCompoundSettings.getFtpTransformCompoundSettings()).getFtpTransformSettings();
        boolean isFtpEnabled = ftpSettings.isEnabled();
        if( isFtpEnabled )
            ftpEnabledRadioButton.setSelected(true);
        else
            ftpDisabledRadioButton.setSelected(true); 
    }
    
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        ftpButtonGroup = new javax.swing.ButtonGroup();
        ftpJPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        ftpEnabledRadioButton = new javax.swing.JRadioButton();
        ftpDisabledRadioButton = new javax.swing.JRadioButton();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(550, 120));
        setMinimumSize(new java.awt.Dimension(550, 120));
        setPreferredSize(new java.awt.Dimension(550, 120));
        ftpJPanel.setLayout(new java.awt.GridBagLayout());

        ftpJPanel.setBorder(new javax.swing.border.TitledBorder(null, "File Transfer Override", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("Warning:  These settings should not be changed unless instructed to do so by support.");
        ftpJPanel.add(jLabel2, new java.awt.GridBagConstraints());

        ftpButtonGroup.add(ftpEnabledRadioButton);
        ftpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        ftpEnabledRadioButton.setText("<html><b>Enable Processing</b> of File Transfer traffic.  (This is the default setting)</html>");
        ftpEnabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        ftpJPanel.add(ftpEnabledRadioButton, gridBagConstraints);

        ftpButtonGroup.add(ftpDisabledRadioButton);
        ftpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        ftpDisabledRadioButton.setText("<html><b>Disable Processing</b> of File Transfer traffic.</html>");
        ftpDisabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        ftpJPanel.add(ftpDisabledRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(ftpJPanel, gridBagConstraints);

    }//GEN-END:initComponents
    

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup ftpButtonGroup;
    public javax.swing.JRadioButton ftpDisabledRadioButton;
    public javax.swing.JRadioButton ftpEnabledRadioButton;
    private javax.swing.JPanel ftpJPanel;
    private javax.swing.JLabel jLabel2;
    // End of variables declaration//GEN-END:variables
    

}
