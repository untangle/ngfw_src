/**
 * $Id: ReportingManagerNewImpl.java,v 1.00 2015/03/04 13:59:12 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.PreparedStatement;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.SqlCondition;

public class ReportingManagerNewImpl implements ReportingManagerNew
{
    private static final Logger logger = Logger.getLogger(ReportingManagerNewImpl.class);

    private static ReportingManagerNewImpl instance = null;
    
    /**
     * This stores the table column metadata lookup results so we don't have to frequently lookup metadata
     * which is slow
     */
    private static HashMap<String,ResultSet> cacheColumnsResults = new HashMap<String,ResultSet>();

    /**
     * This stores the tables metadata lookup results so we don't have to frequently lookup metadata
     * which is slow
     */
    private static ResultSet cacheTablesResults = null;
    
    private ReportingManagerNewImpl() {}

    public static ReportingManagerNewImpl getInstance()
    {
        synchronized ( ReportingManagerNewImpl.class ) {
            if ( instance == null ) {
                instance = new ReportingManagerNewImpl();
            }
        }

        return instance;
    }

    public List<ReportEntry> getReportEntries()
    {
        ReportingNodeImpl node = (ReportingNodeImpl) UvmContextFactory.context().nodeManager().node("untangle-node-reporting");
        if ( node == null ) {
            throw new RuntimeException("Reporting node not found");
        }
        
        LinkedList<ReportEntry> allReportEntries = new LinkedList<ReportEntry>( node.getSettings().getReportEntries() );

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
        ReportingNodeImpl node = (ReportingNodeImpl) UvmContextFactory.context().nodeManager().node("untangle-node-reporting");
        if ( node == null ) {
            throw new RuntimeException("Reporting node not found");
        }

        LinkedList<ReportEntry> newReportEntries = new LinkedList<ReportEntry>(newEntries);
        updateSystemReportEntries( newReportEntries, false );

        ReportingSettings settings = node.getSettings();
        settings.setReportEntries( newReportEntries );
        node.setSettings( settings );
    }

    public List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, SqlCondition[] extraConditions, final int limit )
    {
        PreparedStatement sql = entry.toSql( getDbConnection(), startDate, endDate, extraConditions );

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
    
    public String[] getColumnsForTable( String tableName )
    {
        ArrayList<String> columnNames = new ArrayList<String>();        
        try {
            ResultSet rs = getColumnMetaData( tableName );

            while(rs.next()){
                String columnName = rs.getString(4);
                //String columnType = rs.getString(6);
                columnNames.add( columnName );
            }
        } catch ( Exception e ) {
            logger.warn("Failed to retrieve column names", e);
            return null;
        }

        String[] array = new String[columnNames.size()];
        array = columnNames.toArray(array);
        return array;
    }

    public String getColumnType( String tableName, String columnName )
    {
        try {

            ResultSet rs = getColumnMetaData( tableName );
            while(rs.next()){
                String name = rs.getString(4);
                if ( columnName.equals( name ) ) {
                    return rs.getString(6);
                }
            }
        } catch ( Exception e ) {
            logger.warn("Failed to retrieve column type", e);
            return null;
        }

        logger.warn("Failed to find column \"" + columnName + "\" in \"" + tableName + "\"");
        return null;
    }
    
    public String[] getTables()
    {
        ArrayList<String> tableNames = new ArrayList<String>();        
        try {
            ResultSet rs = cacheTablesResults;
            if ( rs == null ) {
                cacheTablesResults = getDbConnection().getMetaData().getTables( null, "reports", null, null );
                rs = cacheTablesResults;
            } else {
                rs.first();
            }

            while(rs.next()){
                try {
                    String tableName = rs.getString(3);
                    String type = rs.getString(4);

                    // only include tables without a "0" in them
                    // the 0 excludes all partitions because they have the date in them
                    if ("TABLE".equals(type) && !tableName.contains("0")) {
                        tableNames.add( tableName );
                    }
                } catch (Exception e) {
                    logger.warn("Exception fetching table names",e);
                }
            }
        } catch ( Exception e ) {
            logger.warn("Failed to retrieve column names", e);
            return null;
        }

        String[] array = new String[tableNames.size()];
        array = tableNames.toArray(array);
        return array;
    }

    public ArrayList<org.json.JSONObject> getEvents(final String query, final Long policyId, final SqlCondition[] extraConditions, final int limit)
    {
        return ReportingNodeImpl.eventReader.getEvents(query, policyId, extraConditions, limit, null, null);
    }

    public Object getEventsResultSet(final String query, final Long policyId, final SqlCondition[] extraConditions, final int limit)
    {
        return getEventsForDateRangeResultSet(query, policyId, extraConditions, limit, null, null);
    }

    public Object getEventsForDateRangeResultSet(final String query, final Long policyId, final SqlCondition[] extraConditions, final int limit, final Date startDate, final Date endDate)
    {
        return ReportingNodeImpl.eventReader.getEventsResultSet(query, policyId, extraConditions, limit, startDate, endDate);
    }

    protected void updateSystemReportEntries( List<ReportEntry> existingEntries, boolean saveIfChanged )
    {
        boolean updates = false;
        if ( existingEntries == null )
            existingEntries = new LinkedList<ReportEntry>();
        
        String cmd = "/usr/bin/find " + System.getProperty("uvm.lib.dir") + " -path '*/reports/*.js' -print";
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( cmd );
        if (result.getResult() != 0) {
            logger.warn("Failed to find report entries: \"" + cmd + "\" -> "  + result.getResult());
            return;
        }
        try {
            List<String> seenUniqueIds = new LinkedList<String>();
            boolean added = false;
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("Creating Schema: ");
            for ( String line : lines ) {
                logger.info("Reading file: " + line);
                try {
                    ReportEntry newEntry = UvmContextFactory.context().settingsManager().load( ReportEntry.class, line );

                    /* do some error checking around unique ID */
                    if ( newEntry.getUniqueId() == null ) {
                        logger.error("System Report Entry missing uniqueId: " + line);
                    }
                    if ( seenUniqueIds.contains( newEntry.getUniqueId() ) ) {
                        logger.error("System Report Entry duplicate uniqueId: " + line);
                    } else {
                        seenUniqueIds.add( newEntry.getUniqueId() );
                    }
                    
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

    private ResultSet getColumnMetaData( String tableName )
    {
        try {
            ResultSet rs = cacheColumnsResults.get( tableName );
            if ( rs != null ) {
                rs.first();
                return rs;
            }

            rs = getDbConnection().getMetaData().getColumns( null, "reports", tableName, null );
            cacheColumnsResults.put( tableName, rs );
            return rs;
        } catch ( Exception e ) {
            logger.warn("Failed to fetch column meta data", e);
            return null;
        }
    }

    private Connection getDbConnection()
    {
        ReportingNodeImpl node = (ReportingNodeImpl) UvmContextFactory.context().nodeManager().node("untangle-node-reporting");
        if ( node == null ) {
            throw new RuntimeException("Reporting node not found");
        }
        ReportingSettings settings = node.getSettings();
        if ( settings == null ) {
            throw new RuntimeException("Reporting settings not found");
        }
        
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

    
}
