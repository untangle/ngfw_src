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
import java.util.TimeZone;
import com.metavize.gui.util.Util;

public class InitialSetupTimezoneJPanel extends javax.swing.JPanel implements Savable {
    

    public InitialSetupTimezoneJPanel() {
        initComponents();
        
        String[] timezones = TimeZone.getAvailableIDs();
        for(String timezone : timezones){
            if( timezone.startsWith("US/") ){
                timezoneJComboBox.addItem(timezone);
            }
        }
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        
        
        if( !validateOnly ){
            Util.getAdminManager().setTimeZone(TimeZone.getTimeZone((String)timezoneJComboBox.getSelectedItem())); 
        }
    }
    

    private void initComponents() {//GEN-BEGIN:initComponents
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        timezoneJComboBox = new javax.swing.JComboBox();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>Please choose the timezone which the EdgeGuard will be<br>operating in.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("Timezone:");
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 100, -1, -1));

        add(timezoneJComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 90, 260, -1));

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JComboBox timezoneJComboBox;
    // End of variables declaration//GEN-END:variables
    
}
