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

import javax.swing.*;

import com.untangle.gui.transform.*;
import com.untangle.mvvm.tran.HostAddress;
import com.untangle.tran.openvpn.*;

public class MTransformControlsJPanel extends com.untangle.gui.transform.MTransformControlsJPanel{

    private static final String SETUP_NAME = "Setup";
    private static final String WIZARD_NAME = "Wizard";
    private static final String ADVANCED_NAME = "Advanced Settings";
    private static final String EXPORTS_NAME = "Exported Hosts/Networks";
    private static final String CLIENTS_AND_SITES_NAME = "VPN Clients/Sites";
    private static final String POOLS_NAME = "Address Pools";
    private static final String CLIENT_TO_SITE_NAME = "VPN Clients";
    private static final String SITE_TO_SITE_NAME = "VPN Sites";
    private static final String NAME_LOG = "Event Log";

    private static VpnTransform.ConfigState configState, lastConfigState;;
    public static VpnTransform.ConfigState getConfigState(){ return configState; }
    private VpnTransform vpnTransform;
    private static HostAddress vpnServerAddress;
    public static HostAddress getVpnServerAddress(){ return vpnServerAddress; }

    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    public void generateGui(){
        // BASE STATE
        cleanup();
        super.saveJButton.setVisible(true);
        super.reloadJButton.setVisible(true);

        if( VpnTransform.ConfigState.UNCONFIGURED.equals(configState) ){
            // SHOW WIZARD/STATUS

            // WIZARD/STATUS
            WizardJPanel wizardJPanel = new WizardJPanel( vpnTransform, this );
            addTab( WIZARD_NAME, null, wizardJPanel );
            addRefreshable( WIZARD_NAME, wizardJPanel );

            // BUTTON CONTROLS
            super.saveJButton.setVisible(false);
            super.reloadJButton.setVisible(false);
        }
        else if( VpnTransform.ConfigState.CLIENT.equals(configState)){
            // SHOW WIZARD/STATUS, AND EVENT LOG

            // WIZARD/STATUS
            WizardJPanel wizardJPanel = new WizardJPanel( vpnTransform, this );
            addTab( WIZARD_NAME, null, wizardJPanel );
            addRefreshable( WIZARD_NAME, wizardJPanel );

            // EVENT LOG ///////
            LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
            addTab(NAME_LOG, null, logJPanel);
            addShutdownable(NAME_LOG, logJPanel);
        }
        else if( VpnTransform.ConfigState.SERVER_ROUTE.equals(configState) ){
            // SHOW WIZARD/STATUS, CLIENTS, EXPORTS, CLIENT-TO-SITE, SITE-TO-SITE, AND EVENT LOG

            // WIZARD
            WizardJPanel wizardJPanel = new WizardJPanel( vpnTransform, this );
            addRefreshable( WIZARD_NAME, wizardJPanel );
            ServerAdvancedJPanel serverAdvancedJPanel = new ServerAdvancedJPanel();
            serverAdvancedJPanel.setSettingsChangedListener(this);
            addSavable( ADVANCED_NAME, serverAdvancedJPanel );
            addRefreshable( ADVANCED_NAME, serverAdvancedJPanel );

            // EXPORTS
            ConfigExportsJPanel configExportsJPanel = new ConfigExportsJPanel();
            addSavable( EXPORTS_NAME, configExportsJPanel );
            addRefreshable( EXPORTS_NAME, configExportsJPanel );
            configExportsJPanel.setSettingsChangedListener(this);

            // CLIENT TO SITE
            ConfigClientToSiteJPanel configClientToSiteJPanel = new ConfigClientToSiteJPanel();
            addSavable( CLIENT_TO_SITE_NAME, configClientToSiteJPanel );
            addRefreshable( CLIENT_TO_SITE_NAME, configClientToSiteJPanel );
            configClientToSiteJPanel.setSettingsChangedListener(this);

            // SITE TO SITE
            ConfigSiteToSiteJPanel configSiteToSiteJPanel = new ConfigSiteToSiteJPanel();
            addSavable( SITE_TO_SITE_NAME, configSiteToSiteJPanel );
            addRefreshable( SITE_TO_SITE_NAME, configSiteToSiteJPanel );
            configSiteToSiteJPanel.setSettingsChangedListener(this);

            // ADDRESS GROUPS (THIS SHOULD BE AFTER CTS AND STS FOR PREVALIDATION REASONS)
            ConfigAddressGroupsJPanel configAddressGroupsJPanel = new ConfigAddressGroupsJPanel();
            addSavable( POOLS_NAME, configAddressGroupsJPanel );
            addRefreshable( POOLS_NAME, configAddressGroupsJPanel );
            configAddressGroupsJPanel.setSettingsChangedListener(this);

            // DONE TO REARRANGE THE DISPLAY ORDER indepently of the SAVE/REFRESH ORDER
            JTabbedPane wizardJTabbedPane = addTabbedPane(SETUP_NAME, null);
            wizardJTabbedPane.addTab( WIZARD_NAME, null, wizardJPanel );
            addScrollableTab( wizardJTabbedPane, ADVANCED_NAME, null, serverAdvancedJPanel, false, true );
            addTab( EXPORTS_NAME, null, configExportsJPanel );
            JTabbedPane clientsAndSitesJTabbedPane = addTabbedPane(CLIENTS_AND_SITES_NAME, null);
            clientsAndSitesJTabbedPane.addTab( POOLS_NAME, null, configAddressGroupsJPanel );
            clientsAndSitesJTabbedPane.addTab( CLIENT_TO_SITE_NAME, null, configClientToSiteJPanel );
            clientsAndSitesJTabbedPane.addTab( SITE_TO_SITE_NAME, null, configSiteToSiteJPanel );

            // EVENT LOG ///////
            LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransform(), this);
            addTab(NAME_LOG, null, logJPanel);
            addShutdownable(NAME_LOG, logJPanel);
        }
        else if( VpnTransform.ConfigState.SERVER_BRIDGE.equals(configState) ){
            // WE DONT SUPPORT THIS
        }
        else{
            // BAD SHITE HAPPENED
        }
    }

    private void cleanup(){
        removeTab(SETUP_NAME);
        removeTab(WIZARD_NAME);
        removeRefreshable(WIZARD_NAME);
        removeSavable(ADVANCED_NAME);
        removeRefreshable(ADVANCED_NAME);

        removeTab(EXPORTS_NAME);
        removeSavable(EXPORTS_NAME);
        removeRefreshable(EXPORTS_NAME);

        removeTab(CLIENTS_AND_SITES_NAME);
        removeSavable(POOLS_NAME);
        removeRefreshable(POOLS_NAME);
        removeSavable(CLIENT_TO_SITE_NAME);
        removeRefreshable(CLIENT_TO_SITE_NAME);
        removeSavable(SITE_TO_SITE_NAME);
        removeRefreshable(SITE_TO_SITE_NAME);

        removeTab(NAME_LOG);
        removeShutdownable(NAME_LOG);
    }

    public void refreshAll() throws Exception {
        vpnTransform = (VpnTransform) mTransformJPanel.getTransform();
        configState = vpnTransform.getConfigState();
        if( lastConfigState == null )
            lastConfigState = configState;
        vpnServerAddress = vpnTransform.getVpnServerAddress();
        KeyButtonRunnable.setVpnTransform( vpnTransform );
        if( !lastConfigState.equals(configState) ){
            SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                generateGui();
            }});
        }
        super.refreshAll();
        lastConfigState = configState;
    }

}
