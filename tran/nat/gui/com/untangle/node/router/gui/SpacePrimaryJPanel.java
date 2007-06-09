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

package com.untangle.node.nat.gui;

import java.awt.Color;
import java.util.List;
import javax.swing.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.Util;
import com.untangle.uvm.networking.*;
import com.untangle.uvm.node.*;
import com.untangle.node.nat.*;


public class SpacePrimaryJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {

    private static final String EXCEPTION_MTU = "The MTU must be an integer value between "
        + NetworkSpace.MIN_MTU + " and " + NetworkSpace.MAX_MTU + ".";

    private NetworkSpace initNetworkSpace;

    public SpacePrimaryJPanel(NetworkSpace networkSpace) {
        initNetworkSpace = networkSpace;
        initComponents();
        mtuJSpinner.setModel(new SpinnerNumberModel(NetworkSpace.DEFAULT_MTU, NetworkSpace.MIN_MTU, NetworkSpace.MAX_MTU, 1));
        ((JSpinner.NumberEditor)mtuJSpinner.getEditor()).getFormat().setGroupingUsed(false);
        Util.addFocusHighlight(mtuJSpinner);
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // MTU //
        ((JSpinner.DefaultEditor)mtuJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        int mtu = 0;
        try{ mtuJSpinner.commitEdit(); }
        catch(Exception e){
            ((JSpinner.DefaultEditor)mtuJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_MTU);
        }
        mtu = (Integer) mtuJSpinner.getValue();

        // SAVE THE VALUES ////////////////////////////////////
        if( !validateOnly ){
            NetworkSpacesSettings networkSpacesSettings = (NetworkSpacesSettings) settings;
            NetworkSpace thisNetworkSpace = null;
            List<NetworkSpace> networkSpaceList = (List<NetworkSpace>) networkSpacesSettings.getNetworkSpaceList();
            for(NetworkSpace networkSpace : networkSpaceList ){
                if( networkSpace.getBusinessPapers() == initNetworkSpace.getBusinessPapers() )
                    thisNetworkSpace = networkSpace;
            }
            if( thisNetworkSpace == null )
                throw new Exception("network space not found during save: " + initNetworkSpace.getName());

            thisNetworkSpace.setMtu(mtu);
            networkSpacesSettings.setNetworkSpaceList(networkSpaceList);
        }

    }

    int mtuCurrent;

    public void doRefresh(Object settings) {
        NetworkSpacesSettings networkSpacesSettings = (NetworkSpacesSettings) settings;
        NetworkSpace thisNetworkSpace = null;
        for(NetworkSpace networkSpace : networkSpacesSettings.getNetworkSpaceList() )
            if( networkSpace.getBusinessPapers() == initNetworkSpace.getBusinessPapers() )
                thisNetworkSpace = networkSpace;

        // MTU //
        mtuCurrent = thisNetworkSpace.getMtu();
        mtuJSpinner.setValue( mtuCurrent );
        ((JSpinner.DefaultEditor)mtuJSpinner.getEditor()).getTextField().setText(Integer.toString(mtuCurrent));
        ((JSpinner.DefaultEditor)mtuJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);

    }





    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        natButtonGroup = new javax.swing.ButtonGroup();
        forwardingButtonGroup = new javax.swing.ButtonGroup();
        natJPanel = new javax.swing.JPanel();
        jTextArea2 = new javax.swing.JTextArea();
        mtuJPanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        mtuJSpinner = new javax.swing.JSpinner();

        setLayout(new java.awt.GridBagLayout());

        setMinimumSize(new java.awt.Dimension(515, 231));
        setPreferredSize(new java.awt.Dimension(515, 231));
        natJPanel.setLayout(new java.awt.GridBagLayout());

        natJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Public Space", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jTextArea2.setEditable(false);
        jTextArea2.setLineWrap(true);
        jTextArea2.setText("The Public Space is the Space that is bound to the External network interface.  Most of its settings are configured through a config panel in Config -> Networking.  However, for advanced purposes, you may specify the MTU here.");
        jTextArea2.setWrapStyleWord(true);
        jTextArea2.setFocusable(false);
        jTextArea2.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 15);
        natJPanel.add(jTextArea2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(natJPanel, gridBagConstraints);

        mtuJPanel.setLayout(new java.awt.GridBagLayout());

        mtuJPanel.setBorder(new javax.swing.border.TitledBorder(null, "MTU (Maximum Transfer Unit)", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setText("<html>The MTU specifies the maximum amount of data per packet that should be transferred out of this Space.  This value should not be changed unless explicitly necessary.</html>");
        jLabel5.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 15, 0, 15);
        mtuJPanel.add(jLabel5, gridBagConstraints);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("MTU (bytes) ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel4.add(jLabel6, gridBagConstraints);

        mtuJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        mtuJSpinner.setMaximumSize(new java.awt.Dimension(75, 19));
        mtuJSpinner.setMinimumSize(new java.awt.Dimension(75, 19));
        mtuJSpinner.setPreferredSize(new java.awt.Dimension(75, 19));
        mtuJSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent evt) {
                    mtuJSpinnerStateChanged(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        jPanel4.add(mtuJSpinner, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 5, 0);
        mtuJPanel.add(jPanel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        add(mtuJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void mtuJSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_mtuJSpinnerStateChanged
        if( !mtuJSpinner.getValue().equals(mtuCurrent) && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_mtuJSpinnerStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup forwardingButtonGroup;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JPanel mtuJPanel;
    private javax.swing.JSpinner mtuJSpinner;
    private javax.swing.ButtonGroup natButtonGroup;
    private javax.swing.JPanel natJPanel;
    // End of variables declaration//GEN-END:variables

}
