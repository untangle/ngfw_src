/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.openvpn.gui;

import com.untangle.gui.widgets.wizard.*;
import com.untangle.node.openvpn.*;

public class ClientWizardWelcomeJPanel extends MWizardPageJPanel {

    private VpnNode vpnNode;

    public ClientWizardWelcomeJPanel(VpnNode vpnNode) {
        this.vpnNode = vpnNode;

        initComponents();
    }

    public void doSave(Object settings, boolean validateOnly) throws Exception {
        if( !validateOnly){
            vpnNode.startConfig(VpnNode.ConfigState.CLIENT);
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
        jLabel1.setText("<html>Welcome to the<br>Untangle OpenVPN Setup Wizard!</html>");
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, -1, -1));

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>This wizard will help guide you through your initial setup and configuration of OpenVPN as a VPN Client.<br><br><font color=\"#FF0000\">Warning:  Finishing this wizard will cause any previous OpenVPN settings you had to be lost, and overwritten by new settings.  Only finish this wizard if you would like completely new settings.</font></html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 110, 375, -1));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/tran/openvpn/gui/ProductShot.png")));
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables

}
