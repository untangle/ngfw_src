/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.openvpn.gui;

import com.untangle.gui.widgets.wizard.*;
import com.untangle.node.openvpn.*;

public class ClientWizardCongratulationsJPanel extends MWizardPageJPanel {

    private VpnNode vpnNode;

    public ClientWizardCongratulationsJPanel(VpnNode vpnNode) {
        this.vpnNode = vpnNode;

        initComponents();
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setOpaque(false);
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 18));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("<html>Congratulations!<br>OpenVPN is configured as a VPN Client.</html>");
        add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, -1, -1));

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("<html>\nIf necessary, you can change the configuration of OpenVPN<br>\nby launching the Setup Wizard again.</html>");
        add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 110, -1, -1));

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/tran/openvpn/gui/ProductShot.png")));
        add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(-130, 230, -1, -1));

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables

}
