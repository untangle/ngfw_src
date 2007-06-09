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



package com.untangle.node.spyware.gui;

import java.awt.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.uvm.*;
import com.untangle.uvm.security.*;
import com.untangle.uvm.node.*;
import com.untangle.node.spyware.*;


public class UrlConfigJPanel extends javax.swing.JPanel implements Savable<Object>, Refreshable<Object> {


    public UrlConfigJPanel() {
        initComponents();
        Util.addPanelFocus(this, urlEnabledRadioButton);
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(Object settings, boolean validateOnly) throws Exception {

        // URL ENABLED ///////////
        boolean isUrlEnabled = urlEnabledRadioButton.isSelected();

        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            SpywareSettings spywareSettings = (SpywareSettings) settings;
            spywareSettings.setUrlBlacklistEnabled(isUrlEnabled);
        }

    }

    boolean isUrlEnabledCurrent;

    public void doRefresh(Object settings){

        // URL ENABLED /////////
        SpywareSettings spywareSettings = (SpywareSettings) settings;
        isUrlEnabledCurrent = spywareSettings.getUrlBlacklistEnabled();
        if( isUrlEnabledCurrent )
            urlEnabledRadioButton.setSelected(true);
        else
            urlDisabledRadioButton.setSelected(true);
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        ftpButtonGroup = new javax.swing.ButtonGroup();
        contentJPanel = new javax.swing.JPanel();
        urlEnabledRadioButton = new javax.swing.JRadioButton();
        urlDisabledRadioButton = new javax.swing.JRadioButton();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(563, 120));
        setMinimumSize(new java.awt.Dimension(563, 120));
        setPreferredSize(new java.awt.Dimension(563, 120));
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setBorder(new javax.swing.border.EtchedBorder());
        ftpButtonGroup.add(urlEnabledRadioButton);
        urlEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        urlEnabledRadioButton.setText("<html><b>Enable</b> spyware and ad URL blocking</html>");
        urlEnabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    urlEnabledRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 6);
        contentJPanel.add(urlEnabledRadioButton, gridBagConstraints);

        ftpButtonGroup.add(urlDisabledRadioButton);
        urlDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        urlDisabledRadioButton.setText("<html><b>Disable</b> spyware and ad URL blocking</html>");
        urlDisabledRadioButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    urlDisabledRadioButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        contentJPanel.add(urlDisabledRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(contentJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void urlDisabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_urlDisabledRadioButtonActionPerformed
        if( isUrlEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_urlDisabledRadioButtonActionPerformed

    private void urlEnabledRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_urlEnabledRadioButtonActionPerformed
        if( !isUrlEnabledCurrent && (settingsChangedListener != null) )
            settingsChangedListener.settingsChanged(this);
    }//GEN-LAST:event_urlEnabledRadioButtonActionPerformed



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contentJPanel;
    private javax.swing.ButtonGroup ftpButtonGroup;
    public javax.swing.JRadioButton urlDisabledRadioButton;
    public javax.swing.JRadioButton urlEnabledRadioButton;
    // End of variables declaration//GEN-END:variables


}
