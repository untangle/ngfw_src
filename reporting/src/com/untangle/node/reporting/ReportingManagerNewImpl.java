/**
 * $Id: ReportingManagerNewImpl.java,v 1.00 2015/03/04 13:59:12 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SqlCondition;

public class ReportingManagerNewImpl implements ReportingManagerNew
{
    private static final Logger logger = Logger.getLogger(ReportingManagerNewImpl.class);

    private LinkedList<ReportEntry> reportEntries;

    private ReportingNodeImpl node;

    private class ReportEntryDisplayOrderComparator implements Comparator<ReportEntry>
    {
        public int compare( ReportEntry entry1, ReportEntry entry2 ) 
        {
            int num = entry1.getDisplayOrder() - entry2.getDisplayOrder();
            if ( num != 0 )
                return num;
            else {
                if (entry1.getTitle() == null || entry2.getTitle() == null )
                    return 0;
                return entry1.getTitle().compareTo( entry2.getTitle() );
            }
        }
    }    

    public ReportingManagerNewImpl( ReportingNodeImpl node )
    {
        this.node = node;

        loadReportEntries();
    }

    public List<ReportEntry> getReportEntries()
    {
        LinkedList<ReportEntry> allReportEntries = new LinkedList<ReportEntry>( this.reportEntries );

        Collections.sort( allReportEntries, new ReportEntryDisplayOrderComparator() );

        return allReportEntries;
    }

    public List<ReportEntry> getReportEntries( String category )
    {
        List<ReportEntry> allReportEntries = getReportEntries();
        LinkedList<ReportEntry> entries = new LinkedList<ReportEntry>();

        for ( ReportEntry entry: allReportEntries ) {
            if ( category == null || category.equals( entry.getCategory() ) )
                 entries.add( entry );
        }
        return entries;
    }
    
    public void setReportEntries( List<ReportEntry> newEntries )
    {
        this.reportEntries = new LinkedList<ReportEntry>(newEntries);
        updateSystemReportEntries( this.reportEntries, false );
        
        try {
            String nodeID = node.getNodeSettings().getId().toString();
            String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-reporting/" + "report_entries_" + nodeID + ".js";
            UvmContextFactory.context().settingsManager().save( settingsFileName, this.reportEntries );
        } catch ( Exception e ) {
            logger.warn( "Failed to save report entries.", e );
        }
    }

    public List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, SqlCondition[] extraConditions, final int limit )
    {
        String sql = entry.toSql( startDate, endDate, extraConditions );

        logger.info("Getting Data for : " + entry.getTitle());
        logger.info("SQL              : " + sql);

        long t0 = System.currentTimeMillis();
        ArrayList<JSONObject> results = ReportingNodeImpl.eventReader.getEvents( sql, limit );
        long t1 = System.currentTimeMillis();

        logger.info("Query Time      : " + String.format("%5d",(t1 - t0)) + " ms");

        return results;
    }
    
    public List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, final int limit )
    {
        return getDataForReportEntry( entry, startDate, endDate, null, limit );
    }
    
    @SuppressWarnings("unchecked")
    private void loadReportEntries()
    {
        try {
            String nodeID = node.getNodeSettings().getId().toString();
            String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-reporting/" + "report_entries_" + nodeID + ".js";

            logger.info("Loading report entries from file... ");
            this.reportEntries = UvmContextFactory.context().settingsManager().load( LinkedList.class, settingsFileName );

            if ( this.reportEntries == null ) {
                this.reportEntries = new LinkedList<ReportEntry>();
            }

            updateSystemReportEntries( reportEntries, true );

        } catch (Exception e) {
            logger.warn( "Failed to load report entries", e );
        }
    }

    private void updateSystemReportEntries( List<ReportEntry> existingEntries, boolean saveIfChanged )
    {
        boolean updates = false;
        
        String cmd = "/usr/bin/find " + System.getProperty("uvm.lib.dir") + " -path '*/reports/*.js' -print";
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( cmd );
        if (result.getResult() != 0) {
            logger.warn("Failed to find report entries: \"" + cmd + "\" -> "  + result.getResult());
            return;
        }
        try {
            boolean added = false;
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("Creating Schema: ");
            for ( String line : lines ) {
                logger.info("Reading file: " + line);
                try {
                    ReportEntry newEntry = UvmContextFactory.context().settingsManager().load( ReportEntry.class, line );

                    ReportEntry oldEntry = findReportEntry( existingEntries, newEntry.getUniqueId() );
                    if ( oldEntry == null ) {
                        logger.info( "Report Entries Update: Adding  \"" + newEntry.getTitle() + "\"");
                        existingEntries.add( newEntry );
                        updates = true;
                    } else {
                        boolean changed = updateReportEntry( existingEntries, newEntry, oldEntry );
                        if ( changed ) {
                            updates = true;
                            logger.info( "Report Entries Update: Updated \"" + newEntry.getTitle() + "\"");
                        }
                    }
                } catch (Exception e) {
                    logger.warn( "Failed to read report entry from: " + line, e );
                }
            }
        } catch (Exception e) {
            logger.warn( "Failed to check for new entries.", e );
        }

        if ( updates && saveIfChanged ) {
            logger.warn("XXXXXX");
            setReportEntries( existingEntries );
        }
        
        return;
    }

    private ReportEntry findReportEntry( List<ReportEntry> entries, String uniqueId )
    {
        if ( entries == null || uniqueId == null ) {
            logger.warn("Invalid arguments: " + uniqueId, new Exception());
            return null;
        }
        
        for ( ReportEntry entry : entries ) {
            if (uniqueId.equals( entry.getUniqueId() ) )
                return entry;
        }
        return null;
    }

    private boolean updateReportEntry( List<ReportEntry> entries, ReportEntry newEntry, ReportEntry oldEntry )
    {
        String newEntryStr = newEntry.toJSONString();
        String oldEntryStr = oldEntry.toJSONString();

        // no changed are needed if two are identical
        if ( oldEntryStr.equals( newEntryStr ) )
            return false;

        // remove old entry
        if ( ! entries.remove( oldEntry ) ) {
            logger.warn("Failed to update report entry: " + newEntry.getUniqueId());
            return false;
        }

        // copy "changeable" attributes from old settings, replace old entry with new
        newEntry.setEnabled( oldEntry.getEnabled() );
        entries.add( newEntry );

        return true;
    }
    
}
