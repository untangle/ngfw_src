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

import java.awt.Color;
import java.util.Set;
import javax.swing.SwingUtilities;

import com.untangle.gui.util.TimeZone;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.uvm.security.*;


public class InitialSetupPasswordJPanel extends MWizardPageJPanel {

    private static final String EXCEPTION_PASSWORD_MISSING = "The password must be filled in before proceeding.";
    private static final String EXCEPTION_RETYPE_PASSWORD_MISSING = "The confirmation password must be filled in before proceeding.";
    private static final String EXCEPTION_PASSWORD_MISMATCH = "Your password and confirmation password do not match.  They must match before proceeding.";


    public InitialSetupPasswordJPanel() {
        initComponents();
        Util.addFocusHighlight(passwordJPasswordField);
        Util.addFocusHighlight(retypePasswordJPasswordField);
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            for(TimeZone tz : TimeZone.values()){
                timezoneJComboBox.addItem(tz);
            }
            timezoneJComboBox.setSelectedItem(TimeZone.getDefault());
        }});
    }

    public void initialFocus(){
        passwordJPasswordField.requestFocus();
    }

    String password;
    String retypePassword;
    String timezone;
    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
            passwordJPasswordField.setBackground( Color.WHITE );
            retypePasswordJPasswordField.setBackground( Color.WHITE );

            password = new String(passwordJPasswordField.getPassword());
            retypePassword = new String(retypePasswordJPasswordField.getPassword());

            exception = null;

            if(password.length() == 0){
                passwordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_PASSWORD_MISSING);
                return;
            }

            if(retypePassword.length() == 0){
                retypePasswordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_RETYPE_PASSWORD_MISSING);
                return;
            }

            if( !password.equals(retypePassword) ){
                passwordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                retypePasswordJPasswordField.setBackground( Util.INVALID_BACKGROUND_COLOR );
                exception = new Exception(EXCEPTION_PASSWORD_MISMATCH);
                return;
            }

            timezone = ((TimeZone) timezoneJComboBox.getSelectedItem()).getKey();
        }});

        if( exception != null )
            throw exception;

        if( !validateOnly ){
            try{
                InitialSetupWizard.getInfiniteProgressJComponent().startLater("Saving Password and Timezone...");

                // SEND FINAL SETTINGS
                AdminSettings adminSettings = Util.getAdminManager().getAdminSettings();
                Set<User> users = (Set<User>) adminSettings.getUsers();
                for( User user : users )
                    if( user.getLogin().equals("admin") )
                        user.setClearPassword(password);
                Util.getAdminManager().setAdminSettings(adminSettings);
                Util.getAdminManager().setTimeZone( java.util.TimeZone.getTimeZone(timezone) );

                InitialSetupWizard.getInfiniteProgressJComponent().stopLater(1500l);
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

        contentJPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        passwordJPasswordField = new javax.swing.JPasswordField();
        retypePasswordJPasswordField = new javax.swing.JPasswordField();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel6 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        timezoneJComboBox = new javax.swing.JComboBox();
        backgroundJPabel = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setOpaque(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please choose a password for the first account.<br>This account is known as the \"admin\" account.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(jLabel2, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setOpaque(false);
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("Login:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel1, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("Password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel3, gridBagConstraints);

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("Confirm Password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel4, gridBagConstraints);

        jLabel12.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel12.setText(" admin");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel1.add(jLabel12, gridBagConstraints);

        passwordJPasswordField.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel1.add(passwordJPasswordField, gridBagConstraints);

        retypePasswordJPasswordField.setColumns(15);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel1.add(retypePasswordJPasswordField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
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

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("<html>Please choose the timezone the Untangle Server is operating in.<br>This is necessary for report generation and logging purposes.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
        contentJPanel.add(jLabel6, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setOpaque(false);
        jLabel7.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel7.setText("Timezone:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel2.add(jLabel7, gridBagConstraints);

        timezoneJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        timezoneJComboBox.setMaximumSize(new java.awt.Dimension(425, 24));
        timezoneJComboBox.setMinimumSize(new java.awt.Dimension(425, 24));
        timezoneJComboBox.setPreferredSize(new java.awt.Dimension(425, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 0);
        jPanel2.add(timezoneJComboBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        contentJPanel.add(jPanel2, gridBagConstraints);

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
    private javax.swing.JLabel backgroundJPabel;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPasswordField passwordJPasswordField;
    private javax.swing.JPasswordField retypePasswordJPasswordField;
    private javax.swing.JComboBox timezoneJComboBox;
    // End of variables declaration//GEN-END:variables

}
