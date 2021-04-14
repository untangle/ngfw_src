/**
 * $Id$
 */

package com.untangle.uvm;

import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.UvmContext;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.TreeMap;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * Class for managing system configuration
 */
public class ConfigManagerImpl implements ConfigManager
{
    private static final String FACTORY_RESET_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-factory-defaults";
    private static final String SYSTEM_REBOOT_COMMAND = "/sbin/shutdown";

    private final Logger logger = Logger.getLogger(getClass());
    private final UvmContext context = UvmContextFactory.context();

    /**
     * Constructor
     */
    public ConfigManagerImpl()
    {
    }

    /**
     * Creates an API response message from passed parameters. This allows us to
     * do all of the exception checking and handling in this single location
     * rather than in every one of our functions, keeping the code a little
     * cleaner and less cluttered.
     *
     * @param resultCode
     *        - The result code
     * @param resultMessage
     *        - The result message
     * @param resultList
     *        - The result list
     * @return The response JSON object
     */
    private JSONObject createResponse(int resultCode, String resultMessage, TreeMap<String, Object> resultList)
    {
        JSONObject response = new JSONObject();

        try {
            response.put("resultCode", resultCode);
            response.put("resultMessage", resultMessage);
            if (resultList != null) {
                for (Map.Entry<String, Object> entry : resultList.entrySet()) {
                    response.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception exn) {
            logger.warn("Exception creating JSON response", exn);
        }

        logger.debug("RESPONSE = " + response.toString());
        return response;
    }

    /**
     * Creates an API success response message from items in the passed map.
     *
     * @param resultList
     *        - The result list
     * @return The response JSON object
     */
    private JSONObject createResponse(TreeMap<String, Object> resultList)
    {
        return createResponse(0, "Success", resultList);
    }

    /**
     * Creates an API response message from the passed code and message
     *
     * @param resultCode
     *        - The result code
     * @param resultMessage
     *        - The result message
     * @return The response JSON object
     */
    private JSONObject createResponse(int resultCode, String resultMessage)
    {
        return createResponse(resultCode, resultMessage, null);
    }

    /**
     * Called to get system information
     *
     * @return A JSON object with details on system disk, memory, cpu, and other
     *         low level metrics.
     */
    public JSONObject getSystemInfo()
    {
        long diskTotal, diskFree;
        long memTotal, memFree;
        double cpuLoad;
        double upTime;

        JSONObject systemHolder = context.metricManager().getStats();
        JSONObject systemStats;

        try {
            systemStats = systemHolder.getJSONObject("systemStats");
        } catch (Exception exn) {
            systemStats = null;
        }

        try {
            diskTotal = systemStats.getLong("totalDiskSpace");
        } catch (Exception exn) {
            diskTotal = 0;
        }

        try {
            diskFree = systemStats.getLong("freeDiskSpace");
        } catch (Exception exn) {
            diskFree = 0;
        }

        try {
            memTotal = systemStats.getLong("MemTotal");
        } catch (Exception exn) {
            memTotal = 0;
        }

        try {
            memFree = systemStats.getLong("MemFree");
        } catch (Exception exn) {
            memFree = 0;
        }

        try {
            cpuLoad = systemStats.getDouble("systemCpuUtilization");
        } catch (Exception exn) {
            cpuLoad = 0;
        }

        try {
            upTime = systemStats.getDouble("uptime");
        } catch (Exception exn) {
            upTime = 0;
        }

        String deviceName = context.networkManager().getNetworkSettings().getHostName() + "." + context.networkManager().getNetworkSettings().getDomainName();

        // TODO - need to figure out the source for the following:
        // ModelName, Description, MacAddress, Temperature
        TreeMap<String, Object> info = new TreeMap<>();
        info.put("ModelName", "model name goes here");
        info.put("Description", "description goes here");
        info.put("DeviceName", deviceName);
        info.put("SerialNumber", context.getServerUID());
        info.put("FirmwareVersion", context.version());
        info.put("SystemUpTime", upTime);
        info.put("MacAddress", "which mac address?");
        info.put("MemoryTotal", memTotal);
        info.put("MemoryFree", memFree);
        info.put("DiskTotal", diskTotal);
        info.put("DiskFree", diskFree);
        info.put("CPULoad", cpuLoad);
        info.put("Temperature", 98.6);
        return createResponse(info);
    }

    /**
     * Called to get the host name
     *
     * @return A JSON object with the system hostname
     */
    public JSONObject getHostName()
    {
        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        TreeMap<String, Object> info = new TreeMap<>();
        info.put("HostName", netSettings.getHostName());
        return createResponse(info);
    }

    /**
     * Called to set the host name
     *
     * @param argName
     *        The new host name
     * @return A JSON object with the old and new hostname
     */
    public JSONObject setHostName(String argName)
    {
        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        String oldName = netSettings.getHostName();
        netSettings.setHostName(argName);
        context.networkManager().setNetworkSettings(netSettings);

        TreeMap<String, Object> info = new TreeMap<>();
        info.put("OldHostName", oldName);
        info.put("NewHostName", argName);
        return createResponse(info);
    }

    /**
     * Called to get the domain name
     *
     * @return A JSON object with the domain name
     */
    public JSONObject getDomainName()
    {
        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        TreeMap<String, Object> info = new TreeMap<>();
        info.put("DomainName", netSettings.getDomainName());
        return createResponse(info);
    }

    /**
     * Called to set the domain name
     *
     * @param argName
     *        The new domain name
     * @return A JSON object with the old and new domain name
     */
    public JSONObject setDomainName(String argName)
    {
        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        String oldName = netSettings.getDomainName();
        netSettings.setDomainName(argName);
        context.networkManager().setNetworkSettings(netSettings);

        TreeMap<String, Object> info = new TreeMap<>();
        info.put("OldDomainName", oldName);
        info.put("NewDomainName", argName);
        return createResponse(info);
    }

    /**
     * Called to perform a factory reset and reboot
     *
     * @return A JSON object with result code and message
     */
    public JSONObject doFactoryReset()
    {
        context.execManager().exec(FACTORY_RESET_SCRIPT);

        new java.util.Timer().schedule(new java.util.TimerTask()
        {
            /**
             * Execute the reboot command after a short delay
             */
            @Override
            public void run()
            {
                context.execManager().exec(SYSTEM_REBOOT_COMMAND + " -r now");
            }
        }, 5000);

        return createResponse(0, "Factory reset complete. System is restarting.");
    }

    /**
     * Called to perform a firmware update
     *
     * @return
     */
    public JSONObject doFirmwareUpdate()
    {
        // TODO - need to start the firmware update here
        return createResponse(999, "Not yet implemented");
    }

    /**
     * Called to check the firmware update status
     *
     * @return
     */
    public JSONObject checkFirmwareUpdate()
    {
        // TODO - need to return the update status here
        return createResponse(999, "Not yet implemented");
    }

    /**
     * Called to get the device time, zone, and DST flag
     *
     * @return A JSON object with the date/time, timezone, and DST flag
     */
    public JSONObject getDeviceTime()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone timezone = context.systemManager().getTimeZone();
        Date date = new Date(System.currentTimeMillis());
        TreeMap<String, Object> info = new TreeMap<>();
        info.put("SystemTime", formatter.format(date));
        info.put("TimeZone", timezone.getID());
        info.put("DaylightSaving", timezone.inDaylightTime(date));
        return createResponse(info);
    }

    /**
     * Called to set the device time and timezone
     *
     * @param argTime
     *        - The new time
     * @param argZone
     *        - The new timezone
     * @return A JSON object with the result code and message
     */
    public JSONObject setDeviceTime(String argTime, String argZone)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date setTime;

        // parse the passed date and time
        try {
            setTime = formatter.parse(argTime);
        } catch (Exception exn) {
            return createResponse(1, exn.getMessage());
        }

        // find the TimeZone from the passed zone ID
        TimeZone setZone = TimeZone.getTimeZone(argZone);

        // update the system timezone and date
        context.systemManager().setTimeZone(setZone);
        context.systemManager().setDate(setTime.getTime());

        return createResponse(0, "Device time updated");
    }

    /**
     * Called to set the password for the provided username to the provided
     * value.
     *
     * @param argUsername
     *        The username for whom to set the password
     * @param argPassword
     *        The new password
     * @return A JSON object with the result code and message
     */
    public JSONObject setAdminCredentials(String argUsername, String argPassword)
    {
        // get the list of admin users
        AdminSettings adminSettings = context.adminManager().getSettings();

        // search the admin users and update the password when found
        for (AdminUserSettings user : adminSettings.getUsers()) {
            if (argUsername.equals(user.getUsername())) {
                user.setPassword(argPassword);
                context.adminManager().setSettings(adminSettings);
                return createResponse(0, "Password updated");
            }
        }

        // username not found so return error
        return createResponse(1, "Username not found");
    }

    /**
     * Called to the the list of network interfaces
     *
     * @return A Java List of InterfaceSettings objects representing all network
     *         interfaces on the device.
     */
    public List<InterfaceSettings> getNetworkInterfaces()
    {
        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        return netSettings.getInterfaces();
    }

    /**
     * Called to set the list of network interfaces
     *
     * @param argList
     *        A Java List of InterfaceSettings objects with the configuration
     *        for all network interfaces on the device
     * @return A JSON object with the result code and message
     */
    public Object setNetworkInterfaces(List<InterfaceSettings> argList)
    {
        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        netSettings.setInterfaces(argList);
        context.networkManager().setNetworkSettings(netSettings);
        return createResponse(0, "Network interfaces updated");
    }

    /**
     * Called to get the syslog server configuration
     *
     * @return A JSON object with details on the syslog server configuration
     */
    public Object getSyslogServer()
    {
        EventSettings eventSettings = context.eventManager().getSettings();

        TreeMap<String, Object> info = new TreeMap<>();
        info.put("SyslogEnabled", eventSettings.getSyslogEnabled());
        info.put("SyslogHost", eventSettings.getSyslogHost());
        info.put("SyslogPort", eventSettings.getSyslogPort());
        info.put("SyslogProtocol", eventSettings.getSyslogProtocol());
        return createResponse(info);
    }

    /**
     * Called to set the syslog server configuration
     *
     * @param argEnabled
     *        The syslog server enabled flag
     * @param argHost
     *        The syslog server address
     * @param argPort
     *        The syslog server port
     * @param argProtocol
     *        The syslog server protocol (TCP or UDP)
     * @return A JSON object with the result code and message
     */
    public Object setSyslogServer(boolean argEnabled, String argHost, int argPort, String argProtocol)
    {
        String protoName = null;

        if (argProtocol.equalsIgnoreCase("TCP")) {
            protoName = "TCP";
        } else if (argProtocol.equalsIgnoreCase("UDP")) {
            protoName = "UDP";
        } else {
            return createResponse(0, "Invalid protocol");
        }

        EventSettings eventSettings = context.eventManager().getSettings();
        eventSettings.setSyslogEnabled(argEnabled);
        eventSettings.setSyslogHost(argHost);
        eventSettings.setSyslogPort(argPort);
        eventSettings.setSyslogProtocol(protoName);
        context.eventManager().setSettings(eventSettings);

        return createResponse(0, "Syslog server updated");
    }

    /**
     * Called to get the network port statistics
     *
     * @return
     */
    public Object getNetworkPortStats()
    {
        // TODO - gather and return statistics for all network interfaces
        return createResponse(999, "Not yet implemented");
    }

    /**
     * Called to get the traffic metrics
     *
     * @return
     */
    public Object getTrafficMetrics()
    {
        // TODO - gather and return traffic metrics
        return createResponse(999, "Not yet implemented");
    }

    /**
     * Called to get the SNMP settings
     *
     * @return A SnmpSettings object with the device SNMP configuration
     */
    public SnmpSettings getSnmpSettings()
    {
        SystemSettings sysSettings = context.systemManager().getSettings();
        return sysSettings.getSnmpSettings();
    }

    /**
     * Called to set the SNMP settings
     *
     * @param argSettings
     *        A SnmpSettings object with the new SNMP configuration
     * @return A JSON object with the result code and message
     */
    public Object setSnmpSettings(SnmpSettings argSettings)
    {
        SystemSettings sysSettings = context.systemManager().getSettings();
        sysSettings.setSnmpSettings(argSettings);
        context.systemManager().setSettings(sysSettings);
        return createResponse(0, "SNMP configuration updated");
    }

    /**
     * Called to create a system backup
     *
     * @return
     */
    public Object createSystemBackup()
    {
        // TODO - how do we do this
        return createResponse(999, "Not yet implemented");
    }

    /**
     * Called to restore a system backup
     *
     * @param argBackup
     *        The backup to restore
     * @return
     */
    public Object restoreSystembackup(Object argBackup)
    {
        // TODO - how do we do this
        return createResponse(999, "Not yet implemented");
    }

    /**
     * Called to get system notifications
     *
     * @return
     */
    public Object getNotifications()
    {
        // TODO - how do we do this
        return createResponse(999, "Not yet implemented");
    }

    /**
     * Called to get diagnostic information
     *
     * @return
     */
    public Object getDiagnosticInfo()
    {
        // TODO - need to figure out what to return here
        // everything in /var/log/uvm? in what format?
        return createResponse(999, "Not yet implemented");
    }
}
