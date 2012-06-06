/**
 * $Id: SystemManagerImpl.java,v 1.00 2012/05/30 14:17:00 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.CronJob;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SystemManager;
import com.untangle.uvm.SystemSettings;
import com.untangle.uvm.SnmpSettings;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.DayOfWeekMatcher;
import com.untangle.node.util.IOUtil;

/**
 * The Manager for system-related settings
 */
public class SystemManagerImpl implements SystemManager
{
    private static final String EOL = "\n";
    private static final String BLANK_LINE = EOL + EOL;
    private static final String TWO_LINES = BLANK_LINE + EOL;

    private static final String SNMP_DEFAULT_FILE_NAME = "/etc/default/snmpd";
    private static final String SNMP_CONF_FILE_NAME = "/etc/snmp/snmpd.conf";
    private static final String SNMP_WRAPPER_NAME = "/usr/share/untangle/bin/snmpd-restart";

    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/untangle-vm-convert-system-settings.py";

    private final Logger logger = Logger.getLogger(this.getClass());

    private SystemSettings settings;

    private final UpdateTask updateTask = new UpdateTask();
    private CronJob autoUpgradeCronJob;
    
    protected SystemManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        SystemSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "system";

        try {
            readSettings = settingsManager.load( SystemSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are no settings, run the conversion script to see if there are any in the database
         * Then check again for the file
         */
        if (readSettings == null) {
            logger.warn("No settings found - Running conversion script to check DB");
            try {
                String convertCmd = SETTINGS_CONVERSION_SCRIPT + " " + settingsFileName + ".js";
                logger.warn("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            } catch ( Exception e ) {
                logger.warn( "Conversion script failed.", e );
            } 

            try {
                readSettings = settingsManager.load( SystemSettings.class, settingsFileName );
                if (readSettings != null) {
                    logger.warn("Found settings imported from database");
                }
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Failed to load settings:",e);
            }
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.setSettings(defaultSettings());
        }
        else {
            this.settings = readSettings;
            logger.debug("Loading Settings: " + this.settings.toJSONString());
        }

        /**
         * If the settings file date is newer than the system files, re-sync them
         */
        File settingsFile = new File(settingsFileName + ".js");
        File snmpConfFile = new File(SNMP_CONF_FILE_NAME);
        File snmpDefaultFile = new File(SNMP_DEFAULT_FILE_NAME);
        if (settingsFile.lastModified() > snmpConfFile.lastModified() ||
            settingsFile.lastModified() > snmpDefaultFile.lastModified())
            syncSnmpSettings(this.settings.getSnmpSettings());


        this.autoUpgradeCronJob = UvmContextFactory.context().makeCronJob(this.settings.getAutoUpgradeDays(),
                                                                          this.settings.getAutoUpgradeHour(),
                                                                          this.settings.getAutoUpgradeMinute(),
                                                                          updateTask);
        
        logger.info("Initialized SystemManager");
    }

    public SystemSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings(final SystemSettings settings)
    {
        this._setSettings( settings );
    }

    /**
     * @return the public url for the box, this is the address (may be hostname or ip address)
     */
    public String getPublicUrl()
    {
        String httpsPortStr = Integer.toString(this.settings.getHttpsPort());
        String primaryAddressStr = "unconfigured.example.com";
        
        if ( SystemSettings.PUBLIC_URL_EXTERNAL_IP.equals(this.settings.getPublicUrlMethod()) ) {
            IPAddress primaryAddress = UvmContextFactory.context().networkManager().getPrimaryAddress();
            if ( primaryAddress == null ) {
                logger.warn("No WAN IP found");
            } else {
                primaryAddressStr = primaryAddress.getAddr().getHostAddress();
            }
        } else if ( SystemSettings.PUBLIC_URL_HOSTNAME.equals(this.settings.getPublicUrlMethod()) ) {
            if ( UvmContextFactory.context().networkManager().getHostname() == null ) {
                logger.warn("No hostname is configured");
            } else {
                primaryAddressStr = UvmContextFactory.context().networkManager().getHostname();
            }
        } else if ( SystemSettings.PUBLIC_URL_ADDRESS_AND_PORT.equals(this.settings.getPublicUrlMethod()) ) {
            if ( this.settings.getPublicUrlAddress() == null ) {
                logger.warn("No public address configured");
            } else {
                primaryAddressStr = this.settings.getPublicUrlAddress();
                httpsPortStr = Integer.toString(this.settings.getPublicUrlPort());
            }
        } else {
            logger.warn("Unknown public URL method: " + this.settings.getPublicUrlMethod() );
        }
        
        return primaryAddressStr + ":" + httpsPortStr;
    }

    
    private void _setSettings( SystemSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(SystemSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "system", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        this.reconfigure();
    }

    private void reconfigure() 
    {
        logger.info("reconfigure()");

        /* install support if necessary */
        try {
            if ( this.settings.getSupportEnabled()) {
                UvmContextFactory.context().toolboxManager().install("untangle-support-agent");
            } else {
                UvmContextFactory.context().toolboxManager().uninstall("untangle-support-agent");
            }
        } catch ( Exception ex ) {
            logger.error( "Unable to enable support", ex );
        }

        /* rebind HTTPS port if necessary */
        int port = this.settings.getHttpsPort();
        try {
            logger.info("Rebinding HTTPS port: " + port);
            UvmContextFactory.context().localAppServerManager().rebindExternalHttpsPort( port );
            logger.info("Rebinding HTTPS port done.");
        } catch ( Exception e ) {
            if ( !UvmContextFactory.context().state().equals( UvmState.RUNNING )) {
                logger.info( "unable to rebind port at startup, expected. ");
            } else {
                logger.warn( "unable to rebind https to port: " + port, e );
            }
        }

        /* sync SnmpSettings to disk */
        syncSnmpSettings(this.settings.getSnmpSettings());
    
        UvmContextImpl.context().networkManager().refreshNetworkConfig();

        if (this.autoUpgradeCronJob != null)
            this.autoUpgradeCronJob.reschedule(this.settings.getAutoUpgradeDays(),
                                               this.settings.getAutoUpgradeHour(),
                                               this.settings.getAutoUpgradeMinute());

    }

    private SystemSettings defaultSettings()
    {
        SystemSettings newSettings = new SystemSettings();
        newSettings.setInsideHttpEnabled( true );
        newSettings.setOutsideHttpsEnabled( true );
        if (UvmContextFactory.context().isDevel())
            newSettings.setOutsideHttpsAdministrationEnabled( true );
        else
            newSettings.setOutsideHttpsAdministrationEnabled( false );
        newSettings.setOutsideHttpsQuarantineEnabled( true );
        newSettings.setOutsideHttpsReportingEnabled( false );
        newSettings.setOutsideHttpsEnabled( true );
        newSettings.setHttpsPort( 443 );

        newSettings.setPublicUrlMethod( SystemSettings.PUBLIC_URL_EXTERNAL_IP );
        newSettings.setPublicUrlAddress( "hostname.example.com" );
        newSettings.setPublicUrlPort( 443 );

        SnmpSettings snmpSettings = new SnmpSettings();
        snmpSettings.setEnabled(false);
        snmpSettings.setPort(SnmpSettings.STANDARD_MSG_PORT);
        snmpSettings.setCommunityString("CHANGE_ME");
        snmpSettings.setSysContact("MY_CONTACT_INFO");
        snmpSettings.setSysLocation("MY_LOCATION");
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

    private void syncSnmpSettings(SnmpSettings snmpSettings)
    {
        if (snmpSettings == null)
            return;
        
        writeDefaultSnmpCtlFile(snmpSettings);
        writeSnmpdConfFile(snmpSettings);
        restartDaemon();
    }

    private void writeDefaultSnmpCtlFile(SnmpSettings settings)
    {
        StringBuilder snmpdCtl = new StringBuilder();
        snmpdCtl.append("# Generated by Untangle").append(EOL);
        snmpdCtl.append("export MIBDIRS=/usr/share/snmp/mibs").append(EOL);
        snmpdCtl.append("SNMPDRUN=").
            append(settings.isEnabled()?"yes":"no").
            append(EOL);
        //Note the line below also specifies the listening port
        snmpdCtl.append("SNMPDOPTS='-Lsd -Lf /dev/null -p /var/run/snmpd.pid UDP:").
            append(Integer.toString(settings.getPort())).append("'").append(EOL);
        snmpdCtl.append("TRAPDRUN=no").append(EOL);
        snmpdCtl.append("TRAPDOPTS='-Lsd -p /var/run/snmptrapd.pid'").append(EOL);

        strToFile(snmpdCtl.toString(), SNMP_DEFAULT_FILE_NAME);
    }

    private void writeSnmpdConfFile(SnmpSettings settings)
    {

        StringBuilder snmpd_config = new StringBuilder();
        snmpd_config.append("# Generated by Untangle").append(EOL);
        snmpd_config.append("# Turn off SMUX - recommended way from the net-snmp folks").append(EOL);
        snmpd_config.append("# is to bind to a goofy IP").append(EOL);
        snmpd_config.append("smuxsocket 1.0.0.0").append(TWO_LINES);

        if(settings.isSendTraps() &&
           isNotNullOrBlank(settings.getTrapHost()) &&
           isNotNullOrBlank(settings.getTrapCommunity())) {

            snmpd_config.append("# Enable default SNMP traps to be sent").append(EOL);
            snmpd_config.append("trapsink ").
                append(settings.getTrapHost()).append(" ").
                append(settings.getTrapCommunity()).append(" ").
                append(Integer.toString(settings.getTrapPort())).
                append(BLANK_LINE);
            snmpd_config.append("# Enable traps for failed auth (this is a security appliance)").append(EOL);
            snmpd_config.append("authtrapenable  1").append(TWO_LINES);
        }
        else {
            snmpd_config.append("# Not sending traps").append(TWO_LINES);
        }

        snmpd_config.append("# Physical system location").append(EOL);
        snmpd_config.append("syslocation").append(" ").
            append(qqOrNullToDefault(settings.getSysLocation(), "location")).append(BLANK_LINE);
        snmpd_config.append("# System contact info").append(EOL);
        snmpd_config.append("syscontact").append(" ").
            append(qqOrNullToDefault(settings.getSysContact(), "contact")).append(TWO_LINES);

        snmpd_config.append("sysservices 78").append(TWO_LINES);

        snmpd_config.append("pass_persist .1.3.6.1.4.1.30054 /usr/share/untangle/bin/ut-snmpd-extension.py .1.3.6.1.4.1.30054").append(EOL);

        if(isNotNullOrBlank(settings.getCommunityString())) {
            snmpd_config.append("# Simple access rules, so there is only one read").append(EOL);
            snmpd_config.append("# only connumity.").append(EOL);
            snmpd_config.append("com2sec local default ").append(settings.getCommunityString()).append(EOL);
            snmpd_config.append("group MyROGroup v1 local").append(EOL);
            snmpd_config.append("group MyROGroup v2c local").append(EOL);
            snmpd_config.append("group MyROGroup usm local").append(EOL);
            //snmpd_config.append("view mib2 included  .iso.org.dod.internet.mgmt.mib-2").append(EOL);
            //snmpd_config.append("view mib2 included  .iso.org.dod.internet.private.1.30054").append(EOL);
            //snmpd_config.append("access MyROGroup \"\" any noauth exact mib2 none none").append(EOL);
            snmpd_config.append("view mib2 included  .iso").append(EOL);
            snmpd_config.append("access MyROGroup \"\" any noauth exact mib2 none none");
        }
        else {
            snmpd_config.append("# No one has access (no community string)").append(EOL);
        }

        strToFile(snmpd_config.toString(), SNMP_CONF_FILE_NAME);
    }

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
        }
        catch(Exception ex) {
            IOUtil.close(fos);
            tmp.delete();
            logger.error("Unable to create SNMP control file \"" +
                         fileName + "\"", ex);
            return false;
        }
    }

    /**
     * Note that if we've disabled SNMP support (and it was enabled)
     * forcing this "restart" actualy causes it to stop.  Doesn't sound
     * intuitive - but trust me.  The "etc/default/snmpd" file which we
     * write controls this.
     */
    private void restartDaemon()
    {
        try {
            logger.debug("Restarting the snmpd...");
            Integer result = UvmContextFactory.context().execManager().execResult( SNMP_WRAPPER_NAME );
            logger.debug("Restart of SNMPD exited with " + result);
        }
        catch(Exception ex) {
            logger.error("Error restarting snmpd", ex);
        }
    }

    private boolean isNotNullOrBlank(String s)
    {
        return s != null && !"".equals(s.trim());
    }

    private String qqOrNullToDefault(String str, String def)
    {
        return isNotNullOrBlank(str)? str:def;
    }

    private class UpdateTask implements Runnable
    {
        public void run()
        {
            logger.debug("doing automatic update");
            try {
                UvmContextImpl.context().toolboxManager().update();
            } catch (Exception exn) {
                logger.warn("could not update", exn);
            }

            if (getSettings().getAutoUpgrade()) {
                logger.debug("doing automatic upgrade");
                try {
                    UvmContextImpl.context().toolboxManager().upgrade();
                } catch (Exception exn) {
                    logger.warn("could not upgrade", exn);
                }
            }
        }
    }
}
