/**
 * $Id$
 */
package com.untangle.node.reporting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.Reporting;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.User;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipeSpec;

public class ReportingNodeImpl extends NodeBase implements ReportingNode, Reporting
{
    private static final Logger logger = Logger.getLogger(ReportingNodeImpl.class);

    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/reporting-convert-settings.py";
    
    private static final String  REPORTS_SCRIPT = System.getProperty("uvm.home") + "/bin/reporting-generate-reports.py";
    private static final String  REPORTER_LOG_FILE = "/var/log/uvm/reporter.log";
    private static final long    REPORTER_LOG_FILE_READ_TIMEOUT = 180 * 1000; /* 180 seconds */

    private static final String CRON_STRING = "* * * root /usr/share/untangle/bin/reporting-generate-reports.py -d $(date \"+\\%Y-\\%m-\\%d\") > /dev/null 2>&1";
    private static final File CRON_FILE = new File("/etc/cron.d/untangle-reports-nightly");

    private static EventWriterImpl eventWriter = null;
    private static EventReaderImpl eventReader = null;

    private ReportingSettings settings;

    public ReportingNodeImpl( NodeSettings nodeSettings, NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        if (eventWriter == null)
            eventWriter = new EventWriterImpl( this );

        if (eventReader == null)
            eventReader = new EventReaderImpl( this );
    }

    public void setSettings(final ReportingSettings settings)
    {
        this.sanityCheck( settings );

        this._setSettings( settings );

        this.reconfigure();
    }

    public ReportingSettings getSettings()
    {
        return settings;
    }

    public void createSchemas()
    {
        // run commands to create user just in case
        UvmContextFactory.context().execManager().execResult("createuser -U postgres -dSR untangle");
        UvmContextFactory.context().execManager().execResult("createdb -O postgres -U postgres uvm");
        UvmContextFactory.context().execManager().execResult("createlang -U postgres plpgsql uvm");

        String cmd = REPORTS_SCRIPT + " -c";
        synchronized (this) {
            int exitCode = UvmContextFactory.context().execManager().execResult(cmd);
            if (exitCode != 0) {
                logger.warn("Failed to create schemas: \"" + cmd + "\" -> "  + exitCode);
            }
        }
    }

    public void runDailyReport() throws Exception
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date()); // now
        cal.add(Calendar.DATE, 1); // tomorrow
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String ts = df.format(cal.getTime());

        int exitCode = -1;
        logger.info("Running daily report...");
        boolean tryAgain = false;
        int tries = 0;

        synchronized (this) {
            do {
                tries++;
                tryAgain = false;
            
                exitCode = UvmContextFactory.context().execManager().execResult(REPORTS_SCRIPT + " -r 1 -m -d " + ts);

                /* exitCode == 1 means another reports process is running, just wait and try again. */
                if (exitCode == 1)  {
                    logger.warn("Report process already running. Waiting and then trying again...");
                    tryAgain = true;
                    Thread.sleep(10000); // sleep 10 seconds
                }
            }
            while (tryAgain && tries < 20); // try max 20 times (20 * 10 seconds = 200 seconds)
        }        
        if (exitCode != 0) {
            if (exitCode == 1) 
                throw new Exception("A reports process is already running. Please try again later.");
            else
                throw new Exception("Unable to create daily reports. (Exit code: " + exitCode + ")");
        }
    }

    public void flushEvents()
    {
        long currentTime  = System.currentTimeMillis();

        if (this.eventWriter != null)
            this.eventWriter.forceFlush();
    }
    
    public void initializeSettings()
    {
        setSettings( initSettings() );
    }

    public String lookupHostname( InetAddress address )
    {
        ReportingSettings settings = this.getSettings();
        if (settings == null)
            return null;
        LinkedList<ReportingHostnameMapEntry> nameMap = settings.getHostnameMap(); 
        if (nameMap == null)
            return null;
        
        for ( ReportingHostnameMapEntry entry : nameMap ) {
            if ( entry.getAddress() != null && entry.getAddress().contains(address))
                return entry.getHostname();
        }
        return null;
    }

    public void logEvent( LogEvent evt )
    {
        this.eventWriter.logEvent( evt );
    }

    public void forceFlush()
    {
        this.eventWriter.forceFlush();
    }

    public ArrayList getEvents( final String query, final Long policyId, final int limit )
    {
        return this.eventReader.getEvents( query, policyId, limit );
    }

    protected Connection getDbConnection()
    {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://" + settings.getDbHost() + ":" + settings.getDbPort() + "/" + settings.getDbName();
            Properties props = new Properties();
            props.setProperty( "user", settings.getDbUser() );
            props.setProperty( "password", settings.getDbPassword() );
            props.setProperty( "charset", "unicode" );

            return DriverManager.getConnection(url,props);
        }
        catch (Exception e) {
            logger.warn("Failed to connect to DB", e);
            return null;
        }
    }
    
    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return new PipeSpec[0];
    }

    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        ReportingSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-reporting/" + "settings_" + nodeID;

        try {
            readSettings = settingsManager.load( ReportingSettings.class, settingsFileName );
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
                String convertCmd = SETTINGS_CONVERSION_SCRIPT + " " + nodeID.toString() + " " + settingsFileName + ".js";
                logger.warn("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            } catch ( Exception e ) {
                logger.warn( "Conversion script failed.", e );
            } 

            try {
                readSettings = settingsManager.load( ReportingSettings.class, settingsFileName );
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

            this.initializeSettings();
        }
        else {
            logger.info("Loading Settings...");

            this.settings = readSettings;

            this.reconfigure();
            logger.info("Settings: " + this.settings.toJSONString());
        }

        // intialize default settings
        this.createSchemas();
    }

    protected void preStart()
    {
        if (this.settings == null) {
            postInit();
        }

        this.eventWriter.start();
    }

    protected void postStop()
    {
        this.eventWriter.stop();
    }

    
    private ReportingSettings initSettings()
    {
        ReportingSettings settings = new ReportingSettings();

        /* XXX for testing */
        LinkedList<ReportingHostnameMapEntry> foo  = settings.getHostnameMap();
        ReportingHostnameMapEntry bar = new ReportingHostnameMapEntry();
        bar.setAddress(new IPMaskedAddress("192.168.1.100/32"));
        bar.setHostname("foobar");
        foo.add(bar);
        settings.setHostnameMap(foo);
        
        return settings;
    }
    
    private void _setSettings( ReportingSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        try {
            settingsManager.save(ReportingSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-node-reporting/" + "settings_"  + nodeID, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }
    
    private void writeCronFile()
    {
        // write the cron file for nightly runs
        String conf = settings.getGenerationMinute() + " " + settings.getGenerationHour() + " " + CRON_STRING;
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(CRON_FILE));
            out.write(conf, 0, conf.length());
            out.write("\n");
        } catch (IOException ex) {
            logger.error("Unable to write file", ex);
            return;
        }
        try {
            out.close();
        } catch (IOException ex) {
            logger.error("Unable to close file", ex);
            return;
        }
    }

    private void reconfigure() 
    {
        logger.info("Reconfigure()");

        SyslogManagerImpl.reconfigure(this.settings);
        writeCronFile();
    }

    private void sanityCheck( ReportingSettings settings )
    {
        if ( settings.getReportingUsers() != null) {
            for ( ReportingUser user : settings.getReportingUsers() ) {
                if ( user.getOnlineAccess() ) {
                    if ( user.trans_getPasswordHash() == null )
                        throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": \"" + user.getEmailAddress() + "\" " + I18nUtil.marktr("has online access, but no password is set."));
                }
            }
        }


    }
}
