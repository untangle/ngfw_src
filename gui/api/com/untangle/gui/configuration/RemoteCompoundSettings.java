/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.configuration;

import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.util.Util;
import com.untangle.uvm.logging.LoggingSettings;
import com.untangle.uvm.networking.AccessSettings;
import com.untangle.uvm.networking.AddressSettings;
import com.untangle.uvm.security.AdminSettings;
import com.untangle.uvm.security.CertInfo;
import com.untangle.uvm.snmp.SnmpSettings;

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
