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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TimeZone;
import java.util.zip.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SystemManager;
import com.untangle.uvm.SystemSettings;
import com.untangle.uvm.SnmpSettings;
import com.untangle.uvm.ExecManagerResultReader;
import com.untangle.uvm.app.DayOfWeekMatcher;
import com.untangle.uvm.servlet.DownloadHandler;
import com.untangle.uvm.util.IOUtil;

/**
 * The Manager for system-related settings
 */
public class SystemManagerImpl implements SystemManager
{
    private static final int SETTINGS_VERSION = 4;
    private static final String EOL = "\n";
    private static final String BLANK_LINE = EOL + EOL;
    private static final String TWO_LINES = BLANK_LINE + EOL;

    private static final String SNMP_DEFAULT_FILE_NAME = "/etc/default/snmpd";
    private static final String SNMP_CONF_FILE_NAME = "/etc/snmp/snmpd.conf";
    private static final String SNMP_CONF_LIB_FILE_NAME = "/var/lib/snmp/snmpd.conf";
    private static final String SNMP_CONF_SHARE_FILE_NAME = "/usr/share/snmp/snmpd.conf";
    private static final String SNMP_CONFIG = "/usr/bin/net-snmp-config";
    private static final Pattern SNMP_CONF_V3USER_PATTERN = Pattern.compile("usmUser\\s+");
    private static final Pattern SNMP_CONF_SHARE_USER_PATTERN = Pattern.compile("rwuser\\s+");

    private static final String UPGRADE_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-upgrade.py";
    private static final String SET_TIMEZONE_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-set-timezone";

    private static final String CRON_STRING = " root /usr/share/untangle/bin/ut-upgrade.py >/dev/null 2>&1";
    private static final File CRON_FILE = new File("/etc/cron.d/untangle-upgrade");

    // 850K .......... .......... .......... .......... .......... 96% 46.6K 6s
    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile(".*([0-9]+)K[ .]+([0-9%]+) *([0-9]+\\.[0-9]+[KM]).*");
    private static final String TIMEZONE_FILE = "/etc/timezone";

    private final Logger logger = Logger.getLogger(this.getClass());

    private SystemSettings settings;

    private int downloadTotalFileCount;
    private int downloadCurrentFileCount;
    private String downloadCurrentFileProgress = "";
    private String downloadCurrentFileRate = "";

    private Calendar currentCalendar = Calendar.getInstance();

    /**
     * Constructor
     */
    protected SystemManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        SystemSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "system.js";

        try {
            readSettings = settingsManager.load(SystemSettings.class, settingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.setSettings(defaultSettings());
        } else {
            this.settings = readSettings;

            if(this.settings.getVersion() < SETTINGS_VERSION){
                this.settings.setVersion(SETTINGS_VERSION);
                this.settings.setLogRetention(7);
                this.setSettings(this.settings);
            }

            logger.debug("Loading Settings: " + this.settings.toJSONString());
        }

        /**
         * If the settings file date is newer than the system files, re-sync
         * them
         */
        File settingsFile = new File(settingsFileName);
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
         * If auto-upgrade is disabled and cron file exists, delete it
         */
        if (!settings.getAutoUpgrade() && CRON_FILE.exists()) UvmContextFactory.context().execManager().exec("/bin/rm -f " + CRON_FILE);

        if (settings.getSnmpSettings().isEnabled()) restartSnmpDaemon();

        /**
         * If pyconnector state does not match the settings, re-sync them
         */
        pyconnectorSync();
        
        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new SystemSupportLogDownloadHandler() );

        logger.info("Initialized SystemManager");
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
     * Set the settings
     * 
     * @param newSettings
     *        The new settings
     */
    public void setSettings(final SystemSettings newSettings)
    {
        String newApacheCert = newSettings.getWebCertificate();
        String oldApacheCert = null;
        if (settings != null) oldApacheCert = settings.getWebCertificate();

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "system.js", newSettings);
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

        /**
         * If auto-upgrade is enabled and file doesn't exist or is out of date,
         * write it
         */
        if (settings.getAutoUpgrade()) {
            writeCronFile();
        } else {
            if (CRON_FILE.exists()) UvmContextFactory.context().execManager().exec("/bin/rm -f " + CRON_FILE);
        }

        pyconnectorSync();
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
    public boolean downloadUpgrades()
    {
        LinkedList<String> downloadUrls = new LinkedList<>();

        String result = UvmContextFactory.context().execManager().execOutput("apt-get dist-upgrade --yes --print-uris | awk '/^.http/ {print $1}'");
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
        UvmContextFactory.context().hookManager().callCallbacks(HookManager.UVM_PRE_UPGRADE, 1);

        try {
            ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil(UPGRADE_SCRIPT + " -q");
            reader.waitFor();
        } catch (Exception e) {
            logger.warn("Upgrade exception:", e);
        }
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
        if (forceUpdate) UvmContextFactory.context().execManager().execResult("apt-get update");
        int retCode = UvmContextFactory.context().execManager().execResult("apt-get -s dist-upgrade | grep -q '^Inst'");
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
     * If support access in enabled, start pyconnector and enable on startup. If
     * not, stop it and disable on startup
     */
    protected void pyconnectorSync()
    {
        Integer exitValue = UvmContextImpl.context().execManager().execResult("systemctl is-enabled untangle-pyconnector");
        // if running and should not be, stop it
        if ((0 == exitValue) && !settings.getCloudEnabled()) {
            UvmContextFactory.context().execManager().exec( "systemctl disable untangle-pyconnector" );
            UvmContextFactory.context().execManager().exec( "systemctl stop untangle-pyconnector" );
        }
        // if not running and should be, start it
        //(UvmContextImpl.context().isWizardComplete()) || UvmContextImpl.context().isAppliance())
        if ((0 != exitValue) && settings.getCloudEnabled()) {
            UvmContextFactory.context().execManager().exec( "systemctl enable untangle-pyconnector" );
            UvmContextFactory.context().execManager().exec( "systemctl restart untangle-pyconnector" );
        }
    }

    /**
     * Get the size of /var/log for display in the UI.
     *
     * @return Size of all files recursively.
     */
    public Integer getLogDirectorySize()
    {
        return Integer.parseInt(UvmContextFactory.context().execManager().execOutput("/bin/du -sb /var/log").split("\\t")[0]);
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

        return newSettings;
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

        if (settings.getSnmpSettings().isEnabled()) restartSnmpDaemon();
        else stopSnmpDaemon();

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
        snmpdCtl.append("# Generated by Untangle").append(EOL);
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

        StringBuilder snmpd_config = new StringBuilder();
        snmpd_config.append("# Generated by Untangle").append(EOL);
        snmpd_config.append("# Turn off SMUX - recommended way from the net-snmp folks").append(EOL);
        snmpd_config.append("# is to bind to a goofy IP").append(EOL);
        snmpd_config.append("smuxsocket 1.0.0.0").append(TWO_LINES);

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
            snmpd_config.append("access MyROGroup \"\" any noauth exact mib2 none none");
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
            int retCode = UvmContextFactory.context().execManager().execResult(SNMP_CONFIG + " --create-snmpv3-user" + " -a " + settings.getV3AuthenticationProtocol() +
            // !!! escape properly
                    " -A \"" + settings.getV3AuthenticationPassphrase() + "\"" + " -x " + settings.getV3PrivacyProtocol() +
                    // !!! escape properly
                    ((settings.getV3PrivacyPassphrase() != null) && !settings.getV3PrivacyPassphrase().isEmpty() ? " -X \"" + settings.getV3PrivacyPassphrase() + "\"" : "") + " " + settings.getV3Username());
            if (retCode != 0) {
                logger.warn("Unable run create-snmpv3-user: " + retCode);
            }
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
            int tries = 10;
            int count = 0;
            do {
                Thread.sleep(100);
                result = UvmContextFactory.context().execManager().execOutput("pgrep /usr/sbin/snmpd | wc -l");
                count = Integer.parseInt(result.replaceAll("[^0-9]", ""));
                tries--;
            } while ((count > 0) && (tries > 0));
            if (count > 0) {
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
                resp.setHeader("Content-Disposition", "attachment; filename=sytem_logs.zip");

                byte[] buffer = new byte[1024];
                int read;
                out = new ZipOutputStream(resp.getOutputStream());

                File directory = new File("/var/log/uvm");
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
                        if (name.endsWith(".log") == true) {
                            return true;
                        }
                        return false;
                    }
                });

                FileInputStream fis = null;
                for (File f : files) {
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
}
