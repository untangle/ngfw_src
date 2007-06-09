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

package com.untangle.node.ips.gui;

import java.util.List;

import com.untangle.gui.node.Refreshable;
import com.untangle.node.ips.*;

public class IPSStatusJPanel extends javax.swing.JPanel implements Refreshable<Object>{

    public IPSStatusJPanel() {
        initComponents();
    }

    public void doRefresh(Object settings){
        IPSSettings ipsSettings = (IPSSettings) settings;
        List<IPSRule> rules = (List<IPSRule>) ipsSettings.getRules();
        int enabled = 0;
        int logging = 0;
        int blocking = 0;
        for( IPSRule rule : rules){
            enabled++;
            if(rule.isLive())
                blocking++;
            if(rule.getLog())
                logging++;
        }
        activeJLabel.setText(((Integer)enabled).toString());
        loggingJLabel.setText(((Integer)logging).toString());
        blockingJLabel.setText(((Integer)blocking).toString());

    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        serverRoutingJPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        activeJLabel = new javax.swing.JLabel();
        loggingJLabel = new javax.swing.JLabel();
        blockingJLabel = new javax.swing.JLabel();
        clientJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        serverRoutingJPanel.setLayout(new java.awt.GridBagLayout());

        serverRoutingJPanel.setBorder(new javax.swing.border.TitledBorder("Statistics"));
        serverRoutingJPanel.setMaximumSize(new java.awt.Dimension(1061, 64));
        serverRoutingJPanel.setMinimumSize(new java.awt.Dimension(1061, 64));
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Total Signatures Available:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        serverRoutingJPanel.add(jLabel2, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Total Signatures Logging:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        serverRoutingJPanel.add(jLabel3, gridBagConstraints);

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText("Total Signatures Blocking:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        serverRoutingJPanel.add(jLabel4, gridBagConstraints);

        activeJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        activeJLabel.setText("Total Signatures Active:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        serverRoutingJPanel.add(activeJLabel, gridBagConstraints);

        loggingJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        loggingJLabel.setText("Total Signatures Active:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        serverRoutingJPanel.add(loggingJLabel, gridBagConstraints);

        blockingJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        blockingJLabel.setText("Total Signatures Active:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        serverRoutingJPanel.add(blockingJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 218;
        gridBagConstraints.ipady = 30;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 90, 50);
        add(serverRoutingJPanel, gridBagConstraints);

        clientJPanel.setLayout(new java.awt.GridBagLayout());

        clientJPanel.setBorder(new javax.swing.border.TitledBorder("Notice"));
        clientJPanel.setMaximumSize(new java.awt.Dimension(1061, 64));
        clientJPanel.setMinimumSize(new java.awt.Dimension(1061, 64));
        clientJPanel.setPreferredSize(new java.awt.Dimension(1061, 64));
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("<html>Untangle, Inc. continues to maintain the default signature settings through automatic updates.  You are free to modify and add signatures, however, it is not required.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
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
        gridBagConstraints.insets = new java.awt.Insets(90, 50, 0, 50);
        add(clientJPanel, gridBagConstraints);

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel activeJLabel;
    private javax.swing.JLabel blockingJLabel;
    private javax.swing.JPanel clientJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel loggingJLabel;
    private javax.swing.JPanel serverRoutingJPanel;
    // End of variables declaration//GEN-END:variables

}
