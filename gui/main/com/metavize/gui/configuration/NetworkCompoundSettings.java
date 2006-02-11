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
import java.util.TimeZone;

public class NetworkCompoundSettings implements CompoundSettings {

    // NETWORKING CONFIGURATION //
    private NetworkingConfiguration networkingConfiguration;
    public NetworkingConfiguration getNetworkingConfiguration(){ return networkingConfiguration; }

    // TIMEZONE //
    private TimeZone timeZone;
    public TimeZone getTimeZone(){ return timeZone; }
    public void setTimeZone(TimeZone tzIn){ timeZone = tzIn; };

    public void save() throws Exception {
	Util.getNetworkingManager().set(networkingConfiguration);
	Util.getAdminManager().setTimeZone(timeZone);
    }

    public void refresh() throws Exception {
	networkingConfiguration = Util.getNetworkingManager().get();
	timeZone = Util.getAdminManager().getTimeZone();
    }

    public void validate() throws Exception {

    }

}
