/**
 * $Id$
 */

package com.untangle.uvm;

import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.DeviceStatus;
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
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * Class for managing system configuration
 */
public class ConfigManagerImpl implements ConfigManager
{
    private static final String FACTORY_RESET_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-factory-defaults";

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
        new java.util.Timer().schedule(new java.util.TimerTask()
        {
            /**
             * Execute the factory reset after a short delay. The force-reboot
             * flag tells the script to reboot the system rather than restart
             * the uvm when the factory reset operation is finished.
             */
            @Override
            public void run()
            {
                context.execManager().exec("nohup " + FACTORY_RESET_SCRIPT + " force-reboot");
            }
        }, 5000);

        return createResponse(0, "Factory reset initiated. System is restarting.");
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
        if (!context.systemManager().upgradesAvailable(false)) {
            return createResponse(0, "System up to date");
        }
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
     * Called to get the network port statistics. We grab and parse the raw
     * interface stats from /proc/dnet/dev and add additional details from our
     * network device configuration and interface status objects.
     *
     * @return A list of InterfaceMetrics objects with details about each
     *         network interface in the system.
     */
    public List<InterfaceMetrics> getNetworkPortStats()
    {
        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        LinkedList<InterfaceMetrics> metricList = new LinkedList<InterfaceMetrics>();
        BufferedReader reader;
        String readLine;

        // we read and parse /proc/net/dev for the raw interface stats
        try {
            reader = new BufferedReader(new FileReader(new File("/proc/net/dev")));
        } catch (Exception exn) {
            logger.warn("Exception reading network interface details", exn);
            return metricList;
        }

        for (int linetot = 0;;) {
            try {
                readLine = reader.readLine();
            } catch (Exception exn) {
                logger.warn("Exception reading device file:", exn);
                readLine = null;
            }

            if (readLine == null) break;
            String workstr = readLine.trim();

            // increment the line counter and ignore first two header lines
            linetot++;
            if (linetot == 1) continue;
            if (linetot == 2) continue;

            // split the line into columns separated by spaces
            String[] column = workstr.split("\\s+");

            // if we don't find exactly the number of columns we expect ignore the line
            if (column.length != 17) {
                logger.warn("Invalid device line: " + readLine);
                continue;
            }

            // get the device name without the trailing colon or space
            String deviceName = column[0].replace(':', ' ').trim();

            InterfaceSettings faceSettings = null;
            DeviceStatus devStatus = null;

            // look for a configured interface with matching device name
            for (InterfaceSettings item : netSettings.getInterfaces()) {
                if (deviceName.contentEquals(item.getSystemDev())) {
                    faceSettings = item;
                    break;
                }
            }

            // if no matching configured interface ignore the line
            if (faceSettings == null) continue;

            // look for device status with matching device name
            for (DeviceStatus item : context.networkManager().getDeviceStatus()) {
                if (deviceName.contentEquals(item.getDeviceName())) {
                    devStatus = item;
                    break;
                }
            }

            // if no matching device status ignore the line
            if (devStatus == null) continue;

            // create a new interface metrics object
            InterfaceMetrics metric = new InterfaceMetrics();

            // grab some data from the interface and status objects we found
            metric.setPortId(faceSettings.getInterfaceId());
            metric.setPortName(deviceName);
            metric.setPortMac(devStatus.getMacAddress());
            metric.setPortStatus(devStatus.getConnected().name());
            metric.setPortDuplex(devStatus.getDuplex().name());
            metric.setPortSpeed(devStatus.getMbit());

            // parse all of the column values from the raw device status line
            metric.setRxBytes(Long.parseLong(column[1]));
            metric.setRxPackets(Long.parseLong(column[2]));
            metric.setRxErrors(Long.parseLong(column[3]));
            metric.setRxDrop(Long.parseLong(column[4]));
            metric.setRxFifo(Long.parseLong(column[5]));
            metric.setRxFrame(Long.parseLong(column[6]));
            metric.setRxCompressed(Long.parseLong(column[7]));
            metric.setRxMulticast(Long.parseLong(column[8]));

            metric.setTxBytes(Long.parseLong(column[9]));
            metric.setTxPackets(Long.parseLong(column[10]));
            metric.setTxErrors(Long.parseLong(column[11]));
            metric.setTxDrop(Long.parseLong(column[12]));
            metric.setTxFifo(Long.parseLong(column[13]));
            metric.setTxCollisions(Long.parseLong(column[14]));
            metric.setTxCarrier(Long.parseLong(column[15]));
            metric.setTxCompressed(Long.parseLong(column[16]));

            // append the metric to the list we return
            metricList.add(metric);
        }

        try {
            reader.close();
        } catch (Exception exn) {
            logger.warn("Exception closing device file: ", exn);
        }

        return metricList;
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
