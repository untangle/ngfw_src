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
import javax.swing.SwingUtilities;

public class InitialSetupSaveJPanel extends MWizardPageJPanel  {
    
    public InitialSetupSaveJPanel() {
        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {
    }
    
    public void saveStarted(){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
            statusJProgressBar.setValue(0);
            statusJProgressBar.setString("Saving...");
	    statusJProgressBar.setIndeterminate(true);
	}});
    }
    
    public void saveFinished(final String message){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
            statusJProgressBar.setValue(100);
            statusJProgressBar.setString(message);
	    statusJProgressBar.setIndeterminate(false);
	}});
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        statusJProgressBar = new javax.swing.JProgressBar();
        jLabel3 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("<html>Press \"Next page\" to save your<br>EdgeGuard configuration settings.</html>");
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, -1, -1));

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 180, -1, -1));

        statusJProgressBar.setFont(new java.awt.Font("Default", 0, 12));
        statusJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
        statusJProgressBar.setDoubleBuffered(true);
        statusJProgressBar.setMaximumSize(new java.awt.Dimension(32767, 16));
        statusJProgressBar.setMinimumSize(new java.awt.Dimension(10, 16));
        statusJProgressBar.setPreferredSize(new java.awt.Dimension(150, 16));
        statusJProgressBar.setString("");
        statusJProgressBar.setStringPainted(true);
        add(statusJProgressBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 130, 310, -1));

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("<html>Saving settings may take up to a minute to complete.</html>");
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 190, -1, -1));

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JProgressBar statusJProgressBar;
    // End of variables declaration//GEN-END:variables
    
}
