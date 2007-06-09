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

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.client.UvmRemoteContextFactory;



public class NetworkJDialog extends MConfigJDialog {

    private static final String NAME_NETWORKING_CONFIG = "Networking Config";
    private static final String NAME_NETWORK_SETTINGS  = "External Address";
    private static final String NAME_ALIAS_PANEL       = "External Address Aliases";
    private static final String NAME_HOSTNAME          = "Hostname";
    private static final String NAME_PPPOE             = "PPP Over Ethernet (PPPoE)";

    public NetworkJDialog( Frame parentFrame ) {
        super(parentFrame);
        setTitle(NAME_NETWORKING_CONFIG);
        setHelpSource("networking_config");
        compoundSettings = new NetworkCompoundSettings();
    }

    protected Dimension getMinSize(){
        return new Dimension(640, 600);
    }

    protected void generateGui(){
        // NETWORK SETTINGS //////
        NetworkIPJPanel ipJPanel = new NetworkIPJPanel(this);
        addScrollableTab(null, NAME_NETWORK_SETTINGS, null, ipJPanel, false, true);
        addSavable(NAME_NETWORK_SETTINGS, ipJPanel);
        addRefreshable(NAME_NETWORK_SETTINGS, ipJPanel);
        ipJPanel.setSettingsChangedListener(this);

        // ALIASES /////
        NetworkAliasJPanel aliasJPanel = new NetworkAliasJPanel();
        addTab(NAME_ALIAS_PANEL, null, aliasJPanel );
        addSavable(NAME_ALIAS_PANEL, aliasJPanel );
        addRefreshable(NAME_ALIAS_PANEL, aliasJPanel );
        aliasJPanel.setSettingsChangedListener(this);

        // HOSTNAME //////
        NetworkHostnameJPanel hostnameJPanel = new NetworkHostnameJPanel();
        addScrollableTab(null, NAME_HOSTNAME, null, hostnameJPanel, false, true);
        addSavable(NAME_HOSTNAME, hostnameJPanel);
        addRefreshable(NAME_HOSTNAME, hostnameJPanel);
        hostnameJPanel.setSettingsChangedListener(this);

        // PPPOE //
        NetworkPPPOEJPanel pppoeJPanel = new NetworkPPPOEJPanel();
        addScrollableTab(null, NAME_PPPOE, null, pppoeJPanel, false, true);
        addSavable(NAME_PPPOE, pppoeJPanel);
        addRefreshable(NAME_PPPOE, pppoeJPanel);
        pppoeJPanel.setSettingsChangedListener(this);

    }

    protected boolean shouldSave(){
        NetworkSaveSettingsProceedJDialog networkSaveSettingsProceedJDialog = new NetworkSaveSettingsProceedJDialog(this);
        return networkSaveSettingsProceedJDialog.isProceeding();
    }

    protected void saveAll() throws Exception {
        int previousTimeout = UvmRemoteContextFactory.factory().getTimeout();
        UvmRemoteContextFactory.factory().setTimeout(Util.RECONFIGURE_NETWORK_TIMEOUT_MILLIS);
        super.saveAll();
        UvmRemoteContextFactory.factory().setTimeout(previousTimeout);
        // UPDATE STORE
        Util.getPolicyStateMachine().updateStoreModel();
    }

}
