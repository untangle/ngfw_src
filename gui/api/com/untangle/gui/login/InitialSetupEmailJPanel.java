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
import com.untangle.gui.util.Util;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.*;
import com.untangle.mvvm.MailSender;
import com.untangle.mvvm.client.MvvmRemoteContextFactory;
import com.untangle.gui.configuration.EmailConnectivityTestJDialog;

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

    private volatile boolean shouldSaveNetworkSettings = true;
    
    public InitialSetupEmailJPanel() {
        initComponents();
	Util.setPortView(portJSpinner, 25);
	smtpDisabledJRadioButton.setSelected(true);
	setMxRecordsEnabledDependency(true);
    }
    
    public void initialFocus(){			
	smtpDisabledJRadioButton.requestFocus();
        String hostname = (String) InitialSetupWizard.getSharedData();
	addressJTextField.setText( MailSender.DEFAULT_SENDER + "@" + hostname); // XXX this should be directly linked from the actual default value
    }
    
    boolean mxRecords;
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

	    mxRecords = smtpDisabledJRadioButton.isSelected();
	    host = hostJTextField.getText().trim();
            port = (Integer) portJSpinner.getValue();
            login = smtpLoginJTextField.getText().trim();
            password = new String(smtpPasswordJPasswordField.getPassword()).trim();
            address = addressJTextField.getText().trim();

	    exception = null;

	    if( !mxRecords ){
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
	    }
	}});
	
	if( exception != null )
	    throw exception;

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    try{		
		InitialSetupWizard.getInfiniteProgressJComponent().startLater("Saving Email Settings...");
		MailSettings mailSettings = Util.getAdminManager().getMailSettings();
		mailSettings.setUseMxRecords( mxRecords );		
		if( !mxRecords ){
		    mailSettings.setSmtpHost( host );
		    mailSettings.setSmtpPort( port );
		    mailSettings.setAuthUser( login );
		    mailSettings.setAuthPass( password );
		    if( address.length() > 0 )
			mailSettings.setFromAddress( address );
		}
		Util.getAdminManager().setMailSettings( mailSettings );
		InitialSetupWizard.getInfiniteProgressJComponent().stopLater(1500l);		
            }
	    catch(Exception e){
		InitialSetupWizard.getInfiniteProgressJComponent().stopLater(-1l);
		Util.handleExceptionNoRestart("Error sending data", e);
		throw new Exception("A network communication error occurred.  Please retry.");
	    }

	    if(shouldSaveNetworkSettings){
		try{
		    InitialSetupWizard.getInfiniteProgressJComponent().startLater("Saving Final Configuration...");
		    
		    // UPDATE NAT CONFIG
		    try{
			if( InitialSetupRoutingJPanel.getNatEnabled() ){
			    if( InitialSetupRoutingJPanel.getNatChanged() )
				MvvmRemoteContextFactory.factory().setTimeout(Util.DISCONNECT_NETWORK_TIMEOUT_MILLIS);
			    Util.getNetworkManager().setWizardNatEnabled(InitialSetupRoutingJPanel.getAddress(),
									 InitialSetupRoutingJPanel.getNetmask());
			}
			else{
			    MvvmRemoteContextFactory.factory().setTimeout(Util.DISCONNECT_NETWORK_TIMEOUT_MILLIS);
			    Util.getNetworkManager().setWizardNatDisabled();
			}
		    }
		    catch(Exception f){
			Util.handleExceptionNoRestart("Normal termination:",f);
		    }		    
		    InitialSetupWizard.getInfiniteProgressJComponent().stopLater(1500l);
		}
		catch(Exception e){
		    InitialSetupWizard.getInfiniteProgressJComponent().stopLater(-1l);
		    Util.handleExceptionNoRestart("Error sending data", e);
		    throw new Exception("A network communication error occurred.  Please retry.");
		}
	    }
        }

    }
    


        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                smtpButtonGroup = new javax.swing.ButtonGroup();
                contentJPanel = new javax.swing.JPanel();
                jLabel2 = new javax.swing.JLabel();
                smtpDisabledJRadioButton = new javax.swing.JRadioButton();
                smtpEnabledJRadioButton = new javax.swing.JRadioButton();
                jPanel1 = new javax.swing.JPanel();
                hostJLabel = new javax.swing.JLabel();
                hostJTextField = new javax.swing.JTextField();
                portJLabel = new javax.swing.JLabel();
                portJSpinner = new javax.swing.JSpinner();
                smtpLoginJLabel = new javax.swing.JLabel();
                smtpLoginJTextField = new javax.swing.JTextField();
                smtpPasswordJLabel = new javax.swing.JLabel();
                smtpPasswordJPasswordField = new javax.swing.JPasswordField();
                optionalJLabel1 = new javax.swing.JLabel();
                optionalJLabel2 = new javax.swing.JLabel();
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
                jLabel2.setText("<html>Which email server (SMTP) should the Untangle Server use to send emails, reports, etc.?<br><b>This information is optional.</b></html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                contentJPanel.add(jLabel2, gridBagConstraints);

                smtpButtonGroup.add(smtpDisabledJRadioButton);
                smtpDisabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                smtpDisabledJRadioButton.setText("<html>Send Email Directly (default)</html>");
                smtpDisabledJRadioButton.setActionCommand("<html>Send Email Directly</html>");
                smtpDisabledJRadioButton.setFocusPainted(false);
                smtpDisabledJRadioButton.setFocusable(false);
                smtpDisabledJRadioButton.setOpaque(false);
                smtpDisabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                smtpDisabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 50, 0, 0);
                contentJPanel.add(smtpDisabledJRadioButton, gridBagConstraints);

                smtpButtonGroup.add(smtpEnabledJRadioButton);
                smtpEnabledJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
                smtpEnabledJRadioButton.setText("<html>Send Email using the specified SMTP Server</html>");
                smtpEnabledJRadioButton.setFocusPainted(false);
                smtpEnabledJRadioButton.setFocusable(false);
                smtpEnabledJRadioButton.setOpaque(false);
                smtpEnabledJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                smtpEnabledJRadioButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 0, 0);
                contentJPanel.add(smtpEnabledJRadioButton, gridBagConstraints);

                jPanel1.setLayout(new java.awt.GridBagLayout());

                jPanel1.setOpaque(false);
                hostJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                hostJLabel.setText("SMTP Email Server:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel1.add(hostJLabel, gridBagConstraints);

                hostJTextField.setMaximumSize(new java.awt.Dimension(170, 19));
                hostJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
                hostJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                jPanel1.add(hostJTextField, gridBagConstraints);

                portJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                portJLabel.setText("Port:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel1.add(portJLabel, gridBagConstraints);

                portJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
                portJSpinner.setMaximumSize(new java.awt.Dimension(170, 19));
                portJSpinner.setMinimumSize(new java.awt.Dimension(170, 19));
                portJSpinner.setPreferredSize(new java.awt.Dimension(170, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                jPanel1.add(portJSpinner, gridBagConstraints);

                smtpLoginJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                smtpLoginJLabel.setText("Authentication Login:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
                jPanel1.add(smtpLoginJLabel, gridBagConstraints);

                smtpLoginJTextField.setMaximumSize(new java.awt.Dimension(170, 19));
                smtpLoginJTextField.setMinimumSize(new java.awt.Dimension(170, 19));
                smtpLoginJTextField.setPreferredSize(new java.awt.Dimension(170, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(10, 0, 2, 0);
                jPanel1.add(smtpLoginJTextField, gridBagConstraints);

                smtpPasswordJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                smtpPasswordJLabel.setText("Authentication Password:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                jPanel1.add(smtpPasswordJLabel, gridBagConstraints);

                smtpPasswordJPasswordField.setMaximumSize(new java.awt.Dimension(170, 19));
                smtpPasswordJPasswordField.setMinimumSize(new java.awt.Dimension(170, 19));
                smtpPasswordJPasswordField.setPreferredSize(new java.awt.Dimension(170, 19));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                jPanel1.add(smtpPasswordJPasswordField, gridBagConstraints);

                optionalJLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                optionalJLabel1.setText("(optional)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.gridwidth = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 0);
                jPanel1.add(optionalJLabel1, gridBagConstraints);

                optionalJLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                optionalJLabel2.setText("(optional)");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 2;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.gridwidth = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
                jPanel1.add(optionalJLabel2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(10, 15, 0, 15);
                contentJPanel.add(jPanel1, gridBagConstraints);

                jSeparator1.setForeground(new java.awt.Color(156, 156, 156));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 5);
                contentJPanel.add(jSeparator1, gridBagConstraints);

                jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel3.setText("<html>Please choose the \"From Address\" of emails sent from the Untangle Server.</html>");
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
                gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 15);
                contentJPanel.add(jSeparator2, gridBagConstraints);

                jPanel3.setLayout(new java.awt.GridBagLayout());

                jPanel3.setOpaque(false);
                connectivityTestJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                connectivityTestJButton.setText("Run Email Test");
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
                jLabel11.setText("<html>The <b>Email Test is optional</b>, but it can tell you if your mail settings  above are correct, by sending you an email.</html>");
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
                gridBagConstraints.insets = new java.awt.Insets(10, 15, 0, 10);
                contentJPanel.add(jPanel3, gridBagConstraints);

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

    private void smtpEnabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smtpEnabledJRadioButtonActionPerformed
	setMxRecordsEnabledDependency(false);
    }//GEN-LAST:event_smtpEnabledJRadioButtonActionPerformed
    
    private void smtpDisabledJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smtpDisabledJRadioButtonActionPerformed
	setMxRecordsEnabledDependency(true);
    }//GEN-LAST:event_smtpDisabledJRadioButtonActionPerformed
    
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
		shouldSaveNetworkSettings = false;
		doSave(null, false);
		EmailConnectivityTestJDialog connectivityJDialog = new EmailConnectivityTestJDialog((Dialog)InitialSetupEmailJPanel.this.getTopLevelAncestor());
		connectivityJDialog.setVisible(true);
	    }
	    catch(Exception e){
		Util.handleExceptionNoRestart("Error testing connectivity", e);
	    }
	    finally{
		shouldSaveNetworkSettings = true;
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    InitialSetupEmailJPanel.this.connectivityTestJButton.setEnabled(true);
		}});
	    }
	}
    }
    
    private void setMxRecordsEnabledDependency(boolean enabled){
	hostJTextField.setEnabled( !enabled );
	hostJLabel.setEnabled( !enabled );
	portJSpinner.setEnabled( !enabled );
	portJLabel.setEnabled( !enabled );
	smtpLoginJTextField.setEnabled( !enabled );
	smtpLoginJLabel.setEnabled( !enabled );
	smtpPasswordJPasswordField.setEnabled( !enabled );
	smtpPasswordJLabel.setEnabled( !enabled );
	optionalJLabel1.setEnabled( !enabled );
	optionalJLabel2.setEnabled( !enabled );
    }
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        public javax.swing.JTextField addressJTextField;
        private javax.swing.JLabel backgroundJPabel;
        private javax.swing.JButton connectivityTestJButton;
        private javax.swing.JPanel contentJPanel;
        private javax.swing.JLabel hostJLabel;
        public javax.swing.JTextField hostJTextField;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JPanel jPanel3;
        private javax.swing.JSeparator jSeparator1;
        private javax.swing.JSeparator jSeparator2;
        private javax.swing.JLabel optionalJLabel1;
        private javax.swing.JLabel optionalJLabel2;
        private javax.swing.JLabel portJLabel;
        private javax.swing.JSpinner portJSpinner;
        private javax.swing.ButtonGroup smtpButtonGroup;
        public javax.swing.JRadioButton smtpDisabledJRadioButton;
        public javax.swing.JRadioButton smtpEnabledJRadioButton;
        private javax.swing.JLabel smtpLoginJLabel;
        public javax.swing.JTextField smtpLoginJTextField;
        private javax.swing.JLabel smtpPasswordJLabel;
        private javax.swing.JPasswordField smtpPasswordJPasswordField;
        // End of variables declaration//GEN-END:variables
    
}
