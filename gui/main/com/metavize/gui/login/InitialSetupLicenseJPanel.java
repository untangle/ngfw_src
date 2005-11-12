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

import com.metavize.gui.transform.Savable;
import com.metavize.gui.util.Util;
import java.net.URL;

public class InitialSetupLicenseJPanel extends javax.swing.JPanel implements Savable {
    

    public InitialSetupLicenseJPanel() {
        initComponents();
        
        try{
	    URL licenseURL = Util.getClassLoader().getResource("EvalLicense.txt");
	    contentJEditorPane.setPage(licenseURL);
	}
	catch(Exception e){
	    Util.handleExceptionNoRestart("error loading license", e);
	}
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        if( !acceptJRadioButton.isSelected() ){
            throw new Exception("You must accept the license agreement before continuing.");
        }
    }
    

    private void initComponents() {//GEN-BEGIN:initComponents
        acceptButtonGroup = new javax.swing.ButtonGroup();
        contentJScrollPane = new javax.swing.JScrollPane();
        contentJEditorPane = new javax.swing.JEditorPane();
        acceptJRadioButton = new javax.swing.JRadioButton();
        declineJRadioButton = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        contentJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        contentJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        contentJEditorPane.setEditable(false);
        contentJScrollPane.setViewportView(contentJEditorPane);

        add(contentJScrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 60, 460, 210));

        acceptButtonGroup.add(acceptJRadioButton);
        acceptJRadioButton.setText("Accept License Agreement");
        acceptJRadioButton.setOpaque(false);
        add(acceptJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 290, -1, -1));

        acceptButtonGroup.add(declineJRadioButton);
        declineJRadioButton.setText("Decline License Agreement");
        declineJRadioButton.setOpaque(false);
        add(declineJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 310, -1, -1));

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please read and agree to the following license<br>\nagreement before proceeding.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup acceptButtonGroup;
    private javax.swing.JRadioButton acceptJRadioButton;
    private javax.swing.JEditorPane contentJEditorPane;
    private javax.swing.JScrollPane contentJScrollPane;
    private javax.swing.JRadioButton declineJRadioButton;
    private javax.swing.JLabel jLabel2;
    // End of variables declaration//GEN-END:variables
    
}
