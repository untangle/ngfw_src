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

package com.metavize.tran.openvpn.gui;

import com.metavize.gui.widgets.wizard.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.util.*;
import com.metavize.gui.transform.*;

import com.metavize.tran.openvpn.*;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Frame;

public class ServerRoutingWizard extends MWizardJDialog {
    
    private static final String MESSAGE_DIALOG_TITLE = "Setup Wizard Warning";
    private static final String MESSAGE_CLIENT_NOT_CONFIGURED = "You have not finished configuring OpenVPN.  Please run the Setup Wizard again.";

    private MTransformControlsJPanel mTransformControlsJPanel;

    public ServerRoutingWizard(Frame topLevelFrame, boolean isModal, VpnTransform vpnTransform, MTransformControlsJPanel mTransformControlsJPanel) {
        super(topLevelFrame, isModal);
	this.mTransformControlsJPanel = mTransformControlsJPanel;
        setTitle("Metavize OpenVPN Server Routing Setup Wizard");
        addWizardPageJPanel(new ServerRoutingWizardWelcomeJPanel(vpnTransform), "1. Welcome", false, true);
        addWizardPageJPanel(new ServerRoutingWizardCertificateJPanel(vpnTransform), "2. Generate Certificate", false, true);
        addWizardPageJPanel(new ServerRoutingWizardGroupsJPanel(vpnTransform), "3. Add Address Groups", false, true);
        addWizardPageJPanel(new ServerRoutingWizardExportsJPanel(vpnTransform), "4. Export Hosts", false, true);
        addWizardPageJPanel(new ServerRoutingWizardClientsJPanel(vpnTransform), "5. List Clients", false, true);
        addWizardPageJPanel(new ServerRoutingWizardSitesJPanel(vpnTransform), "6. List Sites", false, true);
        addWizardPageJPanel(new ServerRoutingWizardCongratulationsJPanel(vpnTransform), "7. Congratulations", false, true);
    }
    
    protected void wizardFinishedAbnormal(int currentPage){
	new MOneButtonJDialog(this, MESSAGE_DIALOG_TITLE, MESSAGE_CLIENT_NOT_CONFIGURED);
	super.wizardFinishedAbnormal(currentPage);
    }

    protected void wizardFinishedNormal(){
	super.wizardFinishedNormal();
	try{
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		mTransformControlsJPanel.generateGui();
	    }});
	}
	catch(Exception e){ Util.handleExceptionNoRestart("Error updating panel assortment", e); }
    }    
}


