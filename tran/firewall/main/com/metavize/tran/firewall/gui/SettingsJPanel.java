/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: NatEventHandler.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.tran.firewall.gui;


import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;
import com.metavize.tran.firewall.*;
import com.metavize.mvvm.tran.IPaddr;

import java.awt.*;

/**
 *
 * @author  inieves
 */
public class SettingsJPanel extends javax.swing.JPanel implements Savable, Refreshable {
    
    public SettingsJPanel() {
        initComponents();
    }
        
    
    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // ENABLED ///////////
	boolean isDefaultAccept = defaultAcceptJRadioButton.isSelected();
        
        // SAVE THE VALUES ////////////////////////////////////
	if(!validateOnly){
	    FirewallSettings firewallSettings = (FirewallSettings) settings;
	    firewallSettings.setDefaultAccept( isDefaultAccept );
	}
        
    }
    

    public void doRefresh(Object settings) {
	FirewallSettings firewallSettings = (FirewallSettings) settings;

        // ENABLED ///////////
	boolean isDefaultAccept;
	isDefaultAccept = firewallSettings.isDefaultAccept();
	if( isDefaultAccept )
	    defaultAcceptJRadioButton.setSelected(true);
	else
	    defaultBlockJRadioButton.setSelected(true);               

    }



    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        enabledButtonGroup = new javax.swing.ButtonGroup();
        explanationJPanel = new javax.swing.JPanel();
        jTextArea2 = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        defaultAcceptJRadioButton = new javax.swing.JRadioButton();
        defaultBlockJRadioButton = new javax.swing.JRadioButton();
        jLabel1 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setMinimumSize(new java.awt.Dimension(530, 150));
        setPreferredSize(new java.awt.Dimension(530, 150));
        explanationJPanel.setLayout(new java.awt.GridBagLayout());

        explanationJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Default Action", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea2.setEditable(false);
        jTextArea2.setLineWrap(true);
        jTextArea2.setText("The default action specifies what will happen to traffic if no block or pass rules apply.");
        jTextArea2.setWrapStyleWord(true);
        jTextArea2.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        explanationJPanel.add(jTextArea2, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        enabledButtonGroup.add(defaultAcceptJRadioButton);
        defaultAcceptJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        defaultAcceptJRadioButton.setText("Pass");
        defaultAcceptJRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(defaultAcceptJRadioButton, gridBagConstraints);

        enabledButtonGroup.add(defaultBlockJRadioButton);
        defaultBlockJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        defaultBlockJRadioButton.setText("Block");
        defaultBlockJRadioButton.setFocusPainted(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(defaultBlockJRadioButton, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("Default Action:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        explanationJPanel.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(explanationJPanel, gridBagConstraints);

    }//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JRadioButton defaultAcceptJRadioButton;
    public javax.swing.JRadioButton defaultBlockJRadioButton;
    private javax.swing.ButtonGroup enabledButtonGroup;
    private javax.swing.JPanel explanationJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextArea jTextArea2;
    // End of variables declaration//GEN-END:variables
    
}
