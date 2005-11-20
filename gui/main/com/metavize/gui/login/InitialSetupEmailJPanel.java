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
import com.metavize.mvvm.*;
import com.metavize.gui.util.Util;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;
import javax.swing.SwingUtilities;
import javax.swing.SpinnerNumberModel;
import java.awt.Color;

public class InitialSetupEmailJPanel extends MWizardPageJPanel {
    
    private static final String EXCEPTION_PASSWORD_MISSING = "A \"Password\" must be specified if a \"Login\" is specified.";
    private static final String EXCEPTION_LOGIN_MISSING = "A \"Login\" must be specified if a \"Password\" is specified.";
    private static final String EXCEPTION_HOSTNAME_MISSING = "A \"Hostanme\" must be specified if \"Login\" or \"Password\" are specified.";
    private static final String EXCEPTION_ADDRESS_MISSING = "A \"From Address\" must be specified.";
    
    public InitialSetupEmailJPanel() {
        initComponents();
	portJSpinner.setModel(new SpinnerNumberModel(25,0,65535,1));
    }

    String host;
    int port;
    String login;
    String password;
    String address;
    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
	    hostJTextField.setBackground( Color.WHITE );
	    smtpLoginJTextField.setBackground( Color.WHITE );
	    smtpPasswordJPasswordField.setBackground( Color.WHITE );
	    addressJTextField.setBackground( Color.WHITE );

	    host = hostJTextField.getText().trim();
            port = (Integer) portJSpinner.getValue();
            login = smtpLoginJTextField.getText().trim();
            password = new String(smtpPasswordJPasswordField.getPassword()).trim();
            address = addressJTextField.getText().trim();

	    exception = null;

	    // CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED /////
	    if( (login.length() > 0) && (password.length() == 0) ){
		smtpPasswordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		exception = new Exception(EXCEPTION_PASSWORD_MISSING);
		return;
	    }
	    else if( (login.length() == 0) && (password.length() > 0) ){
		smtpLoginJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		exception = new Exception(EXCEPTION_LOGIN_MISSING);
		return;
	    }
	    
	    // CHECK THAT IF EITHER LOGIN OR PASSWORD ARE FILLED, A HOSTNAME IS GIVEN
	    if( (login.length() > 0) && (password.length() > 0) && (host.length() == 0) ){
		hostJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		exception = new Exception(EXCEPTION_HOSTNAME_MISSING);
		return;
	    }
	}});

	if( exception != null )
	    throw exception;

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
            if( host.length() > 0){
                MailSettings mailSettings = Util.getAdminManager().getMailSettings();

                mailSettings.setSmtpHost( host );
                mailSettings.setSmtpPort( port );
                mailSettings.setAuthUser( login );
                mailSettings.setAuthPass( password );
                if( address.length() > 0 )
                    mailSettings.setFromAddress( address );

                Util.getAdminManager().setMailSettings( mailSettings );
            }
        }

    }
    


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel2 = new javax.swing.JLabel();
        restrictIPJPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        hostJTextField = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        portJSpinner = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        smtpLoginJTextField = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        smtpPasswordJPasswordField = new javax.swing.JPasswordField();
        restrictIPJPanel2 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        addressJTextField = new javax.swing.JTextField();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please an email server (SMTP)<br> which EdgeGuard will use to send emails.<br><br>This information is not required.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        restrictIPJPanel1.setLayout(new java.awt.GridBagLayout());

        restrictIPJPanel1.setOpaque(false);
        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("Hostname:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel6, gridBagConstraints);

        hostJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        hostJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        hostJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel1.add(hostJTextField, gridBagConstraints);

        jLabel13.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel13.setText("Port:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel13, gridBagConstraints);

        portJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        portJSpinner.setFocusable(false);
        portJSpinner.setMaximumSize(new java.awt.Dimension(75, 19));
        portJSpinner.setMinimumSize(new java.awt.Dimension(75, 19));
        portJSpinner.setPreferredSize(new java.awt.Dimension(75, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel1.add(portJSpinner, gridBagConstraints);

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel9.setText("Authentication Login:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel9, gridBagConstraints);

        smtpLoginJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        smtpLoginJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        smtpLoginJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel1.add(smtpLoginJTextField, gridBagConstraints);

        jLabel14.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel14.setText("Authentication Password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel14, gridBagConstraints);

        smtpPasswordJPasswordField.setMaximumSize(new java.awt.Dimension(150, 19));
        smtpPasswordJPasswordField.setMinimumSize(new java.awt.Dimension(150, 19));
        smtpPasswordJPasswordField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel1.add(smtpPasswordJPasswordField, gridBagConstraints);

        add(restrictIPJPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 90, -1, -1));

        restrictIPJPanel2.setLayout(new java.awt.GridBagLayout());

        restrictIPJPanel2.setOpaque(false);
        jLabel7.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel7.setText("From Address:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel2.add(jLabel7, gridBagConstraints);

        addressJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        addressJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        addressJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        restrictIPJPanel2.add(addressJTextField, gridBagConstraints);

        add(restrictIPJPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(119, 200, -1, -1));

    }//GEN-END:initComponents

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField addressJTextField;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JTextField hostJTextField;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JSpinner portJSpinner;
    private javax.swing.JPanel restrictIPJPanel1;
    private javax.swing.JPanel restrictIPJPanel2;
    private javax.swing.JTextField smtpLoginJTextField;
    private javax.swing.JPasswordField smtpPasswordJPasswordField;
    // End of variables declaration//GEN-END:variables
    
}
