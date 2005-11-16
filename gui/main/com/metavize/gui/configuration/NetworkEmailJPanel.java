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

package com.metavize.gui.configuration;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.mvvm.snmp.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;
import javax.swing.*;

public class NetworkEmailJPanel extends javax.swing.JPanel implements Savable, Refreshable {

    private static final String EXCEPTION_PASSWORD_MISSING = "A \"Password\" must be specified if a \"Login\" is specified.";
    private static final String EXCEPTION_LOGIN_MISSING = "A \"Login\" must be specified if a \"Password\" is specified.";
    private static final String EXCEPTION_HOSTNAME_MISSING = "A \"Hostanme\" must be specified if \"Login\" or \"Password\" are specified.";
    private static final String EXCEPTION_ADDRESS_MISSING = "A \"From Address\" must be specified.";

    
    public NetworkEmailJPanel() {
        initComponents();
	portJSpinner.setModel(new SpinnerNumberModel(0,0,65535,1));
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {

	// HOSTNAME ///////
	String host = hostJTextField.getText();
	hostJTextField.setBackground( Color.WHITE );

	// PORT //////
	int port = (Integer) portJSpinner.getValue();

	// SMTP LOGIN /////
	String login = smtpLoginJTextField.getText();
	smtpLoginJTextField.setBackground( Color.WHITE );

	// SMTP PASSWORD /////
	String password = new String(smtpPasswordJPasswordField.getPassword());
	smtpPasswordJPasswordField.setBackground( Color.WHITE );

        // FROM ADDRESS /////
        String address = addressJTextField.getText();
	addressJTextField.setBackground( Color.WHITE );

	// CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED /////
	if( (login.length() > 0) && (password.length() == 0) ){
	    smtpPasswordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_PASSWORD_MISSING);
	}
	else if( (login.length() == 0) && (password.length() > 0) ){
	    smtpLoginJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_LOGIN_MISSING);
	}

	// CHECK THAT IF EITHER LOGIN OR PASSWORD ARE FILLED, A HOSTNAME IS GIVEN
	if( (login.length() > 0) && (password.length() > 0) && (host.length() == 0) ){
	    hostJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_HOSTNAME_MISSING);
	}

	// CHECK THAT A FROM ADDRESS IS SUPPLIED
	if( address.length() == 0 ){
	    addressJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_ADDRESS_MISSING);
	}

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
            MailSettings mailSettings = Util.getAdminManager().getMailSettings();

            mailSettings.setSmtpHost( host );
            mailSettings.setSmtpPort( port );
            mailSettings.setAuthUser( login );
            mailSettings.setAuthPass( password );
            mailSettings.setFromAddress( address );
            
            Util.getAdminManager().setMailSettings( mailSettings );
        }

    }

    public void doRefresh(Object settings){
	MailSettings mailSettings = Util.getAdminManager().getMailSettings();

	// HOST /////
	String host = mailSettings.getSmtpHost();
	hostJTextField.setText( host );
	hostJTextField.setBackground( Color.WHITE );

	// PORT /////
	int port = mailSettings.getSmtpPort();
	portJSpinner.setValue( port );

	// LOGIN //////
	String login = mailSettings.getAuthUser();
	smtpLoginJTextField.setText( login );

	// PASSWOR /////
	String password = mailSettings.getAuthPass();
	smtpPasswordJPasswordField.setText( password );

	// FROM ADDRESS //////
	String address = mailSettings.getFromAddress();
	addressJTextField.setText( address );	
    }
    
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        snmpButtonGroup = new javax.swing.ButtonGroup();
        trapButtonGroup = new javax.swing.ButtonGroup();
        externalRemoteJPanel = new javax.swing.JPanel();
        enableRemoteJPanel = new javax.swing.JPanel();
        restrictIPJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        hostJTextField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        portJSpinner = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        smtpLoginJTextField = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        smtpPasswordJPasswordField = new javax.swing.JPasswordField();
        jSeparator2 = new javax.swing.JSeparator();
        restrictIPJPanel1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        addressJTextField = new javax.swing.JTextField();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 343));
        setMinimumSize(new java.awt.Dimension(563, 343));
        setPreferredSize(new java.awt.Dimension(563, 343));
        externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Outgoing Email Server (SMTP)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        enableRemoteJPanel.setLayout(new java.awt.GridBagLayout());

        restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("Hostname:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel5, gridBagConstraints);

        hostJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        hostJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        hostJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel.add(hostJTextField, gridBagConstraints);

        jLabel11.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel11.setText("Port:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel11, gridBagConstraints);

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
        restrictIPJPanel.add(portJSpinner, gridBagConstraints);

        jLabel8.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel8.setText("Authentication Login:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel8, gridBagConstraints);

        smtpLoginJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        smtpLoginJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        smtpLoginJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel.add(smtpLoginJTextField, gridBagConstraints);

        jLabel12.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel12.setText("Authentication Password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel.add(jLabel12, gridBagConstraints);

        smtpPasswordJPasswordField.setMaximumSize(new java.awt.Dimension(150, 19));
        smtpPasswordJPasswordField.setMinimumSize(new java.awt.Dimension(150, 19));
        smtpPasswordJPasswordField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        restrictIPJPanel.add(smtpPasswordJPasswordField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 5, 0);
        enableRemoteJPanel.add(restrictIPJPanel, gridBagConstraints);

        jSeparator2.setForeground(new java.awt.Color(200, 200, 200));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        enableRemoteJPanel.add(jSeparator2, gridBagConstraints);

        restrictIPJPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("From Address:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        restrictIPJPanel1.add(jLabel6, gridBagConstraints);

        addressJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
        addressJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
        addressJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        restrictIPJPanel1.add(addressJTextField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 79, 5, 0);
        enableRemoteJPanel.add(restrictIPJPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        externalRemoteJPanel.add(enableRemoteJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(externalRemoteJPanel, gridBagConstraints);

    }//GEN-END:initComponents
    

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JTextField addressJTextField;
    private javax.swing.JPanel enableRemoteJPanel;
    private javax.swing.JPanel externalRemoteJPanel;
    public javax.swing.JTextField hostJTextField;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSpinner portJSpinner;
    private javax.swing.JPanel restrictIPJPanel;
    private javax.swing.JPanel restrictIPJPanel1;
    public javax.swing.JTextField smtpLoginJTextField;
    private javax.swing.JPasswordField smtpPasswordJPasswordField;
    private javax.swing.ButtonGroup snmpButtonGroup;
    private javax.swing.ButtonGroup trapButtonGroup;
    // End of variables declaration//GEN-END:variables
    

}
