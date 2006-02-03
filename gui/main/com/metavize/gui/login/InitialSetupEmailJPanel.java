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

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.*;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;


import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.SpinnerNumberModel;
import java.awt.Color;

public class InitialSetupEmailJPanel extends MWizardPageJPanel {
    
    private static final String EXCEPTION_PASSWORD_MISSING = "A \"Password\" must be specified if a \"Login\" is specified.";
    private static final String EXCEPTION_LOGIN_MISSING = "A \"Login\" must be specified if a \"Password\" is specified.";
    private static final String EXCEPTION_HOSTNAME_MISSING = "A \"Hostname\" must be specified if \"Login\" or \"Password\" are specified.";
    //private static final String EXCEPTION_ADDRESS_MISSING = "A \"From Address\" must be specified.";
    
    public InitialSetupEmailJPanel() {
        initComponents();
		portJSpinner.setModel(new SpinnerNumberModel(25,0,65535,1));
    }
    
    public void initialFocus(){
			
	hostJTextField.requestFocus();
	NetworkingConfiguration networkingConfiguration = Util.getNetworkingManager().get();
        String hostname = networkingConfiguration.hostname();
		addressJTextField.setText("edgeguard@" + hostname); // XXX this should be directly linked from the actual default value
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
                jPanel1 = new javax.swing.JPanel();
                jLabel8 = new javax.swing.JLabel();
                hostJTextField = new javax.swing.JTextField();
                jLabel15 = new javax.swing.JLabel();
                portJSpinner = new javax.swing.JSpinner();
                jLabel10 = new javax.swing.JLabel();
                smtpLoginJTextField = new javax.swing.JTextField();
                jLabel16 = new javax.swing.JLabel();
                smtpPasswordJPasswordField = new javax.swing.JPasswordField();
                jPanel2 = new javax.swing.JPanel();
                jLabel12 = new javax.swing.JLabel();
                addressJTextField = new javax.swing.JTextField();
                jLabel3 = new javax.swing.JLabel();
                jLabel4 = new javax.swing.JLabel();
                jLabel1 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();

                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>Please choose an email server (SMTP) which EdgeGuard will use to send emails, reports, etc.  <b>This information is optional.</b></html>");
                add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 400, -1));

                jPanel1.setLayout(new java.awt.GridBagLayout());

                jPanel1.setOpaque(false);
                jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel8.setText("Hostname:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel1.add(jLabel8, gridBagConstraints);

                hostJTextField.setMaximumSize(new java.awt.Dimension(170, 19));
                hostJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
                hostJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                jPanel1.add(hostJTextField, gridBagConstraints);

                jLabel15.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel15.setText("Port:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel1.add(jLabel15, gridBagConstraints);

                portJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
                portJSpinner.setMaximumSize(new java.awt.Dimension(170, 19));
                portJSpinner.setMinimumSize(new java.awt.Dimension(170, 19));
                portJSpinner.setPreferredSize(new java.awt.Dimension(170, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                jPanel1.add(portJSpinner, gridBagConstraints);

                jLabel10.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel10.setText("Authentication Login:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
                jPanel1.add(jLabel10, gridBagConstraints);

                smtpLoginJTextField.setMaximumSize(new java.awt.Dimension(170, 19));
                smtpLoginJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
                smtpLoginJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 2, 0);
                jPanel1.add(smtpLoginJTextField, gridBagConstraints);

                jLabel16.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel16.setText("Authentication Password:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel1.add(jLabel16, gridBagConstraints);

                smtpPasswordJPasswordField.setMaximumSize(new java.awt.Dimension(170, 19));
                smtpPasswordJPasswordField.setMinimumSize(new java.awt.Dimension(170, 19));
                smtpPasswordJPasswordField.setPreferredSize(new java.awt.Dimension(170, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                jPanel1.add(smtpPasswordJPasswordField, gridBagConstraints);

                add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 60, 340, 110));

                jPanel2.setLayout(new java.awt.GridBagLayout());

                jPanel2.setOpaque(false);
                jLabel12.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel12.setText("From Address:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel2.add(jLabel12, gridBagConstraints);

                addressJTextField.setMaximumSize(new java.awt.Dimension(170, 19));
                addressJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
                addressJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                jPanel2.add(addressJTextField, gridBagConstraints);

                add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(58, 200, 375, 30));

                jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel3.setText("<html>Please choose the \"From Address\" of emails sent from EdgeGuard.</html>");
                add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 180, -1, -1));

                jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/login/ProductShot.png")));
                add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("(optional)");
                add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 121, -1, -1));

                jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel5.setText("(optional)");
                add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 143, -1, -1));

        }//GEN-END:initComponents

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        public javax.swing.JTextField addressJTextField;
        private javax.swing.ButtonGroup buttonGroup1;
        public javax.swing.JTextField hostJTextField;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel15;
        private javax.swing.JLabel jLabel16;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JSpinner portJSpinner;
        public javax.swing.JTextField smtpLoginJTextField;
        private javax.swing.JPasswordField smtpPasswordJPasswordField;
        // End of variables declaration//GEN-END:variables
    
}
