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
import com.metavize.mvvm.security.*;
import java.util.Set;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.TimeZone;

public class InitialSetupPasswordJPanel extends MWizardPageJPanel {
    
    private static final String EXCEPTION_PASSWORD_MISSING = "The password must be filled in before proceeding.";
    private static final String EXCEPTION_RETYPE_PASSWORD_MISSING = "The retype password must be filled in before proceeding.";
    private static final String EXCEPTION_PASSWORD_MISMATCH = "Your password and retype password do not match.  They must match before proceeding.";

	private final String[] timezones = {"US/Eastern (GMT-5)", "US/Central (GMT-6)", "US/Mountain (GMT-7)", "US/Pacific (GMT-8)", "US/Alaska (GMT-9)", "US/Hawaii (GMT-10)"};
	
    public InitialSetupPasswordJPanel() {
        initComponents();
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    for(String timezone : timezones){
		timezoneJComboBox.addItem(timezone);
	    }
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
		
		timezone = (String) timezoneJComboBox.getSelectedItem();
		timezone = timezone.substring(0, timezone.indexOf(' '));
	}});

        if( exception != null )
	    throw exception;
        
        if( !validateOnly ){
	    try{
		InitialSetupWizard.getInfiniteProgressJComponent().startLater("Saving Final Configuration...");
		AdminSettings adminSettings = Util.getAdminManager().getAdminSettings();
		Set<User> users = (Set<User>) adminSettings.getUsers();
		for( User user : users )
		    if( user.getLogin().equals("admin") )
			user.setClearPassword(password);
		Util.getAdminManager().setAdminSettings(adminSettings);
		Util.getAdminManager().setTimeZone( TimeZone.getTimeZone(timezone) );
		
		try{
		    if( InitialSetupRoutingJPanel.getNatEnabled() ){
			Util.getNetworkManager().setWizardNatEnabled(InitialSetupRoutingJPanel.getAddress(),
								     InitialSetupRoutingJPanel.getNetmask());
		    }
		    else{
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
    

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                jLabel2 = new javax.swing.JLabel();
                jPanel1 = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                jLabel4 = new javax.swing.JLabel();
                jLabel12 = new javax.swing.JLabel();
                passwordJPasswordField = new javax.swing.JPasswordField();
                retypePasswordJPasswordField = new javax.swing.JPasswordField();
                jLabel5 = new javax.swing.JLabel();
                jLabel6 = new javax.swing.JLabel();
                jLabel7 = new javax.swing.JLabel();
                timezoneJComboBox = new javax.swing.JComboBox();

                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>Please choose a password for the first account.  This account is known as the \"admin\" account.</html>");
                add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, 360, -1));

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

                add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 70, -1, -1));

                jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/login/ProductShot.png")));
                add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

                jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel6.setText("<html>Please choose the timezone your EdgeGuard is operating in.<br>This is necessary for report generation and logging purposes.</html>");
                add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 150, -1, -1));

                jLabel7.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel7.setText("Timezone:");
                add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 200, -1, -1));

                timezoneJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
                add(timezoneJComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(115, 195, 260, -1));

        }//GEN-END:initComponents
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPasswordField passwordJPasswordField;
        private javax.swing.JPasswordField retypePasswordJPasswordField;
        private javax.swing.JComboBox timezoneJComboBox;
        // End of variables declaration//GEN-END:variables
    
}
