/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.configuration;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.CompoundSettings;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.networking.RemoteSettings;
import com.metavize.mvvm.networking.DynamicDNSSettings;


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
	Util.getNetworkingManager().set(networkingConfiguration);
	Util.getNetworkManager().setDynamicDnsSettings(dynamicDNSSettings);
	Util.getNetworkManager().setRemoteSettings(remoteSettings);
    }

    public void refresh() throws Exception {
	networkingConfiguration = Util.getNetworkingManager().get();
	dynamicDNSSettings = Util.getNetworkManager().getDynamicDnsSettings();
	remoteSettings = Util.getNetworkManager().getRemoteSettings();
    }

    public void validate() throws Exception {
        networkingConfiguration.validate();
    }

}
