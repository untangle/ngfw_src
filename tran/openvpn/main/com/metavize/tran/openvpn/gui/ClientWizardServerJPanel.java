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

package com.metavize.tran.openvpn.gui;

import com.metavize.tran.openvpn.*;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.gui.widgets.wizard.*;
import com.metavize.gui.util.Util;
import com.metavize.mvvm.client.*;
import javax.swing.SwingUtilities;
import java.awt.Color;

public class ClientWizardServerJPanel extends MWizardPageJPanel {
    
    private static final String EXCEPTION_ADDRESS_FORMAT = "The address must be a valid IP address.";
    private static final String EXCEPTION_NO_PASSWORD = "You must supply a password to connect to the server.";

    private VpnTransform vpnTransform;
    
    public ClientWizardServerJPanel(VpnTransform vpnTransform) {
        this.vpnTransform = vpnTransform;
        initComponents();
    }

    String address;
    IPaddr addressIPaddr;
    String password;
    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
	SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
	    serverJTextField.setBackground( Color.WHITE );
            passwordJTextField.setBackground( Color.WHITE );

	    address = serverJTextField.getText().trim();
            password = passwordJTextField.getText().trim();
            
	    exception = null;

            if( address.length() <= 0 ){
		serverJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		exception = new Exception(EXCEPTION_ADDRESS_FORMAT);
		return;
            }
            
            try{
                addressIPaddr = IPaddr.parse(address);
            }
            catch(Exception e){
                exception = new Exception(EXCEPTION_ADDRESS_FORMAT);
                return;
            }
            
	    if( password.length() <= 0 ){
		passwordJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		exception = new Exception(EXCEPTION_NO_PASSWORD);
		return;
            }
	}});

	if( exception != null )
	    throw exception;
        
        
        if( !validateOnly){
            vpnTransform.downloadConfig( addressIPaddr, password );
        }
    }
    

    private void initComponents() {//GEN-BEGIN:initComponents
        jLabel2 = new javax.swing.JLabel();
        serverJTextField = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        passwordJTextField = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please enter the IP address and password of the EdgeGuard<br>(or OpenVPN server) which you would like to connect to.<br>This connection will allow you to share the exported hosts<br>and networks of the server.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 360, -1));

        serverJTextField.setColumns(19);
        add(serverJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 118, -1, -1));

        jLabel16.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setText("Server IP Address:");
        add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 120, -1, -1));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/tran/openvpn/gui/ProductShot.png")));
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

        passwordJTextField.setColumns(19);
        add(passwordJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 140, -1, -1));

        jLabel17.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel17.setText("Password:");
        add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(97, 140, -1, -1));

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTextField passwordJTextField;
    private javax.swing.JTextField serverJTextField;
    // End of variables declaration//GEN-END:variables
    
}
