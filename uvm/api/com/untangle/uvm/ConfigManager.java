/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.DhcpStaticEntry;

import java.util.List;
import org.json.JSONObject;

/**
 * Manager for Configuration
 */
public interface ConfigManager
{
    Object getSystemInfo();

    Object getHostName();
    Object setHostName(String argName);

    Object getDomainName();
    Object setDomainName(String argName);

    Object doFactoryReset();

    Object doFirmwareUpdate();

    Object checkFirmwareUpdate();

    Object getDeviceTime();
    Object setDeviceTime(String argTime, String argZone);

    Object setAdminCredentials(String argUsername, String argPassword);

    Object getNetworkInterfaces();
    Object setNetworkInterfaces(List<InterfaceSettings> faceList);

    Object getSyslogServer();
    Object setSyslogServer(boolean argEnabled, String argHost, int argPort, String argProtocol);

    List<InterfaceMetrics> getNetworkPortStats();

    SnmpSettings getSnmpSettings();
    Object setSnmpSettings(SnmpSettings argSettings);

    Object createSystemBackup();

    Object restoreSystemBackup(String argFileName, String maintainRegex);

    Object createDiagnosticDump();

    Object getConnectedClients();

    Object getEnabledFeatures();
    Object refreshEnabledFeatures();

    Object getTimeZones();

    Object getStaticDhcpReservations();
    Object setStaticDhcpReservations(List<DhcpStaticEntry> argList);
    Object getActiveDhcpLeases();
}
