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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.tran.openvpn.*;

public class ServerRoutingWizard extends MWizardJDialog {

    private static final String MESSAGE_DIALOG_TITLE = "Setup Wizard Warning";
    private static final String MESSAGE_CLIENT_NOT_CONFIGURED = "You have not finished configuring OpenVPN.  Please run the Setup Wizard again.";

    private MTransformControlsJPanel mTransformControlsJPanel;

    public static ServerRoutingWizard factory(Window topLevelWindow, VpnTransform vpnTransform,
                                              MTransformControlsJPanel mTransformControlsJPanel) {
        if( topLevelWindow instanceof Frame )
            return new ServerRoutingWizard((Frame)topLevelWindow, vpnTransform, mTransformControlsJPanel);
        else if( topLevelWindow instanceof Dialog )
            return new ServerRoutingWizard((Dialog)topLevelWindow, vpnTransform, mTransformControlsJPanel);
        else
            return null;
    }

    public ServerRoutingWizard(Frame topLevelFrame, VpnTransform vpnTransform, MTransformControlsJPanel mTransformControlsJPanel) {
        super(topLevelFrame, true);
        init(mTransformControlsJPanel, vpnTransform);
    }

    public ServerRoutingWizard(Dialog topLevelDialog, VpnTransform vpnTransform, MTransformControlsJPanel mTransformControlsJPanel) {
        super(topLevelDialog, true);
        init(mTransformControlsJPanel, vpnTransform);
    }

    private void init(MTransformControlsJPanel mTransformControlsJPanel, VpnTransform vpnTransform){
        this.mTransformControlsJPanel = mTransformControlsJPanel;
        setTitle("Untangle OpenVPN Server Routing Setup Wizard");
        addWizardPageJPanel(new ServerRoutingWizardWelcomeJPanel(vpnTransform),         "1. Welcome", false, true);
        addWizardPageJPanel(new ServerRoutingWizardCertificateJPanel(vpnTransform),     "2. Generate Certificate", false, true);
        addWizardPageJPanel(new ServerRoutingWizardGroupsJPanel(vpnTransform),          "3. Add Address Pools", false, true);
        addWizardPageJPanel(new ServerRoutingWizardExportsJPanel(vpnTransform),         "4. Add Exports", false, true);
        addWizardPageJPanel(new ServerRoutingWizardClientsJPanel(vpnTransform),         "5. Add VPN Clients", false, true);
        addWizardPageJPanel(new ServerRoutingWizardSitesJPanel(vpnTransform),           "6. Add VPN Sites", false, true);
        addWizardPageJPanel(new ServerRoutingWizardCongratulationsJPanel(vpnTransform), "7. Congratulations", true, true);
    }

    protected Dimension getTitleJPanelPreferredSize(){ return new Dimension(250,360); }
    protected Dimension getContentJPanelPreferredSize(){ return new Dimension(485,360); }

    protected void wizardFinishedAbnormal(int currentPage){
        if( currentPage <= 5 ){
            MOneButtonJDialog.factory(this, "", MESSAGE_CLIENT_NOT_CONFIGURED, MESSAGE_DIALOG_TITLE, "");
            super.wizardFinishedAbnormal(currentPage);
        }
        else
            this.wizardFinishedNormal();
    }

    protected void wizardFinishedNormal(){
        super.wizardFinishedNormal();
        mTransformControlsJPanel.getInfiniteProgressJComponent().startLater("Reconfiguring...");
        mTransformControlsJPanel.refreshGui();
        mTransformControlsJPanel.getInfiniteProgressJComponent().stopLater(0l);
    }
}


