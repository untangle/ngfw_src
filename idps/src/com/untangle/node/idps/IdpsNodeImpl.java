/**
 * $Id: IdpsNodeImpl.java 38584 2014-09-03 23:23:07Z dmorris $
 */
package com.untangle.node.idps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.servlet.DownloadHandler;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.IOUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.ExecManagerResultReader;

public class IdpsNodeImpl extends NodeBase implements IdpsNode
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String STAT_SCAN = "scan";
    private static final String STAT_DETECT = "detect";
    private static final String STAT_BLOCK = "block";
    
    private final EventHandler handler;
    private final PipelineConnector [] connectors = new PipelineConnector[0];
    private final IdpsEventMonitor idpsEventMonitor;    

    private EventLogQuery allEventQuery;
    private EventLogQuery blockedEventQuery;

    private static final String GET_UPDATES_SCRIPT = "idps-get-updates.py";
    private static final String IPTABLES_SCRIPT = "/etc/untangle-netd/iptables-rules.d/740-snort";
    private static final String GET_LAST_UPDATE = System.getProperty( "uvm.bin.dir" ) + "/idps-get-last-update-check";

    private float memoryThreshold = .25f;
    private boolean updatedSettingsFlag = false;

    public IdpsNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        handler = new EventHandler(this);

        this.addMetric(new NodeMetric(STAT_SCAN, I18nUtil.marktr("Sessions scanned")));
        this.addMetric(new NodeMetric(STAT_DETECT, I18nUtil.marktr("Sessions logged")));
        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
        
        this.idpsEventMonitor   = new IdpsEventMonitor( this );

        this.allEventQuery = new EventLogQuery(I18nUtil.marktr("All Events"),
                                               "SELECT * FROM reports.idps_events " + 
                                               "ORDER BY time_stamp DESC");

        this.blockedEventQuery = new EventLogQuery(I18nUtil.marktr("Blocked Events"),
                                               "SELECT * FROM reports.idps_events " + 
                                               "WHERE blocked IS TRUE " +
                                               "ORDER BY time_stamp DESC");

        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new IdpsSettingsDownloadHandler() );
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    public EventLogQuery[] getEventQueries()
    {
        return new EventLogQuery[] { this.allEventQuery, this.blockedEventQuery };
    }

    protected void postInit()
    {
        logger.info("Post init");

        readNodeSettings();
    }

    protected void preStop()
    {
        removeCronEntry();
        try{
            this.idpsEventMonitor.disable();
        }catch( Exception e ){
            logger.warn( "Error disabling IDPS Event Monitor", e );
        }
    }

    protected void postStop()
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount( "snort-untangle" );
        iptablesRules();
    }

    protected void preStart()
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount( "snort-untangle" );
        this.idpsEventMonitor.start();
        this.idpsEventMonitor.enable();
    }

    protected void postStart(){
        iptablesRules();
        addCronEntry();
    }

    public void reconfigure(){
        String nodeId = this.getNodeSettings().getId().toString();
        String configCmd = new String(System.getProperty("uvm.bin.dir") + 
            "/idps-create-config.py" + 
            " --node_id " + nodeId +
            " --iptables_script " + IPTABLES_SCRIPT
        );

        String result = UvmContextFactory.context().execManager().execOutput(configCmd );
        try{
            String lines[] = result.split("\\r?\\n");
            logger.warn("idps config: ");
            for ( String line : lines ){
                logger.warn("idps config: " + line);
            }
        }catch( Exception e ){
            logger.warn( "Unable to generate snort configuration:", e );
        }
        reloadEventMonitorMap();
        stop();
        start();
    }

    public Date getLastUpdate()
    {
        try {
            String result = UvmContextFactory.context().execManager().execOutput( GET_LAST_UPDATE + " rules");
            long timeSeconds = Long.parseLong( result.trim());

            return new Date( timeSeconds * 1000l );
        } catch ( Exception e ) {
            logger.warn( "Unable to get last update.", e );
            return null;
        } 
    }

    public Date getLastUpdateCheck()
    {
        try {
            String result = UvmContextFactory.context().execManager().execOutput( GET_LAST_UPDATE );
            long timeSeconds = Long.parseLong( result.trim());

            return new Date( timeSeconds * 1000l );
        } catch ( Exception e ) {
            logger.warn( "Unable to get last update.", e );
            return null;
        } 
    }

    // private methods ---------------------------------------------------------

    private void readNodeSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-idps/settings_" + nodeID + ".js";

        logger.info("Loading settings from " + settingsFile);

    }

    public void setScanCount( long value )
    {
        this.setMetric(STAT_SCAN, value);
    }

    public void setDetectCount( long value)
    {
        this.setMetric(STAT_DETECT, value);
    }

    public void setBlockCount( long value )
    {
        this.setMetric(STAT_BLOCK, value);
    }

    /**
     * Insert or remove iptables rules if snort daemon is running
     */
    private synchronized void iptablesRules()
    {
        File f = new File( IPTABLES_SCRIPT  );
        if( !f.exists() ){
            logger.warn("Cannot find init script:" + IPTABLES_SCRIPT);
        }

        ExecManagerResult result = UvmContextFactory.context().execManager().exec( IPTABLES_SCRIPT );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( IPTABLES_SCRIPT + ": ");
            for ( String line : lines )
                logger.info( IPTABLES_SCRIPT + ": " + line);
        } catch (Exception e) {
            logger.warn( "Unable to process " + IPTABLES_SCRIPT + ":", e );
        }

        if ( result.getResult() != 0 ) {
            logger.error("Failed to run " + IPTABLES_SCRIPT+ " (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to manage rules");
        }
    }

    /**
     * If cron entry does not exist, add it.
     */
    private void addCronEntry()
    {
        File fCron = new File( "/etc/cron.daily/" + GET_UPDATES_SCRIPT );
        if( fCron.exists() ){
            return;
        }
        File fBin = new File( System.getProperty("uvm.bin.dir") + "/" + GET_UPDATES_SCRIPT );

        try {
            IOUtil.copyFile( fBin, fCron );
        } catch (IOException e) {
            logger.warn("Unable to copy updates file to cron.daily:  ", e);
        }
    }

    private void removeCronEntry()
    {
        File fCron = new File( "/etc/cron.daily/" + GET_UPDATES_SCRIPT );
        if( fCron.exists() == false ){
            return;
        }

        IOUtil.delete( fCron );
    }

    public String getSettingsFileName()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeId = this.getNodeSettings().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-node-idps/settings_" + nodeId + ".js";
        return settingsName;
    }

    public String getWizardSettingsFileName()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeId = this.getNodeSettings().getId().toString();

        ExecManager execManager = UvmContextFactory.context().createExecManager();
        String memorySettings = "";
        long totalMemory = 0;
        String result = execManager.execOutput( "/usr/bin/awk '/MemTotal:/ {print $2}' /proc/meminfo" );
        execManager.close();
        if ( result != null &&
             ! "".equals(result) ) {
            totalMemory = Long.parseLong( result.trim() ) * 1024;
        }
        if( totalMemory <= 1073741824L + ( 1073741824L * memoryThreshold ) ){
            memorySettings = "_1GB";
        }else if( totalMemory <= 2147483648L + ( 2147483648L * memoryThreshold ) ){
            memorySettings = "_2GB";
        }
        // Otherwise use "defaults.js"

        String settingsName = System.getProperty("uvm.lib.dir") + "/untangle-node-idps/defaults" + memorySettings + ".js";
        return settingsName;
    }

    public void initializeSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeId = this.getNodeSettings().getId().toString();
        String tempFileName = "/tmp/settings_" + getNodeSettings().getNodeName() + "_" + nodeId + ".js";

        String configCmd = new String(System.getProperty("uvm.bin.dir") + 
            "/idps-sync-settings.py" + 
            " --node_id " + nodeId +
            " --rules /usr/share/untangle-snort-config/current/rules" +
            " --settings " + tempFileName
        );
        String result = UvmContextFactory.context().execManager().execOutput(configCmd );
        try{
            String lines[] = result.split("\\r?\\n");
            logger.warn("idps config: ");
            for ( String line : lines ){
                logger.warn("idps config: " + line);
            }
        }catch( Exception e ){
            logger.warn("Unable to initialize settings: ", e );
        }

        try {
            settingsManager.save( getSettingsFileName(), tempFileName, true );
        } catch (Exception exn) {
            logger.error("Could not save node settings", exn);
        }
    }

    public void createDefaultSettings( String filename )
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();

        String configCmd = new String(System.getProperty("uvm.bin.dir") + 
            "/idps-generate-defaults.py" + 
            " --templates /usr/share/untangle-snort-config/current/templates" +
            " --rules /usr/share/untangle-snort-config/current/rules" +
            " --defaults " + System.getProperty("uvm.lib.dir") + "/untangle-node-idps" +
            " --filename " + filename
        );
        String result = UvmContextFactory.context().execManager().execOutput(configCmd );
        try{
            String lines[] = result.split("\\r?\\n");
            logger.warn("idps config: ");
            for ( String line : lines ){
                logger.warn("idps config: " + line);
            }
        }catch( Exception e ){
            logger.warn("Unable to generate defaults: ", e );
        }
    }

    public void saveSettings( String tempFileName )
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();

        try {
            settingsManager.save( getSettingsFileName(), tempFileName, true );
        } catch (Exception exn) {
            logger.error("Could not save node settings", exn);
        }
    }

    public void setUpdatedSettingsFlag( boolean updatedSettingsFlag ){
        this.updatedSettingsFlag = updatedSettingsFlag;
    }
    public boolean getUpdatedSettingsFlag(){
        return this.updatedSettingsFlag;
    }

    public void reloadEventMonitorMap(){
        this.idpsEventMonitor.unified2Parser.reloadEventMap();
    }

    private class IdpsSettingsDownloadHandler implements DownloadHandler
    {
        private static final String CHARACTER_ENCODING = "utf-8";

        @Override
        public String getName()
        {
            return "IdpsSettings";
        }
        
        public void serveDownload( HttpServletRequest req, HttpServletResponse resp )
        {

            String action = req.getParameter("arg1");
            String nodeId = req.getParameter("arg2");

            UvmContext uvm = UvmContextFactory.context();
            NodeManager nm = uvm.nodeManager();
            IdpsNode node = (IdpsNode) nm.node( Long.parseLong(nodeId) );

            if (node == null ) {
                logger.warn("Invalid parameters: " + nodeId );
                return;
            }

            if( action.equals("load") ||
                action.equals("wizard") ){
                String settingsName;
                if( action.equals("wizard") ){  
                    settingsName = node.getWizardSettingsFileName();
                }else{
                    settingsName = node.getSettingsFileName();
                }
                try{
                    resp.setCharacterEncoding(CHARACTER_ENCODING);
                    resp.setHeader("Content-Type","application/json");

                    File f = new File( settingsName );
                    if( !f.exists() ){
                        if( action.equals("wizard") ){  
                            node.createDefaultSettings( settingsName );
                        }else{
                            node.initializeSettings();
                        }
                    }
                    byte[] buffer = new byte[1024];
                    int read;
                    FileInputStream fis = new FileInputStream(settingsName);
                    OutputStream out = resp.getOutputStream();
                
                    while ( ( read = fis.read( buffer ) ) > 0 ) {
                        out.write( buffer, 0, read);
                    }

                    fis.close();
                    out.flush();
                    out.close();

                } catch (Exception e) {
                    logger.warn("Failed to load IDPS settings",e);
                }
                node.setUpdatedSettingsFlag( false );
            }else if( action.equals("save")) {
                SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
                String tempSettingsName = "/tmp/untangle-node-idps_settings_" + nodeId + ".js";
                try{
                    byte[] buffer = new byte[1024];
                    int read;
                    InputStream in = req.getInputStream();
                    FileOutputStream fos = new FileOutputStream( tempSettingsName );

                    while ( ( read = in.read( buffer ) ) > 0 ) {
                        fos.write( buffer, 0, read);
                    }

                    in.close();
                    fos.flush();
                    fos.close();

                    node.saveSettings( tempSettingsName );

                }catch( IOException e ){
                    logger.warn("Failed to save IDPS settings");
                }

                node.reconfigure();
            }
        }
    }
}
