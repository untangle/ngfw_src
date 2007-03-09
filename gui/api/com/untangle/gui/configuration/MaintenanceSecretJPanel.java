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

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.mvvm.snmp.*;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.networking.MiscSettings;
import com.untangle.mvvm.tran.*;

import java.awt.*;
import javax.swing.*;

public class MaintenanceSecretJPanel extends javax.swing.JPanel
    implements Savable<MaintenanceCompoundSettings>, Refreshable<MaintenanceCompoundSettings> {

    
    public MaintenanceSecretJPanel() {
        initComponents();
		jScrollPane1.getVerticalScrollBar().setFocusable(false);
		jScrollPane1.getHorizontalScrollBar().setFocusable(false);
		jScrollPane2.getVerticalScrollBar().setFocusable(false);
		jScrollPane2.getHorizontalScrollBar().setFocusable(false);
		Util.addPanelFocus(this, script1JTextArea);
		Util.addFocusHighlight(script1JTextArea);
		Util.addFocusHighlight(script2JTextArea);
    }

    public void doSave(MaintenanceCompoundSettings maintenanceCompoundSettings, boolean validateOnly) throws Exception {

        // SCRIPT ///
        String script = script1JTextArea.getText();
        script1JTextArea.setBackground( Color.WHITE );
	
        // CUSTOM RULES ///
        String rules = script2JTextArea.getText();
        script2JTextArea.setBackground( Color.WHITE );

        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            MiscSettings miscSettings = maintenanceCompoundSettings.getMiscSettings();
            miscSettings.setPostConfigurationScript( script );
            miscSettings.setCustomRules( rules );
        }
    }

    public void doRefresh(MaintenanceCompoundSettings maintenanceCompoundSettings){
        MiscSettings miscSettings = maintenanceCompoundSettings.getMiscSettings();
	
        // SCRIPT //
        script1JTextArea.setBackground( Color.WHITE );
        String script1 = miscSettings.getPostConfigurationScript();
        script1JTextArea.setText( script1 );

        // CUSTOM RULES //
        script2JTextArea.setBackground( Color.WHITE );
        String rules = miscSettings.getCustomRules();
        script2JTextArea.setText( rules );
    }
    
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                snmpButtonGroup = new javax.swing.ButtonGroup();
                trapButtonGroup = new javax.swing.ButtonGroup();
                externalRemoteJPanel = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                script1JTextArea = new javax.swing.JTextArea();
                jScrollPane2 = new javax.swing.JScrollPane();
                script2JTextArea = new javax.swing.JTextArea();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(563, 343));
                setMinimumSize(new java.awt.Dimension(563, 343));
                setPreferredSize(new java.awt.Dimension(563, 343));
                externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Support Script", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                externalRemoteJPanel.setMinimumSize(new java.awt.Dimension(88, 323));
                externalRemoteJPanel.setPreferredSize(new java.awt.Dimension(730, 323));
                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("<html><b><font color=\"#ff0000\">Warning: Do not make any changes to the text below unless told to do so explicitly by Untangle Support.</b></font></html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
                externalRemoteJPanel.add(jLabel1, gridBagConstraints);

                jScrollPane1.setViewportView(script1JTextArea);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
                externalRemoteJPanel.add(jScrollPane1, gridBagConstraints);

                jScrollPane2.setViewportView(script2JTextArea);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 10);
                externalRemoteJPanel.add(jScrollPane2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
                add(externalRemoteJPanel, gridBagConstraints);

        }//GEN-END:initComponents
    

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel externalRemoteJPanel;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JTextArea script1JTextArea;
        private javax.swing.JTextArea script2JTextArea;
        private javax.swing.ButtonGroup snmpButtonGroup;
        private javax.swing.ButtonGroup trapButtonGroup;
        // End of variables declaration//GEN-END:variables
    

}
