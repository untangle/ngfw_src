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

package com.untangle.gui.configuration;

import com.untangle.gui.transform.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.NetworkingConfiguration;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class MaintenanceJDialog extends MConfigJDialog {

    private static final String NAME_MAINTENANCE_CONFIG = "Support Config";
    private static final String NAME_REMOTE_SETTINGS    = "Access Restrictions";
    private static final String NAME_PROTOCOL_OVERRIDE  = "Manual Protocol Override";
    private static final String NAME_NETWORK_INTERFACES = "Network Interfaces";
    private static final String NAME_SECRET_PANEL       = "Advanced Support";
    private static final String NAME_PING_TEST          = "Ping Test";

    private static boolean showHiddenPanel;
    public static void setShowHiddenPanel(boolean showHiddenPanelX){ showHiddenPanel = showHiddenPanelX; }

    public MaintenanceJDialog( Frame parentFrame ) {
	super(parentFrame);
        setTitle(NAME_MAINTENANCE_CONFIG);
	compoundSettings = new MaintenanceCompoundSettings();
    }

    protected void generateGui(){
        // GENERAL SETTINGS //////
        MaintenanceAccessJPanel maintenanceAccessJPanel = new MaintenanceAccessJPanel();
	addScrollableTab(null, NAME_REMOTE_SETTINGS, null, maintenanceAccessJPanel, false, true);
	addSavable(NAME_REMOTE_SETTINGS, maintenanceAccessJPanel);
	addRefreshable(NAME_REMOTE_SETTINGS, maintenanceAccessJPanel);

	// CASINGS //
        MCasingJPanel[] mCasingJPanels = ((MaintenanceCompoundSettings)compoundSettings).getCasingJPanels();
	if( mCasingJPanels.length > 0 ){
	    JTabbedPane overrideJTabbedPane = addTabbedPane(NAME_PROTOCOL_OVERRIDE, null);
	    for(MCasingJPanel mCasingJPanel : mCasingJPanels){
		String casingDisplayName = mCasingJPanel.getDisplayName();
		addScrollableTab(overrideJTabbedPane, casingDisplayName, null, mCasingJPanel, false, true);
		addSavable(casingDisplayName, mCasingJPanel);
		addRefreshable(casingDisplayName, mCasingJPanel);
	    }
	}
	else {
            JPanel messageJPanel = new JPanel();
            messageJPanel.setLayout(new BorderLayout());
            JLabel messageJLabel = new JLabel("There are currently no protocols being used by the rack.");
            messageJLabel.setHorizontalAlignment(SwingConstants.CENTER);
            messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
            messageJPanel.add(messageJLabel);
            addTab(NAME_PROTOCOL_OVERRIDE, null, messageJPanel);
        }

	// NETWORK INTERFACES //////
	MaintenanceInterfaceJPanel maintenanceInterfaceJPanel = new MaintenanceInterfaceJPanel();
	addTab(NAME_NETWORK_INTERFACES, null, maintenanceInterfaceJPanel);
	addSavable(NAME_NETWORK_INTERFACES, maintenanceInterfaceJPanel);
	addRefreshable(NAME_NETWORK_INTERFACES, maintenanceInterfaceJPanel);

    // PING TEST //
    MaintenancePingJPanel maintenancePingJPanel = new MaintenancePingJPanel();
    addTab(NAME_PING_TEST, null, maintenancePingJPanel);

	// SECRET HIDDEN PANEL //////
	if( showHiddenPanel ){
	    MaintenanceSecretJPanel maintenanceSecretJPanel = new MaintenanceSecretJPanel();
	    addTab(NAME_SECRET_PANEL, null, maintenanceSecretJPanel);
	    addSavable(NAME_SECRET_PANEL, maintenanceSecretJPanel);
	    addRefreshable(NAME_SECRET_PANEL, maintenanceSecretJPanel);
	}
    }


    protected boolean shouldSave(){
        NetworkSaveSettingsProceedJDialog networkSaveSettingsProceedJDialog = new NetworkSaveSettingsProceedJDialog(this);
        return networkSaveSettingsProceedJDialog.isProceeding();	
    }


}
