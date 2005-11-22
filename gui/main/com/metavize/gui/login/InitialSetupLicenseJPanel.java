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
import com.metavize.gui.util.Util;
import java.net.URL;

import java.awt.event.*;
import javax.swing.*;

public class InitialSetupLicenseJPanel extends MWizardPageJPanel implements AdjustmentListener {

    private static final String EXCEPTION_NOT_ACCEPT = "You must accept the license agreement before continuing.";
    
    private JScrollBar verticalScrollBar;
    private boolean readAgreement = false;
    
    public InitialSetupLicenseJPanel() {
        initComponents();
        
        verticalScrollBar = contentJScrollPane.getVerticalScrollBar();
        verticalScrollBar.addAdjustmentListener(this);
        
        try{
	    URL licenseURL = Util.getClassLoader().getResource("EvalLicense.txt");
	    contentJEditorPane.setPage(licenseURL);
	}
	catch(Exception e){
	    Util.handleExceptionNoRestart("error loading license", e);
	}
    }
    
    public void adjustmentValueChanged(AdjustmentEvent evt){
        if( (evt.getValue() == (verticalScrollBar.getMaximum()-verticalScrollBar.getModel().getExtent())) && !readAgreement ){
            acceptJRadioButton.setEnabled(true);
            declineJRadioButton.setEnabled(true);
            readAgreement = true;
        }
    }

    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){

	    exception = null;

	    if( !acceptJRadioButton.isSelected() ){
		exception = new Exception(EXCEPTION_NOT_ACCEPT);
	    }
	}});

	if( exception != null )
	    throw exception;
    }
    

    private void initComponents() {//GEN-BEGIN:initComponents
        acceptButtonGroup = new javax.swing.ButtonGroup();
        contentJScrollPane = new javax.swing.JScrollPane();
        contentJEditorPane = new javax.swing.JEditorPane();
        acceptJRadioButton = new javax.swing.JRadioButton();
        declineJRadioButton = new javax.swing.JRadioButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        contentJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        contentJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        contentJEditorPane.setEditable(false);
        contentJScrollPane.setViewportView(contentJEditorPane);

        add(contentJScrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(7, 80, 460, 210));

        acceptButtonGroup.add(acceptJRadioButton);
        acceptJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        acceptJRadioButton.setText("<html><b>Accept</b> License Agreement</html>");
        acceptJRadioButton.setEnabled(false);
        acceptJRadioButton.setFocusPainted(false);
        acceptJRadioButton.setFocusable(false);
        acceptJRadioButton.setMinimumSize(new java.awt.Dimension(185, 23));
        acceptJRadioButton.setPreferredSize(new java.awt.Dimension(185, 23));
        add(acceptJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 320, -1, -1));

        acceptButtonGroup.add(declineJRadioButton);
        declineJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        declineJRadioButton.setText("<html><b>Decline</b> License Agreement</html>");
        declineJRadioButton.setEnabled(false);
        declineJRadioButton.setFocusPainted(false);
        declineJRadioButton.setFocusable(false);
        declineJRadioButton.setMinimumSize(new java.awt.Dimension(185, 23));
        declineJRadioButton.setPreferredSize(new java.awt.Dimension(185, 23));
        add(declineJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 300, -1, -1));

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please read and agree to the following license agreement before<br>proceeding.  You must scroll to the bottom before you can agree.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, -1, -1));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/login/ProductShot.png")));
        jLabel3.setEnabled(false);
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup acceptButtonGroup;
    private javax.swing.JRadioButton acceptJRadioButton;
    private javax.swing.JEditorPane contentJEditorPane;
    private javax.swing.JScrollPane contentJScrollPane;
    private javax.swing.JRadioButton declineJRadioButton;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables
    
}
