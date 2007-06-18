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

package com.untangle.node.firewall.gui;


import java.awt.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.Util;
import com.untangle.node.firewall.*;

public class SettingsJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {

    public SettingsJPanel() {
        initComponents();
        Util.addPanelFocus(this, defaultAcceptJRadioButton);
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // ENABLED ///////////
        boolean isDefaultAccept = defaultAcceptJRadioButton.isSelected();

        // SAVE THE VALUES ////////////////////////////////////
        if(!validateOnly){
            FirewallSettings firewallSettings = (FirewallSettings) settings;
            firewallSettings.setDefaultAccept( isDefaultAccept );
        }

    }


    boolean isDefaultAcceptCurrent;

    public void doRefresh(Object settings) {
        FirewallSettings firewallSettings = (FirewallSettings) settings;

        // ENABLED ///////////
        isDefaultAcceptCurrent = firewallSettings.isDefaultAccept();
        if( isDefaultAcceptCurrent )
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
        jTextArea2.setFocusable(false);
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
        defaultAcceptJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    defaultAcceptJRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(defaultAcceptJRadioButton, gridBagConstraints);

        enabledButtonGroup.add(defaultBlockJRadioButton);
        defaultBlockJRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        defaultBlockJRadioButton.setText("Block");
        defaultBlockJRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    defaultBlockJRadioButtonActionPerformed(evt);
                }
            });

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

    private void defaultBlockJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultBlockJRadioButtonActionPerformed
        if( isDefaultAcceptCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_defaultBlockJRadioButtonActionPerformed

    private void defaultAcceptJRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_defaultAcceptJRadioButtonActionPerformed
        if( !isDefaultAcceptCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_defaultAcceptJRadioButtonActionPerformed


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
