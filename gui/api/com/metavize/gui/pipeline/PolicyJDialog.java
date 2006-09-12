/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.pipeline;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.mvvm.policy.*;
import com.metavize.gui.configuration.*;

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

        // USER POLICIES //////
        PolicyCustomJPanel policyCustomJPanel = new PolicyCustomJPanel();
        policiesJTabbedPane.addTab(NAME_USER_POLICIES, null, policyCustomJPanel);
	addSavable(NAME_USER_POLICIES, policyCustomJPanel);
	addRefreshable(NAME_USER_POLICIES, policyCustomJPanel);

        // AVAILABLE RACKS ////// (THIS MUST BE LAST BECAUSE IT VALIDATES SETTINGS)
        PolicyAvailableJPanel policyAvailableJPanel = new PolicyAvailableJPanel();
        addTab(NAME_AVAILABLE_POLICIES, null, policyAvailableJPanel);
	addSavable(NAME_AVAILABLE_POLICIES, policyAvailableJPanel);
	addRefreshable(NAME_AVAILABLE_POLICIES, policyAvailableJPanel);
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
