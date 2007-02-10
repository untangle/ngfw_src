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
import com.untangle.mvvm.networking.AccessSettings;
import com.untangle.mvvm.networking.AddressSettings;
import com.untangle.mvvm.snmp.SnmpSettings;
import com.untangle.mvvm.logging.LoggingSettings;
import com.untangle.mvvm.security.CertInfo;
import com.untangle.mvvm.security.AdminSettings;

public class RemoteCompoundSettings implements CompoundSettings {

    // Access Configuration //
    private AccessSettings accessSettings;
    public AccessSettings getAccessSettings(){ return accessSettings; }

    // Access Configuration //
    private AddressSettings addressSettings;
    public AddressSettings getAddressSettings(){ return addressSettings; }

    // SNMP SETTINGS //
    private SnmpSettings snmpSettings;
    public SnmpSettings getSnmpSettings(){ return snmpSettings; }

    // ADMIN SETTINGS //
    private AdminSettings adminSettings;
    public AdminSettings getAdminSettings(){ return adminSettings; }

    // LOGGING SETTINGS //
    private LoggingSettings loggingSettings;
    public LoggingSettings getLoggingSettings(){ return loggingSettings; }

    // CERT INFO //
    private CertInfo certInfo;
    public CertInfo getCurrentCertInfo(){ return certInfo; }

    public void save() throws Exception {
        Util.getNetworkManager().setSettings(accessSettings, addressSettings);
	Util.getAdminManager().getSnmpManager().setSnmpSettings(snmpSettings);
	Util.getAdminManager().setAdminSettings(adminSettings);
	Util.getLoggingManager().setLoggingSettings(loggingSettings);
	// certInfo is not meant to be saved back out, only read in
    }

    public void refresh() throws Exception {
	accessSettings = Util.getNetworkManager().getAccessSettings();
        addressSettings = Util.getNetworkManager().getAddressSettings();
	snmpSettings = Util.getAdminManager().getSnmpManager().getSnmpSettings();
	adminSettings = Util.getAdminManager().getAdminSettings();
	loggingSettings = Util.getLoggingManager().getLoggingSettings();
	certInfo = Util.getAppServerManager().getCertInfo(Util.getAppServerManager().getCurrentServerCert());
    }

    public void validate() throws Exception {
        accessSettings.validate();
        addressSettings.validate();
    }

}
