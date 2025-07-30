/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TimeZone;
import java.util.zip.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.generic.SystemSettingsGeneric;
import com.untangle.uvm.network.NetworkSettings;
import org.apache.commons.lang3.StringUtils;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SystemManager;
import com.untangle.uvm.SystemSettings;
import com.untangle.uvm.SnmpSettings;
import com.untangle.uvm.ExecManagerResultReader;
import com.untangle.uvm.app.DayOfWeekMatcher;
import com.untangle.uvm.event.AdminLoginEvent;
import com.untangle.uvm.servlet.DownloadHandler;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.FileDirectoryMetadata;
import com.untangle.uvm.util.IOUtil;
import com.untangle.uvm.util.StringUtil;

/**
 * The Manager for system-related settings
 */
public class SystemManagerImpl implements SystemManager
{
    private static final int SETTINGS_VERSION = 6;
    private static final String ZIP_FILE = "system_logs.zip";
    private static final String EOL = "\n";
    private static final String BLANK_LINE = EOL + EOL;
    private static final String TWO_LINES = BLANK_LINE + EOL;

    private static final String SYSTEM_LOG_DIR = "/var/log";
    private static final String SNMP_DEFAULT_FILE_NAME = "/etc/default/snmpd";
    private static final String SNMP_CONF_FILE_NAME = "/etc/snmp/snmpd.conf";
    private static final String SNMP_CONF_LIB_FILE_NAME = "/var/lib/snmp/snmpd.conf";
    private static final String SNMP_CONF_SHARE_FILE_NAME = "/usr/share/snmp/snmpd.conf";
    private static final Pattern SNMP_CONF_V3USER_PATTERN = Pattern.compile("usmUser\\s+");
    private static final Pattern SNMP_CONF_SHARE_USER_PATTERN = Pattern.compile("rwuser\\s+");

    private static final String UPGRADE_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-upgrade.py";
    private static final String SET_TIMEZONE_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-set-timezone";
    private static final String SNMP_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-snmp.sh";

    private static final String CRON_STRING = " root /usr/share/untangle/bin/ut-upgrade.py >/dev/null 2>&1";
    private static final File CRON_FILE = new File("/etc/cron.d/untangle-upgrade");
    private static final String BDAM_LICENSE_UPDATE_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-bdam-license-update.py";
    private static final File BDAM_CRON_FILE = new File("/etc/cron.daily/bdam-cron");

    // 850K .......... .......... .......... .......... .......... 96% 46.6K 6s
    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile(".*([0-9]+)K[ .]+([0-9%]+) *([0-9]+\\.[0-9]+[KM]).*");
    private static final String TIMEZONE_FILE = "/etc/timezone";

    // must update file in mods-enabled since it is a symlink to our own version
    private final static String FREERADIUS_EAP_CONFIG = "/etc/freeradius/3.0/mods-enabled/eap";

    private final Logger logger = LogManager.getLogger(this.getClass());

    private SystemSettings settings;

    private int downloadTotalFileCount;
    private int downloadCurrentFileCount;
    private String downloadCurrentFileProgress = StringUtils.EMPTY;
    private String downloadCurrentFileRate = StringUtils.EMPTY;

    private Calendar currentCalendar = Calendar.getInstance();

    private String SettingsFileName = StringUtils.EMPTY;

    private boolean isUpgrading = false;
    private boolean skipDiskCheck = false;

    private List<FileDirectoryMetadata> logFiles;

    private static final String CRITICAL_DEVICE_TEMPERATURE = "CRITICAL_DEVICE_TEMPERATURE";
    private final static String GET_DEVICE_TEMPERATURE_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-temperature-status.sh";
    private static final String CRON_TEMPERATURE_STRING = "*/15 * * * * root /usr/share/untangle/bin/ut-temperature-status.py >/dev/null 2>&1";
    private static final File CRON_TEMPERATURE_FILE = new File("/etc/cron.d/ut-temperature-status-cron");

    /**
     * Constructor
     */
    protected SystemManagerImpl()
    {
        this.SettingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "system.js";

        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        SystemSettings readSettings = null;

        try {
            readSettings = settingsManager.load(SystemSettings.class, SettingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.setSettings(defaultSettings(), false);
        } else {
            if(readSettings.getRadiusProxyPassword() != null){
                readSettings.setRadiusProxyEncryptedPassword(PasswordUtil.getEncryptPassword(readSettings.getRadiusProxyPassword()));
                readSettings.setRadiusProxyPassword(null);
            }
            this.settings = readSettings;

            if (this.settings.getVersion() < SETTINGS_VERSION) {
                this.settings.setVersion(SETTINGS_VERSION);
                this.settings.setLogRetention(7);
                this.getSettings().setThresholdTemperature(105.0);
                this.setSettings(this.settings, false);
            }

            logger.debug("Loading Settings: " + this.settings.toJSONString());
        }

        /**
         * If the settings file date is newer than the system files, re-sync
         * them
         */
        File settingsFile = new File(SettingsFileName);
        File snmpConfFile = new File(SNMP_CONF_FILE_NAME);
        File snmpDefaultFile = new File(SNMP_DEFAULT_FILE_NAME);
        if (settingsFile.lastModified() > snmpConfFile.lastModified() || settingsFile.lastModified() > snmpDefaultFile.lastModified()) syncSnmpSettings(this.settings.getSnmpSettings());

        setTimeSource();

        /**
         * If timezone on box is different (example: kernel upgrade), reset it:
         */
        TimeZone currentZone = getTimeZone();
        if (!currentZone.equals(TimeZone.getDefault())) {
            try {
                setTimeZone(currentZone);
            } catch (Exception e) {
                logger.warn("Exception setting timezone", e);
            }
        }

        /**
         * If auto-upgrade is enabled and file doesn't exist or is out of date,
         * write it
         */
        if (settings.getAutoUpgrade() && !CRON_FILE.exists()) writeCronFile();
        if (settings.getAutoUpgrade() && settingsFile.lastModified() > CRON_FILE.lastModified()) writeCronFile();

        /**
         * Write bdam_cron to auto update license 
         */
        if (!BDAM_CRON_FILE.exists())
            writeBDAMCronFile();

        /**
         * If auto-upgrade is disabled and cron file exists, delete it
         */
        if (!settings.getAutoUpgrade() && CRON_FILE.exists()) UvmContextFactory.context().execManager().exec("/bin/rm -f " + CRON_FILE);

        if (settings.getSnmpSettings().isEnabled()) restartSnmpDaemon();

        /**
         * If pyconnector or freeradius state does not match the settings,
         * re-sync them
         */
        pyconnectorSync();
        radiusServerSync();
        radiusProxySync();

        /**
         * Write ut-temperature-status-cron to check if device temperature reached critical threshold
         */
        if (!CRON_TEMPERATURE_FILE.exists())
            writeCRONTemperatureFile();
        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new SystemSupportLogDownloadHandler());
        initLogFilesMetadata();

        logger.info("Initialized SystemManager");
    }

    /**
     * NGFW-13958 load metadata of all the log files that need to be part of the exported zip
     */
    private void initLogFilesMetadata() {
        List<FileDirectoryMetadata> logFilesList = new ArrayList<>();
        // matches 'auth.log*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR, "^auth[.]log.*"));
        // matches 'bctid.log*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR, "^bctid[.]log.*"));
        // matches 'bdadmserver.log*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR, "^bdamserver[.]log.*"));
        // matches '*'
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR + "/clamav", ".*"));
        // matches 'dhcp.log*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR, "^dhcp[.]log.*"));
        // matches 'ipsec.log*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR, "^ipsec[.]log.*"));
        // matches 'kern.log*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR, "^kern[.]log.*"));
        // matches 'l2tpd.log*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR, "^l2tpd[.]log.*"));
        // matches 'openvpn.log*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR + "/openvpn", "^openvpn[.]log.*"));
        // matches 'pppoe.log*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR, "^pppoe[.]log.*"));
        // matches 'suricata.log*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR + "/suricata", "^suricata[.]log.*"));
        // matches 'syslog*' at the start of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR, "^syslog.*"));
        // matches '*.*' at the end of the string
        logFilesList.add(new FileDirectoryMetadata(SYSTEM_LOG_DIR + "/uvm", ".*[.].*$"));

        logFiles = Collections.unmodifiableList(logFilesList);
    }

    /**
     * Get the settings
     * 
     * @return The settings
     */
    public SystemSettings getSettings()
    {
        return this.settings;
    }

    /**
    * Set settings without regards to the dirtyRadiusFields
    *
    * @param newSettings
    *        The new settings
    */
    public void setSettings(final SystemSettings newSettings) {
        setSettings(newSettings, false);
    }

    /**
     * Get SystemSettingsGeneric for Vue UI
     * @return SystemSettingsGeneric
     */
    public SystemSettingsGeneric getSystemSettingsV2() {
        // Get current network settings
        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        // transform the systemSettings and networkSettings to systemSettingsGeneric
        return this.settings.transformLegacyToGenericSettings(networkSettings);
    }

    /**
     * Sets the SystemSettings and NetworkSettings (Hostname/Services)
     * @param systemSettingsGeneric SystemSettingsGeneric
     */
    public void setSystemSettingsV2(final SystemSettingsGeneric systemSettingsGeneric) {
        // Get current network settings
        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();

        // update hostname and web services fields with value coming from postData
        systemSettingsGeneric.transformGenericToLegacySettings(this.settings, networkSettings);

        // Set Network Settings with updated values.
        UvmContextFactory.context().networkManager().setNetworkSettings(networkSettings);

        // TODO Set SystemSettings when those fields will be transformed in future
    }

    /**
    * Get decrypted passowrd from encrypted password
    *
    * @param encryptedPassword
    * @return password
    */
    public String getDecryptedPassword(String encryptedPassword){
        return PasswordUtil.getDecryptPassword(encryptedPassword);
    }

    /**
    * Get encrypted password from password
    *
    * @param password
    * @return encrypted password
    */
    public String getEncryptedPassword(String password){
        return PasswordUtil.getEncryptPassword(password);
    }

    /**
     * Set the settings
     * 
     * @param newSettings
     *        The new settings
     * @param dirtyRadiusFields
     *        If the Radius Proxy fields are 'dirty' and so a computer account needs to be added
     */
    public void setSettings(final SystemSettings newSettings, boolean dirtyRadiusFields)
    {
        String newApacheCert = newSettings.getWebCertificate();
        String oldApacheCert = null;
        if (settings != null) oldApacheCert = settings.getWebCertificate();

        /**
         * Save the settings
         */
        if(dirtyRadiusFields){
            newSettings.setRadiusProxyEncryptedPassword(PasswordUtil.getEncryptPassword(newSettings.getRadiusProxyPassword()));
            newSettings.setRadiusProxyPassword(null);
        }
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(this.SettingsFileName, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }

        /* sync settings to disk */
        syncSnmpSettings(this.settings.getSnmpSettings());

        /* if the web server cert changed we need to restart apache */
        if ((oldApacheCert == null) || (newApacheCert.equals(oldApacheCert) == false)) {
            activateApacheCertificate();
        }

        /* update the radius server with the configured certificate */
        activateRadiusCertificate();

        /**
         * If auto-upgrade is enabled and file doesn't exist or is out of date,
         * write it
         */
        if (settings.getAutoUpgrade()) {
            writeCronFile();
        } else {
            if (CRON_FILE.exists()) UvmContextFactory.context().execManager().exec("/bin/rm -f " + CRON_FILE);
        }

        UvmContextFactory.context().syncSettings().run(this.SettingsFileName);

        pyconnectorSync();
        radiusServerSync();
        radiusProxySync();

        //Set radiusComputerAccountExists to false if fields are dirty
        if (dirtyRadiusFields) {
            UvmContextFactory.context().localDirectory().setRadiusProxyComputerAccountExists(false);
        }

/*

This logic isn't working. The UI throws a bunch of exceptions and failure
messages generated from exec'ing the create computer account command when
called at this point. This happens when testing on a clean and never
before configured system (ie: all settings wiped). Maybe a timing thing
between the saving of the files, and restarting the external daemons?
Not sure but I couldn't figure it out so I restored the UI button for
creating the computer account and commented this as a quick fix until we
can look deeper. - mahotz

        // If radius proxy enabled and a computer account needs to be added and fields were changed, add a computer account
        if (this.settings.getRadiusProxyEnabled() && !UvmContextFactory.context().localDirectory().getRadiusProxyComputerAccountExists() && dirtyRadiusFields) {
            ExecManagerResult addComputerAccount = UvmContextFactory.context().localDirectory().addRadiusComputerAccount();
            if (addComputerAccount.getResult() != 0) {
                throw new RuntimeException("Unable to create AD Computer Account automatically: " + addComputerAccount.getOutput());
            } else {
                UvmContextFactory.context().localDirectory().setRadiusProxyComputerAccountExists(true);
            }
        }
*/
    }

    /**
     * Get the time zone
     * 
     * @return The time zone
     */
    @Override
    public TimeZone getTimeZone()
    {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(TIMEZONE_FILE));
            String str = in.readLine();
            str = str.trim();
            TimeZone current = TimeZone.getTimeZone(str);
            return current;
        } catch (Exception x) {
            logger.warn("Unable to get timezone, using java default:", x);
            return TimeZone.getDefault();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                logger.error("Unable to close file", ex);
            }
        }
    }

    /**
     * Set the time zone
     * 
     * @param timezone
     *        The time zone
     */
    @Override
    public void setTimeZone(TimeZone timezone)
    {
        String id = timezone.getID();

        Integer exitValue = UvmContextImpl.context().execManager().execResult(SET_TIMEZONE_SCRIPT + " " + id);
        if (0 != exitValue) {
            String message = "Unable to set time zone (" + exitValue + ") to: " + id;
            logger.error(message);
            throw new RuntimeException(message);
        }

        // if timezone has changed update JVM
        if (TimeZone.getDefault() != null && !TimeZone.getDefault().getID().equals(id)) {
            logger.info("Timezone changed from " + TimeZone.getDefault().getID() + " to " + id);
            logger.warn("Attempting to update JVM timezone");
            // set a flag so a warning is displayed
            UvmContextImpl.getInstance().notificationManager().setTimezoneChanged(true);
            // in testing this does some really weird things
            // only do this if the timezone has actually changed
            // Note: Only works for threads who haven't yet cached the zone!
            TimeZone.setDefault(timezone);
        }

        this.currentCalendar = Calendar.getInstance();
    }

    /**
     * Get if upgrading
     * 
     * @return If currently upgrading
     */
    public boolean getIsUpgrading() {
        return this.isUpgrading;
    }

    /**
     * Set the isUpgrading flag
     * 
     * @param isUpgrading the value to set to
     */
    private void setIsUpgrading(boolean isUpgrading) {
        this.isUpgrading = isUpgrading;
    }

    /**
     * Get skip disk health check flag
     * @return skipDiskCheck flag
     */
    public boolean isSkipDiskCheck() { 
        return skipDiskCheck; 
    }

    /**
     * Set skip disk health check flag
     * @param skipDiskCheck
     */
    public void setSkipDiskCheck(boolean skipDiskCheck) { 
        this.skipDiskCheck = skipDiskCheck; 
    }

    /**
     * Get the calendar
     * 
     * @return The calendar
     */
    @Override
    public Calendar getCalendar()
    {
        return this.currentCalendar;
    }

    /**
     * Get the date
     * 
     * @return The date
     */
    public String getDate()
    {
        return (new Date(System.currentTimeMillis())).toString();
    }

    /**
     * Get the system time in milliseconds
     * 
     * @return The system time in milliseconds
     */
    public long getMilliseconds()
    {
        return System.currentTimeMillis();
    }

    /**
     * Set the date
     * 
     * @param timestamp
     *        The date
     */
    @Override
    public void setDate(long timestamp)
    {
        if (timestamp != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            String formattedCurrentDate = sdf.format(date.getTime());
            String formattedNewDate = sdf.format(timestamp);
            UvmContextFactory.context().execManager().exec("date -s '" + formattedNewDate + "'");
            logger.info("Time manually changed from " + formattedCurrentDate + " to " + formattedNewDate);
        }
    }

    /**
     * Set the time source
     */
    public void setTimeSource()
    {
        /**
         * Glob for NTP init in startup and based on whether found or not and if
         * it conflicts with the current timeSource file, synchronize
         * appropriately.
         */
        File directory = new File("/etc/rc3.d");
        File[] files = directory.listFiles(new FilenameFilter()
        {
            /**
             * Accept matcher for file search
             * 
             * @param directory
             *        The file directory
             * @param name
             *        The file name
             * @return True to accept the file, false to reject
             */
            @Override
            public boolean accept(File directory, String name)
            {
                return (name.endsWith("ntp") == true);
            }
        });
        if ((files.length > 0) && settings.getTimeSource().equals("manual")) {
            UvmContextFactory.context().execManager().exec("systemctl disable ntp");
            UvmContextFactory.context().execManager().exec("systemctl stop ntp");
            logger.info("Time changed from NTP to manual");
        } else if ((files.length == 0) && settings.getTimeSource().equals("ntp")) {
            UvmContextFactory.context().execManager().exec("update-rc.d ntp defaults");
            /**
             * Restart NTP while performing an NTP time synchronization
             */
            UvmContextFactory.context().forceTimeSync();
            logger.info("Time changed from manual to NTP");
        }

    }

    /**
     * Get the time zones
     * 
     * @return The time zones
     */
    public String getTimeZones()
    {
        String[] timezones = TimeZone.getAvailableIDs();
        List<TimeZone> all = new ArrayList<>();
        for (String tz : timezones) {
            all.add(TimeZone.getTimeZone(tz));
        }
        // remove TZs that the OS doesnt know
        for (Iterator<TimeZone> iter = all.iterator(); iter.hasNext();) {
            TimeZone tz = iter.next();
            String path = "/usr/share/zoneinfo/" + tz.getID();
            if (!(new File(path).exists())) {
                iter.remove();
            }
        }
        final long d = new Date().getTime();
        Collections.sort(all, new Comparator<TimeZone>()
        {
            /**
             * Compare function for sorting
             * 
             * @param o1
             *        Object one
             * @param o2
             *        Object two
             * @return Comparison result
             */
            @Override
            public int compare(TimeZone o1, TimeZone o2)
            {
                if (o1.getOffset(d) < o2.getOffset(d)) return -1;
                if (o1.getOffset(d) > o2.getOffset(d)) return 1;
                return 0;
            }
        });
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        boolean first = true;
        for (TimeZone tz : all) {
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append("['").append(tz.getID()).append("','").append(getTZString(tz, d)).append("']");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * The old version of this function called 'date +%:z' to figure out the
     * current timezone offset, which was returning a value that seemed to be
     * cached for the process, and did not reflect real time changes. This new
     * version calculates the offset using the timezone that has been configured
     * using setTimeZone. Found this on the interwebs:
     * http://stackoverflow.com/questions/11399491/java-timezone-offset
     * 
     * @return The time zone offset
     */
    public Integer getTimeZoneOffset()
    {
        TimeZone tz = getTimeZone();
        Calendar cal = Calendar.getInstance(tz);
        Integer offset = tz.getOffset(cal.getTimeInMillis());
        logger.info("getTimeZoneOffset calculated value = " + offset);
        return (offset);
    }

    /**
     * Download upgrades
     * 
     * @return True for success, false for failure
     */
    public String checkDiskHealth()
    {
        return UvmContextFactory.context().execManager().execOutput(System.getProperty("uvm.bin.dir") + "/ut-system-mgr-helpers.sh diskHealthCheck");
    }

    /**
     * Download upgrades
     * 
     * @return True for success, false for failure
     */
    public boolean downloadUpgrades()
    {
        LinkedList<String> downloadUrls = new LinkedList<>();

        String result = UvmContextFactory.context().execManager().execOutput(System.getProperty("uvm.bin.dir") + "/ut-system-mgr-helpers.sh downloadUpgrades");
        try {
            String lines[] = result.split("\\r?\\n");
            for (String line : lines) {
                if (line.length() < 3) continue;
                try {
                    // remove first and last character (quotes)
                    String newUrl = line.substring(1, line.length() - 1);
                    logger.info("To Download: " + newUrl);
                    downloadUrls.add(newUrl);
                } catch (Exception e) {
                    logger.error("Error parsing downloads line: " + line, e);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing downloads", e);
            return false;
        }

        this.downloadTotalFileCount = downloadUrls.size();
        this.downloadCurrentFileCount = 0;

        // run wget to fetch each each URL and track download progress
        for (String url : downloadUrls) {
            this.downloadCurrentFileCount++;

            try {
                logger.info("Downloading " + url);

                // String[] strs = {"/bin/bash", "-c", "wget -c --progress=dot -P /var/cache/apt/archives/ " + url};
                // ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil( strs );
                ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil("wget -c --progress=dot -P /var/cache/apt/archives/ " + url);

                String bufferedOutput = "";
                // read from stdout/stderr
                for (String output = reader.readLineStderr(); output != null; output = reader.readFromOutput()) {
                    bufferedOutput = bufferedOutput + output;
                    if (!bufferedOutput.contains("\n")) {
                        Thread.sleep(10);
                        continue; // wait for at least one line of full output
                    }

                    String lines[] = bufferedOutput.split("\\r?\\n");
                    for (String line : lines) {
                        logger.debug("output: \"" + line + "\"");
                        Matcher match = DOWNLOAD_PATTERN.matcher(line);
                        if (match.matches()) {
                            int bytesDownloaded = Integer.parseInt(match.group(1)) * 1000;
                            String progress = match.group(2);
                            String speed = match.group(3);

                            this.downloadCurrentFileProgress = progress;
                            this.downloadCurrentFileRate = speed + "B/sec";

                            logger.info("Updating file download progress/speed: " + this.downloadCurrentFileProgress + " / " + this.downloadCurrentFileRate);
                        }
                        bufferedOutput = line; // the last line should set this
                    }
                }

                Integer retCode = reader.getResult();
                if (retCode == null || retCode != 0) {
                    logger.error("Error downloading updates: wget returned " + retCode);
                    return false;
                }

                reader.destroy();
            } catch (Exception e) {
                logger.error("Exception downloading updates", e);
                return false;
            }
        }

        return true;
    }

    /**
     * Get the download status
     * 
     * @return The download status
     */
    public org.json.JSONObject getDownloadStatus()
    {
        org.json.JSONObject json = new org.json.JSONObject();

        try {
            json.put("downloadTotalFileCount", downloadTotalFileCount);
            json.put("downloadCurrentFileCount", downloadCurrentFileCount);
            json.put("downloadCurrentFileProgress", downloadCurrentFileProgress);
            json.put("downloadCurrentFileRate", downloadCurrentFileRate);
        } catch (Exception e) {
            logger.error("Error generating WebUI startup object", e);
        }
        return json;
    }

    /**
     * Upgrade the system
     */
    public void upgrade()
    {
        // Call pre-upgrade hook
        this.setIsUpgrading(true);
        UvmContextFactory.context().hookManager().callCallbacks(HookManager.UVM_PRE_UPGRADE, 1);

        try {
            ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil(UPGRADE_SCRIPT + " -q");
            reader.waitFor();
        } catch (Exception e) {
            logger.warn("Upgrade exception:", e);
        }
        this.setSkipDiskCheck(false);
        this.setIsUpgrading(false);
        /*
         * probably will never return as the upgrade usually kills the
         * untangle-vm if it is upgraded
         */
        return;
    }

    /**
     * See if upgrades are available
     * 
     * @param forceUpdate
     *        Force update flag
     * @return Upgrade check result
     */
    public boolean upgradesAvailable(boolean forceUpdate)
    {
        if (forceUpdate) UvmContextFactory.context().execManager().execResult("apt-get update --yes --allow-releaseinfo-change");
        int retCode = UvmContextFactory.context().execManager().execResult(System.getProperty("uvm.bin.dir") + "/ut-system-mgr-helpers.sh upgradesAvailable");
        return (retCode == 0);
    }

    /**
     * Check for upgrades with the force update flag set
     * 
     * @return Upgrade check result
     */
    public boolean upgradesAvailable()
    {
        return upgradesAvailable(true);
    }

    /**
     * This test all the risks which might cause upgrade failure before actual upgrade starts
     * 
     * @return Set of risks which might cause upgrade failure before actual upgrade starts 
     */
    public Set<UpgradeFailures> canUpgrade() 
    {
        Set<UpgradeFailures> upgradeIssues = new HashSet<>();
      
        try {
            UpgradeFailures failure = testDiskSpace();
            if (failure != null) {
                upgradeIssues.add(failure);
            }
        } catch (Exception e) {
            logger.warn("Disk space check failed", e);
        }
        return upgradeIssues;
    }

    /**
     * This test that disk free % is less than 75%
     * 
     * @return UpgradeFailures type of failure
     */
    private UpgradeFailures testDiskSpace() {
        int percentUsed;
        try {
            percentUsed = getUsedDiskSpacePercentage();
        } catch (Exception e) {
            return UpgradeFailures.FAILED_TO_TEST;
        }

        if (percentUsed > 75) {
            return UpgradeFailures.LOW_DISK;
        } else {
            return null; // No failures
        }
    }


    /**
     * This calculate the used disk space in percent
     * 
     * @return percentUsed  used disk space 
     */
    public int getUsedDiskSpacePercentage(){
        int percentUsed;
        try {
            File rootFile = new File("/");
            long totalSpace = rootFile.getTotalSpace();
            long usedSpace = rootFile.getUsableSpace();
            percentUsed =(int) ((1-((double) usedSpace / totalSpace) )* 100);
        } catch (Exception e) {
            logger.warn("Unable to determine free disk space", e);
            throw e;
        }
        return percentUsed;
    }

    /**
     * If support access in enabled, start pyconnector and enable on startup. If
     * not, stop it and disable on startup
     */
    protected void pyconnectorSync()
    {
        Integer exitValue = UvmContextImpl.context().execManager().execResult("systemctl is-enabled untangle-pyconnector");
        // if running and should not be, stop it
        if ((0 == exitValue) && !settings.getCloudEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl disable untangle-pyconnector");
            UvmContextFactory.context().execManager().exec("systemctl stop untangle-pyconnector");
        }
        // if not running and should be, start it
        //(UvmContextImpl.context().isWizardComplete()) || UvmContextImpl.context().isAppliance())
        if ((0 != exitValue) && settings.getCloudEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl enable untangle-pyconnector");
            UvmContextFactory.context().execManager().exec("systemctl restart untangle-pyconnector");
        }
    }

    /**
     * If radius server is enabled, start freeradius.service and enable on
     * startup. If not, stop it and disable on startup
     */
    protected void radiusServerSync()
    {
        Integer exitValue = UvmContextImpl.context().execManager().execResult("systemctl is-enabled freeradius.service");
        // if running and should not be, stop it
        if ((0 == exitValue) && !settings.getRadiusServerEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl disable freeradius.service");
            UvmContextFactory.context().execManager().exec("systemctl stop freeradius.service");
        }
        // if not running and should be, start it
        if ((0 != exitValue) && settings.getRadiusServerEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl enable freeradius.service");
            UvmContextFactory.context().execManager().exec("systemctl restart freeradius.service");
        }
    }

    /**
     * If radius proxy is enabled, start winbind.service and enable on startup.
     * If not, stop it and disable on startup.
     */
    protected void radiusProxySync()
    {
        Integer exitValue;

        /**
         * we never need the nmbd service so make sure it never runs
         */
        UvmContextFactory.context().execManager().exec("systemctl stop nmbd.service");
        UvmContextFactory.context().execManager().exec("systemctl disable nmbd.service");
        UvmContextFactory.context().execManager().exec("systemctl mask nmbd.service");

        /**
         * manage the smbd service which is the samba daemon required for radius proxy
         */
        exitValue = UvmContextImpl.context().execManager().execResult("systemctl is-enabled smbd.service");

        // if running and should not be, stop it
        if ((0 == exitValue) && !settings.getRadiusProxyEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl stop smbd.service");
            UvmContextFactory.context().execManager().exec("systemctl disable smbd.service");
        }
        // if not running and should be, start it
        if ((0 != exitValue) && settings.getRadiusProxyEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl enable smbd.service");
            UvmContextFactory.context().execManager().exec("systemctl restart smbd.service");
        }

        /**
         * manage the winbind service which is another daemon required for radius proxy
         */
        exitValue = UvmContextImpl.context().execManager().execResult("systemctl is-enabled winbind.service");

        // if running and should not be, stop it
        if ((0 == exitValue) && !settings.getRadiusProxyEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl stop winbind.service");
            UvmContextFactory.context().execManager().exec("systemctl disable winbind.service");
        }
        // if not running and should be, start it
        if ((0 != exitValue) && settings.getRadiusProxyEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl enable winbind.service");
            UvmContextFactory.context().execManager().exec("systemctl restart winbind.service");
        }
    }

    /**
     * Get the size of /var/log for display in the UI.
     *
     * @return Long size of all files recursively.
     */
    public Long getLogDirectorySize()
    {
        Long result = 0L;
        try{
            result = Long.parseLong(UvmContextFactory.context().execManager().execOutput("/usr/bin/du -sb /var/log").split("\\t")[0]);
        }catch(Exception ex){
            logger.error("Unable to parse size of log directory", ex);
        }
        return result;
    }

    /**
     * Create the default system settings
     * 
     * @return The default system settings
     */
    private SystemSettings defaultSettings()
    {
        SystemSettings newSettings = new SystemSettings();
        newSettings.setVersion(SETTINGS_VERSION);

        SnmpSettings snmpSettings = new SnmpSettings();
        snmpSettings.setEnabled(false);
        snmpSettings.setPort(SnmpSettings.STANDARD_MSG_PORT);
        snmpSettings.setCommunityString("CHANGE_ME");
        snmpSettings.setSysContact("MY_CONTACT_INFO");
        snmpSettings.setSysLocation("MY_LOCATION");
        snmpSettings.setV3AuthenticationProtocol("sha");
        snmpSettings.setV3PrivacyProtocol("aes");
        snmpSettings.setSendTraps(false);
        snmpSettings.setTrapHost("MY_TRAP_HOST");
        snmpSettings.setTrapCommunity("MY_TRAP_COMMUNITY");
        snmpSettings.setTrapPort(SnmpSettings.STANDARD_TRAP_PORT);

        newSettings.setSnmpSettings(snmpSettings);

        newSettings.setAutoUpgrade(true);
        newSettings.setAutoUpgradeHour(23);
        newSettings.setAutoUpgradeMinute((new java.util.Random()).nextInt(60));
        newSettings.setAutoUpgradeDays(DayOfWeekMatcher.getAnyMatcher());

        // pass the settings to the OEM override function and return the override settings
        SystemSettings overrideSettings = (SystemSettings)UvmContextFactory.context().oemManager().applyOemOverrides(newSettings);
        return overrideSettings;
    }

    /**
     * Sync the SNMP settings
     * 
     * @param snmpSettings
     *        The settings
     */
    private void syncSnmpSettings(SnmpSettings snmpSettings)
    {
        if (snmpSettings == null) return;

        writeDefaultSnmpCtlFile(snmpSettings);
        writeSnmpdConfFile(snmpSettings);

        if (settings.getSnmpSettings().isEnabled()){
            restartSnmpDaemon();
        }else{
            stopSnmpDaemon();
        }

        // The SNMPv3 manager does its own snmpd management, if neccessary
        writeSnmpdV3User(snmpSettings);
    }

    /**
     * Write the default SNMP control file
     * 
     * @param settings
     *        Thg SNMP settings
     */
    private void writeDefaultSnmpCtlFile(SnmpSettings settings)
    {
        StringBuilder snmpdCtl = new StringBuilder();
        snmpdCtl.append("# Generated by Arista").append(EOL);
        snmpdCtl.append("export MIBDIRS=/usr/share/snmp/mibs").append(EOL);
        snmpdCtl.append("SNMPDRUN=").append(settings.isEnabled() ? "yes" : "no").append(EOL);
        //Note the line below also specifies the listening port
        snmpdCtl.append("SNMPDOPTS='-LS6d -Lf /dev/null -p /var/run/snmpd.pid UDP:").append(Integer.toString(settings.getPort())).append("'").append(EOL);
        snmpdCtl.append("TRAPDRUN=no").append(EOL);
        snmpdCtl.append("TRAPDOPTS='-LS6d -p /var/run/snmptrapd.pid'").append(EOL);

        strToFile(snmpdCtl.toString(), SNMP_DEFAULT_FILE_NAME);
    }

    /**
     * Write the SNMPD config file
     * 
     * @param settings
     *        The SNMP settings
     */
    private void writeSnmpdConfFile(SnmpSettings settings)
    {
        logger.warn("writeSnmpdConfFile...");
        StringBuilder snmpd_config = new StringBuilder();
        snmpd_config.append("# Generated by Arista").append(EOL);

        snmpd_config.append("dontLogTCPWrappersConnects 1").append(EOL);

        if (settings.isSendTraps() && isNotNullOrBlank(settings.getTrapHost()) && isNotNullOrBlank(settings.getTrapCommunity())) {

            snmpd_config.append("# Enable default SNMP traps to be sent").append(EOL);
            snmpd_config.append("trapsink ").append(settings.getTrapHost()).append(" ").append(settings.getTrapCommunity()).append(" ").append(Integer.toString(settings.getTrapPort())).append(BLANK_LINE);
            snmpd_config.append("# Enable traps for failed auth (this is a security appliance)").append(EOL);
            snmpd_config.append("authtrapenable  1").append(TWO_LINES);
        } else {
            snmpd_config.append("# Not sending traps").append(TWO_LINES);
        }

        snmpd_config.append("# Physical system location").append(EOL);
        snmpd_config.append("syslocation").append(" ").append(qqOrNullToDefault(settings.getSysLocation(), "location")).append(BLANK_LINE);
        snmpd_config.append("# System contact info").append(EOL);
        snmpd_config.append("syscontact").append(" ").append(qqOrNullToDefault(settings.getSysContact(), "contact")).append(TWO_LINES);

        snmpd_config.append("sysservices 78").append(TWO_LINES);

        // removed extension to add faceplate stats
        // no longer works
        // snmpd_config.append("pass_persist .1.3.6.1.4.1.30054 /usr/share/untangle/bin/ut-snmpd-extension.py .1.3.6.1.4.1.30054").append(EOL);

        if (isNotNullOrBlank(settings.getCommunityString())) {
            snmpd_config.append("# Simple access rules, so there is only one read").append(EOL);
            snmpd_config.append("# only connumity.").append(EOL);

            if ((false == settings.isEnabled()) || (false == settings.isV3Enabled()) || (false == settings.isV3Required())) {
                snmpd_config.append("com2sec local default ").append(settings.getCommunityString()).append(EOL);
                snmpd_config.append("group MyROGroup v1 local").append(EOL);
                snmpd_config.append("group MyROGroup v2c local").append(EOL);
            }

            snmpd_config.append("group MyROGroup usm local").append(EOL);
            //snmpd_config.append("view mib2 included  .iso.org.dod.internet.mgmt.mib-2").append(EOL);
            //snmpd_config.append("view mib2 included  .iso.org.dod.internet.private.1.30054").append(EOL);
            //snmpd_config.append("access MyROGroup \"\" any noauth exact mib2 none none").append(EOL);
            snmpd_config.append("view mib2 included  .iso").append(EOL);
            snmpd_config.append("access MyROGroup \"\" any noauth exact mib2 none none").append(EOL);
        } else {
            snmpd_config.append("# No one has access (no community string)").append(EOL);
        }

        strToFile(snmpd_config.toString(), SNMP_CONF_FILE_NAME);
    }

    /**
     * Write the SNMP V3 user configuration
     * 
     * @param settings
     *        The SNMP settings
     */
    private void writeSnmpdV3User(SnmpSettings settings)
    {
        /*
         * Modify SNMP library configuration in-place, removing existing users
         */
        boolean foundExistingUser = false;
        StringBuilder snmpdLib_config = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(SNMP_CONF_LIB_FILE_NAME));
            for (String l = br.readLine(); null != l; l = br.readLine()) {
                Matcher matcher = SNMP_CONF_V3USER_PATTERN.matcher(l);
                if (matcher.find()) {
                    foundExistingUser = true;
                    continue;
                }
                snmpdLib_config.append(l).append(EOL);
            }
        } catch (Exception x) {
            logger.warn("Unable to open SNMP library configuration file: " + SNMP_CONF_LIB_FILE_NAME);
            return;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                logger.error("Unable to close file", ex);
            }
        }

        StringBuilder snmpdShare_config = new StringBuilder();
        br = null;
        try {
            br = new BufferedReader(new FileReader(SNMP_CONF_SHARE_FILE_NAME));
            for (String l = br.readLine(); null != l; l = br.readLine()) {
                Matcher matcher = SNMP_CONF_SHARE_USER_PATTERN.matcher(l);
                if (matcher.find()) {
                    foundExistingUser = true;
                    continue;
                }
                snmpdShare_config.append(l).append(EOL);
            }
        } catch (Exception x) {

        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                logger.error("Unable to close file", ex);
            }
        }
        if ((false == foundExistingUser) && (false == settings.isV3Enabled())) {
            return;
        }
        /*
         * SNMPv3 management requires explicit server shutdown/startup.
         */
        stopSnmpDaemon();

        if (true == foundExistingUser) {
            /*
             * Remove existing user
             */
            strToFile(snmpdLib_config.toString(), SNMP_CONF_LIB_FILE_NAME);
            strToFile(snmpdShare_config.toString(), SNMP_CONF_SHARE_FILE_NAME);
        }

        if (settings.isEnabled() && settings.isV3Enabled()) {
            /*
             * Add v3 user
             */
            ExecManagerResult result = UvmContextFactory.context().execManager().exec( SNMP_SCRIPT +
                " create_snmp3_user" +
                " " + settings.getV3Username() +
                " " + settings.getV3AuthenticationProtocol() +
                " \"" + settings.getV3AuthenticationPassphrase() + "\"" +
                " " + settings.getV3PrivacyProtocol() +
                " \"" + settings.getV3PrivacyPassphrase() + "\""
            );
            logger.warn("result=" + result.getResult() + ", output=" + result.getOutput());
        }

        startSnmpDaemon();

        return;
    }

    /**
     * Write a string to a file
     * 
     * @param s
     *        The string
     * @param fileName
     *        The file name
     * @return True for success, otherwise false
     */
    private boolean strToFile(String s, String fileName)
    {
        FileOutputStream fos = null;
        File tmp = null;
        try {
            tmp = File.createTempFile("snmpcf", ".tmp");
            fos = new FileOutputStream(tmp);
            fos.write(s.getBytes());
            fos.flush();
            fos.close();
            IOUtil.copyFile(tmp, new File(fileName));
            tmp.delete();
            return true;
        } catch (Exception ex) {
            IOUtil.close(fos);
            if (tmp != null) {
                tmp.delete();
            }
            logger.error("Unable to create SNMP control file \"" + fileName + "\"", ex);
            return false;
        }
    }

    /**
     * Note that if we've disabled SNMP support (and it was enabled) forcing
     * this "restart" actualy causes it to stop. Doesn't sound intuitive - but
     * trust me. The "etc/default/snmpd" file which we write controls this.
     */
    private void restartSnmpDaemon()
    {
        try {
            logger.debug("Restarting the snmpd...");

            String command = "systemctl restart snmpd";
            String result = UvmContextFactory.context().execManager().execOutput(command);
            try {
                String lines[] = result.split("\\r?\\n");
                logger.info(command + ": ");
                for (String line : lines)
                    logger.info(command + ": " + line);
            } catch (Exception e) {
            }

        } catch (Exception ex) {
            logger.error("Error restarting snmpd", ex);
        }
    }

    /**
     * Stop the SNMP daemon
     */
    private void stopSnmpDaemon()
    {
        try {
            logger.debug("Stopping the snmpd...");

            String command = "systemctl stop snmpd";
            String result = UvmContextFactory.context().execManager().execOutput(command);
            try {
                String lines[] = result.split("\\r?\\n");
                logger.info(command + ": ");
                for (String line : lines)
                    logger.info(command + ": " + line);
            } catch (Exception e) {
            }
            // The daemon must be completely shut down for purposes such as adding an snmpv3 user
            // and returning from the init script doesn't 100% guarantee that it's shut down.
            int tries = 100;
            boolean running = true;
            do {
                Thread.sleep(100);
                running = (UvmContextFactory.context().execManager().execResult("pgrep snmpd") == 0);
                tries--;
            } while ((running == true) && (tries > 0));
            if (running == true) {
                logger.info("Waiting for snmpd shutdown took too long");
            }
        } catch (Exception ex) {
            logger.error("Error stopping snmpd", ex);
        }
    }

    /**
     * Start the SNMP daemon
     */
    private void startSnmpDaemon()
    {
        try {
            logger.debug("Starting the snmpd...");

            String command = "systemctl start snmpd";
            String result = UvmContextFactory.context().execManager().execOutput(command);
            try {
                String lines[] = result.split("\\r?\\n");
                logger.info(command + ": ");
                for (String line : lines)
                    logger.info(command + ": " + line);
            } catch (Exception e) {
            }

        } catch (Exception ex) {
            logger.error("Error starting snmpd", ex);
        }
    }

    /**
     * See if a string is null or empty
     * 
     * @param s
     *        The string
     * @return True if not null and not empty, otherwise false
     */
    private boolean isNotNullOrBlank(String s)
    {
        return s != null && !"".equals(s.trim());
    }

    /**
     * Used to replace a null or blank string with the provided default
     * 
     * @param str
     *        The string to check
     * @param def
     *        The default to return if the string is null or blank
     * @return The original string or the default if the original is null or
     *         blank
     */
    private String qqOrNullToDefault(String str, String def)
    {
        return isNotNullOrBlank(str) ? str : def;
    }

    /**
     * Write the auto upgrade CRON file
     */
    private void writeCronFile()
    {
        // do not write cron job in dev env 
        if (UvmContextFactory.context().isDevel()) {
            if (CRON_FILE.exists()) UvmContextFactory.context().execManager().exec("/bin/rm -f " + CRON_FILE);
            return;
        }

        String daysOfWeek;
        if (settings.getAutoUpgradeDays() == null || settings.getAutoUpgradeDays().getCronString() == null) {
            logger.error("Invalid dayOfWeek matcher: " + settings.getAutoUpgradeDays());
            /* assume all days */
            daysOfWeek = "*";
        } else {
            daysOfWeek = settings.getAutoUpgradeDays().getCronString();
        }

        // write the cron file for upgrades
        String conf = settings.getAutoUpgradeMinute() + " " + settings.getAutoUpgradeHour() + " * * " + daysOfWeek + CRON_STRING;
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(CRON_FILE));
            out.write(conf, 0, conf.length());
            out.write("\n");
        } catch (IOException ex) {
            logger.error("Unable to write file", ex);
            return;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                logger.error("Unable to close file", ex);
                return;
            }
        }
    }

    /** 
     * Write BDAM licence update cronjob file.
     */
    private void writeBDAMCronFile()
    {
        // write the cron file for nightly runs
        String cronStr =  "#!/bin/sh" + Constants.NEW_LINE + BDAM_LICENSE_UPDATE_SCRIPT;
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(BDAM_CRON_FILE));
            out.write(cronStr, 0, cronStr.length());
            out.write(Constants.NEW_LINE);
        } catch (IOException ex) {
            logger.error("Unable to write file", ex);
            return;
        }finally{
            if(out != null){
                try {
                    out.close();
                } catch (IOException ex) {
                    logger.error("Unable to close file", ex);
                }
            }
        }

        // Make files executable
        UvmContextFactory.context().execManager().execResult( "chmod 755 " + BDAM_CRON_FILE);
        UvmContextFactory.context().execManager().execResult( "chmod 755 " + BDAM_LICENSE_UPDATE_SCRIPT);
    }

    /** 
     * Write device temperaure cronjob file.
     */
    private void writeCRONTemperatureFile()
    {
        // write the cron file for 15 minute runs
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(CRON_TEMPERATURE_FILE));
            out.write(CRON_TEMPERATURE_STRING, 0, CRON_TEMPERATURE_STRING.length());
            out.write(Constants.NEW_LINE);
        } catch (IOException ex) {
            logger.error("Unable to write file", ex);
            return;
        }finally{
            if(out != null){
                try {
                    out.close();
                } catch (IOException ex) {
                    logger.error("Unable to close file", ex);
                }
            }
        }

        // Make files executable
        UvmContextFactory.context().execManager().execResult( "chmod 755 " + CRON_TEMPERATURE_FILE);
        UvmContextFactory.context().execManager().execResult( "chmod 755 " + GET_DEVICE_TEMPERATURE_SCRIPT);
    }

    /**
     * Handler for Admin requests to download the support log
     */
    private class SystemSupportLogDownloadHandler implements DownloadHandler
    {
        private static final String CHARACTER_ENCODING = "utf-8";

        /**
         * Get the handler name
         * 
         * @return The handler name
         */
        @Override
        public String getName()
        {
            return "SystemSupportLogs";
        }

        /**
         * Download handler for system log download requests
         * 
         * @param req
         *        The request
         * @param resp
         *        The response
         */
        public void serveDownload(HttpServletRequest req, HttpServletResponse resp)
        {
            ZipOutputStream out = null;
            try {
                resp.setCharacterEncoding(CHARACTER_ENCODING);
                resp.setHeader("Content-Type", "application/octet-stream");
                resp.setHeader("Content-Disposition", "attachment; filename=" + ZIP_FILE);

                byte[] buffer = new byte[1024];
                int read;
                out = new ZipOutputStream(resp.getOutputStream());

                List<File[]> filesList = new ArrayList<>();
                // List of file arrays, each containing list of log files from a specific directory
                for (FileDirectoryMetadata logFile : logFiles) {
                    File[] files = logFile.getDirectory().listFiles(FileDirectoryMetadata.getFileNameFilter(logFile.getFileMatchPattern()));
                    filesList.add(files);
                }

                FileInputStream fis = null;
                for (File[] filesArr : filesList) {
                    for (File f : filesArr) {
                        try {
                            fis = new FileInputStream(f.getCanonicalFile());
                            out.putNextEntry(new ZipEntry(f.getName()));
                            while ((read = fis.read(buffer)) > 0) {
                                out.write(buffer, 0, read);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to write log file.", e);
                        } finally {
                            try {
                                if (fis != null) {
                                    fis.close();
                                }
                            } catch (IOException ex) {
                                logger.error("Unable to close file", ex);
                            }
                        }
                    }
                }
                out.flush();
            } catch (Exception e) {
                logger.warn("Failed to archive files.", e);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException ex) {
                    logger.error("Unable to close archive file", ex);
                }
            }
        }
    }

    /**
     * Get the time zone string
     * 
     * @param tz
     *        The time zone
     * @param d
     *        The date?
     * @return The time zone string
     */
    private String getTZString(TimeZone tz, long d)
    {
        long offset = tz.getOffset(d) / 1000;
        long hours = Math.abs(offset) / 3600;
        long minutes = (Math.abs(offset) / 60) % 60;
        if (offset < 0) {
            return "~UTC-" + (hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes);
        } else {
            return "~UTC+" + (hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes);
        }
    }

    /**
     * If the certificate assigned to Web services changes, we copy the new
     * certificate into the location where Apache looks for it, and restart.
     */
    public void activateApacheCertificate()
    {
        // copy the configured pem file to the apache directory and restart
        UvmContextFactory.context().execManager().exec("cp " + CertificateManager.CERT_STORE_PATH + getSettings().getWebCertificate() + " " + CertificateManager.APACHE_PEM_FILE);
        UvmContextFactory.context().execManager().exec("/usr/sbin/apache2ctl graceful");
    }

    /**
     * Update the certificate in all of the freeradius configuration files.
     */
    public void activateRadiusCertificate()
    {
        if (!settings.getRadiusServerEnabled()) return;

        String certFull = CertificateManager.CERT_STORE_PATH + getSettings().getRadiusCertificate();
        int dotLocation = certFull.indexOf('.');
        if (dotLocation < 0) {
            logger.warn("Invalid filename for RADIUS certificate: + certFull");
            return;
        }

        String certBase = certFull.substring(0, dotLocation);

        try {
            updateRadiusCertificateConfig(FREERADIUS_EAP_CONFIG, certBase);
        } catch (Exception exn) {
            logger.warn("Exception activating RADIUS certificate", exn);
            return;
        }

        // make sure the freeradius daemon can read the crt and key files
        UvmContextFactory.context().execManager().exec("chmod a+r " + certBase + ".crt");
        UvmContextFactory.context().execManager().exec("chmod a+r " + certBase + ".key");
        UvmContextFactory.context().execManager().exec("systemctl restart freeradius.service");
    }

    /**
     * Modify a freeradius configuration file in place
     *
     * The configuration files for freeradius have a massive amount of comments
     * with dozens of actual configuration lines scattered all over. Since there
     * are numerous files, I didn't want to extract the active lines from each
     * of the distribution configs we need to manage here and create a bunch of
     * static config templates like I did for the radiusd.conf file. So instead
     * I came up with this solution that looks for non-comment lines with the
     * certificate_file and private_key_file options we need to manage, and
     * adjust them while leaving everything else unchanged. This simple approach
     * only works here because the lines we need to modify are not commented. A
     * whole lot of extra parsing and whitespace detection would be needed to
     * use this approach to enable or set commented items in one of the config
     * files. Search tokens that are long and unique is also helpful here.
     *
     * @param fileName
     *        The file to modify
     * @param certBase
     *        The base filename of the certificate
     * @throws Exception
     */
    public void updateRadiusCertificateConfig(String fileName, String certBase) throws Exception
    {
        // make sure the file exists
        File checker = new File(fileName);
        if (!checker.exists()) {
            logger.warn("Missing RADIUS file: " + fileName);
            return;
        }

        java.util.Scanner scanner = new Scanner(new File(fileName));
        List<String> config = new ArrayList<String>();
        while (scanner.hasNextLine()) {
            config.add(scanner.nextLine());
        }
        scanner.close();

        FileWriter fw = new FileWriter(fileName, false);

        for (String line : config) {
            // lines with comments are written without modification
            if (line.contains("#")) {
                fw.write(line + "\n");
                continue;
            }

            int crtloc = line.indexOf("certificate_file");
            int keyloc = line.indexOf("private_key_file");

            // lines without cert entries are written without modification
            if ((crtloc < 0) && (keyloc < 0)) {
                fw.write(line + "\n");
                continue;
            }

            // write certificate_file entries using our configured crt file
            if (crtloc >= 0) {
                fw.write(line.substring(0, crtloc));
                fw.write("certificate_file = " + certBase + ".crt\n");
            }

            // write private_key_file entries using our configured key file
            if (keyloc >= 0) {
                fw.write(line.substring(0, keyloc));
                fw.write("private_key_file = " + certBase + ".key\n");
            }
        }

        fw.flush();
        fw.close();
    }

    /**
     * Send Disk check failure event log.
     * 
     * @param diskCheckErrors
     *        String diskCheckErrors
     */
    public void logDiskCheckFailure( String diskCheckErrors )
    {
        logger.warn("Logging CriticalAlertEvent for Disk Check Failure. Errors: {}", diskCheckErrors);
        CriticalAlertEvent alert = new CriticalAlertEvent("DISK_CHECK_FAILURE", "Disk health checks failed, Upgrade aborted", "Errors: " + diskCheckErrors);
        UvmContextFactory.context().logEvent(alert);
    }

    /**
     * Get device temperature information.
     * 
     * @return The device temperature string
     */
    public String getDeviceTemperatureInfo()
    {
        logger.debug(" Getting device temparature getDeviceTemperatureInfo()");
        return UvmContextFactory.context().execManager().execOutput(String.format("%s", GET_DEVICE_TEMPERATURE_SCRIPT));
    }

    /**
     * Send Temperature Exceeded Critical Threshold event log.
     * 
     * @param temperatureErrors
     *        String temperatureErrors
     */
    public void logCriticalTemperature( String temperatureErrors )
    {
        logger.warn("Logging CriticalAlertEvent for Device temparature reaching critical level. Errors: {}", temperatureErrors);
        CriticalAlertEvent alert = new CriticalAlertEvent(CRITICAL_DEVICE_TEMPERATURE, "Temperature Exceeded Critical Threshold", "Errors: " + temperatureErrors);
        UvmContextFactory.context().logEvent(alert);
    }
}
