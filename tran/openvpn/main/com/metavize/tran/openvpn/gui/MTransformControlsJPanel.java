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

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String WIZARD_NAME = "Setup Wizard & Status";
    private static final String EXPORTS_NAME = "Exported Hosts/Networks";
    private static final String CLIENTS_NAME = "Client Groups";
    private static final String CLIENT_TO_SITE_NAME = "Client to Site List";
    private static final String SITE_TO_SITE_NAME = "Site to Site List";
    private static final String NAME_LOG = "Event Log";

    private static final String CERTIFICATE_PANEL_NAME = "Certificate Management";
    private static final String NET_SETTINGS_1_PANEL_NAME = "Network Settings, Part 1";
    private static final String NET_SETTINGS_2_PANEL_NAME = "Network Settings, Part 2";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){

	TransformContext transformContext = mTransformJPanel.getTransformContext();
	VpnTransform vpnTransform = (VpnTransform) transformContext.transform();
	VpnTransform.ConfigState configState = vpnTransform.getConfigState();

	super.mTabbedPane.removeAll();
	if( VpnTransform.ConfigState.UNCONFIGURED == configState ){
	    // SHOW WIZARD/STATUS

	    // WIZARD/STATUS
	    WizardJPanel wizardJPanel = new WizardJPanel( vpnTransform, this );
	    super.mTabbedPane.addTab( WIZARD_NAME, null, wizardJPanel );
	}
	else if( VpnTransform.ConfigState.CLIENT == configState ){
	    // SHOW WIZARD/STATUS, AND EVENT LOG

	    // WIZARD/STATUS
	    WizardJPanel wizardJPanel = new WizardJPanel( vpnTransform, this );
	    super.mTabbedPane.addTab( WIZARD_NAME, null, wizardJPanel );

	    // EVENT LOG ///////
	    //LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	    //super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
	    //super.shutdownableMap.put(NAME_LOG, logJPanel);	    
	}
	else if( VpnTransform.ConfigState.SERVER_ROUTE == configState ){
	    // SHOW WIZARD/STATUS, CLIENTS, EXPORTS, CLIENT-TO-SITE, SITE-TO-SITE, AND EVENT LOG

	    // WIZARD/STATUS
	    WizardJPanel wizardJPanel = new WizardJPanel( vpnTransform, this );
	    super.mTabbedPane.addTab( WIZARD_NAME, null, wizardJPanel );

	    // EXPORTS
	    ExportsConfigJPanel exportsConfigJPanel = new ExportsConfigJPanel();
	    super.savableMap.put( EXPORTS_NAME, exportsConfigJPanel );
	    super.refreshableMap.put( EXPORTS_NAME, exportsConfigJPanel );
	    super.mTabbedPane.addTab( EXPORTS_NAME, null, exportsConfigJPanel );
	    
	    // CLIENT TO SITE
	    ClientToSiteConfigJPanel clientToSiteConfigJPanel = new ClientToSiteConfigJPanel();
	    super.savableMap.put( CLIENT_TO_SITE_NAME, clientToSiteConfigJPanel );
	    super.refreshableMap.put( CLIENT_TO_SITE_NAME, clientToSiteConfigJPanel );
	    super.mTabbedPane.addTab( CLIENT_TO_SITE_NAME, null, clientToSiteConfigJPanel );
	    
	    // SITE TO SITE
	    SiteToSiteConfigJPanel siteToSiteConfigJPanel = new SiteToSiteConfigJPanel();
	    super.savableMap.put( SITE_TO_SITE_NAME, siteToSiteConfigJPanel );
	    super.refreshableMap.put( SITE_TO_SITE_NAME, siteToSiteConfigJPanel );
	    super.mTabbedPane.addTab( SITE_TO_SITE_NAME, null, siteToSiteConfigJPanel );
	    
	    // CLIENTS (THIS SHOULD BE AFTER CTS AND STS FOR PREVALIDATION REASONS)
	    ClientsConfigJPanel clientsConfigJPanel = new ClientsConfigJPanel();
	    super.savableMap.put( CLIENTS_NAME, clientsConfigJPanel );
	    super.refreshableMap.put( CLIENTS_NAME, clientsConfigJPanel );
	    super.mTabbedPane.addTab( CLIENTS_NAME, null, clientsConfigJPanel );

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
	
	RobertsJPanel robertsJPanel       = new RobertsJPanel( transformContext );
	NetSettingsOneJPanel netOneJPanel = new NetSettingsOneJPanel( transformContext );
	NetSettingsTwoJPanel netTwoJPanel = new NetSettingsTwoJPanel( transformContext );
	super.mTabbedPane.addTab( CERTIFICATE_PANEL_NAME, null, robertsJPanel );
	super.mTabbedPane.addTab( NET_SETTINGS_1_PANEL_NAME, null, netOneJPanel );
	super.mTabbedPane.addTab( NET_SETTINGS_2_PANEL_NAME, null, netTwoJPanel );

	    // EXPORTS
	    ExportsConfigJPanel exportsConfigJPanel = new ExportsConfigJPanel();
	    super.savableMap.put( EXPORTS_NAME, exportsConfigJPanel );
	    super.refreshableMap.put( EXPORTS_NAME, exportsConfigJPanel );
	    super.mTabbedPane.addTab( EXPORTS_NAME, null, exportsConfigJPanel );
	    
	    // CLIENT TO SITE
	    ClientToSiteConfigJPanel clientToSiteConfigJPanel = new ClientToSiteConfigJPanel();
	    super.savableMap.put( CLIENT_TO_SITE_NAME, clientToSiteConfigJPanel );
	    super.refreshableMap.put( CLIENT_TO_SITE_NAME, clientToSiteConfigJPanel );
	    super.mTabbedPane.addTab( CLIENT_TO_SITE_NAME, null, clientToSiteConfigJPanel );
	    
	    // SITE TO SITE
	    SiteToSiteConfigJPanel siteToSiteConfigJPanel = new SiteToSiteConfigJPanel();
	    super.savableMap.put( SITE_TO_SITE_NAME, siteToSiteConfigJPanel );
	    super.refreshableMap.put( SITE_TO_SITE_NAME, siteToSiteConfigJPanel );
	    super.mTabbedPane.addTab( SITE_TO_SITE_NAME, null, siteToSiteConfigJPanel );
	    
	    // CLIENTS (THIS SHOULD BE AFTER CTS AND STS FOR PREVALIDATION REASONS)
	    ClientsConfigJPanel clientsConfigJPanel = new ClientsConfigJPanel();
	    super.savableMap.put( CLIENTS_NAME, clientsConfigJPanel );
	    super.refreshableMap.put( CLIENTS_NAME, clientsConfigJPanel );
	    super.mTabbedPane.addTab( CLIENTS_NAME, null, clientsConfigJPanel );

    }

    
}
