/**
 * $Id$
 */
package com.untangle.node.reports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.Reporting;
import com.untangle.uvm.node.HostnameLookup;
import com.untangle.uvm.servlet.DownloadHandler;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipelineConnector;
import org.apache.commons.codec.binary.Base64;

public class ReportsApp extends NodeBase implements Reporting, HostnameLookup
{
    public static final String REPORTS_EVENT_LOG_DOWNLOAD_HANDLER = "reportsEventLogExport";
    
    private static final Logger logger = Logger.getLogger(ReportsApp.class);

    private static final String REPORTS_GENERATE_TABLES_SCRIPT = System.getProperty("uvm.home") + "/bin/reports-generate-tables.py";
    private static final String REPORTS_GENERATE_REPORTS_SCRIPT = System.getProperty("uvm.home") + "/bin/reports-generate-reports.py";
    private static final String REPORTS_LOG = System.getProperty("uvm.log.dir") + "/reporter.log";

    private static final String CRON_STRING = "* * * root /usr/share/untangle/bin/reports-generate-reports.py -d $(date \"+\\%Y-\\%m-\\%d\") > /dev/null 2>&1";
    private static final File CRON_FILE = new File("/etc/cron.d/untangle-reports-nightly");
    private static final File SYSLOG_CONF_FILE = new File("/etc/rsyslog.d/untangle-remote.conf");

    protected static EventWriterImpl eventWriter = null;
    protected static EventReaderImpl eventReader = null;
    protected static ReportsManagerImpl    reportsManager = null;

    private ReportsSettings settings;
    
    public ReportsApp( NodeSettings nodeSettings, NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        if (eventWriter == null)
            eventWriter = new EventWriterImpl( this );
        if (eventReader == null)
            eventReader = new EventReaderImpl( this );
        if (reportsManager == null)
            reportsManager = new ReportsManagerImpl( this );
        ReportsManagerNewImpl.getInstance().setReportsNode( this );
        
        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new EventLogExportDownloadHandler() );
        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new ReportsEventLogExportDownloadHandler() );
        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new ImageDownloadHandler() );
    }

    public void setSettings( final ReportsSettings newSettings )
    {
        this.sanityCheck( newSettings );

        /**
         * Set the Alert Rules IDs
         */
        int idx = 0;
        for (AlertRule rule : newSettings.getAlertRules()) {
            rule.setRuleId(++idx);
        }
        
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "untangle-node-reports/" + "settings_"  + nodeID + ".js", newSettings );
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
        SyslogManagerImpl.reconfigure(this.settings);
    }

    public ReportsSettings getSettings()
    {
        return this.settings;
    }

    public void createSchemas()
    {
        // run commands to create user just in case
        UvmContextFactory.context().execManager().execResult("createuser -U postgres -dSR untangle >/dev/null 2>&1");
        UvmContextFactory.context().execManager().execResult("createdb -O postgres -U postgres uvm >/dev/null 2>&1");
        UvmContextFactory.context().execManager().execResult("createlang -U postgres plpgsql uvm >/dev/null 2>&1");

        // table the tablefunc module
        UvmContextFactory.context().execManager().execResult("psql -U postgres -c \"CREATE EXTENSION tablefunc\" uvm >/dev/null 2>&1");

        synchronized (this) {
            String cmd = REPORTS_GENERATE_TABLES_SCRIPT;
            ExecManagerResult result = UvmContextFactory.context().execManager().exec(cmd);
            if (result.getResult() != 0) {
                logger.warn("Failed to create schemas: \"" + cmd + "\" -> "  + result.getResult());
            }
            try {
                String lines[] = result.getOutput().split("\\r?\\n");
                logger.info("Creating Schema: ");
                for ( String line : lines )
                    logger.info("Schema: " + line);
            } catch (Exception e) {}
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

        flushEvents();
        
        synchronized (this) {
            do {
                tries++;
                tryAgain = false;
            
                exitCode = UvmContextFactory.context().execManager().execResult(REPORTS_GENERATE_REPORTS_SCRIPT + " -r 1 -m -d " + ts);

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

        if (ReportsApp.eventWriter != null)
            ReportsApp.eventWriter.forceFlush();
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
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://" + settings.getDbHost() + ":" + settings.getDbPort() + "/" + settings.getDbName();
            Properties props = new Properties();
            props.setProperty( "user", settings.getDbUser() );
            props.setProperty( "password", settings.getDbPassword() );
            props.setProperty( "charset", "unicode" );
            //props.setProperty( "logUnclosedConnections", "true" );

            return DriverManager.getConnection(url,props);
        }
        catch (Exception e) {
            logger.warn("Failed to connect to DB", e);
            return null;
        }
    }

    public ReportsManager getReportsManager()
    {
        return ReportsApp.reportsManager;
    }

    public ReportsManagerNew getReportsManagerNew()
    {
        return ReportsManagerNewImpl.getInstance();
    }
    
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return new PipelineConnector[0];
    }

    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        ReportsSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-reports/" + "settings_" + nodeID + ".js";

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
         * 11.1 conversion
         */
        if ( settings.getAlertRules() == null ) {
            settings.setAlertRules( defaultAlertRules() );
        }

        /**
         * Report updates
         */
        ReportsManagerNewImpl.getInstance().updateSystemReportEntries( settings.getReportEntries(), true );
        ReportsManagerNewImpl.getInstance().updateSystemEventEntries( settings.getEventEntries(), true );
        
        /* intialize schema (if necessary) */
        this.createSchemas();

        /* sync settings to disk if necessary */
        File settingsFile = new File( settingsFileName );
        if (settingsFile.lastModified() > CRON_FILE.lastModified())
            writeCronFile();
        if (settingsFile.lastModified() > SYSLOG_CONF_FILE.lastModified())
            SyslogManagerImpl.reconfigure(this.settings);
        SyslogManagerImpl.setEnabled(this.settings);
        
        /* Start the servlet */
        UvmContextFactory.context().tomcatManager().loadServlet("/reports", "reports");

        /* Enable to run event writing performance tests */
        // new Thread(new PerformanceTest()).start();
    }

    protected void preStart()
    {
        if (this.settings == null) {
            postInit();
        }

        ReportsApp.eventWriter.start( this );
    }

    protected void postStop()
    {
        ReportsApp.eventWriter.stop();
    }

    @Override
    protected void preDestroy() 
    {
        UvmContextFactory.context().tomcatManager().unloadServlet("/reports");
    }
    
    private LinkedList<AlertRule> defaultAlertRules()
    {
        LinkedList<AlertRule> rules = new LinkedList<AlertRule>();
        
        LinkedList<AlertRuleMatcher> matchers;
        AlertRuleMatcher matcher1;
        AlertRuleMatcher matcher2;
        AlertRule alertRule;
        
        matchers = new LinkedList<AlertRuleMatcher>();
        matcher1 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "class", "=", "*FailDEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "action", "=", "DISCONNECTED" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "WAN is offline", false, 0 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleMatcher>();
        matcher1 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "class", "=", "*SystemStatEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "load1", ">", "20" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "Server load is very high", true, 60 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleMatcher>();
        matcher1 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "class", "=", "*SystemStatEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "diskFreePercent", "<", ".2" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "Free disk space is low", true, 60 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleMatcher>();
        matcher1 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "class", "=", "*SystemStatEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "memFreePercent", "<", ".1" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "Free Memory is low", true, 60 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleMatcher>();
        matcher1 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "class", "=", "*ClassDLogEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "protochain", "=", "*BITTORRE*" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "Host is using Bittorrent", true, 60 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleMatcher>();
        matcher1 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "class", "=", "*PenaltyBoxEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "action", "=", "1" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "Host put in penalty box", false, 0 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleMatcher>();
        matcher1 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "class", "=", "*HttpResponseEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "contentLength", ">", "1000000000" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "Host is doing large download", true, 60 );
        rules.add( alertRule );
        
        matchers = new LinkedList<AlertRuleMatcher>();
        matcher1 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "class", "=", "*CaptureUserEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "event", "=", "FAILED" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "Failed Captive Portal login", false, 0 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleMatcher>();
        matcher1 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "class", "=", "*VirusHttpEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleMatcher( AlertRuleMatcher.MatcherType.FIELD_CONDITION, new AlertRuleMatcherField( "clean", "=", "False" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "HTTP virus blocked", false, 0 );
        rules.add( alertRule );

        return rules;
    }

    private ReportsSettings defaultSettings()
    {
        ReportsSettings settings = new ReportsSettings();
        settings.setAlertRules( defaultAlertRules() );
        return settings;
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

    public class PerformanceTest implements Runnable
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
                com.untangle.uvm.node.SessionEvent testEvent = new com.untangle.uvm.node.SessionEvent();
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
                testEvent.setPolicyId((long)1);
                testEvent.setUsername("test_username");
                testEvent.setHostname("test_hostname");
                logEvent(testEvent);
            }
        }
        
    }
    
    private class ReportsEventLogExportDownloadHandler extends EventLogExportDownloadHandler
    {
        @Override
        public String getName()
        {
            return REPORTS_EVENT_LOG_DOWNLOAD_HANDLER;
        }
        
        @Override
        public void serveDownload( HttpServletRequest req, HttpServletResponse resp )
        {
            String appName = req.getParameter("app");
            String detailName = req.getParameter("section");
            String type = req.getParameter("type");
            String value = req.getParameter("value");
            String dateArg = req.getParameter("date");
            String columnListStr = req.getParameter("colList");
            int numDays = 0;
            Date d;
            
            try {
                numDays = Integer.parseInt(req.getParameter("numDays"));
                long timestamp = Long.parseLong(dateArg);
                d = new Date(timestamp);
            } catch (Exception e) {
                logger.warn("Invalid parameters: " + numDays);
                return;
            }
            
            logger.info("Export CSV( name:" + appName + " detailName: " + detailName + " date: " + d + ")");
            SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd");
            String name = sdf.format(d) + "-" + appName + "-" + detailName;
            ResultSetReader resultSetReader = reportsManager.getAllDetailDataResultSet(d, numDays, appName, detailName, type, value);
            
            toCsv(resultSetReader, resp, columnListStr, name);
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
                EventEntry query = (EventEntry) UvmContextFactory.context().getSerializer().fromJSON( arg2 );
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

                ReportsApp reports = (ReportsApp) UvmContextFactory.context().nodeManager().node("untangle-node-reports");
                if (reports == null) {
                    logger.warn("reports node not found");
                    return;
                }
                ResultSetReader resultSetReader = ReportsManagerNewImpl.getInstance().getEventsForDateRangeResultSet( query, conditions, -1, startDate, endDate);
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
}
