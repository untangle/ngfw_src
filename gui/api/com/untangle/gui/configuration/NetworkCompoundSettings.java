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

import com.untangle.gui.util.Util;
import com.untangle.gui.transform.CompoundSettings;
import com.untangle.mvvm.NetworkingConfiguration;
import com.untangle.mvvm.networking.RemoteSettings;
import com.untangle.mvvm.networking.DynamicDNSSettings;


public class NetworkCompoundSettings implements CompoundSettings {

    // NETWORKING CONFIGURATION //
    private NetworkingConfiguration networkingConfiguration;
    public NetworkingConfiguration getNetworkingConfiguration(){ return networkingConfiguration; }

    // REMOTE SETTINGS //
    private RemoteSettings remoteSettings;
    public RemoteSettings getRemoteSettings(){ return remoteSettings; }

    // DYNAMIC DNS SETTINGS //
    private DynamicDNSSettings dynamicDNSSettings;
    public DynamicDNSSettings getDynamicDNSSettings(){ return dynamicDNSSettings; }

    public void save() throws Exception {
	Util.getNetworkManager().setNetworkingConfiguration(networkingConfiguration);
	Util.getNetworkManager().setDynamicDnsSettings(dynamicDNSSettings);
	Util.getNetworkManager().setRemoteSettings(remoteSettings);
    }

    public void refresh() throws Exception {
	networkingConfiguration = Util.getNetworkManager().getNetworkingConfiguration();
	dynamicDNSSettings = Util.getNetworkManager().getDynamicDnsSettings();
	remoteSettings = Util.getNetworkManager().getRemoteSettings();
    }

    public void validate() throws Exception {
        networkingConfiguration.validate();
    }

}
