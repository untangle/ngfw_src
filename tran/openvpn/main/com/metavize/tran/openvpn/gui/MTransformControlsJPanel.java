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

import com.metavize.mvvm.tran.TransformContext;

import com.metavize.gui.util.Util;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;


public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String EXPORTS_NAME = "Exported Hosts/Networks";
    private static final String CLIENTS_NAME = "Client Groups";
    private static final String CLIENT_TO_SITE_NAME = "Client to Site List";
    private static final String SITE_TO_SITE_NAME = "Site to Site List";
    private static final String CERTIFICATE_PANEL_NAME = "Certificate Management";
    private static final String NET_SETTINGS_1_PANEL_NAME = "Network Settings, Part 1";
    private static final String NET_SETTINGS_2_PANEL_NAME = "Network Settings, Part 2";
    private static final String GROUP_PANEL_NAME  = "Address groups";
    private static final String CLIENT_PANEL_NAME = "Clients";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){

	// EXPORTS
	ExportsConfigJPanel exportsConfigJPanel = new ExportsConfigJPanel();
	super.savableMap.put( EXPORTS_NAME, exportsConfigJPanel );
	super.refreshableMap.put( EXPORTS_NAME, exportsConfigJPanel );
	super.mTabbedPane.addTab( EXPORTS_NAME, null, exportsConfigJPanel );

	// CLIENTS
	ClientsConfigJPanel clientsConfigJPanel = new ClientsConfigJPanel();
	super.savableMap.put( CLIENTS_NAME, clientsConfigJPanel );
	super.refreshableMap.put( CLIENTS_NAME, clientsConfigJPanel );
	super.mTabbedPane.addTab( CLIENTS_NAME, null, clientsConfigJPanel );

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

	// SOME LIST /////
        TransformContext transformContext = mTransformJPanel.getTransformContext();

	RobertsJPanel robertsJPanel       = new RobertsJPanel( transformContext );
        NetSettingsOneJPanel netOneJPanel = new NetSettingsOneJPanel( transformContext );
        NetSettingsTwoJPanel netTwoJPanel = new NetSettingsTwoJPanel( transformContext );
        GroupJPanel groupJPanel           = new GroupJPanel( transformContext );
        ClientJPanel clientJPanel         = new ClientJPanel( transformContext );
        
        super.mTabbedPane.addTab( CERTIFICATE_PANEL_NAME, null, robertsJPanel );
        super.mTabbedPane.addTab( NET_SETTINGS_1_PANEL_NAME, null, netOneJPanel );
        super.mTabbedPane.addTab( NET_SETTINGS_2_PANEL_NAME, null, netTwoJPanel );
        //super.mTabbedPane.addTab( GROUP_PANEL_NAME, null, groupJPanel );
        //super.mTabbedPane.addTab( CLIENT_PANEL_NAME, null, clientJPanel );
        
	//super.savableMap.put(NAME_SOME_LIST, someJPanel);
	//super.refreshableMap.put(NAME_SOME_LIST, someJPanel);

        // EVENT LOG ///////
        //LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
        //super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
	//super.shutdownableMap.put(NAME_LOG, logJPanel);
    }

    
}
