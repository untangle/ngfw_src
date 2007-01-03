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
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.util.Util;
import java.net.URL;

import java.io.*;

import java.awt.Window;
import java.awt.event.*;
import javax.swing.*;

public class InitialSetupLicenseJPanel extends MWizardPageJPanel implements AdjustmentListener {

    private static final String EXCEPTION_NOT_ACCEPT = "You must accept the license agreement before proceeding.";
    
    private JScrollBar verticalScrollBar;
    private boolean readAgreement = false;
    
    public InitialSetupLicenseJPanel() {
        initComponents();
        
        verticalScrollBar = contentJScrollPane.getVerticalScrollBar();
        verticalScrollBar.addAdjustmentListener(this);
        
        try{
            InputStream licenseInputStream = Util.getClassLoader().getResourceAsStream("License.txt");
            InputStreamReader licenseInputStreamReader = new InputStreamReader(licenseInputStream);
            BufferedReader licenseBufferedReader = new BufferedReader(licenseInputStreamReader);
            StringBuilder licenseStringBuilder = new StringBuilder();
            String licenseLine;
            while( true ){
                licenseLine=licenseBufferedReader.readLine();
                if(licenseLine==null)
                    break;
                else
                    licenseStringBuilder.append(licenseLine).append("\n");
            }

            contentJEditorPane.setContentType("text/plain");
            contentJEditorPane.setText(licenseStringBuilder.toString());
            contentJEditorPane.setFont(new java.awt.Font("Courier", 0, 11));
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
    Window topLevelWindow;

    public void doSave(Object settings, boolean validateOnly) throws Exception {
	topLevelWindow = (Window) this.getTopLevelAncestor();

        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){

	    exception = null;

	    if( !acceptJRadioButton.isSelected() ){
		if( declineJRadioButton.isSelected() ){
		    MOneButtonJDialog.factory(topLevelWindow,"","<html>You have declined the Untangle License Agreement.  The setup wizard will now exit.  You may run the setup wizard again later.</html>","Setup Wizard Warning","Warning");
		    System.exit(0);
		    return;		
		}
		else{
		    exception = new Exception(EXCEPTION_NOT_ACCEPT);
		    return;
		}
	    }
	}});

	if( exception != null )
	    throw exception;
    }
    

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                acceptButtonGroup = new javax.swing.ButtonGroup();
                contentJPanel = new javax.swing.JPanel();
                jLabel2 = new javax.swing.JLabel();
                contentJScrollPane = new javax.swing.JScrollPane();
                jPanel1 = new javax.swing.JPanel();
                contentJEditorPane = new javax.swing.JEditorPane();
                actionJPanel = new javax.swing.JPanel();
                acceptJRadioButton = new javax.swing.JRadioButton();
                declineJRadioButton = new javax.swing.JRadioButton();
                backgroundJPabel = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setOpaque(false);
                contentJPanel.setLayout(new java.awt.GridBagLayout());

                contentJPanel.setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>Please read and accept the following license agreement before proceeding.<br>  <b><font color=\"#FF0000\">You must scroll to the bottom before you can accept.</font></b></html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jLabel2, gridBagConstraints);

                contentJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                contentJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                contentJScrollPane.setFocusable(false);
                jPanel1.setLayout(new java.awt.BorderLayout());

                contentJEditorPane.setEditable(false);
                contentJEditorPane.setFocusable(false);
                jPanel1.add(contentJEditorPane, java.awt.BorderLayout.CENTER);

                actionJPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                acceptButtonGroup.add(acceptJRadioButton);
                acceptJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                acceptJRadioButton.setText("<html><b>Accept</b> License Agreement</html>");
                acceptJRadioButton.setEnabled(false);
                acceptJRadioButton.setMinimumSize(new java.awt.Dimension(185, 23));
                acceptJRadioButton.setPreferredSize(new java.awt.Dimension(185, 23));
                actionJPanel.add(acceptJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 30, -1, -1));

                acceptButtonGroup.add(declineJRadioButton);
                declineJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                declineJRadioButton.setText("<html><b>Decline</b> License Agreement</html>");
                declineJRadioButton.setEnabled(false);
                declineJRadioButton.setMinimumSize(new java.awt.Dimension(185, 23));
                declineJRadioButton.setPreferredSize(new java.awt.Dimension(185, 23));
                actionJPanel.add(declineJRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 10, -1, -1));

                jPanel1.add(actionJPanel, java.awt.BorderLayout.SOUTH);

                contentJScrollPane.setViewportView(jPanel1);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
                contentJPanel.add(contentJScrollPane, gridBagConstraints);

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
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.ButtonGroup acceptButtonGroup;
        private javax.swing.JRadioButton acceptJRadioButton;
        private javax.swing.JPanel actionJPanel;
        private javax.swing.JLabel backgroundJPabel;
        private javax.swing.JEditorPane contentJEditorPane;
        private javax.swing.JPanel contentJPanel;
        private javax.swing.JScrollPane contentJScrollPane;
        private javax.swing.JRadioButton declineJRadioButton;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JPanel jPanel1;
        // End of variables declaration//GEN-END:variables
    
}
