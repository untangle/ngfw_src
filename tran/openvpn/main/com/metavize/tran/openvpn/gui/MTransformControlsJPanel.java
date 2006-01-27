/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.openvpn.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;

import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.openvpn.*;

import javax.swing.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String WIZARD_NAME = "Status & Wizard";
    private static final String EXPORTS_NAME = "Exported Hosts & Networks";
    private static final String CLIENTS_AND_SITES_NAME = "VPN Clients & Sites";
    private static final String POOLS_NAME = "Address Pools";
    private static final String CLIENT_TO_SITE_NAME = "VPN Clients";
    private static final String SITE_TO_SITE_NAME = "VPN Sites";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }
    
    void refreshGui(){
	reloadJButton.doClick();
    }
    
    protected void generateGui(){

	TransformContext transformContext = mTransformJPanel.getTransformContext();
	VpnTransform vpnTransform = (VpnTransform) transformContext.transform();
	VpnTransform.ConfigState configState = vpnTransform.getConfigState();

	KeyButtonRunnable.setVpnTransform( vpnTransform );

	// BASE STATE
	removeAllTabs();
	super.saveJButton.setVisible(true);
	super.reloadJButton.setVisible(true);

	if( VpnTransform.ConfigState.UNCONFIGURED == configState ){
	    // SHOW WIZARD/STATUS

	    // WIZARD/STATUS
	    WizardJPanel wizardJPanel = new WizardJPanel( vpnTransform, this );
	    addTab( WIZARD_NAME, null, wizardJPanel );

	    // BUTTON CONTROLS
	    super.saveJButton.setVisible(false);
	    super.reloadJButton.setVisible(false);	
	}
	else if( VpnTransform.ConfigState.CLIENT == configState ){
	    // SHOW WIZARD/STATUS, AND EVENT LOG

	    // WIZARD/STATUS
	    WizardJPanel wizardJPanel = new WizardJPanel( vpnTransform, this );
	    addTab( WIZARD_NAME, null, wizardJPanel );

	    // EVENT LOG ///////
	    //LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	    //super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
	    //super.shutdownableMap.put(NAME_LOG, logJPanel);	    
	}
	else if( VpnTransform.ConfigState.SERVER_ROUTE == configState ){
	    // SHOW WIZARD/STATUS, CLIENTS, EXPORTS, CLIENT-TO-SITE, SITE-TO-SITE, AND EVENT LOG

	    // WIZARD/STATUS
	    WizardJPanel wizardJPanel = new WizardJPanel( vpnTransform, this );

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

            // DONE TO REARRANGE THE DISPLAY ORDER indepently of the SAVE ORDER
	    addTab( WIZARD_NAME, null, wizardJPanel );
	    addTab( EXPORTS_NAME, null, configExportsJPanel );
	    JTabbedPane clientsAndSitesJTabbedPane = addTabbedPane(CLIENTS_AND_SITES_NAME, null);
	    clientsAndSitesJTabbedPane.addTab( POOLS_NAME, null, configAddressGroupsJPanel );
	    clientsAndSitesJTabbedPane.addTab( CLIENT_TO_SITE_NAME, null, configClientToSiteJPanel );
	    clientsAndSitesJTabbedPane.addTab( SITE_TO_SITE_NAME, null, configSiteToSiteJPanel );

	    // EVENT LOG ///////
	    //LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	    //super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
	    //super.shutdownableMap.put(NAME_LOG, logJPanel);	    
	}
	else if( VpnTransform.ConfigState.SERVER_BRIDGE == configState ){
	    // WE DONT SUPPORT THIS
	}
	else{
	    // BAD SHITE HAPPENED
	}
	

    }

    
}
