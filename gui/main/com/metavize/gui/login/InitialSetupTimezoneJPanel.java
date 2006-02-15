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
import java.util.TimeZone;
import com.metavize.gui.util.Util;
import javax.swing.SwingUtilities;

public class InitialSetupTimezoneJPanel extends MWizardPageJPanel {
    
	private final String[] timezones = {"US/Eastern (GMT-5)", "US/Central (GMT-6)", "US/Mountain (GMT-7)", "US/Pacific (GMT-8)", "US/Alaska (GMT-9)", "US/Hawaii (GMT-10)"};
		
    public InitialSetupTimezoneJPanel() {
        initComponents();
        
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    for(String timezone : timezones){
		timezoneJComboBox.addItem(timezone);
	    }
	}});
    }

    public void initialFocus(){
	timezoneJComboBox.requestFocus();
    }
	
    String timezone;

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
	    timezone = (String) timezoneJComboBox.getSelectedItem();
		timezone = timezone.substring(0, timezone.indexOf(' '));
	}});

        if( !validateOnly ){
	    try{
		InitialSetupWizard.getInfiniteProgressJComponent().startLater("Saving Timezone...");
		Util.getAdminManager().setTimeZone( TimeZone.getTimeZone(timezone) ); 
		InitialSetupWizard.getInfiniteProgressJComponent().stopLater(1500l);
	    }
	    catch(Exception e){
		InitialSetupWizard.getInfiniteProgressJComponent().stopLater(-1l);
		throw e;
	    }
        }
    }
    

        private void initComponents() {//GEN-BEGIN:initComponents
                jLabel2 = new javax.swing.JLabel();
                jLabel1 = new javax.swing.JLabel();
                timezoneJComboBox = new javax.swing.JComboBox();
                jLabel3 = new javax.swing.JLabel();

                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                setOpaque(false);
                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>Please choose the timezone your EdgeGuard is operating in.<br>This is necessary for report generation and logging purposes.</html>");
                add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, -1, -1));

                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("Timezone:");
                add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 100, -1, -1));

                timezoneJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
                add(timezoneJComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(115, 95, 260, -1));

                jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/login/ProductShot.png")));
                add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

        }//GEN-END:initComponents
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JComboBox timezoneJComboBox;
        // End of variables declaration//GEN-END:variables
    
}
