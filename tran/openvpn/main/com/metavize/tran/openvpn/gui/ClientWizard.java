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
import java.awt.Color;
import java.awt.Frame;

public class ClientWizard extends MWizardJDialog {
    
    private static final String MESSAGE_DIALOG_TITLE = "Setup Wizard Warning";
    private static final String MESSAGE_CLIENT_NOT_CONFIGURED = "You have not finished configuring OpenVPN.  Please run the Setup Wizard again.";

    private MTransformControlsJPanel mTransformControlsJPanel;

    public ClientWizard(Frame topLevelFrame, boolean isModal, VpnTransform vpnTransform, MTransformControlsJPanel mTransformControlsJPanel) {
        super(topLevelFrame, isModal);
	this.mTransformControlsJPanel = mTransformControlsJPanel;
        setTitle("Metavize OpenVPN Client Setup Wizard");
        addWizardPageJPanel(new ClientWizardWelcomeJPanel(vpnTransform), "1. Welcome", false, true);
        addWizardPageJPanel(new ClientWizardServerJPanel(vpnTransform), "2. Connect to Server", false, true);
        addWizardPageJPanel(new ClientWizardCongratulationsJPanel(vpnTransform), "3. Congratulations", false, true);
    }
    
    protected void wizardFinishedAbnormal(int currentPage){
	new MOneButtonJDialog(this, MESSAGE_DIALOG_TITLE, MESSAGE_CLIENT_NOT_CONFIGURED);
	super.wizardFinishedAbnormal(currentPage);
    }

    protected void wizardFinishedNormal(){
	super.wizardFinishedNormal();
	mTransformControlsJPanel.generateGui();
    }    
}


