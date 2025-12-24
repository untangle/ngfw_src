/**
 * $Id$
 */

package com.untangle.uvm;

import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.DhcpStaticEntry;
import com.untangle.uvm.network.DeviceStatus;
import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.app.Reporting;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.net.InetAddress;
import java.sql.ResultSetMetaData;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.jabsorb.JSONSerializer;
import org.json.JSONObject;

/**
 * Class for managing system configuration
 */
public class ConfigManagerImpl implements ConfigManager
{
    private static final String FACTORY_RESET_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-factory-defaults";
    private static final String DIAGNOSTIC_DUMP_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-diagnostic-dump";

    private final Logger logger = LogManager.getLogger(getClass());
    private final UvmContext context = UvmContextFactory.context();

    // this is managed by our getDatabaseConnection member
    private volatile Connection sharedConnection = null;

    // the is the JSON serializer we use when returning settings objects
    private JSONSerializer serializer;

    // this is the file where we read the system temperature
    private String temperatureSourceFile = null;

    /**
     * Constructor
     *
     * @param serializer
     *        - The serializer we should use when returning settings objects
     */
    public ConfigManagerImpl(JSONSerializer serializer)
    {
        this.serializer = serializer;
        temperatureSourceFile = findTemperatureSourceFile();
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

        logger.debug("RESPONSE = {}", response);
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
        logger.info("CMAN_HIST getSystemInfo()");

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
        String macAddress = getSystemMacAddress();
        String systemTemperature = getSystemTemperature();

        String serialNumber = context.getServerSerialNumber();
        if (serialNumber == null) serialNumber = "0";

        // TODO - vendor2 expects the Description but it doesn't map to anything
        // we configure or track on our platform so we just return empty for now
        TreeMap<String, Object> info = new TreeMap<>();
        info.put("ModelName", context.getApplianceModel());
        info.put("Description", "");
        info.put("DeviceName", deviceName);
        info.put("SerialNumber", serialNumber);
        info.put("ServerUID", context.getServerUID());
        info.put("FirmwareVersion", context.version());
        info.put("SystemUpTime", upTime);
        info.put("MacAddress", macAddress);
        info.put("MemoryTotal", memTotal);
        info.put("MemoryFree", memFree);
        info.put("DiskTotal", diskTotal);
        info.put("DiskFree", diskFree);
        info.put("CPULoad", cpuLoad);
        info.put("Temperature", systemTemperature);
        return createResponse(info);
    }

    /**
     * Called to get the host name
     *
     * @return A JSON object with the system hostname
     */
    public JSONObject getHostName()
    {
        logger.info("CMAN_HIST getHostName()");

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
        logger.info("CMAN_HIST setHostName() = {}", argName);

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
        logger.info("CMAN_HIST getDomainName()");

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
        logger.info("CMAN_HIST setDomainName() = {}", argName);

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
                context.execManager().exec("nohup " + FACTORY_RESET_SCRIPT + " force-reboot >/dev/null 2>&1 &");
            }
        }, 1000);

        return createResponse(0, "Factory reset initiated. System is restarting.");
    }

    /**
     * Called to perform a firmware update
     *
     * @return A JSON object with result code and message
     */
    public JSONObject doFirmwareUpdate()
    {
        logger.info("CMAN_HIST doFirmwareUpdate()");

        // check if upgrades available
        if (!context.systemManager().upgradesAvailable()) {
            return createResponse(0, "Upgrade Not Needed");
        }
        // do the upgrade after a short delay
        new java.util.Timer().schedule(new java.util.TimerTask()
        {
            /**
             * Slight delay before starting the upgrade
             */
            @Override
            public void run()
            {
                context.systemManager().upgrade();
            }
        }, 2000);

        return createResponse(0, "Upgrade Started");
    }

    /**
     * Called to check the firmware update status
     *
     * @return A JSON object with a result code and message indicating the
     *         status of the system firmware.
     *
     */
    public JSONObject checkFirmwareUpdate()
    {
        logger.info("CMAN_HIST checkFirmwareUpdate()");

        // check if currently upgrading first
        if (context.systemManager().getIsUpgrading()) {
            return createResponse(0, "In Progress");
        }
        // check if upgrades available
        if (!context.systemManager().upgradesAvailable()) {
            return createResponse(0, "System Up To Date");
        }
        return createResponse(0, "Update Available");
    }

    /**
     * Called to get the device time, zone, and DST flag
     *
     * @return A JSON object with the date/time, timezone, and DST flag
     */
    public JSONObject getDeviceTime()
    {
        logger.info("CMAN_HIST getDeviceTime()");

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
        logger.info("CMAN_HIST setDeviceTime() = {}|{}", argTime , argZone);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date setTime;

        // parse the passed date and time
        try {
            setTime = formatter.parse(argTime);
        } catch (Exception exn) {
            logger.warn("Exception parsing argTime: {}", argTime, exn);
            return createResponse(1, exn.toString());
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
        logger.info("CMAN_HIST setAdminCredentials() = {}", argUsername);

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
     * Called to get the the list of network interfaces. We use the serializer
     * passed to our constructor which is the same one used by the settings
     * manager. Otherwise the JSON-RPC handler can generate fixups which makes
     * it more difficult to parse and process the list of interfaces.
     *
     * @return A JSON object containing the list of InterfaceSettings objects
     *         representing all network interfaces on the device.
     */
    public Object getNetworkInterfaces()
    {
        logger.info("CMAN_HIST getNetworkInterfaces()");

        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        JSONObject response = null;
        String output = null;

        try {
            output = serializer.toJSON(netSettings.getInterfaces());
        } catch (Exception exn) {
            logger.error("Exception serializing interfaces", exn);
            return createResponse(1, exn.toString());
        }

        try {
            response = new JSONObject(output);
        } catch (Exception exn) {
            logger.error("Exception creating JSON", exn);
            return createResponse(2, exn.toString());
        }

        return response;
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
        logger.info("CMAN_HIST setNetworkInterfaces()");

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
        logger.info("CMAN_HIST getSyslogServer()");

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
        logger.info("CMAN_HIST setSyslogServer() = {}|{}|{}|{}", argEnabled , argHost , argPort , argProtocol);

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
        logger.info("CMAN_HIST getNetworkPortStats()");

        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        List<DeviceStatus> devStatusList = context.networkManager().getDeviceStatus();
        LinkedList<InterfaceMetrics> metricList = new LinkedList<>();
        BufferedReader reader;
        String readLine;

        // we read and parse /proc/net/dev for the raw interface stats
        try {
            reader = new BufferedReader(new FileReader("/proc/net/dev"));
        } catch (Exception exn) {
            logger.warn("Exception opening /proc/net/dev", exn);
            return metricList;
        }

        for (int linetot = 0;;) {
            try {
                readLine = reader.readLine();
            } catch (Exception exn) {
                logger.warn("Exception reading /proc/net/dev", exn);
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
                logger.warn("Invalid device line: {}", readLine);
                continue;
            }

            // get the device name without the trailing colon or space
            String deviceName = column[0].replace(':', ' ').trim();

            InterfaceSettings faceSettings = null;
            InterfaceStatus faceStatus = null;
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
            for (DeviceStatus item : devStatusList) {
                if (deviceName.contentEquals(item.getDeviceName())) {
                    devStatus = item;
                    break;
                }
            }

            // if no matching device status ignore the line
            if (devStatus == null) continue;

            // get the interface status for the interface
            faceStatus = context.networkManager().getInterfaceStatus(faceSettings.getInterfaceId());

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

            if (faceStatus != null) {
                metric.setV4Address(faceStatus.getV4Address());
                metric.setV4Netmask(faceStatus.getV4Netmask());
                metric.setV4Gateway(faceStatus.getV4Gateway());
                metric.setV4Dns1(faceStatus.getV4Dns1());
                metric.setV4Dns2(faceStatus.getV4Dns2());
                metric.setV4PrefixLength(faceStatus.getV4PrefixLength());
                metric.setV6Address(faceStatus.getV6Address());
                metric.setV6PrefixLength(faceStatus.getV6PrefixLength());
                metric.setV6Gateway(faceStatus.getV6Gateway());
            }

            // append the metric to the list we return
            metricList.add(metric);
        }

        try {
            reader.close();
        } catch (Exception exn) {
            logger.warn("Exception closing /proc/net/dev", exn);
        }

        return metricList;
    }

    /**
     * Called to get the SNMP settings
     *
     * @return A SnmpSettings object with the device SNMP configuration
     */
    public SnmpSettings getSnmpSettings()
    {
        logger.info("CMAN_HIST getSnmpSettings()");

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
        logger.info("CMAN_HIST setSnmpSettings()");

        SystemSettings sysSettings = context.systemManager().getSettings();
        sysSettings.setSnmpSettings(argSettings);
        context.systemManager().setSettings(sysSettings);
        return createResponse(0, "SNMP configuration updated");
    }

    /**
     * Called to create a system backup
     *
     * @apiNote The backup file will be created in the /tmp directory. The
     *          caller should remove the file when it is no longer needed.
     *
     * @return A JSON Object with the result code and message plus the name of
     *         the backup file that was created.
     */
    public Object createSystemBackup()
    {
        logger.info("CMAN_HIST createSystemBackup()");

        File backupFile = context.backupManager().createBackup();
        String fileName;

        try {
            fileName = backupFile.getCanonicalPath();
        } catch (Exception exn) {
            return createResponse(1, exn.toString());
        }

        TreeMap<String, Object> info = new TreeMap<>();
        info.put("BackupFile", fileName);
        return createResponse(info);
    }

    /**
     * Called to restore a system backup
     *
     * @param argFileName
     *        The backup file to restore
     * @param maintainRegex
     *        An optional regex expression of existing files or directories that
     *        should not be replaced by content in the backup.
     * @return A JSON object with the result code and message
     */
    public Object restoreSystemBackup(String argFileName, String maintainRegex)
    {
        logger.info("CMAN_HIST restoreSystemBackup() = {}|{}", argFileName , maintainRegex);

        // The UI currently passes the following maintainRegex values to restoreBackup:
        // Restore all settings = ''
        // Restore all except keep current network settings = '.*/network.*',

        File file = new File(argFileName);
        String resultMessage = null;

        // pass the file and regex to the restoreBackup function
        try {
            resultMessage = context.backupManager().restoreBackup(file, maintainRegex);
        } catch (Exception exn) {
            return createResponse(1, exn.toString());
        }

        // null return messages indicates an error
        if (resultMessage != null) {
            return createResponse(2, resultMessage);
        }

        // return the success message
        return createResponse(0, "System restore initiated");
    }

    /**
     * Called to perform a database query. Shouldn't be called from an API unless proper input validation is done.
     *
     * @apiNote The database connection used in this function has the read-only
     *          flag set to maximize performance and to prevent accidental
     *          modifications. It is good practice to include a LIMIT clause in
     *          any query that could potentially return a very large amount of
     *          data to avoid accidental overload of system resources.
     *
     * @param argQuery
     *        The database query to perform
     * @return A JSON object with the result code and message plus the row
     *         count, row data, and the query that was executed.
     */
    private Object doDatabaseQuery(String argQuery)
    {
        logger.info("CMAN_HIST doDatabaseQuery() = {}", argQuery);

        String problem = null;

        // we need the reporting app to do anything with the database
        Reporting reportsApp = (Reporting) UvmContextFactory.context().appManager().app("reports");
        if (reportsApp == null) {
            return createResponse(1, "The reports application is not installed");
        }

        // get the database connection
        Connection dbConnection = getDatabaseConnection(reportsApp);
        if (dbConnection == null) {
            return createResponse(2, "The reports application returned null connection");
        }

        // create a response map and add the argumented command
        TreeMap<String, Object> info = new TreeMap<>();
        info.put("Query", argQuery);

        List<JSONObject> resultData = new LinkedList<>();
        List<String> columnList = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSetMetaData metaData = null;
        ResultSet resultSet = null;

        try {
            statement = dbConnection.prepareStatement(argQuery);
            statement.execute();
            resultSet = statement.getResultSet();
            metaData = resultSet.getMetaData();

            // get all of the column names
            for (int x = 0; x < metaData.getColumnCount(); x++) {
                columnList.add(metaData.getColumnName(x + 1));
            }

            int rowCount = 0;

            // walk the result set and add each row as a JSONObject using the
            // simple format: column_name = result_value
            while (resultSet.next()) {
                JSONObject json = new JSONObject();
                for (int x = 0; x < metaData.getColumnCount(); x++) {
                    Object item = resultSet.getObject(x + 1);
                    appendJsonItem(json, columnList.get(x), item);
                }
                resultData.add(json);
                rowCount++;
            }

            // put the row count and row data in the response
            info.put("RowCount", rowCount);
            info.put("RowData", resultData);
        } catch (Exception exn) {
            logger.warn("DATABASE ERROR", exn);
            problem = exn.toString();
        }

        // always close the result set if not null
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (Exception exn) {
                logger.warn("Exception closing ResultSet:", exn);
            }
        }

        // always close the statement if not null
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception exn) {
                logger.warn("Exception closing PreparedStatement:", exn);
            }
        }

        if (problem != null) {
            return createResponse(2, problem);
        }

        return createResponse(info);
    }

    /**
     * Called internally to format datbase results in a JSON friendly way. We
     * add common objects (Integer, Long, Float, etc.) as the native data type
     * and everything else with the toString method.
     *
     * @param json
     *        The JSONObject to which the item should be added
     * @param name
     *        The name for the item to be appended
     * @param item
     *        The item to be appended
     */
    private void appendJsonItem(JSONObject json, String name, Object item)
    {
        if (item == null) return;

        try {
            if (item instanceof Integer) {
                json.put(name, item);
                return;
            }
            if (item instanceof Long) {
                json.put(name, item);
                return;
            }
            if (item instanceof Float) {
                json.put(name, item);
                return;
            }
            if (item instanceof Double) {
                json.put(name, item);
                return;
            }
            if (item instanceof Boolean) {
                json.put(name, item);
                return;
            }
            if (item instanceof String) {
                json.put(name, item);
                return;
            }
            if (item instanceof java.sql.Timestamp) {
                java.sql.Timestamp local = (java.sql.Timestamp) item;
                json.put(name, local.getTime());
                return;
            }
            json.put(name, item.toString());
        } catch (Exception exn) {
            logger.error(exn);
        }
    }

    /**
     * Private function to get the persistent shared connection
     *
     * @param reportsApp
     *        The Reporting application
     * @return A valid database connection or null for error
     */
    private Connection getDatabaseConnection(Reporting reportsApp)
    {
        try {
            // if we have a valid connection check isValid first which allows
            // us to close, clear and flow into the get logic
            if (sharedConnection != null) {
                if (!sharedConnection.isValid(0)) {
                    sharedConnection.close();
                    sharedConnection = null;
                }
            }

            // get a datbase connection if we do not already have one
            if (sharedConnection == null) {
                logger.info("Requesting database connection from reporting app");
                sharedConnection = reportsApp.getDbConnection();
                if (sharedConnection == null) {
                    return null;
                }

                // we only support read operations so set the flag for safety and efficiency
                sharedConnection.setReadOnly(true);
            }
        } catch (Exception exn) {
            logger.warn("Exception getting database connection", exn);
            sharedConnection = null;
        }

        return sharedConnection;
    }

    /**
     * Gets the MAC address of the first WAN interface
     *
     * @return The MAC address
     */
    private String getSystemMacAddress()
    {
        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        String macAddress = "00:00:00:00:00:00";
        InterfaceSettings faceSettings = null;
        DeviceStatus devStatus = null;
        String deviceName = null;

        // look for the first WAN interface
        for (InterfaceSettings item : netSettings.getInterfaces()) {
            if (item.getIsWan()) {
                faceSettings = item;
                deviceName = item.getSystemDev();
                break;
            }
        }

        // if no WAN interface found just return the default
        if (faceSettings == null) return macAddress;

        // look for device status with matching device name
        for (DeviceStatus item : context.networkManager().getDeviceStatus()) {
            if (deviceName.contentEquals(item.getDeviceName())) {
                devStatus = item;
                break;
            }
        }

        // if we found the matching device status record grab the MAC address
        if (devStatus != null) {
            macAddress = devStatus.getMacAddress();
        }

        return macAddress;
    }

    /**
     * Gets the system temperature
     *
     * @return The system temperature
     */
    private String getSystemTemperature()
    {
        String systemTemperature = "0";

        // return default if we didn't locate a source for the value
        if (temperatureSourceFile == null) {
            return systemTemperature;
        }

        BufferedReader reader = null;
        String string = null;

        try {
            File temperatureFile = new File(temperatureSourceFile);
            if (temperatureFile.exists()) {
                reader = new BufferedReader(new FileReader(temperatureFile));
                string = reader.readLine();
            }
            if (string != null) {
                systemTemperature = string;
            }
        } catch (Exception exn) {
            logger.error("Unable to read temperature file", exn);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exn) {
                logger.error("Unable to close temperature file", exn);
            }
        }

        return systemTemperature;
    }

    /**
     * Searches for the thermal zone that monitors the system temperature. This
     * is accomplished by looking for a type file that contains x86_pkg_temp in
     * one of the thermal_zone directories.
     *
     * @return The file where the system temperature can be read or an empty
     *         string if the x86_pkg_temp could not be located.
     */
    private String findTemperatureSourceFile()
    {
        File thermalZonePath = new File("/sys/devices/virtual/thermal");
        String discoveryFile = null;

        // NGFW-13936 - Make sure the directory actually exists
        if (!thermalZonePath.exists()) {
            return discoveryFile;
        }

        for (File zone : thermalZonePath.listFiles()) {
            if (discoveryFile != null) {
                return discoveryFile;
            }
            if (!zone.isDirectory()) continue;
            if (!zone.getName().startsWith("thermal_zone")) continue;

            BufferedReader reader = null;
            String string = null;

            try {
                File sensorFile = new File("/sys/devices/virtual/thermal/" + zone.getName() + "/type");
                if (sensorFile.exists()) {
                    reader = new BufferedReader(new FileReader(sensorFile));
                    string = reader.readLine();
                    if ((string != null) && (string.equals("x86_pkg_temp"))) {
                        discoveryFile = zone.getAbsolutePath() + "/temp";
                        logger.info("Discovered system temperature source: {}", discoveryFile);
                    }
                }
            } catch (Exception exn) {
                logger.error("Unable to read sensor file", exn);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException exn) {
                    logger.error("Unable to close sensor file", exn);
                }
            }

        }

        return discoveryFile;
    }

    /**
     * Called to get diagnostic information
     *
     * @apiNote The dump file will be created with the format
     *          /tmp/diagdump_timestamp_serial.tar.gz. The caller should remove
     *          the file when no longer needed.
     *
     * @return A JSON Object with the result code and message plus the name and
     *         size of the diagnostic file that was created and the status
     *         message returned by the file creation script.
     */
    public Object createDiagnosticDump()
    {
        logger.info("CMAN_HIST createDiagnosticDump()");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
        StringBuilder fileName = new StringBuilder();

        Date date = new Date(System.currentTimeMillis());
        fileName.append("/tmp/diagdump");
        fileName.append("_");
        fileName.append(formatter.format(date));
        fileName.append("_");
        fileName.append(context.getServerUID());
        fileName.append(".tar.gz");

        String output = context.execManager().execOutput(DIAGNOSTIC_DUMP_SCRIPT + " " + fileName);
        if (output.startsWith("ERROR:")) {
            return createResponse(1, output);
        }

        File target = new File(fileName.toString());
        if (!target.exists()) {
            return createResponse(2, "The diagnostic dump file could not be created");
        }

        TreeMap<String, Object> info = new TreeMap<>();
        info.put("FileName", fileName.toString());
        info.put("FileSize", target.length());
        info.put("DumpMessage", output);
        return createResponse(info);
    }

    /**
     * Called to ge the list of active hosts
     *
     * @return The list of active hosts
     */
    public Object getConnectedClients()
    {
        logger.info("CMAN_HIST getConnectedClients()");
        return context.hostTable().getHosts();
    }

    /**
     * Called to get the list of licenses
     *
     * @return The list of licenses
     */
    public Object getEnabledFeatures()
    {
        logger.info("CMAN_HIST getEnabledFeatures()");
        return context.licenseManager().getLicenses();
    }

    /**
     * Called to refresh the licenses
     *
     * @return A JSON object with result code and message
     */
    public Object refreshEnabledFeatures()
    {
        logger.info("CMAN_HIST refreshEnabledFeatures()");
        context.licenseManager().reloadLicenses(true);
        return createResponse(0, "Enabled features refresh completed");
    }

    /**
     * Called to get the list of system time zones
     *
     * @return The list of system time zones
     */
    public Object getTimeZones()
    {
        logger.info("CMAN_HIST getTimeZones()");
        return context.systemManager().getTimeZones();
    }

    /**
     * Called to get the list of static DHCP reservations
     *
     * @return The list of static DHCP reservations
     */
    public Object getStaticDhcpReservations()
    {
        logger.info("CMAN_HIST setStaticDhcpReservations()");

        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        return netSettings.getStaticDhcpEntries();
    }

    /**
     * Called to set the list of static DHCP reservations
     *
     * @param argList
     *        The list of reservations
     *
     * @return A JSON Object with the result code and message plus the name and
     */
    public Object setStaticDhcpReservations(List<DhcpStaticEntry> argList)
    {
        logger.info("CMAN_HIST setStaticDhcpReservations()");

        NetworkSettings netSettings = context.networkManager().getNetworkSettings();
        netSettings.setStaticDhcpEntries(argList);
        context.networkManager().setNetworkSettings(netSettings);
        return createResponse(0, "Static DHCP reservations updated");
    }

    /**
     * Called to get the list of active DHCP leases
     *
     * @return The list of active DHCP leases
     */
    public Object getActiveDhcpLeases()
    {
        logger.info("CMAN_HIST getActiveDhcpLeases()");

        LinkedList<DhcpStaticEntry> activeList = new LinkedList<>();

        BufferedReader reader;
        String readLine;

        // we read and parse /var/lib/misc/dnsmasq.leases to get the active reservations
        try {
            reader = new BufferedReader(new FileReader(new File("/var/lib/misc/dnsmasq.leases")));
        } catch (Exception exn) {
            logger.warn("Exception opening /var/lib/misc/dnsmasq.leases", exn);
            return activeList;
        }

        for (;;) {

            try {
                readLine = reader.readLine();
            } catch (Exception exn) {
                logger.warn("Exception reading /var/lib/misc/dnsmasq.leases", exn);
                readLine = null;
            }

            if (readLine == null) break;
            String workstr = readLine.trim();

            // split the line into columns separated by spaces
            String[] column = workstr.split("\\s+");

            // if we don't find exactly the number of columns we expect ignore the line
            if (column.length != 5) {
                logger.warn("Invalid reservation line: {}", readLine);
                continue;
            }

            // create a static entry with the reservation details and append to the list
            DhcpStaticEntry entry = new DhcpStaticEntry();
            InetAddress hostAddr = null;

            try {
                hostAddr = InetAddress.getByName(column[2]);
            } catch (Exception exn) {
                logger.error(exn);
            }

            if (hostAddr == null) continue;

            entry.setMacAddress(column[1]);
            entry.setAddress(hostAddr);
            entry.setDescription(column[3]);
            activeList.add(entry);
        }

        try {
            reader.close();
        } catch (Exception exn) {
            logger.warn("Exception closing /proc/net/dev", exn);
        }

        return activeList;
    }
}
