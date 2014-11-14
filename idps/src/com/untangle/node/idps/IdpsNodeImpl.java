/**
 * $Id: IdpsNodeImpl.java 38584 2014-09-03 23:23:07Z dmorris $
 */
package com.untangle.node.idps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

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
import com.untangle.uvm.util.I18nUtil;
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
    private final PipelineConnector [] connectors;

    private EventLogQuery allEventQuery;
    private EventLogQuery blockedEventQuery;

    public IdpsNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        logger.warn("node name=" + nodeProperties.getName() );

        handler = new EventHandler(this);

        this.addMetric(new NodeMetric(STAT_SCAN, I18nUtil.marktr("Sessions scanned")));
        this.addMetric(new NodeMetric(STAT_DETECT, I18nUtil.marktr("Sessions logged")));
        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
        
        this.connectors = null;

        this.allEventQuery = new EventLogQuery(
            I18nUtil.marktr("All Events"),
            "SELECT e.timestamp as time_stamp," +
                " i.ip_src as username," +
                " i.ip_src as c_client_addr," +
                " i.ip_dst as s_server_addr," +
                " e.blocked as idps_blocked," + 
                " s.sig_sid as idps_ruleid," + 
                " s.sig_name as idps_description" + 
            " FROM snort.event as e" + 
                " inner join snort.iphdr i" +
                    " on e.cid = i.cid and e.sid = i.sid"+
                " inner join snort.signature s" +
                " on e.signature = s.sig_id" +
            " ORDER BY time_stamp DESC"
        );

        this.blockedEventQuery = new EventLogQuery(
            I18nUtil.marktr("Blocked Events"),
            "SELECT e.timestamp as time_stamp, " +
                " i.ip_src as username, " +
                " i.ip_src as c_client_addr, " +
                " i.ip_dst as s_server_addr, " +
                " e.blocked as idps_blocked, " + 
                " s.sig_sid as idps_ruleid, " + 
                " s.sig_name as idps_description" + 
            " FROM snort.event as e " + 
                " inner join snort.iphdr i" +
                    " on e.cid = i.cid and e.sid = i.sid"+
                " inner join snort.signature s" +
                " on e.signature = s.sig_id" +
            " WHERE" +
                "e.blocked = 1" +
            " ORDER BY time_stamp DESC"
        );

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

    protected void postStop()
    {
    }

    protected void preStart()
    {
        logger.info("Pre Start");
    }

    protected void postInit()
    {
        logger.info("Post init");

        readNodeSettings();
        reconfigure();
    }

    // private methods ---------------------------------------------------------

    private void readNodeSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-idps/settings_" + nodeID + ".js";

        logger.info("Loading settings from " + settingsFile);

    }

    private void reconfigure()
    {
    }

    public void incrementScanCount()
    {
        this.incrementMetric(STAT_SCAN);
    }

    public void incrementDetectCount()
    {
        this.incrementMetric(STAT_DETECT);
    }

    public void incrementBlockCount()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    public String getSettingsFileName()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeId = this.getNodeSettings().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-node-idps/settings_" + nodeId + ".js";
        return settingsName;
    }

    public void initializeSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeId = this.getNodeSettings().getId().toString();
        String tempFileName = "/tmp/settings_" + getNodeSettings().getNodeName() + "_" + nodeId + ".js";

        String configCmd = new String(System.getProperty("uvm.bin.dir") + 
            "/idps-sync-settings" + 
            " --node " + nodeId +
            " --rules /usr/local/etc/snort/snort.rules" + 
            " --conf=/usr/local/etc/snort/snort.conf" +
            " --settings=" + tempFileName
        );
        String result = UvmContextFactory.context().execManager().execOutput(configCmd );
        try{
            String lines[] = result.split("\\r?\\n");
            logger.warn("idps config: ");
            for ( String line : lines ){
                logger.warn("idps config: " + line);
            }
        }catch( Exception e ){

        }

        try {
            settingsManager.save( getSettingsFileName(), tempFileName, true );
        } catch (Exception exn) {
            logger.error("Could not save node settings", exn);
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

        // synchronized(this) {
        //     String configCmd = new String(System.getProperty("uvm.bin.dir") + "/idps-create-config" + " --node " + node);
        //     String result = UvmContextFactory.context().execManager().execOutput(configCmd );
        //     try{
        //         String lines[] = result.split("\\r?\\n");
        //         logger.warn("idps config: ");
        //         for ( String line : lines ){
        //             logger.warn("idps config: " + line);
        //         }
        //     }catch( Exception e ){}
                        
        //     try{
        //         // Must fork for...reasons?
        //         ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil("/etc/init.d/snort restart");
        //     }catch( Exception ex ){
        //         logger.error("Error restarting snort", ex);
        //     }
        // }
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

            if( action.equals("load") ){
                String settingsName = node.getSettingsFileName();
                try{
                    resp.setCharacterEncoding(CHARACTER_ENCODING);
                    resp.setHeader("Content-Type","application/json");

                    File f = new File( settingsName );
                    if( !f.exists() ){
                        node.initializeSettings();
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
            }else if( action.equals("save")) {
                SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
                String tempSettingsName = "/tmp/untangle-node-idps_settings_" + node + ".js";
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
            }
        }
    }
}
