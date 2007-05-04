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

import com.untangle.gui.transform.CompoundSettings;
import com.untangle.gui.util.Util;
import com.untangle.mvvm.networking.AddressSettings;
import com.untangle.mvvm.networking.BasicNetworkSettings;
import com.untangle.mvvm.networking.DynamicDNSSettings;


public class NetworkCompoundSettings implements CompoundSettings {

    // NETWORKING CONFIGURATION //
    private BasicNetworkSettings basicSettings;
    public BasicNetworkSettings getBasicSettings(){ return basicSettings; }

    // HOSTNAME CONFIGURATION //
    private AddressSettings addressSettings;
    public AddressSettings getAddressSettings(){ return addressSettings; }

    // DYNAMIC DNS SETTINGS //
    private DynamicDNSSettings dynamicDNSSettings;
    public DynamicDNSSettings getDynamicDNSSettings(){ return dynamicDNSSettings; }

    public void save() throws Exception {
        Util.getNetworkManager().setDynamicDnsSettings(dynamicDNSSettings);
        Util.getNetworkManager().setSettings(basicSettings,addressSettings);
    }

    public void refresh() throws Exception {
        basicSettings = Util.getNetworkManager().getBasicSettings();
        dynamicDNSSettings = Util.getNetworkManager().getDynamicDnsSettings();
        addressSettings = Util.getNetworkManager().getAddressSettings();
    }

    public void validate() throws Exception {
        addressSettings.validate();
    }

}
