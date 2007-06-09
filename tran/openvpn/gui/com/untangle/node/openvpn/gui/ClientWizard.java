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

package com.untangle.tran.openvpn.gui;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.tran.openvpn.*;

public class ClientWizard extends MWizardJDialog {

    private static final String MESSAGE_DIALOG_TITLE = "Setup Wizard Warning";
    private static final String MESSAGE_CLIENT_NOT_CONFIGURED = "You have not finished configuring OpenVPN.  Please run the Setup Wizard again.";

    private MTransformControlsJPanel mTransformControlsJPanel;

    public static ClientWizard factory(Window topLevelWindow, VpnTransform vpnTransform,
                                       MTransformControlsJPanel mTransformControlsJPanel) {
        if( topLevelWindow instanceof Frame )
            return new ClientWizard((Frame)topLevelWindow, vpnTransform, mTransformControlsJPanel);
        else if( topLevelWindow instanceof Dialog )
            return new ClientWizard((Dialog)topLevelWindow, vpnTransform, mTransformControlsJPanel);
        else
            return null;
    }

    public ClientWizard(Frame topLevelFrame, VpnTransform vpnTransform, MTransformControlsJPanel mTransformControlsJPanel) {
        super(topLevelFrame, true);
        init(mTransformControlsJPanel, vpnTransform);
    }

    public ClientWizard(Dialog topLevelDialog, VpnTransform vpnTransform, MTransformControlsJPanel mTransformControlsJPanel) {
        super(topLevelDialog, true);
        init(mTransformControlsJPanel, vpnTransform);
    }

    private void init(MTransformControlsJPanel mTransformControlsJPanel, VpnTransform vpnTransform){
        this.mTransformControlsJPanel = mTransformControlsJPanel;
        setTitle("Untangle OpenVPN Client Setup Wizard");
        addWizardPageJPanel(new ClientWizardWelcomeJPanel(vpnTransform), "1. Welcome", false, true);
        addWizardPageJPanel(new ClientWizardServerJPanel(vpnTransform), "2. Download Configuration", false, true);
        addWizardPageJPanel(new ClientWizardCongratulationsJPanel(vpnTransform), "3. Congratulations", false, true);
    }

    protected void wizardFinishedAbnormal(int currentPage){
        if( currentPage <= 1 ){
            MOneButtonJDialog.factory(this, "", MESSAGE_CLIENT_NOT_CONFIGURED, MESSAGE_DIALOG_TITLE, "");
            super.wizardFinishedAbnormal(currentPage);
        }
        else
            this.wizardFinishedNormal();
    }

    protected void wizardFinishedNormal(){
        super.wizardFinishedNormal();
        mTransformControlsJPanel.getInfiniteProgressJComponent().startLater("Reconfiguring...");
        mTransformControlsJPanel.getInfiniteProgressJComponent().stopLater(3000l);
        mTransformControlsJPanel.refreshGui();
    }

}


