/**
 * $Id$
 */
package com.untangle.app.reports;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItem;

import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.EventManager;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.AdminUserSettings;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.event.EventRule;
import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.network.FilterRule;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.Reporting;
import com.untangle.uvm.app.HostnameLookup;
import com.untangle.uvm.servlet.DownloadHandler;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import org.apache.commons.codec.binary.Base64;

public class ReportsApp extends AppBase implements Reporting, HostnameLookup
{
    public static final String REPORTS_EVENT_LOG_DOWNLOAD_HANDLER = "reportsEventLogExport";
    
    private static final Logger logger = Logger.getLogger(ReportsApp.class);

    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd";
    private static final String REPORTS_GENERATE_TABLES_SCRIPT = System.getProperty("uvm.bin.dir") + "/reports-generate-tables.py";
    private static final String REPORTS_CLEAN_TABLES_SCRIPT = System.getProperty("uvm.bin.dir") + "/reports-clean-tables.py";
    private static final String REPORTS_VACUUM_TABLES_SCRIPT = System.getProperty("uvm.bin.dir") + "/reports-vacuum-yesterdays-tables.sh";
    private static final String REPORTS_GOOGLE_DATA_BACKUP_SCRIPT = System.getProperty("uvm.bin.dir") + "/reports-google-backup-yesterdays-data.sh";
    private static final String REPORTS_GOOGLE_CSV_BACKUP_SCRIPT = System.getProperty("uvm.bin.dir") + "/reports-google-backup-yesterdays-csv.sh";
    private static final String REPORTS_GENERATE_REPORTS_SCRIPT = System.getProperty("uvm.bin.dir") + "/reports-generate-reports.py";
    private static final String REPORTS_GENERATE_FIXED_REPORTS_SCRIPT = System.getProperty("uvm.bin.dir") + "/reports-generate-fixed-reports.py";
    private static final String REPORTS_RESTORE_DATA_SCRIPT = System.getProperty("uvm.bin.dir") + "/reports-restore-backup.sh";
    private static final String REPORTS_DB_DRIVER_FILE = System.getProperty("uvm.conf.dir") + "/database-driver";

    private static final File CRON_FILE = new File("/etc/cron.daily/reports-cron");

    protected static EventWriterImpl eventWriter = null;
    protected static EventReaderImpl eventReader = null;
    protected static String dbDriver = "postgresql";
    
    private ReportsSettings settings;
    
    public ReportsApp( AppSettings appSettings, AppProperties appProperties )
    {
        super( appSettings, appProperties );

        determineDbDriver();

        if (eventWriter == null)
            eventWriter = new EventWriterImpl( this );
        if (eventReader == null)
            eventReader = new EventReaderImpl( this );
        ReportsManagerImpl.getInstance().setReportsApp( this );
        
        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new EventLogExportDownloadHandler() );
        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new ImageDownloadHandler() );
        UvmContextFactory.context().servletFileManager().registerUploadHandler( new ReportsDataRestoreUploadHandler() );
    }

    public void setSettings( final ReportsSettings newSettings )
    {
        this.sanityCheck( newSettings );

        /**
         * Set the Email Template Ids
         */
        HashMap<Integer,Integer> mapOldNewEmailTemplateIds = new HashMap<Integer,Integer>();
        int idx = 0;
        for (EmailTemplate template : newSettings.getEmailTemplates()) {
            idx = ++idx;
            mapOldNewEmailTemplateIds.put(template.getTemplateId(), idx);
            template.setTemplateId(idx);
        }

        /* Manage id changes for users with email report flag. */
        if ( newSettings.getReportsUsers().size() > 0){
            for ( ReportsUser user : newSettings.getReportsUsers() ) {
                if (user.getEmailSummaries()){
                    if( user.getEmailTemplateIds() != null ){
                        if( user.getEmailTemplateIds().size() > 0 ) {
                            /* Walk existing list and map to new values. */
                            List<Integer> oldEmailTemplateIds = user.getEmailTemplateIds();
                            LinkedList<Integer> newEmailTemplateIds = new LinkedList<Integer>();
                            Integer newEmailTemplateId;
                            for(int i = 0; i < oldEmailTemplateIds.size(); i++){
                                newEmailTemplateId = mapOldNewEmailTemplateIds.get(oldEmailTemplateIds.get(i));
                                if(newEmailTemplateId != null){
                                    newEmailTemplateIds.push(newEmailTemplateId);
                                }
                            }
                            user.setEmailTemplateIds(newEmailTemplateIds);
                        }
                        if( user.getEmailTemplateIds().size() == 0) {
                            /* If never set or all removed, add the default. */
                            LinkedList<Integer> emailTemplateIds = new LinkedList<Integer>();
                            emailTemplateIds.push(1);
                            user.setEmailTemplateIds(emailTemplateIds);
                        }
                    }
                }
            }
        }
        
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "reports/" + "settings_"  + nodeID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        /**
         * Sync settings to disk
         */
        writeCronFile();
    }

    public ReportsSettings getSettings()
    {
        return this.settings;
    }

    public void initializeDB()
    {
        synchronized (this) {
            if ( "postgresql".equals( ReportsApp.dbDriver ) ) {

                /**
                 * Run the script to generate/update the tables
                 */
                String cmd = REPORTS_GENERATE_TABLES_SCRIPT  + " -d postgresql";
                ExecManagerResult result = UvmContextFactory.context().execManager().exec( cmd );

                if (result.getResult() != 0) {
                    logger.warn("Failed to create tables: \"" + cmd + "\" -> "  + result.getResult());
                }
                try {
                    String lines[] = result.getOutput().split("\\r?\\n");
                    logger.info("Creating Tables: ");
                    for ( String line : lines )
                        logger.info("Tables: " + line);
                } catch (Exception e) {}

                /**
                 * Set global properties
                 * Postgres uses the "reports" schema and supports partitions
                 */
                LogEvent.setSchemaPrefix("reports.");
                LogEvent.setPartitionsSupported(true);
            }
            else if ( "sqlite".equals( ReportsApp.dbDriver ) ) {

                /**
                 * Run the script to generate/update the tables
                 */
                String cmd = REPORTS_GENERATE_TABLES_SCRIPT + " -d sqlite";
                ExecManagerResult result = UvmContextFactory.context().execManager().exec( cmd );
                if (result.getResult() != 0) {
                    logger.warn("Failed to create tables: \"" + cmd + "\" -> "  + result.getResult());
                }
                try {
                    String lines[] = result.getOutput().split("\\r?\\n");
                    logger.info("Creating Tables: ");
                    for ( String line : lines )
                        logger.info("Tables: " + line);
                } catch (Exception e) {}

                /**
                 * Set global properties
                 * SQlite does not use a schema and does not support partitions
                 */
                LogEvent.setSchemaPrefix("");
                LogEvent.setPartitionsSupported(false);
            }
        }
    }

    public void runFixedReport() throws Exception
    {
        flushEvents();

        synchronized (this) {
            String url = "https://" + UvmContextFactory.context().networkManager().getPublicUrl() + "/reports/";
            for( EmailTemplate emailTemplate : settings.getEmailTemplates() ){
                FixedReports fixedReports = new FixedReports();
                List<ReportsUser> users = new LinkedList<ReportsUser>();
                for ( ReportsUser user : settings.getReportsUsers() ) {
                    if( user.getEmailSummaries() && user.getEmailTemplateIds().contains(emailTemplate.getTemplateId()) ){
                        users.add(user);
                    }
                }
                if( users.size() > 0){
                    fixedReports.generate(emailTemplate, users, url, ReportsManagerImpl.getInstance());
                }
            }
        }        
    }

    public void flushEvents()
    {
        forceFlush();
    }

    protected int getEventsPendingCount()
    {
        if (ReportsApp.eventWriter != null)
            return ReportsApp.eventWriter.getEventsPendingCount();

        return 0;
    }
    
    public void initializeSettings()
    {
        setSettings( defaultSettings() );
    }

    public String lookupHostname( InetAddress address )
    {
        ReportsSettings settings = this.getSettings();
        if (settings == null)
            return null;
        LinkedList<ReportsHostnameMapEntry> nameMap = settings.getHostnameMap(); 
        if (nameMap == null)
            return null;
        
        for ( ReportsHostnameMapEntry entry : nameMap ) {
            if ( entry.getAddress() != null && entry.getAddress().contains(address))
                return entry.getHostname();
        }
        return null;
    }

    public void logEvent( LogEvent evt )
    {
        ReportsApp.eventWriter.logEvent( evt );
    }

    public void forceFlush()
    {
        logger.info("forceFlush() ...");
        if (ReportsApp.eventWriter != null)
            ReportsApp.eventWriter.forceFlush();
    }

    public double getAvgWriteTimePerEvent()
    {
        return ReportsApp.eventWriter.getAvgWriteTimePerEvent();
    }

    public long getWriteDelaySec()
    {
        return ReportsApp.eventWriter.getWriteDelaySec();
    }
    
    public Connection getDbConnection()
    {
        try {
            String url = null;
            Properties props = new Properties();

            if ( "postgresql".equals( ReportsApp.dbDriver ) ) {
                Class.forName("org.postgresql.Driver");
                url = "jdbc:" + ReportsApp.dbDriver + "://" + settings.getDbHost() + ":" + settings.getDbPort() + "/" + settings.getDbName();

                props.setProperty( "user", settings.getDbUser() );
                props.setProperty( "password", settings.getDbPassword() );
                props.setProperty( "charset", "unicode" );
                //props.setProperty( "logUnclosedConnections", "true" );
            }
            else if ( "sqlite".equals( ReportsApp.dbDriver ) ) {
                File dir = new File("/var/lib/sqlite");
                if ( !dir.exists() )
                    UvmContextFactory.context().execManager().execResult( "mkdir -p /var/lib/sqlite");
                Class.forName("org.sqlite.JDBC");
                url = "jdbc:" + ReportsApp.dbDriver + ":" + "/var/lib/sqlite/reports.db";
            } else {
                logger.warn("Unknown driver: " + ReportsApp.dbDriver );
                return null;
            }

            return DriverManager.getConnection(url,props);
        }
        catch (Exception e) {
            logger.warn("Failed to connect to DB", e);
            return null;
        }
    }

    public ReportsManager getReportsManager()
    {
        return ReportsManagerImpl.getInstance();
    }
    
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return new PipelineConnector[0];
    }

    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        ReportsSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/reports/" + "settings_" + nodeID + ".js";

        conversion_paths_13_0_0();

        try {
            readSettings = settingsManager.load( ReportsSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
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
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        /**
         * 12.1 conversion
         */
        if ( settings.getVersion() == 1 ) {
            logger.warn("Running v12.1 conversion...");
            conversion_12_1();
        }

        /**
         * 12.2 conversion
         */
        if ( settings.getVersion() == 3 ) {
            logger.warn("Running v12.2 conversion...");
            conversion_12_2_0();
        }
        /*
         * 13.3 conversion
         */
        if ( settings.getVersion() == 4 ) {
            logger.warn("Running v13.0 conversion...");
            conversion_13_0_0();
        }

        /**
         * Report updates
         */
        ReportsManagerImpl.getInstance().updateSystemReportEntries( settings.getReportEntries(), true );
        
        /* sync settings to disk if necessary */
        File settingsFile = new File( settingsFileName );
        if (settingsFile.lastModified() > CRON_FILE.lastModified()){
            writeCronFile();
        }
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        if (this.settings == null) {
            postInit();
        }

        /* Start the servlet */
        UvmContextFactory.context().tomcatManager().loadServlet("/reports", "reports");

        /* Start the database */
        if ( "postgresql".equals( ReportsApp.dbDriver ) )
            UvmContextFactory.context().daemonManager().incrementUsageCount( "postgresql" );

        /* Intialize database tables (if necessary) */
        this.initializeDB();

        ReportsApp.eventWriter.start( this );

        /* Enable to run event writing performance tests */
        // new Thread(new PerformanceTest()).start();
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        ReportsApp.eventWriter.stop();

        UvmContextFactory.context().tomcatManager().unloadServlet("/reports");

        if ( "postgresql".equals( ReportsApp.dbDriver ) )
            UvmContextFactory.context().daemonManager().decrementUsageCount( "postgresql" );
    }

    @Override
    protected void preDestroy() 
    {
    }
    

    
    private LinkedList<EmailTemplate> defaultEmailTemplates()
    {
        LinkedList<EmailTemplate> templates = new LinkedList<EmailTemplate>();

        EmailTemplate emailTemplate;
        LinkedList<String> enabledConfigIds;
        LinkedList<String> enabledAppIds;

        enabledConfigIds = new LinkedList<String>();
        enabledConfigIds.add("_recommended");
        enabledAppIds = new LinkedList<String>();
        enabledAppIds.add("_recommended");
        emailTemplate = new EmailTemplate( I18nUtil.marktr("Daily Reports"), I18nUtil.marktr("Recommended daily reports (default)"), 86400, false, enabledConfigIds, enabledAppIds);
        emailTemplate.setReadOnly(true);
        templates.add( emailTemplate );

        return templates;

    }

    private LinkedList<ReportsUser> defaultReportsUsers(LinkedList<ReportsUser> reportsUsers)
    {
        List<Integer> templateIds = new LinkedList<Integer>();
        templateIds.add(0);

        if(reportsUsers == null){
            reportsUsers = new LinkedList<ReportsUser>();
        }else{
            for(ReportsUser reportsUser : reportsUsers){
                if(reportsUser.getEmailSummaries() && reportsUser.getEmailTemplateIds() == null){
                    reportsUser.setEmailTemplateIds(templateIds);
                }
            }
        }

        Boolean adminUserFound = false;
        for(ReportsUser reportsUser : reportsUsers){
            if(reportsUser.getEmailAddress().equals("admin")){
                adminUserFound = true;
            }
        }

        if( adminUserFound == false ){
            ReportsUser adminUser = new ReportsUser();
            adminUser.setEmailAddress("admin");
            adminUser.setEmailAlerts(true);
            adminUser.setEmailSummaries(true);
            adminUser.setEmailTemplateIds(templateIds);
            reportsUsers.add(adminUser);
        }

        return reportsUsers;
    }

    private ReportsSettings defaultSettings()
    {
        ReportsSettings settings = new ReportsSettings();
        settings.setVersion( 5 );
        settings.setEmailTemplates( defaultEmailTemplates() );
        settings.setReportsUsers( defaultReportsUsers( null ) );

        if ( "sqlite".equals( ReportsApp.dbDriver ) ) {
            settings.setDbRetention( 1 );
        } else {
            settings.setDbRetention( 7 );
        }

        return settings;
    }
    
    private String determineDbDriver()
    {
        try {
            File keyFile = new File(REPORTS_DB_DRIVER_FILE);
            if (keyFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(keyFile));
                String value = reader.readLine();

                switch( value ) {
                case "sqlite":
                    break;
                case "postgresql":
                    break;
                default:
                    logger.warn("Unknown database driver: " + value);
                    logger.warn("Using postgresql driver...");
                    value = "postgresql";
                }

                ReportsApp.dbDriver = value;
                return ReportsApp.dbDriver;
            }
        } catch (IOException x) {
            logger.error("Unable to read DB driver", x);
        }
        return ReportsApp.dbDriver;
    }

    private void writeCronFile()
    {
        // write the cron file for nightly runs
        String cronStr = "#!/bin/sh" + "\n" +
            REPORTS_GENERATE_TABLES_SCRIPT + " | logger -t uvmreports" + "\n" +
            REPORTS_CLEAN_TABLES_SCRIPT + " -d " + ReportsApp.dbDriver + " " + settings.getDbRetention() + " | logger -t uvmreports" + "\n" +
            REPORTS_VACUUM_TABLES_SCRIPT + " | logger -t uvmreports" + "\n" +
            REPORTS_GENERATE_FIXED_REPORTS_SCRIPT + " | logger -t uvmreports" + "\n";

        if ( settings.getGoogleDriveUploadData() ) {
            String dir = settings.getGoogleDriveDirectory();
            if ( dir != null )
                dir = " -d \"" + settings.getGoogleDriveDirectory() + "\"";
            else
                dir = "";
            
            cronStr += REPORTS_GOOGLE_DATA_BACKUP_SCRIPT + " " + dir + " | logger -t uvmreports" + "\n";
        }
        if ( settings.getGoogleDriveUploadCsv() ) {
            String dir = settings.getGoogleDriveDirectory();
            if ( dir != null )
                dir = " -d \"" + settings.getGoogleDriveDirectory() + "\"";
            else
                dir = "";
            
            cronStr += REPORTS_GOOGLE_CSV_BACKUP_SCRIPT + " " + dir + " | logger -t uvmreports" + "\n";
        }

        
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(CRON_FILE));
            out.write(cronStr, 0, cronStr.length());
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

        // Make it executable
        UvmContextFactory.context().execManager().execResult( "chmod 755 " + CRON_FILE );
    }

    private void sanityCheck( ReportsSettings settings )
    {
        if ( settings.getReportsUsers() != null) {
            for ( ReportsUser user : settings.getReportsUsers() ) {
                if ( user.getOnlineAccess() ) {
                    if ( user.trans_getPasswordHash() == null )
                        throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": \"" + user.getEmailAddress() + "\" " + I18nUtil.marktr("has online access, but no password is set."));
                }
            }
        }
    }

    private int restoreData( FileItem item )
    {
        try {
            //validate zip
            if (!item.getName().endsWith(".gz")) {
                throw new RuntimeException("Invalid name: " + item.getName());
            }

            String filename = "/tmp/reports_restore_data.sql.gz";
            File file = new File(filename);
            if (file.exists())
                file.delete();
            item.write( file );

            String cmd = REPORTS_RESTORE_DATA_SCRIPT + " -f " + filename;
            return UvmContextFactory.context().execManager().execResult(cmd);
        } catch (Exception e) {
            logger.error(e);
            throw new RuntimeException("Restore Data Failed");
        }
    }

    private void conversion_12_1()
    {
        settings.setVersion( 2 );

        for ( ReportEntry entry : settings.getReportEntries() ) {
            if ( entry.getType() == ReportEntry.ReportEntryType.PIE_GRAPH && entry.getPieStyle() == null ) {
                entry.setPieStyle( ReportEntry.PieStyle.PIE );
            }
        }
        
        setSettings( settings );
    }

    private void conversion_12_2_0()
    {
        settings.setVersion( 4 );

        try {
            settings.setEmailTemplates( defaultEmailTemplates() );
        } catch (Exception e) {
            logger.warn("Conversion Exception",e);
        }

        try {
            settings.setReportsUsers( defaultReportsUsers(settings.getReportsUsers()) );
        } catch (Exception e) {
            logger.warn("Conversion Exception",e);
        }

        setSettings( settings );
    }

    private void conversion_paths_13_0_0()
    {
        int result = UvmContextFactory.context().execManager().execResult("/bin/grep -q com.untangle.app.reports.AlertRule " + System.getProperty("uvm.settings.dir") + "/" + "/reports/" + "/settings*.js");
        if ( result != 0 )
            return;

        // Convert event rule paths to new locations for 12.2 to 13.0
        String[] oldNames = new String[] {
                                "com.untangle.app.reports.AlertRuleCondition",
                                "com.untangle.app.reports.AlertRuleConditionField",
                                "com.untangle.app.reports.AlertRule"
                             };
        String[] newNames = new String[] {
                                "com.untangle.uvm.event.EventRuleCondition",
                                "com.untangle.uvm.event.EventRuleConditionField",
                                "com.untangle.uvm.event.AlertRule"
                             };
        for ( int i = 0 ; i < oldNames.length ; i++ ) {
            String oldStr = oldNames[i];
            String newStr = newNames[i];
            UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + "/reports/" + "/*");
        }
    }

    private void conversion_13_0_0()
    {
        settings.setVersion( 5 );

        try {
            if(settings.getAlertRules() != null){
                EventManager eventManager = UvmContextFactory.context().eventManager();
                if(eventManager != null){
                    EventSettings eventSettings = eventManager.getSettings();
                    if(eventSettings != null){
                        eventSettings.setAlertRules(settings.getAlertRules());

                        // Syslog
                        eventSettings.setSyslogEnabled(settings.getSyslogEnabled());
                        eventSettings.setSyslogHost(settings.getSyslogHost());
                        eventSettings.setSyslogPort(settings.getSyslogPort());
                        eventSettings.setSyslogProtocol(settings.getSyslogProtocol());

                        eventSettings.setVersion(settings.getVersion());

                        eventManager.setSettings(eventSettings);

                        settings.setAlertRules(null);
                    }
                }
            }

            // Rename "Alert" report categories to "Event"
            if(settings.getReportEntries() != null){
                for ( ReportEntry entry : settings.getReportEntries() ) {
                    if ( entry.getTitle().contains("Alert") && entry.getCategory().equals("Reports") ){
                        entry.setCategory( "Events" );
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Conversion Exception",e);
        }

        setSettings( settings );
    }



    private class PerformanceTest implements Runnable
    {
        public void run()
        {
            for (int i=0; i<5; i++) logger.warn("--- Running Performance Tests ---");

            java.util.Random r = new java.util.Random();
            long sessionId = r.nextLong();

            InetAddress c_client;
            InetAddress c_server;
            InetAddress s_client;
            InetAddress s_server;
        
            try {
                c_client = InetAddress.getByName("192.168.1.100");
                c_server = InetAddress.getByName("1.2.3.4");
                s_client = InetAddress.getByName("4.3.2.1");
                s_server = InetAddress.getByName("1.2.3.4");
            } catch (Exception e) {
                logger.warn("Failed to run tests.",e);
                return;
            }
        
            while (true) {
                if ((sessionId%10) == 0) try {Thread.sleep(1);} catch (Exception e){}
                
                sessionId++;
                com.untangle.uvm.app.SessionEvent testEvent = new com.untangle.uvm.app.SessionEvent();
                testEvent.setSessionId(sessionId);
                testEvent.setBypassed(false);
                testEvent.setProtocol((short)6);
                testEvent.setClientIntf(2);
                testEvent.setServerIntf(2);
                testEvent.setCClientAddr(c_client);
                testEvent.setSClientAddr(s_client);
                testEvent.setCServerAddr(c_server);
                testEvent.setSServerAddr(s_server);
                testEvent.setCClientPort(1234);
                testEvent.setSClientPort(1234);
                testEvent.setCServerPort(80);
                testEvent.setSServerPort(80);
                testEvent.setPolicyId(1);
                testEvent.setUsername("test_username");
                testEvent.setHostname("test_hostname");
                logEvent(testEvent);
            }
        }
        
    }
    
    private class EventLogExportDownloadHandler implements DownloadHandler
    {
        private static final String CHARACTER_ENCODING = "utf-8";

        @Override
        public String getName()
        {
            return "eventLogExport";
        }
        
        protected void toCsv( ResultSetReader resultSetReader, HttpServletResponse resp, String columnListStr, String name )
        {
            if (resultSetReader == null)
                return;

            try {        
                // Write content type and also length (determined via byte array).
                resp.setCharacterEncoding(CHARACTER_ENCODING);
                resp.setHeader("Content-Type","text/csv");
                resp.setHeader("Content-Disposition","attachment; filename="+name+".csv");
                // Write the header
                resp.getWriter().write(columnListStr + "\n");
                resp.getWriter().flush();

                ResultSet resultSet = resultSetReader.getResultSet();
                if (resultSet == null)
                    return;

                ResultSetMetaData metadata = resultSet.getMetaData();
                int numColumns = metadata.getColumnCount();
                String[] columnList = columnListStr.split(",");

                // Write each row 
                while (resultSet.next()) {
                    // build JSON object from columns
                    int writtenColumnCount = 0;

                    for ( String columnName : columnList ) {
                        Object o = null;
                        try {
                            o = resultSet.getObject( columnName );
                        } catch (Exception e) {
                            // do nothing - object not found
                        }
                        String oStr = "";
                        if (o != null)
                            oStr = o.toString().replaceAll(",","");
                    
                        if (writtenColumnCount != 0)
                            resp.getWriter().write(",");
                        resp.getWriter().write(oStr);
                        writtenColumnCount++;
                    }
                    resp.getWriter().write("\n");
                }
            } catch (Exception e) {
                logger.warn("Failed to export CSV.",e);
            } finally {
                resultSetReader.closeConnection();
            }
        }
        
        private Date getDate(String ts)
        {
            try {
                long l = Long.parseLong(ts);
                if ( l != -1) {
                    return new Date(l);
                }
            } catch (NumberFormatException ex) {}
            return null;
        }
        
        public void serveDownload( HttpServletRequest req, HttpServletResponse resp )
        {
            try {
                String arg1 = req.getParameter("arg1");
                String arg2 = req.getParameter("arg2");
                String arg3 = req.getParameter("arg3");
                String arg4 = req.getParameter("arg4");
                String arg5 = req.getParameter("arg5");
                String arg6 = req.getParameter("arg6");

                if ( "".equals(arg2) || arg2 == null )
                    throw new RuntimeException("Invalid arguments");

                String name = arg1;
                ReportEntry query = (ReportEntry) UvmContextFactory.context().getSerializer().fromJSON( arg2 );
                SqlCondition[] conditions;
                String columnListStr = arg4;
                Date startDate = getDate(arg5);
                Date endDate = getDate(arg6);

                if ( "".equals(arg3) || arg3 == null )
                    conditions = null;
                else
                    conditions = (SqlCondition[]) UvmContextFactory.context().getSerializer().fromJSON( req.getParameter("arg3") );

                if (name == null || query == null || columnListStr == null) {
                    logger.warn("Invalid parameters: " + name + " , " + query + " , " + columnListStr);
                    return;
                }

                logger.info("Export CSV( name:" + name + " query: " + query + " columnList: " + columnListStr + ")");

                ReportsApp reports = (ReportsApp) UvmContextFactory.context().appManager().app("reports");
                if (reports == null) {
                    logger.warn("reports node not found");
                    return;
                }
                ResultSetReader resultSetReader = ReportsManagerImpl.getInstance().getEventsForDateRangeResultSet( query, conditions, -1, startDate, endDate);
                toCsv( resultSetReader, resp, columnListStr, name );
            } catch (Exception e) {
                logger.warn( "Failed to build CSV.", e );
                throw new RuntimeException(e);
            }
        }
    }
    
    // called by the UI to download images
    private class ImageDownloadHandler implements DownloadHandler
    {
        @Override
        public String getName()
        {
            return "imageDownload";
        }

        @Override
        public void serveDownload(HttpServletRequest req, HttpServletResponse resp)
        {
            try {
                String fileName = req.getParameter("arg1");
                String dataUrl = req.getParameter("arg2");
                String encodingPrefix = "base64,";
                int contentStartIndex = dataUrl.indexOf(encodingPrefix) + encodingPrefix.length();
                byte[] imageData = Base64.decodeBase64(dataUrl.substring(contentStartIndex).getBytes());
                
                resp.setContentType("image/png");
                resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                OutputStream out = resp.getOutputStream();
                
                out.write(imageData);
                
                out.flush();
                out.close();
            } catch (Exception e) {
                logger.warn("Failed to download image",e);
            }
        }
    }

    private class ReportsDataRestoreUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "reportsDataRestore";
        }
        
        @Override
        public String handleFile(FileItem fileItem, String argument) throws Exception
        {
            try {
                int ret = restoreData(fileItem);

                if ( ret == 0 ) {
                    return I18nUtil.marktr("Successfully restored data");
                } else {
                    return I18nUtil.marktr("Error restoring data:") + " " + ret;
                }
                    
            } catch ( Exception e ) {
                return I18nUtil.marktr("Error restoring data:") + " " + e.toString();
            }
         }
    }
    
}
