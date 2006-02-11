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

import java.net.URL;

import com.metavize.gui.widgets.wizard.*;
import com.metavize.gui.util.Util;
import com.metavize.mvvm.client.*;
import javax.swing.SwingUtilities;
import java.awt.Color;

public class InitialSetupKeyJPanel extends MWizardPageJPanel {
    
    private static final String EXCEPTION_KEY_FORMAT = "The key must be exactly 16 alpha-numeric digits long (excluding dashes and spaces).  Please make sure your key is the correct length.";

    public InitialSetupKeyJPanel() {
        initComponents();
    }

	public void initialFocus(){
		keyJTextField.requestFocus();
	}
	
    String key;
    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
	SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
	    keyJTextField.setBackground( Color.WHITE );

	    key = keyJTextField.getText().replaceAll("-","").replaceAll(" ","").toLowerCase();

	    exception = null;

	    if( key.length() != 16 ){
		keyJTextField.setBackground( Util.INVALID_BACKGROUND_COLOR );
		exception = new Exception(EXCEPTION_KEY_FORMAT);
		return;
	    }	    
	}});

	if( exception != null )
	    throw exception;
        
        
        if( !validateOnly){
            URL url = Util.getServerCodeBase();
            boolean isActivated = com.metavize.mvvm.client.MvvmRemoteContextFactory.factory().isActivated( url.getHost(), url.getPort(), 0, Util.isSecureViaHttps() );
            if( !isActivated ){
                MvvmRemoteContext mvvmContext = MvvmRemoteContextFactory.factory().activationLogin( url.getHost(), url.getPort(),
                											key,
                        										0,
                                									Util.getClassLoader(),
                                        								Util.isSecureViaHttps() );
                                                                                   
                    Util.setMvvmContext(mvvmContext);
            }
	    
        }
    }
    

        private void initComponents() {//GEN-BEGIN:initComponents
                jLabel2 = new javax.swing.JLabel();
                keyJTextField = new javax.swing.JTextField();
                jLabel16 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();

                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>Please enter the 16-digit EdgeGuard activation key. (With or without dashes) The key can be found on the side of your EdgeGuard appliance, and also on your QuickStart Guide.<br><b>This information is required.</b></html>");
                add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 400, -1));

                keyJTextField.setColumns(19);
                add(keyJTextField, new org.netbeans.lib.awtextra.AbsoluteConstraints(85, 118, -1, -1));

                jLabel16.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                jLabel16.setText("Key:");
                add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 120, -1, -1));

                jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/login/ProductShot.png")));
                add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

        }//GEN-END:initComponents
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel jLabel16;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JTextField keyJTextField;
        // End of variables declaration//GEN-END:variables
    
}
