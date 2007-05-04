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



package com.untangle.tran.http.gui;

import java.awt.*;
import javax.swing.*;

import com.untangle.gui.configuration.*;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.mvvm.*;
import com.untangle.mvvm.security.*;
import com.untangle.mvvm.tran.*;
import com.untangle.tran.http.HttpSettings;


public class MCasingJPanel extends com.untangle.gui.transform.MCasingJPanel<MaintenanceCompoundSettings> {

    private static final String EXCEPTION_URI_LENGTH = "The Max URI length must be between "
        + HttpSettings.MIN_URI_LENGTH
        + " and " + HttpSettings.MAX_URI_LENGTH + ".";

    public MCasingJPanel() {
        initComponents();

        maxUriJSpinner.setModel( new SpinnerNumberModel((Integer)HttpSettings.MIN_URI_LENGTH,
                                                        (Integer)HttpSettings.MIN_URI_LENGTH,
                                                        (Integer)HttpSettings.MAX_URI_LENGTH,
                                                        (Integer)1) );
        maxHeaderJSpinner.setModel( new SpinnerNumberModel((Integer)HttpSettings.MIN_HEADER_LENGTH,
                                                           (Integer)HttpSettings.MIN_HEADER_LENGTH,
                                                           (Integer)HttpSettings.MAX_HEADER_LENGTH,
                                                           (Integer)1) );
        maxUriLimitsJLabel.setText("(max=" + HttpSettings.MAX_URI_LENGTH + " min=" + HttpSettings.MIN_URI_LENGTH + ")");
        maxHeaderLimitsJLabel.setText("(max=" + HttpSettings.MAX_HEADER_LENGTH + " min=" + HttpSettings.MIN_HEADER_LENGTH + ")");

        Util.addPanelFocus(this, httpEnabledRadioButton);
        Util.addFocusHighlight(maxUriJSpinner);
        Util.addFocusHighlight(maxHeaderJSpinner);
    }

    public String getDisplayName(){ return "HTTP Settings"; }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
        this.settingsChangedListener = settingsChangedListener;
    }
    ///////////////////////////////////////////

    public void doSave(MaintenanceCompoundSettings maintenanceCompoundSettings, boolean validateOnly) throws Exception {

        // HTTP ENABLED ///////////
        boolean isHttpEnabled = httpEnabledRadioButton.isSelected();

        // LONG URIS //////////////
        boolean blockLongUris = longUriDisabledRadioButton.isSelected();
        try{ maxUriJSpinner.commitEdit(); }
        catch(Exception e){
            ((JSpinner.DefaultEditor)maxUriJSpinner.getEditor()).getTextField().setBackground(Util.INVALID_BACKGROUND_COLOR);
            throw new Exception(EXCEPTION_URI_LENGTH);
        }
        int maxUriLength = (Integer) maxUriJSpinner.getValue();

        // LONG HEADERS //////////////
        boolean blockLongHeaders = longHeadersDisabledRadioButton.isSelected();
        int maxHeaderLength = (Integer) maxHeaderJSpinner.getValue();

        // NON-HTTP ALLOWED //////////
        boolean nonHttpBlocked = nonHttpDisabledRadioButton.isSelected();

        // SAVE SETTINGS ////////////
        if( !validateOnly ){
            HttpSettings httpSettings = ((HttpTransformCompoundSettings)maintenanceCompoundSettings.getHttpTransformCompoundSettings()).getHttpTransformSettings();
            httpSettings.setEnabled(isHttpEnabled);
            httpSettings.setBlockLongUris(blockLongUris);
            httpSettings.setMaxUriLength(maxUriLength);
            httpSettings.setBlockLongHeaders(blockLongHeaders);
            httpSettings.setMaxHeaderLength(maxHeaderLength);
            httpSettings.setNonHttpBlocked(nonHttpBlocked);
        }

    }

    public void doRefresh(MaintenanceCompoundSettings maintenanceCompoundSettings){
        HttpSettings httpSettings = ((HttpTransformCompoundSettings)maintenanceCompoundSettings.getHttpTransformCompoundSettings()).getHttpTransformSettings();

        // HTTP ENABLED /////////
        boolean isHttpEnabled = httpSettings.isEnabled();
        if( isHttpEnabled )
            httpEnabledRadioButton.setSelected(true);
        else
            httpDisabledRadioButton.setSelected(true);
        Util.addSettingChangeListener(settingsChangedListener, this, httpEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, httpDisabledRadioButton);

        // LONG URIS ////////////
        boolean blockLongUris = httpSettings.getBlockLongUris();
        if( blockLongUris )
            longUriDisabledRadioButton.setSelected(true);
        else
            longUriEnabledRadioButton.setSelected(true);
        Util.addSettingChangeListener(settingsChangedListener, this, longUriEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, longUriDisabledRadioButton);

        int maxUriLength = httpSettings.getMaxUriLength();
        maxUriJSpinner.setValue( (Integer) maxUriLength );
        ((JSpinner.DefaultEditor)maxUriJSpinner.getEditor()).getTextField().setBackground(Color.WHITE);
        Util.addSettingChangeListener(settingsChangedListener, this, maxUriJSpinner);


        // LONG HEADERS ////////////
        boolean blockLongHeaders = httpSettings.getBlockLongHeaders();
        if( blockLongHeaders )
            longHeadersDisabledRadioButton.setSelected(true);
        else
            longHeadersEnabledRadioButton.setSelected(true);
        Util.addSettingChangeListener(settingsChangedListener, this, longHeadersEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, longHeadersDisabledRadioButton);

        int maxHeaderLength = httpSettings.getMaxHeaderLength();
        maxHeaderJSpinner.setValue( (Integer) maxHeaderLength );
        Util.addSettingChangeListener(settingsChangedListener, this, maxHeaderJSpinner);

        // NON-HTTP BLOCKED /////////
        boolean nonHttpBlocked = httpSettings.isNonHttpBlocked();
        if( nonHttpBlocked )
            nonHttpDisabledRadioButton.setSelected(true);
        else
            nonHttpEnabledRadioButton.setSelected(true);
        Util.addSettingChangeListener(settingsChangedListener, this, nonHttpEnabledRadioButton);
        Util.addSettingChangeListener(settingsChangedListener, this, nonHttpDisabledRadioButton);
    }


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        httpButtonGroup = new javax.swing.ButtonGroup();
        longUriButtonGroup = new javax.swing.ButtonGroup();
        headerButtonGroup = new javax.swing.ButtonGroup();
        nonHttpButtonGroup = new javax.swing.ButtonGroup();
        webJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        httpEnabledRadioButton = new javax.swing.JRadioButton();
        httpDisabledRadioButton = new javax.swing.JRadioButton();
        uriJPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        longUriEnabledRadioButton = new javax.swing.JRadioButton();
        longUriDisabledRadioButton = new javax.swing.JRadioButton();
        jSeparator3 = new javax.swing.JSeparator();
        uriSpinnerJPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        maxUriJSpinner = new javax.swing.JSpinner();
        maxUriLimitsJLabel = new javax.swing.JLabel();
        headerJPanel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        longHeadersEnabledRadioButton = new javax.swing.JRadioButton();
        longHeadersDisabledRadioButton = new javax.swing.JRadioButton();
        jSeparator4 = new javax.swing.JSeparator();
        headerSpinnerJpanel = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        maxHeaderJSpinner = new javax.swing.JSpinner();
        maxHeaderLimitsJLabel = new javax.swing.JLabel();
        nonHttpJPanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        nonHttpEnabledRadioButton = new javax.swing.JRadioButton();
        nonHttpDisabledRadioButton = new javax.swing.JRadioButton();

        setLayout(new java.awt.GridBagLayout());

        setMaximumSize(new java.awt.Dimension(550, 550));
        setMinimumSize(new java.awt.Dimension(550, 550));
        setPreferredSize(new java.awt.Dimension(550, 550));
        webJPanel.setLayout(new java.awt.GridBagLayout());

        webJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Web Override", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setText("Warning:  These settings should not be changed unless instructed to do so by support.");
        webJPanel.add(jLabel1, new java.awt.GridBagConstraints());

        httpButtonGroup.add(httpEnabledRadioButton);
        httpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        httpEnabledRadioButton.setText("<html><b>Enable Processing</b> of web traffic.  (This is the default setting)</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        webJPanel.add(httpEnabledRadioButton, gridBagConstraints);

        httpButtonGroup.add(httpDisabledRadioButton);
        httpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        httpDisabledRadioButton.setText("<html><b>Disable Processing</b> of web traffic.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        webJPanel.add(httpDisabledRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(webJPanel, gridBagConstraints);

        uriJPanel.setLayout(new java.awt.GridBagLayout());

        uriJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Long URIs", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("Warning:  These settings should not be changed unless instructed to do so by support.");
        uriJPanel.add(jLabel2, new java.awt.GridBagConstraints());

        longUriButtonGroup.add(longUriEnabledRadioButton);
        longUriEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        longUriEnabledRadioButton.setText("<html><b>Enable Processing</b> of long URIs.  The traffic is considered \"Non-Http\".  (This is the default setting)</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        uriJPanel.add(longUriEnabledRadioButton, gridBagConstraints);

        longUriButtonGroup.add(longUriDisabledRadioButton);
        longUriDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        longUriDisabledRadioButton.setText("<html><b>Disable Processing</b> of long URIs.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        uriJPanel.add(longUriDisabledRadioButton, gridBagConstraints);

        jSeparator3.setForeground(new java.awt.Color(180, 180, 180));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        uriJPanel.add(jSeparator3, gridBagConstraints);

        uriSpinnerJPanel.setLayout(new java.awt.GridBagLayout());

        jLabel3.setText("Max URI Length (characters)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        uriSpinnerJPanel.add(jLabel3, gridBagConstraints);

        maxUriJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        maxUriJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        maxUriJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        maxUriJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        uriSpinnerJPanel.add(maxUriJSpinner, gridBagConstraints);

        maxUriLimitsJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        maxUriLimitsJLabel.setText("(max= min= )");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 0);
        uriSpinnerJPanel.add(maxUriLimitsJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        uriJPanel.add(uriSpinnerJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(uriJPanel, gridBagConstraints);

        headerJPanel.setLayout(new java.awt.GridBagLayout());

        headerJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Long Headers", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setText("Warning:  These settings should not be changed unless instructed to do so by support.");
        headerJPanel.add(jLabel4, new java.awt.GridBagConstraints());

        headerButtonGroup.add(longHeadersEnabledRadioButton);
        longHeadersEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        longHeadersEnabledRadioButton.setText("<html><b>Enable Processing</b> of long headers.  The traffic is considered \"Non-Http\".  (This is the default setting)</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        headerJPanel.add(longHeadersEnabledRadioButton, gridBagConstraints);

        headerButtonGroup.add(longHeadersDisabledRadioButton);
        longHeadersDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        longHeadersDisabledRadioButton.setText("<html><b>Disable Processing</b> of long headers.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        headerJPanel.add(longHeadersDisabledRadioButton, gridBagConstraints);

        jSeparator4.setForeground(new java.awt.Color(180, 180, 180));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        headerJPanel.add(jSeparator4, gridBagConstraints);

        headerSpinnerJpanel.setLayout(new java.awt.GridBagLayout());

        jLabel5.setText("Max Header Length (characters)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        headerSpinnerJpanel.add(jLabel5, gridBagConstraints);

        maxHeaderJSpinner.setFont(new java.awt.Font("Dialog", 0, 12));
        maxHeaderJSpinner.setMaximumSize(new java.awt.Dimension(100, 20));
        maxHeaderJSpinner.setMinimumSize(new java.awt.Dimension(100, 20));
        maxHeaderJSpinner.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        headerSpinnerJpanel.add(maxHeaderJSpinner, gridBagConstraints);

        maxHeaderLimitsJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        maxHeaderLimitsJLabel.setText("(max= min= )");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 0);
        headerSpinnerJpanel.add(maxHeaderLimitsJLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        headerJPanel.add(headerSpinnerJpanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(headerJPanel, gridBagConstraints);

        nonHttpJPanel.setLayout(new java.awt.GridBagLayout());

        nonHttpJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Non-Http Blocking", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 16)));
        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setText("Warning:  These settings should not be changed unless instructed to do so by support.");
        nonHttpJPanel.add(jLabel6, new java.awt.GridBagConstraints());

        nonHttpButtonGroup.add(nonHttpEnabledRadioButton);
        nonHttpEnabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        nonHttpEnabledRadioButton.setText("<html><b>Allow</b> non-Http traffic to travel over port 80.  (This is the default setting)</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        nonHttpJPanel.add(nonHttpEnabledRadioButton, gridBagConstraints);

        nonHttpButtonGroup.add(nonHttpDisabledRadioButton);
        nonHttpDisabledRadioButton.setFont(new java.awt.Font("Dialog", 0, 12));
        nonHttpDisabledRadioButton.setText("<html><b>Stop</b> non-Http traffic from traveling over port 80.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        nonHttpJPanel.add(nonHttpDisabledRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 10);
        add(nonHttpJPanel, gridBagConstraints);

    }//GEN-END:initComponents



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup headerButtonGroup;
    private javax.swing.JPanel headerJPanel;
    private javax.swing.JPanel headerSpinnerJpanel;
    private javax.swing.ButtonGroup httpButtonGroup;
    public javax.swing.JRadioButton httpDisabledRadioButton;
    public javax.swing.JRadioButton httpEnabledRadioButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    public javax.swing.JRadioButton longHeadersDisabledRadioButton;
    public javax.swing.JRadioButton longHeadersEnabledRadioButton;
    private javax.swing.ButtonGroup longUriButtonGroup;
    public javax.swing.JRadioButton longUriDisabledRadioButton;
    public javax.swing.JRadioButton longUriEnabledRadioButton;
    private javax.swing.JSpinner maxHeaderJSpinner;
    private javax.swing.JLabel maxHeaderLimitsJLabel;
    private javax.swing.JSpinner maxUriJSpinner;
    private javax.swing.JLabel maxUriLimitsJLabel;
    private javax.swing.ButtonGroup nonHttpButtonGroup;
    public javax.swing.JRadioButton nonHttpDisabledRadioButton;
    public javax.swing.JRadioButton nonHttpEnabledRadioButton;
    private javax.swing.JPanel nonHttpJPanel;
    private javax.swing.JPanel uriJPanel;
    private javax.swing.JPanel uriSpinnerJPanel;
    private javax.swing.JPanel webJPanel;
    // End of variables declaration//GEN-END:variables


}
