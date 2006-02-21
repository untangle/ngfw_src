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
import com.metavize.mvvm.networking.DynamicDNSSettings;
import java.util.TimeZone;

public class NetworkCompoundSettings implements CompoundSettings {

    // NETWORKING CONFIGURATION //
    private NetworkingConfiguration networkingConfiguration;
    public NetworkingConfiguration getNetworkingConfiguration(){ return networkingConfiguration; }

    // DYNAMIC DNS SETTINGS //
    private DynamicDNSSettings dynamicDNSSettings;
    public DynamicDNSSettings getDynamicDNSSettings(){ return dynamicDNSSettings; }

    // TIMEZONE //
    private TimeZone timeZone;
    public TimeZone getTimeZone(){ return timeZone; }
    public void setTimeZone(TimeZone tzIn){ timeZone = tzIn; };

    public void save() throws Exception {
	Util.getNetworkingManager().set(networkingConfiguration);
	Util.getNetworkManager().setDynamicDnsSettings( dynamicDNSSettings );
	Util.getAdminManager().setTimeZone(timeZone);
    }

    public void refresh() throws Exception {
	networkingConfiguration = Util.getNetworkingManager().get();
	dynamicDNSSettings = Util.getNetworkManager().getDynamicDnsSettings();
	timeZone = Util.getAdminManager().getTimeZone();
    }

    public void validate() throws Exception {

    }

}
