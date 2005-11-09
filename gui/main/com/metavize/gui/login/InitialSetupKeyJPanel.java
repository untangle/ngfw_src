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
import com.metavize.gui.util.Util;
import com.metavize.mvvm.client.*;

public class InitialSetupKeyJPanel extends javax.swing.JPanel implements Savable {
    

    public InitialSetupKeyJPanel() {
        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
        String key = keyJTextField.getText();
        key = key.replaceAll("-","");
        key = key.replaceAll(" ","");
        if( key.length() != 16 )
            throw new Exception("The key must be exactly 16 alpha-numeric digits long.");
        
        if( !validateOnly){ 
            MvvmRemoteContextFactory.factory().activationLogin( Util.getServerCodeBase().getHost(), key, 0, Util.getClassLoader(), Util.isSecureViaHttps() );
        }
    }
    

    private void initComponents() {//GEN-BEGIN:initComponents
        jLabel2 = new javax.swing.JLabel();
        keyJTextField = new javax.swing.JTextField();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please enter the 16-digit EdgeGuard activation key<br> into the field below. (With or without dashes)</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        keyJTextField.setColumns(19);
        add(keyJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 120, -1, -1));

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField keyJTextField;
    // End of variables declaration//GEN-END:variables
    
}
