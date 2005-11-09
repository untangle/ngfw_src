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
import java.util.Arrays;
import com.metavize.gui.util.Util;
import com.metavize.mvvm.security.*;
import java.util.Set;

public class InitialSetupPasswordJPanel extends javax.swing.JPanel implements Savable {
    

    public InitialSetupPasswordJPanel() {
        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
        String password = new String(passwordJPasswordField.getPassword());
        if(password.length() == 0)
            throw new Exception("You must fill out the password.");
        
        String retypePassword = new String(retypePasswordJPasswordField.getPassword());
        if(retypePassword.length() == 0)
            throw new Exception("You must fill out the retype password.");
        
        if( !password.equals(retypePassword) )
            throw new Exception("Your password and retype password do not match.  They must match.");
        
        if( !validateOnly ){
            AdminSettings adminSettings = Util.getAdminManager().getAdminSettings();
            Set<User> users = (Set<User>) adminSettings.getUsers();
            for( User user : users )
                if( user.getLogin().equals("admin") )
                    user.setClearPassword(password);
            Util.getAdminManager().setAdminSettings(adminSettings);
        }
    }
    

    private void initComponents() {//GEN-BEGIN:initComponents
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        passwordJPasswordField = new javax.swing.JPasswordField();
        retypePasswordJPasswordField = new javax.swing.JPasswordField();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please choose a password for the first account.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("Login:");
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 70, -1, -1));

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setText("Password:");
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 90, -1, -1));

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("Retype Password:");
        add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 110, -1, -1));

        jLabel12.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel12.setText("admin");
        add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 70, -1, -1));

        passwordJPasswordField.setColumns(15);
        add(passwordJPasswordField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 90, -1, -1));

        retypePasswordJPasswordField.setColumns(15);
        add(retypePasswordJPasswordField, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 110, -1, -1));

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPasswordField passwordJPasswordField;
    private javax.swing.JPasswordField retypePasswordJPasswordField;
    // End of variables declaration//GEN-END:variables
    
}
