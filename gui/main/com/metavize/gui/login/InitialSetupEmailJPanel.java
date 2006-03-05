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
import com.metavize.gui.configuration.EmailConnectivityTestJDialog;

import java.util.Arrays;
import javax.swing.SwingUtilities;
import javax.swing.SpinnerNumberModel;
import java.awt.Color;
import java.awt.Dialog;

public class InitialSetupEmailJPanel extends MWizardPageJPanel {
    
    private static final String EXCEPTION_PASSWORD_MISSING = "A \"Password\" must be specified if a \"Login\" is specified.";
    private static final String EXCEPTION_LOGIN_MISSING = "A \"Login\" must be specified if a \"Password\" is specified.";
    private static final String EXCEPTION_HOSTNAME_MISSING = "A \"Hostname\" must be specified if \"Login\" or \"Password\" are specified.";
    //private static final String EXCEPTION_ADDRESS_MISSING = "A \"From Address\" must be specified.";
    
    public InitialSetupEmailJPanel() {
        initComponents();
	Util.setPortView(portJSpinner, 25);
    }
    
    public void initialFocus(){			
	hostJTextField.requestFocus();
        String hostname = (String) InitialSetupWizard.getSharedData();
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
	    try{
		if( host.length() > 0){
		    InitialSetupWizard.getInfiniteProgressJComponent().startLater("Saving Email Settings...");
		    MailSettings mailSettings = Util.getAdminManager().getMailSettings();
		    mailSettings.setSmtpHost( host );
		    mailSettings.setSmtpPort( port );
		    mailSettings.setAuthUser( login );
		    mailSettings.setAuthPass( password );
		    if( address.length() > 0 )
			mailSettings.setFromAddress( address );
		    Util.getAdminManager().setMailSettings( mailSettings );
		    InitialSetupWizard.getInfiniteProgressJComponent().stopLater(1500l);
		}
            }
	    catch(Exception e){
		InitialSetupWizard.getInfiniteProgressJComponent().stopLater(-1l);
		Util.handleExceptionNoRestart("Error sending data", e);
		throw new Exception("A network communication error occurred.  Please retry.");
	    }
        }

    }
    


        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                buttonGroup1 = new javax.swing.ButtonGroup();
                contentJPanel = new javax.swing.JPanel();
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
                jLabel1 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();
                jSeparator1 = new javax.swing.JSeparator();
                jLabel3 = new javax.swing.JLabel();
                jPanel2 = new javax.swing.JPanel();
                jLabel12 = new javax.swing.JLabel();
                addressJTextField = new javax.swing.JTextField();
                jSeparator2 = new javax.swing.JSeparator();
                jPanel3 = new javax.swing.JPanel();
                connectivityTestJButton = new javax.swing.JButton();
                jLabel11 = new javax.swing.JLabel();
                backgroundJPabel = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                setOpaque(false);
                contentJPanel.setLayout(new java.awt.GridBagLayout());

                contentJPanel.setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>Which email server (SMTP) should EdgeGuard use to send emails, reports, etc?<br><b>This information is optional.</b></html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jLabel2, gridBagConstraints);

                jPanel1.setLayout(new java.awt.GridBagLayout());

                jPanel1.setOpaque(false);
                jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel8.setText("SMTP Email Server:");
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

                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("(optional)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.gridwidth = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 0);
                jPanel1.add(jLabel1, gridBagConstraints);

                jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel5.setText("(optional)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.gridwidth = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
                jPanel1.add(jLabel5, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jPanel1, gridBagConstraints);

                jSeparator1.setForeground(new java.awt.Color(156, 156, 156));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jSeparator1, gridBagConstraints);

                jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel3.setText("<html>Please choose the \"From Address\" of emails sent from EdgeGuard.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jLabel3, gridBagConstraints);

                jPanel2.setLayout(new java.awt.GridBagLayout());

                jPanel2.setMinimumSize(new java.awt.Dimension(350, 19));
                jPanel2.setOpaque(false);
                jPanel2.setPreferredSize(new java.awt.Dimension(350, 19));
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

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jPanel2, gridBagConstraints);

                jSeparator2.setForeground(new java.awt.Color(156, 156, 156));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jSeparator2, gridBagConstraints);

                jPanel3.setLayout(new java.awt.GridBagLayout());

                jPanel3.setOpaque(false);
                connectivityTestJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                connectivityTestJButton.setText("Run Connectivity Test");
                connectivityTestJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                connectivityTestJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 6;
                gridBagConstraints.weighty = 1.0;
                jPanel3.add(connectivityTestJButton, gridBagConstraints);

                jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel11.setText("<html>The <b>Connectivity Test is optional</b>, but it can tell you if your mail settings  above are correct, by sending you an email.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 6;
                gridBagConstraints.gridwidth = 8;
                gridBagConstraints.gridheight = 2;
                gridBagConstraints.ipadx = 172;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 0);
                jPanel3.add(jLabel11, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jPanel3, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                add(contentJPanel, gridBagConstraints);

                backgroundJPabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/login/ProductShot.png")));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weightx = 1.0;
                add(backgroundJPabel, gridBagConstraints);

        }//GEN-END:initComponents

		private void connectivityTestJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectivityTestJButtonActionPerformed
				new ConnectivityThread();
		}//GEN-LAST:event_connectivityTestJButtonActionPerformed

		private class ConnectivityThread extends Thread{
				public ConnectivityThread(){
				    super("MVCLIENT-EmailConnectivityThread");
						setDaemon(true);
						connectivityTestJButton.setEnabled(false);
						start();
				}
				public void run(){
				try{
						doSave(null, false);
						EmailConnectivityTestJDialog connectivityJDialog = new EmailConnectivityTestJDialog((Dialog)InitialSetupEmailJPanel.this.getTopLevelAncestor());
						connectivityJDialog.setVisible(true);
				}
				catch(Exception e){
						Util.handleExceptionNoRestart("Error testing connectivity", e);
				}
				finally{
						SwingUtilities.invokeLater( new Runnable(){ public void run(){
						InitialSetupEmailJPanel.this.connectivityTestJButton.setEnabled(true);
						}});
				}
				}
		}
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        public javax.swing.JTextField addressJTextField;
        private javax.swing.JLabel backgroundJPabel;
        private javax.swing.ButtonGroup buttonGroup1;
        private javax.swing.JButton connectivityTestJButton;
        private javax.swing.JPanel contentJPanel;
        public javax.swing.JTextField hostJTextField;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel15;
        private javax.swing.JLabel jLabel16;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JPanel jPanel3;
        private javax.swing.JSeparator jSeparator1;
        private javax.swing.JSeparator jSeparator2;
        private javax.swing.JSpinner portJSpinner;
        public javax.swing.JTextField smtpLoginJTextField;
        private javax.swing.JPasswordField smtpPasswordJPasswordField;
        // End of variables declaration//GEN-END:variables
    
}
