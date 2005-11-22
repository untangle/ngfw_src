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

package com.metavize.gui.login;

import com.metavize.gui.widgets.wizard.*;
import java.util.Arrays;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.gui.util.Util;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.gui.configuration.NetworkConnectivityTestJDialog;

import java.awt.Dialog;

public class InitialSetupConnectivityJPanel extends MWizardPageJPanel {
   

    public InitialSetupConnectivityJPanel() {
        initComponents();
    }

   
    private void initComponents() {//GEN-BEGIN:initComponents
        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel10 = new javax.swing.JLabel();
        connectivityTestJButton = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel10.setText("<html>The <b>Connectivity Test</b> is not required, but it can tell you if<br>EdgeGuard can contact DNS and the internet with the settings<br>you have just saved.</html>");
        add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, -1, -1));

        connectivityTestJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        connectivityTestJButton.setText("Run Connectivity Test");
        connectivityTestJButton.setFocusPainted(false);
        connectivityTestJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectivityTestJButtonActionPerformed(evt);
            }
        });

        add(connectivityTestJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 110, -1, -1));

        jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel11.setText("<html>If the connectivity test does not pass, you should try different<br>network settings.  You may go back to the \"Network Settings\"<br> page by pressing the \"Previous page\" button.</html>");
        add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 160, -1, -1));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/login/ProductShot.png")));
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

    }//GEN-END:initComponents

    private void connectivityTestJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectivityTestJButtonActionPerformed
        try{
	    NetworkConnectivityTestJDialog connectivityJDialog = new NetworkConnectivityTestJDialog((Dialog)getTopLevelAncestor(), true);
	    connectivityJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error showing connectivity tester", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error showing connectivity tester", f); }
	}
    }//GEN-LAST:event_connectivityTestJButtonActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton connectivityTestJButton;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables
    
}
