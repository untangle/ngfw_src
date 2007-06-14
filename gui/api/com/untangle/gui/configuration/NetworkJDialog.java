/*
 * $HeadURL:$
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
