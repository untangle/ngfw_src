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
import com.metavize.mvvm.client.*;
import javax.swing.SwingUtilities;
import java.awt.Color;

public class InitialSetupKeyJPanel extends MWizardPageJPanel {
    
    private static final String EXCEPTION_KEY_FORMAT = "The key must be exactly 16 alpha-numeric digits long.";

    public InitialSetupKeyJPanel() {
        initComponents();
    }

    String key;
    Exception exception;

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
	SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
	    keyJTextField.setBackground( Color.WHITE );

	    key = keyJTextField.getText().replaceAll("-","").replaceAll(" ","");

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
            boolean isActivated = com.metavize.mvvm.client.MvvmRemoteContextFactory.factory().isActivated( Util.getServerCodeBase().getHost(), 0, Util.isSecureViaHttps() );
            if( !isActivated ){
        	    MvvmRemoteContext mvvmContext = MvvmRemoteContextFactory.factory().activationLogin( Util.getServerCodeBase().getHost(),
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
