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

    private static final String NAME_POLICY_MANAGER = "Policy Manager";
    private static final String NAME_SYSTEM_POLICIES = "Default Policies";
    private static final String NAME_USER_POLICIES = "Custom Policies";
    private static final String NAME_AVAILABLE_POLICIES = "Available Racks";
    
    public PolicyJDialog( ) {
    }

    protected Dimension getMinSize(){
	return new Dimension(700, 480);
    }
    
    protected void generateGui(){
        this.setTitle(NAME_POLICY_MANAGER);
        
        // SYSTEM POLICIES //////
        DefaultPolicyJPanel defaultPolicyJPanel = new DefaultPolicyJPanel();
        addTab(NAME_SYSTEM_POLICIES, null, defaultPolicyJPanel);
	addSavable(NAME_SYSTEM_POLICIES, defaultPolicyJPanel);
	addRefreshable(NAME_SYSTEM_POLICIES, defaultPolicyJPanel);

        // USER POLICIES //////
        CustomPolicyJPanel customPolicyJPanel = new CustomPolicyJPanel();
        addTab(NAME_USER_POLICIES, null, customPolicyJPanel);
	addSavable(NAME_USER_POLICIES, customPolicyJPanel);
	addRefreshable(NAME_USER_POLICIES, customPolicyJPanel);

        // AVAILABLE RACKS ////// (THIS MUST BE LAST BECAUSE IT VALIDATES SETTINGS)
        AvailablePolicyJPanel availablePolicyJPanel = new AvailablePolicyJPanel();
        addTab(NAME_AVAILABLE_POLICIES, null, availablePolicyJPanel);
	addSavable(NAME_AVAILABLE_POLICIES, availablePolicyJPanel);
	addRefreshable(NAME_AVAILABLE_POLICIES, availablePolicyJPanel);
    }
    
    protected void sendSettings(Object settings) throws Exception {
	Util.getPolicyManager().setPolicyConfiguration( (PolicyConfiguration) settings );
    }
    
    protected void refreshSettings(){
	settings = Util.getPolicyManager().getPolicyConfiguration();
    }

    protected void saveAll(){
	// ASK THE USER IF HE REALLY WANTS TO SAVE SETTINGS ////////
        SaveSettingsProceedJDialog saveSettingsProceedJDialog = new SaveSettingsProceedJDialog();
        boolean isProceeding = saveSettingsProceedJDialog.isProceeding();
        if( isProceeding ){
            super.saveAll();
        }
    }

}
