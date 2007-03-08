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

package com.untangle.tran.openvpn.gui;

import com.untangle.gui.util.Util;
import com.untangle.gui.transform.Refreshable;

import java.awt.Window;

import com.untangle.tran.openvpn.*;

public class WizardJPanel extends javax.swing.JPanel implements Refreshable<Object>{
    
    private VpnTransform vpnTransform;
    private MTransformControlsJPanel mTransformControlsJPanel;

    public WizardJPanel(VpnTransform vpnTransform, MTransformControlsJPanel mTransformControlsJPanel) {
        this.vpnTransform = vpnTransform;
	this.mTransformControlsJPanel = mTransformControlsJPanel;
        initComponents();
    }

    public void doRefresh(Object settings){
	VpnTransform.ConfigState configState = com.untangle.tran.openvpn.gui.MTransformControlsJPanel.getConfigState();
	if( VpnTransform.ConfigState.UNCONFIGURED.equals(configState) ){
	    statusJLabel.setText("Unconfigured: Use buttons below.");
	}
	else if( VpnTransform.ConfigState.CLIENT.equals(configState) ){
	    statusJLabel.setText("VPN Client: Connected to " + com.untangle.tran.openvpn.gui.MTransformControlsJPanel.getVpnServerAddress().toString());
	    serverRoutingJButton.setEnabled(true);
	    clientJButton.setEnabled(false);
	}
	else if( VpnTransform.ConfigState.SERVER_ROUTE.equals(configState) ){
	    statusJLabel.setText("VPN Server");
	    serverRoutingJButton.setEnabled(false);
	    clientJButton.setEnabled(true);
	}
	else{
	    // bad shite happened
	    serverRoutingJButton.setEnabled(false);
	    clientJButton.setEnabled(false);
	}

    }
    

        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                statusJPanel = new javax.swing.JPanel();
                statusJLabel = new javax.swing.JLabel();
                someJLabel = new javax.swing.JLabel();
                clientJPanel = new javax.swing.JPanel();
                clientJButton = new javax.swing.JButton();
                jLabel1 = new javax.swing.JLabel();
                serverRoutingJPanel = new javax.swing.JPanel();
                serverRoutingJButton = new javax.swing.JButton();
                jLabel2 = new javax.swing.JLabel();

                setLayout(new java.awt.GridBagLayout());

                statusJPanel.setLayout(new java.awt.GridBagLayout());

                statusJPanel.setBorder(new javax.swing.border.EtchedBorder());
                statusJPanel.setMaximumSize(new java.awt.Dimension(1061, 29));
                statusJPanel.setMinimumSize(new java.awt.Dimension(1061, 29));
                statusJPanel.setPreferredSize(new java.awt.Dimension(1061, 29));
                statusJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
                statusJLabel.setText("Unconfigured");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 5);
                statusJPanel.add(statusJLabel, gridBagConstraints);

                someJLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
                someJLabel.setText("<html><b>Current Mode:</b></html>");
                someJLabel.setMaximumSize(new java.awt.Dimension(175, 15));
                someJLabel.setMinimumSize(new java.awt.Dimension(175, 15));
                someJLabel.setPreferredSize(new java.awt.Dimension(175, 15));
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
                statusJPanel.add(someJLabel, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.ipadx = 175;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 175, 50);
                add(statusJPanel, gridBagConstraints);

                clientJPanel.setLayout(new java.awt.GridBagLayout());

                clientJPanel.setBorder(new javax.swing.border.EtchedBorder());
                clientJPanel.setMaximumSize(new java.awt.Dimension(1061, 64));
                clientJPanel.setMinimumSize(new java.awt.Dimension(1061, 64));
                clientJPanel.setPreferredSize(new java.awt.Dimension(1061, 64));
                clientJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                clientJButton.setText("<html><center>Configure as<br><b>VPN Client</b></center></html>");
                clientJButton.setMaximumSize(new java.awt.Dimension(175, 50));
                clientJButton.setMinimumSize(new java.awt.Dimension(175, 50));
                clientJButton.setPreferredSize(new java.awt.Dimension(175, 50));
                clientJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                clientJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
                clientJPanel.add(clientJButton, gridBagConstraints);

                jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel1.setText("<html>This allows your Untangle Server to connect to a remote Untangle Server, so they can share exported hosts or exported networks.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 5);
                clientJPanel.add(jLabel1, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.ipadx = 218;
                gridBagConstraints.ipady = 16;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(150, 50, 0, 50);
                add(clientJPanel, gridBagConstraints);

                serverRoutingJPanel.setLayout(new java.awt.GridBagLayout());

                serverRoutingJPanel.setBorder(new javax.swing.border.EtchedBorder());
                serverRoutingJPanel.setMaximumSize(new java.awt.Dimension(1061, 64));
                serverRoutingJPanel.setMinimumSize(new java.awt.Dimension(1061, 64));
                serverRoutingJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                serverRoutingJButton.setText("<html><center>Configure as<br><b>VPN Server</b></center></html>");
                serverRoutingJButton.setMaximumSize(new java.awt.Dimension(175, 50));
                serverRoutingJButton.setMinimumSize(new java.awt.Dimension(175, 50));
                serverRoutingJButton.setPreferredSize(new java.awt.Dimension(175, 50));
                serverRoutingJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                serverRoutingJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
                serverRoutingJPanel.add(serverRoutingJButton, gridBagConstraints);

                jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
                jLabel2.setText("<html>This lets your Untangle Server allow VPN clients to connect to it.  You can export hosts and networks, that will then be accessible to connected clients.</html>");
                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 10, 5, 5);
                serverRoutingJPanel.add(jLabel2, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.ipadx = 218;
                gridBagConstraints.ipady = 16;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 50, 20, 50);
                add(serverRoutingJPanel, gridBagConstraints);

        }//GEN-END:initComponents

    private void serverRoutingJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverRoutingJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
        serverRoutingJButton.setEnabled(false);
	ServerRoutingWizard.factory((Window)this.getTopLevelAncestor(),vpnTransform,mTransformControlsJPanel).setVisible(true);
	serverRoutingJButton.setEnabled(true);
    }//GEN-LAST:event_serverRoutingJButtonActionPerformed

    private void clientJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clientJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
	clientJButton.setEnabled(false);
	ClientWizard.factory((Window)this.getTopLevelAncestor(),vpnTransform,mTransformControlsJPanel).setVisible(true);
	clientJButton.setEnabled(true);
    }//GEN-LAST:event_clientJButtonActionPerformed
    
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton clientJButton;
        private javax.swing.JPanel clientJPanel;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JButton serverRoutingJButton;
        private javax.swing.JPanel serverRoutingJPanel;
        private javax.swing.JLabel someJLabel;
        private javax.swing.JLabel statusJLabel;
        private javax.swing.JPanel statusJPanel;
        // End of variables declaration//GEN-END:variables
    
}
