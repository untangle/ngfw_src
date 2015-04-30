/**
 * $Id: ReportingManagerNewImpl.java,v 1.00 2015/03/04 13:59:12 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.UvmContextFactory;

public class ReportingManagerNewImpl implements ReportingManagerNew
{
    private static final Logger logger = Logger.getLogger(ReportingManagerNewImpl.class);

    private ArrayList<ReportEntry> systemReportEntries;
    private ArrayList<ReportEntry> customReportEntries;

    private ReportingNodeImpl node;

    public ReportingManagerNewImpl( ReportingNodeImpl node )
    {
        this.node = node;

        loadReportEntries();
    }

    public ArrayList<ReportEntry> getReportEntries()
    {
        ArrayList<ReportEntry> allReportEntries = new ArrayList<ReportEntry>( this.systemReportEntries );
        allReportEntries.addAll( this.customReportEntries );
        return allReportEntries;
    }

    public ArrayList<ReportEntry> getReportEntries( String category )
    {
        ArrayList<ReportEntry> allReportEntries = getReportEntries();
        ArrayList<ReportEntry> entries = new ArrayList<ReportEntry>();

        for ( ReportEntry entry: allReportEntries ) {
            if ( category == null || category.equals( entry.getCategory() ) )
                 entries.add( entry );
        }
        return entries;
    }
    
    public void setCustomReportEntries( ArrayList<ReportEntry> newEntries )
    {
        this.customReportEntries = newEntries;

        try {
            String nodeID = node.getNodeSettings().getId().toString();
            String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-reporting/" + "custom_report_entries_" + nodeID + ".js";
            UvmContextFactory.context().settingsManager().save( settingsFileName, this.customReportEntries );
        } catch ( Exception e ) {
            logger.warn( "Failed to save report entries.", e );
        }
    }

    public ArrayList<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, final int limit )
    {
        String sql = entry.toSql( startDate, endDate );

        logger.info("Getting Data for : " + entry.getTitle());
        logger.info("SQL              : " + sql);
        
        return ReportingNodeImpl.eventReader.getEvents( sql, limit );
    }
    
    @SuppressWarnings("unchecked")
    private void loadReportEntries()
    {
        try {
            String nodeID = node.getNodeSettings().getId().toString();
            String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-reporting/" + "custom_report_entries_" + nodeID + ".js";

            logger.info("Loading report entries from file... ");
            this.customReportEntries = UvmContextFactory.context().settingsManager().load( ArrayList.class, settingsFileName );

            if ( this.customReportEntries == null ) {
                this.customReportEntries = new ArrayList<ReportEntry>();
            }

            this.systemReportEntries = loadSystemReportEntries();

        } catch (Exception e) {
            logger.warn( "Failed to load report entries", e );
        }
    }

    private ArrayList<ReportEntry> loadSystemReportEntries()
    {
        ArrayList<ReportEntry> systemReportEntries = new ArrayList<ReportEntry>();
        
        String cmd = "/usr/bin/find " + System.getProperty("uvm.lib.dir") + " -path '*/reports/*.js' -print";
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( cmd );
        if (result.getResult() != 0) {
            logger.warn("Failed to find report entries: \"" + cmd + "\" -> "  + result.getResult());
            return systemReportEntries;
        }
        try {
            boolean added = false;
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("Creating Schema: ");
            for ( String line : lines ) {
                logger.info("Reading file: " + line);
                try {
                    ReportEntry newEntry = UvmContextFactory.context().settingsManager().load( ReportEntry.class, line );
                    systemReportEntries.add( newEntry );

                    //Date oneDayAgo = new Date((new Date()).getTime() - (1000L * 60L * 60L * 24L));
                    //logger.info("XXX DEBUG: " + newEntry.toSql(oneDayAgo, null));
                } catch (Exception e) {
                    logger.warn( "Failed to read report entry from: " + line, e );
                }
            }
        } catch (Exception e) {
            logger.warn( "Failed to check for new entries.", e );
        }

        return systemReportEntries;
    }

    
}
