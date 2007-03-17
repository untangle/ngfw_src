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

package com.untangle.gui.pipeline;

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.mvvm.policy.*;
import com.untangle.gui.configuration.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class PolicyJDialog extends MConfigJDialog {

    private static final String NAME_POLICY_MANAGER     = "Policy Manager";
    private static final String NAME_POLICIES           = "Policy-To-Rack Map";
    private static final String NAME_SYSTEM_POLICIES    = "Default Policies";
    private static final String NAME_USER_POLICIES      = "Custom Policies";
    private static final String NAME_AVAILABLE_POLICIES = "Rack List";
    
    public PolicyJDialog( Frame parentFrame ) {
	super(parentFrame);
        setTitle(NAME_POLICY_MANAGER);
        setHelpSource("policy_manager");
	compoundSettings = new PolicyCompoundSettings();
    }

    protected Dimension getMinSize(){
	return new Dimension(700, 480);
    }
    
    protected void generateGui(){        
        // POLICIES //////
	JTabbedPane policiesJTabbedPane = addTabbedPane(NAME_POLICIES, null);

        // SYSTEM POLICIES //////
        PolicyDefaultJPanel policyDefaultJPanel = new PolicyDefaultJPanel();
        policiesJTabbedPane.addTab(NAME_SYSTEM_POLICIES, null, policyDefaultJPanel);
	addSavable(NAME_SYSTEM_POLICIES, policyDefaultJPanel);
	addRefreshable(NAME_SYSTEM_POLICIES, policyDefaultJPanel);
	policyDefaultJPanel.setSettingsChangedListener(this);

        // USER POLICIES //////
        PolicyCustomJPanel policyCustomJPanel = new PolicyCustomJPanel(this);
        policiesJTabbedPane.addTab(NAME_USER_POLICIES, null, policyCustomJPanel);
	addSavable(NAME_USER_POLICIES, policyCustomJPanel);
	addRefreshable(NAME_USER_POLICIES, policyCustomJPanel);
	policyCustomJPanel.setSettingsChangedListener(this);

        // AVAILABLE RACKS ////// (THIS MUST BE LAST BECAUSE IT VALIDATES SETTINGS)
        PolicyAvailableJPanel policyAvailableJPanel = new PolicyAvailableJPanel();
        addTab(NAME_AVAILABLE_POLICIES, null, policyAvailableJPanel);
	addSavable(NAME_AVAILABLE_POLICIES, policyAvailableJPanel);
	addRefreshable(NAME_AVAILABLE_POLICIES, policyAvailableJPanel);
	policyAvailableJPanel.setSettingsChangedListener(this);
    }
    
    protected void saveAll() throws Exception{
	// ASK THE USER IF HE REALLY WANTS TO SAVE SETTINGS ////////
        NetworkSaveSettingsProceedJDialog saveSettingsProceedJDialog = new NetworkSaveSettingsProceedJDialog(this);
        boolean isProceeding = saveSettingsProceedJDialog.isProceeding();
        if( isProceeding ){
            super.saveAll();
        }
    }

}
