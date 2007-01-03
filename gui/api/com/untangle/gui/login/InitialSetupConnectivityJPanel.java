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

package com.untangle.gui.login;

import com.untangle.gui.widgets.wizard.*;
import java.util.Arrays;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.gui.util.Util;
import com.untangle.mvvm.NetworkingConfiguration;
import com.untangle.gui.configuration.NetworkConnectivityTestJDialog;

import java.awt.Dialog;

public class InitialSetupConnectivityJPanel extends MWizardPageJPanel {
   

    public InitialSetupConnectivityJPanel() {
        initComponents();
    }

	public void initialFocus(){
		connectivityTestJButton.requestFocus();
	}
   
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                buttonGroup1 = new javax.swing.ButtonGroup();
                contentJPanel = new javax.swing.JPanel();
                jLabel10 = new javax.swing.JLabel();
                connectivityTestJButton = new javax.swing.JButton();
                jLabel11 = new javax.swing.JLabel();
                backgroundJPabel = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setOpaque(false);
                contentJPanel.setLayout(new java.awt.GridBagLayout());

                contentJPanel.setOpaque(false);
                jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel10.setText("<html>The <b>Connectivity Test is optional</b>, but it can tell you if the Untangle Server can contact DNS and the internet with the settings you have just saved.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jLabel10, gridBagConstraints);

                connectivityTestJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                connectivityTestJButton.setText("Run Connectivity Test");
                connectivityTestJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                connectivityTestJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
                contentJPanel.add(connectivityTestJButton, gridBagConstraints);

                jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel11.setText("<html>If the connectivity test does not pass, you should try different external address settings.  You may go back to the \"External Address\" page by pressing the \"Previous page\" button.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(30, 15, 0, 15);
                contentJPanel.add(jLabel11, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                add(contentJPanel, gridBagConstraints);

                backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/login/ProductShot.png")));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weightx = 1.0;
                add(backgroundJPabel, gridBagConstraints);

        }//GEN-END:initComponents

    private void connectivityTestJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectivityTestJButtonActionPerformed
        try{
	    NetworkConnectivityTestJDialog connectivityJDialog = new NetworkConnectivityTestJDialog((Dialog)getTopLevelAncestor());
	    connectivityJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error showing connectivity tester", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error showing connectivity tester", f); }
	}
    }//GEN-LAST:event_connectivityTestJButtonActionPerformed

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel backgroundJPabel;
        private javax.swing.ButtonGroup buttonGroup1;
        private javax.swing.JButton connectivityTestJButton;
        private javax.swing.JPanel contentJPanel;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel11;
        // End of variables declaration//GEN-END:variables
    
}
