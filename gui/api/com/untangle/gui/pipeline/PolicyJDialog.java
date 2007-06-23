/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.pipeline;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.configuration.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.policy.*;
import com.untangle.uvm.license.LicenseStatus;
import com.untangle.uvm.license.ProductIdentifier;

import com.untangle.gui.widgets.premium.*;



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
        
        LicenseStatus status = Util.getLicenseStatus(ProductIdentifier.POLICY_MANAGER);
        String timeLeft = status.getTimeRemaining();
        if (!status.isExpired()) {
            // IF THIS IS A TRIAL, UPDATE THE TITLE BAR
            if (status.isTrial()) {
                setTitle(NAME_POLICY_MANAGER + " : " + status.getType() + " (" + timeLeft + ")");
            }
            // AVAILABLE RACKS ////// (THIS MUST BE LAST BECAUSE IT VALIDATES SETTINGS)
            PolicyAvailableJPanel policyAvailableJPanel = new PolicyAvailableJPanel();
            addTab(NAME_AVAILABLE_POLICIES, null, policyAvailableJPanel);
            addSavable(NAME_AVAILABLE_POLICIES, policyAvailableJPanel);
            addRefreshable(NAME_AVAILABLE_POLICIES, policyAvailableJPanel);
            policyAvailableJPanel.setSettingsChangedListener(this);
        }
        else {
            // IF THIS IS A TRIAL, UPDATE THE TITLE BAR
            if (status.hasLicense() && status.isTrial()) {
                setTitle(NAME_POLICY_MANAGER + " : " + status.getType() + " (" + timeLeft + ")");
            }

            PremiumJPanel policyPremiumJPanel = new PremiumJPanel();            
            addTab(NAME_AVAILABLE_POLICIES, null, policyPremiumJPanel);
        }
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
