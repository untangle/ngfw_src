/*
 * NetworkJPanel.java
 *
 * Created on February 22, 2005, 1:10 PM
 */

package com.metavize.gui.configuration;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;

/**
 *
 * @author  inieves
 */
public class MaintenanceJPanel extends javax.swing.JPanel implements Savable, Refreshable {

    
    public MaintenanceJPanel() {
        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // SSH ENABLED ////////
	boolean isSshEnabled = sshEnabledRadioButton.isSelected();
        
        // REPORTING ENABLED //////
        boolean isExceptionReportingEnabled = reportJCheckBox.isSelected();
        
	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    NetworkingConfiguration networkingConfiguration = (NetworkingConfiguration) settings;
            networkingConfiguration.isSshEnabled( isSshEnabled );
            networkingConfiguration.isExceptionReportingEnabled( isExceptionReportingEnabled );
        }

    }

    public void doRefresh(Object settings){
        NetworkingConfiguration networkingConfiguration = (NetworkingConfiguration) settings;
        
        // SSH ENABLED ///////
	boolean isSshEnabled = networkingConfiguration.isSshEnabled();
	if( isSshEnabled )
            sshEnabledRadioButton.setSelected(true);
        else
            sshDisabledRadioButton.setSelected(true);
        
        // REPORTING ENABLED ////
        boolean isExceptionReportingEnabled = networkingConfiguration.isExceptionReportingEnabled();
        reportJCheckBox.setSelected( isExceptionReportingEnabled );
    }
    
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        dhcpButtonGroup = new javax.swing.ButtonGroup();
        tcpWindowButtonGroup = new javax.swing.ButtonGroup();
        externalAdminButtonGroup = new javax.swing.ButtonGroup();
        internalAdminButtonGroup = new javax.swing.ButtonGroup();
        restrictAdminButtonGroup = new javax.swing.ButtonGroup();
        sshButtonGroup = new javax.swing.ButtonGroup();
        maintainRemoteJPanel = new javax.swing.JPanel();
        sshEnabledRadioButton = new javax.swing.JRadioButton();
        sshDisabledRadioButton = new javax.swing.JRadioButton();
        jSeparator2 = new javax.swing.JSeparator();
        reportJCheckBox = new javax.swing.JCheckBox();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 161));
        setMinimumSize(new java.awt.Dimension(563, 161));
        setPreferredSize(new java.awt.Dimension(563, 161));
        maintainRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        maintainRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Secure Remote Maintenance", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        sshButtonGroup.add(sshEnabledRadioButton);
        sshEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        sshEnabledRadioButton.setText("<html><b>Allow</b> secure remote maintenance for Metavize's troubleshooting purposes.</html>");
        sshEnabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        maintainRemoteJPanel.add(sshEnabledRadioButton, gridBagConstraints);

        sshButtonGroup.add(sshDisabledRadioButton);
        sshDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        sshDisabledRadioButton.setText("<html><b>Disallow</b> secure remote maintenance.  (This is the default setting.)</html>");
        sshDisabledRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        maintainRemoteJPanel.add(sshDisabledRadioButton, gridBagConstraints);

        jSeparator2.setForeground(new java.awt.Color(200, 200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        maintainRemoteJPanel.add(jSeparator2, gridBagConstraints);

        reportJCheckBox.setFont(new java.awt.Font("Dialog", 0, 12));
        reportJCheckBox.setText("<html><b>Report</b> any unexpected problems to Metavize.  An email will be sent to Metavize if an unexpected condition occurs.  No information about your network traffic will be transmitted.  (This is disabled by default)</html>");
        reportJCheckBox.setFocusPainted(false);
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
    private javax.swing.ButtonGroup dhcpButtonGroup;
    private javax.swing.ButtonGroup externalAdminButtonGroup;
    private javax.swing.ButtonGroup internalAdminButtonGroup;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPanel maintainRemoteJPanel;
    private javax.swing.JCheckBox reportJCheckBox;
    private javax.swing.ButtonGroup restrictAdminButtonGroup;
    private javax.swing.ButtonGroup sshButtonGroup;
    public javax.swing.JRadioButton sshDisabledRadioButton;
    public javax.swing.JRadioButton sshEnabledRadioButton;
    private javax.swing.ButtonGroup tcpWindowButtonGroup;
    // End of variables declaration//GEN-END:variables
    

}
