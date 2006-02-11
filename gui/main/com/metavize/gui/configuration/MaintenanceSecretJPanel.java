/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.configuration;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.mvvm.snmp.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;
import javax.swing.*;

public class MaintenanceSecretJPanel extends javax.swing.JPanel
    implements Savable<MaintenanceCompoundSettings>, Refreshable<MaintenanceCompoundSettings> {

    
    public MaintenanceSecretJPanel() {
        initComponents();
    }

    public void doSave(MaintenanceCompoundSettings maintenanceCompoundSettings, boolean validateOnly) throws Exception {

	// SCRIPT ///
	String script = scriptJTextArea.getText();
	scriptJTextArea.setBackground( Color.WHITE );
	

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    NetworkingConfiguration networkingConfiguration = maintenanceCompoundSettings.getNetworkingConfiguration();
            networkingConfiguration.setPostConfigurationScript( script );
        }

    }

    public void doRefresh(MaintenanceCompoundSettings maintenanceCompoundSettings){
	NetworkingConfiguration networkingConfiguration = maintenanceCompoundSettings.getNetworkingConfiguration();
	
	// SCRIPT //
	String script = networkingConfiguration.getPostConfigurationScript();
	scriptJTextArea.setText( script );	
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                snmpButtonGroup = new javax.swing.ButtonGroup();
                trapButtonGroup = new javax.swing.ButtonGroup();
                externalRemoteJPanel = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                scriptJTextArea = new javax.swing.JTextArea();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(563, 343));
                setMinimumSize(new java.awt.Dimension(563, 343));
                setPreferredSize(new java.awt.Dimension(563, 343));
                externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Support Script", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                externalRemoteJPanel.setMinimumSize(new java.awt.Dimension(88, 323));
                externalRemoteJPanel.setPreferredSize(new java.awt.Dimension(730, 323));
                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("<html><b><font color=\"#ff0000\">Warning: Do not make any changes to the text below unless told to do so explicitly by Metavize Technical Support.</b></font></html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
                externalRemoteJPanel.add(jLabel1, gridBagConstraints);

                jScrollPane1.setViewportView(scriptJTextArea);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
                externalRemoteJPanel.add(jScrollPane1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
                add(externalRemoteJPanel, gridBagConstraints);

        }//GEN-END:initComponents
    

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel externalRemoteJPanel;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JTextArea scriptJTextArea;
        private javax.swing.ButtonGroup snmpButtonGroup;
        private javax.swing.ButtonGroup trapButtonGroup;
        // End of variables declaration//GEN-END:variables
    

}
