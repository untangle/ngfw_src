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
        super.mTabbedPane.addTab( GROUP_PANEL_NAME, null, groupJPanel );
        super.mTabbedPane.addTab( CLIENT_PANEL_NAME, null, clientJPanel );
        
	//super.savableMap.put(NAME_SOME_LIST, someJPanel);
	//super.refreshableMap.put(NAME_SOME_LIST, someJPanel);

        // EVENT LOG ///////
        //LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
        //super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
	//super.shutdownableMap.put(NAME_LOG, logJPanel);
    }

    
}
