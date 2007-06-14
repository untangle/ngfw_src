/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.openvpn.gui;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.wizard.*;
import com.untangle.node.openvpn.*;

public class ClientWizard extends MWizardJDialog {

    private static final String MESSAGE_DIALOG_TITLE = "Setup Wizard Warning";
    private static final String MESSAGE_CLIENT_NOT_CONFIGURED = "You have not finished configuring OpenVPN.  Please run the Setup Wizard again.";

    private MNodeControlsJPanel mNodeControlsJPanel;

    public static ClientWizard factory(Window topLevelWindow, VpnNode vpnNode,
                                       MNodeControlsJPanel mNodeControlsJPanel) {
        if( topLevelWindow instanceof Frame )
            return new ClientWizard((Frame)topLevelWindow, vpnNode, mNodeControlsJPanel);
        else if( topLevelWindow instanceof Dialog )
            return new ClientWizard((Dialog)topLevelWindow, vpnNode, mNodeControlsJPanel);
        else
            return null;
    }

    public ClientWizard(Frame topLevelFrame, VpnNode vpnNode, MNodeControlsJPanel mNodeControlsJPanel) {
        super(topLevelFrame, true);
        init(mNodeControlsJPanel, vpnNode);
    }

    public ClientWizard(Dialog topLevelDialog, VpnNode vpnNode, MNodeControlsJPanel mNodeControlsJPanel) {
        super(topLevelDialog, true);
        init(mNodeControlsJPanel, vpnNode);
    }

    private void init(MNodeControlsJPanel mNodeControlsJPanel, VpnNode vpnNode){
        this.mNodeControlsJPanel = mNodeControlsJPanel;
        setTitle("Untangle OpenVPN Client Setup Wizard");
        addWizardPageJPanel(new ClientWizardWelcomeJPanel(vpnNode), "1. Welcome", false, true);
        addWizardPageJPanel(new ClientWizardServerJPanel(vpnNode), "2. Download Configuration", false, true);
        addWizardPageJPanel(new ClientWizardCongratulationsJPanel(vpnNode), "3. Congratulations", false, true);
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
        mNodeControlsJPanel.getInfiniteProgressJComponent().startLater("Reconfiguring...");
        mNodeControlsJPanel.getInfiniteProgressJComponent().stopLater(3000l);
        mNodeControlsJPanel.refreshGui();
    }

}


