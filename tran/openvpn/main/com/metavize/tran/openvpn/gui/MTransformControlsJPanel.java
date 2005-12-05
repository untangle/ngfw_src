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
    private static final String EXPORTS_NAME = "Exported Hosts & Networks";
    private static final String CLIENTS_NAME = "Address Pools";
    private static final String CLIENT_TO_SITE_NAME = "VPN Clients";
    private static final String SITE_TO_SITE_NAME = "VPN Sites";
    private static final String NAME_LOG = "Event Log";

    private static final String WIZARD_SIMUL_NAME = "Wizard Simulation";
    
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

	    // EXPORTS
	    ConfigExportsJPanel configExportsJPanel = new ConfigExportsJPanel();
	    super.savableMap.put( EXPORTS_NAME, configExportsJPanel );
	    super.refreshableMap.put( EXPORTS_NAME, configExportsJPanel );
	    
	    // CLIENT TO SITE
	    ConfigClientToSiteJPanel configClientToSiteJPanel = new ConfigClientToSiteJPanel();
	    super.savableMap.put( CLIENT_TO_SITE_NAME, configClientToSiteJPanel );
	    super.refreshableMap.put( CLIENT_TO_SITE_NAME, configClientToSiteJPanel );
	    
	    // SITE TO SITE
	    ConfigSiteToSiteJPanel configSiteToSiteJPanel = new ConfigSiteToSiteJPanel();
	    super.savableMap.put( SITE_TO_SITE_NAME, configSiteToSiteJPanel );
	    super.refreshableMap.put( SITE_TO_SITE_NAME, configSiteToSiteJPanel );
	    
	    // ADDRESS GROUPS (THIS SHOULD BE AFTER CTS AND STS FOR PREVALIDATION REASONS)
	    ConfigAddressGroupsJPanel configAddressGroupsJPanel = new ConfigAddressGroupsJPanel();
	    super.savableMap.put( CLIENTS_NAME, configAddressGroupsJPanel );
	    super.refreshableMap.put( CLIENTS_NAME, configAddressGroupsJPanel );

            // DONE TO REARRANGE THE DISPLAY ORDER indepently of the SAVE ORDER
	    super.mTabbedPane.addTab( WIZARD_NAME, null, wizardJPanel );
	    super.mTabbedPane.addTab( EXPORTS_NAME, null, configExportsJPanel );
	    super.mTabbedPane.addTab( CLIENTS_NAME, null, configAddressGroupsJPanel );
	    super.mTabbedPane.addTab( CLIENT_TO_SITE_NAME, null, configClientToSiteJPanel );
	    super.mTabbedPane.addTab( SITE_TO_SITE_NAME, null, configSiteToSiteJPanel );

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
