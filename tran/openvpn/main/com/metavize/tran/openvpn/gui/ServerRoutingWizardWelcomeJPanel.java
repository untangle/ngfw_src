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

import com.metavize.gui.widgets.wizard.*;

import com.metavize.tran.openvpn.*;

public class ServerRoutingWizardWelcomeJPanel extends MWizardPageJPanel {
    
    private VpnTransform vpnTransform;
    
    public ServerRoutingWizardWelcomeJPanel(VpnTransform vpnTransform) {
        this.vpnTransform = vpnTransform;
        
        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {  
        if( !validateOnly){
            vpnTransform.startConfig(VpnTransform.ConfigState.SERVER_ROUTE);
        }
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("<html>Welcome to the<br>Metavize OpenVPN Setup Wizard!</html>");
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, -1, -1));

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>This wizard will help guide you through your<br> initial setup and configuration of OpenVPN<br>as a VPN Routing Server.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 110, -1, -1));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/tran/openvpn/gui/ProductShot.png")));
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables
    
}
