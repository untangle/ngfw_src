/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
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
import com.metavize.mvvm.snmp.SnmpSettings;
import com.metavize.mvvm.logging.LoggingSettings;
import com.metavize.mvvm.security.CertInfo;
import com.metavize.mvvm.security.AdminSettings;

public class RemoteCompoundSettings implements CompoundSettings {

    // NETWORKING CONFIGURATION //
    private NetworkingConfiguration networkingConfiguration;
    public NetworkingConfiguration getNetworkingConfiguration(){ return networkingConfiguration; }

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
	Util.getNetworkManager().setNetworkingConfiguration(networkingConfiguration);
	Util.getAdminManager().getSnmpManager().setSnmpSettings(snmpSettings);
	Util.getAdminManager().setAdminSettings(adminSettings);
	Util.getLoggingManager().setLoggingSettings(loggingSettings);
	// certInfo is not meant to be saved back out, only read in
    }

    public void refresh() throws Exception {
	networkingConfiguration = Util.getNetworkManager().getNetworkingConfiguration();
	snmpSettings = Util.getAdminManager().getSnmpManager().getSnmpSettings();
	adminSettings = Util.getAdminManager().getAdminSettings();
	loggingSettings = Util.getLoggingManager().getLoggingSettings();
	certInfo = Util.getAppServerManager().getCertInfo(Util.getAppServerManager().getCurrentServerCert());
    }

    public void validate() throws Exception {

    }

}
