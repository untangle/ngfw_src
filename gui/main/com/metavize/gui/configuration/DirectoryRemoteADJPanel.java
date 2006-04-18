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

import com.metavize.mvvm.addrbook.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.mvvm.snmp.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;
import javax.swing.*;

public class DirectoryRemoteADJPanel extends javax.swing.JPanel
    implements Savable<DirectoryCompoundSettings>, Refreshable<DirectoryCompoundSettings> {

    private static final String EXCEPTION_PASSWORD_MISSING = "A \"Password\" must be specified if a \"Login\" is specified.";
    private static final String EXCEPTION_LOGIN_MISSING = "A \"Login\" must be specified if a \"Password\" is specified.";
    private static final String EXCEPTION_HOSTNAME_MISSING = "A \"Hostanme\" must be specified if \"Login\" or \"Password\" are specified.";
    private static final String EXCEPTION_SEARCH_BASE_MISSING = "A \"Search Base\" must be specified.";
    
    public DirectoryRemoteADJPanel() {
        initComponents();
	Util.setPortView(portJSpinner, 25);
    }

    public void doSave(DirectoryCompoundSettings directoryCompoundSettings, boolean validateOnly) throws Exception {

	// HOSTNAME ///////
	String host = hostJTextField.getText();

	// PORT //////
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
	int port = 0;
	try{ portJSpinner.commitEdit(); }
	catch(Exception e){ 
	    ((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
	    throw new Exception(Util.EXCEPTION_PORT_RANGE);
	}
        port = (Integer) portJSpinner.getValue();

	// LOGIN /////
	String login = loginJTextField.getText();

	// PASSWORD /////
	String password = new String(passwordJPasswordField.getPassword());

        // SEARCH BASE /////
        String searchBase = baseJTextField.getText();

	// CHECK THAT BOTH PASSWORD AND LOGIN ARE FILLED OR UNFILLED /////
	passwordJPasswordField.setBackground( Color.WHITE );
	loginJTextField.setBackground( Color.WHITE );
	if( (login.length() > 0) && (password.length() == 0) ){
	    passwordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_PASSWORD_MISSING);
	}
	else if( (login.length() == 0) && (password.length() > 0) ){
	    loginJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_LOGIN_MISSING);
	}

	// CHECK THAT IF EITHER LOGIN OR PASSWORD ARE FILLED, A HOSTNAME IS GIVEN
	hostJTextField.setBackground( Color.WHITE );
	if( (login.length() > 0) && (password.length() > 0) && (host.length() == 0) ){
	    hostJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_HOSTNAME_MISSING);
	}

	// CHECK THAT A SEARCH BASE IS SUPPLIED
	baseJTextField.setBackground( Color.WHITE );
	if( searchBase.length() == 0 ){
	    baseJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
	    throw new Exception(EXCEPTION_SEARCH_BASE_MISSING);
	}

	// SAVE SETTINGS ////////////
	if( !validateOnly ){
	    RepositorySettings repositorySettings = directoryCompoundSettings.getAddressBookSettings().getADRepositorySettings();
	    repositorySettings.setSuperuserDN( login );
	    repositorySettings.setSuperuserPass( password );
	    repositorySettings.setLDAPHost( host );
	    repositorySettings.setLDAPPort( port );
	    repositorySettings.setSearchBase( searchBase );
        }

    }

    private String hostCurrent;
    private int portCurrent;
    private String loginCurrent;
    private String passwordCurrent;
    private String searchBaseCurrent;

    public void doRefresh(DirectoryCompoundSettings directoryCompoundSettings){
	RepositorySettings repositorySettings = directoryCompoundSettings.getAddressBookSettings().getADRepositorySettings();

	// HOST /////
	hostCurrent = repositorySettings.getLDAPHost();
	hostJTextField.setText( hostCurrent );
	hostJTextField.setBackground( Color.WHITE );

	// PORT /////
	portCurrent = repositorySettings.getLDAPPort();
	portJSpinner.setValue( portCurrent );
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setText(Integer.toString(portCurrent));
	((JSpinner.DefaultEditor)portJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);

	// LOGIN //////
	loginCurrent = repositorySettings.getSuperuserDN();
	loginJTextField.setText( loginCurrent );
	loginJTextField.setBackground( Color.WHITE );

	// PASSWORD /////
	passwordCurrent = repositorySettings.getSuperuserPass();
	passwordJPasswordField.setText( passwordCurrent );
	passwordJPasswordField.setBackground( Color.WHITE );

	// SEARCH BASE //////
	searchBaseCurrent = repositorySettings.getSearchBase();
	baseJTextField.setText( searchBaseCurrent );	
	baseJTextField.setBackground( Color.WHITE );

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
                loginJTextField = new javax.swing.JTextField();
                jLabel12 = new javax.swing.JLabel();
                passwordJPasswordField = new javax.swing.JPasswordField();
                jSeparator2 = new javax.swing.JSeparator();
                restrictIPJPanel1 = new javax.swing.JPanel();
                jLabel6 = new javax.swing.JLabel();
                baseJTextField = new javax.swing.JTextField();

                setLayout(new java.awt.GridBagLayout());

                setMaximumSize(new java.awt.Dimension(563, 286));
                setMinimumSize(new java.awt.Dimension(563, 286));
                setPreferredSize(new java.awt.Dimension(563, 286));
                externalRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                externalRemoteJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Active Directory (AD) Server", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
                enableRemoteJPanel.setLayout(new java.awt.GridBagLayout());

                restrictIPJPanel.setLayout(new java.awt.GridBagLayout());

                jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel5.setText("Host:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(jLabel5, gridBagConstraints);

                hostJTextField.setMaximumSize(new java.awt.Dimension(200, 19));
                hostJTextField.setMinimumSize(new java.awt.Dimension(200, 19));
                hostJTextField.setPreferredSize(new java.awt.Dimension(200, 19));
                hostJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                hostJTextFieldCaretUpdate(evt);
                        }
                });

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
                portJSpinner.setMaximumSize(new java.awt.Dimension(75, 19));
                portJSpinner.setMinimumSize(new java.awt.Dimension(75, 19));
                portJSpinner.setPreferredSize(new java.awt.Dimension(75, 19));
                portJSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                        public void stateChanged(javax.swing.event.ChangeEvent evt) {
                                portJSpinnerStateChanged(evt);
                        }
                });

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

                loginJTextField.setMaximumSize(new java.awt.Dimension(150, 19));
                loginJTextField.setMinimumSize(new java.awt.Dimension(150, 19));
                loginJTextField.setPreferredSize(new java.awt.Dimension(150, 19));
                loginJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                loginJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(loginJTextField, gridBagConstraints);

                jLabel12.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel12.setText("Authentication Password:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel.add(jLabel12, gridBagConstraints);

                passwordJPasswordField.setMaximumSize(new java.awt.Dimension(150, 19));
                passwordJPasswordField.setMinimumSize(new java.awt.Dimension(150, 19));
                passwordJPasswordField.setPreferredSize(new java.awt.Dimension(150, 19));
                passwordJPasswordField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                passwordJPasswordFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
                restrictIPJPanel.add(passwordJPasswordField, gridBagConstraints);

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
                jLabel6.setText("Search Base:");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
                restrictIPJPanel1.add(jLabel6, gridBagConstraints);

                baseJTextField.setMaximumSize(new java.awt.Dimension(200, 19));
                baseJTextField.setMinimumSize(new java.awt.Dimension(200, 19));
                baseJTextField.setPreferredSize(new java.awt.Dimension(200, 19));
                baseJTextField.addCaretListener(new javax.swing.event.CaretListener() {
                        public void caretUpdate(javax.swing.event.CaretEvent evt) {
                                baseJTextFieldCaretUpdate(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                restrictIPJPanel1.add(baseJTextField, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 91, 5, 0);
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

    private void portJSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_portJSpinnerStateChanged

    }//GEN-LAST:event_portJSpinnerStateChanged
    
    private void hostJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_hostJTextFieldCaretUpdate

    }//GEN-LAST:event_hostJTextFieldCaretUpdate
    
	private void loginJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_loginJTextFieldCaretUpdate

	}//GEN-LAST:event_loginJTextFieldCaretUpdate
    
	private void passwordJPasswordFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_passwordJPasswordFieldCaretUpdate

	}//GEN-LAST:event_passwordJPasswordFieldCaretUpdate
    
    private void baseJTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_baseJTextFieldCaretUpdate

    }//GEN-LAST:event_baseJTextFieldCaretUpdate
    

    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        public javax.swing.JTextField baseJTextField;
        private javax.swing.JPanel enableRemoteJPanel;
        private javax.swing.JPanel externalRemoteJPanel;
        public javax.swing.JTextField hostJTextField;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JSeparator jSeparator2;
        public javax.swing.JTextField loginJTextField;
        private javax.swing.JPasswordField passwordJPasswordField;
        private javax.swing.JSpinner portJSpinner;
        private javax.swing.JPanel restrictIPJPanel;
        private javax.swing.JPanel restrictIPJPanel1;
        private javax.swing.ButtonGroup snmpButtonGroup;
        private javax.swing.ButtonGroup trapButtonGroup;
        // End of variables declaration//GEN-END:variables
    

}
