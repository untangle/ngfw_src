/**
 * $Id: ReportingManagerNewImpl.java,v 1.00 2015/03/04 13:59:12 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager.SettingsException;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.PolicyManager;

public class ReportingManagerNewImpl implements ReportingManagerNew
{
    private static final Logger logger = Logger.getLogger(ReportingManagerNewImpl.class);

    private static ReportingManagerNewImpl instance = null;

    private static ReportingNodeImpl node = null;
    
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
    
    /**
     * This stores all the node properties. It is used to reference information about the different nodes/categories
     */
    private List<NodeProperties> nodePropertiesList = null;

    protected ReportingManagerNewImpl()
    {
        this.nodePropertiesList = UvmContextFactory.context().nodeManager().getAllNodeProperties();

        // sort by view position
        Collections.sort(this.nodePropertiesList, new Comparator<NodeProperties>() {
            public int compare(NodeProperties np1, NodeProperties np2) {
                int vp1 = np1.getViewPosition();
                int vp2 = np2.getViewPosition();
                if (vp1 == vp2) {
                    return np1.getName().compareToIgnoreCase(np2.getName());
                } else if (vp1 < vp2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

    }

    public static ReportingManagerNewImpl getInstance()
    {
        synchronized ( ReportingManagerNewImpl.class ) {
            if ( instance == null ) {
                instance = new ReportingManagerNewImpl();
            }
        }

        return instance;
    }

    public void setReportingNode( ReportingNodeImpl node )
    {
        ReportingManagerNewImpl.node = node;
    }
    
    
    public boolean isReportingEnabled()
    {
        return node != null && NodeSettings.NodeState.RUNNING.equals(node.getRunState());
    }

    public List<ReportEntry> getReportEntries()
    {
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

        /**
         * If fetching the reports for an app, check that it is installed and has a valid license
         * If the license is not valid, return an empty list
         * If the category isnt an app name, just continue.
         */
        NodeProperties nodeProperties = findNodeProperties( category );
        if ( nodeProperties != null ) {
            if ( ! UvmContextFactory.context().licenseManager().isLicenseValid( nodeProperties.getName() ) ) {
                logger.warn("Not showing report entries for \"" + category + "\" because of invalid license."); 
                return entries;
            }
            if ( UvmContextFactory.context().nodeManager().node( nodeProperties.getName() ) == null ) {
                logger.warn("Not showing report entries for \"" + category + "\" because it isnt installed."); 
                return entries;
            }
        }
        
        for ( ReportEntry entry: allReportEntries ) {
            if ( category == null || category.equals( entry.getCategory() ) )
                 entries.add( entry );
            else if ( "Summary".equals( category ) && entry.getType() == ReportEntry.ReportEntryType.TEXT )
                 entries.add( entry );
                
        }
        return entries;
    }

    public List<JSONObject> getCurrentApplications()
    {
    	ArrayList<JSONObject> currentApplications = new ArrayList<JSONObject>();

        for ( NodeProperties nodeProperties : this.nodePropertiesList ) {
            if ( ! UvmContextFactory.context().licenseManager().isLicenseValid( nodeProperties.getName() ) ) {
                continue;
            }
            if ( UvmContextFactory.context().nodeManager().node( nodeProperties.getName() ) == null ) {
                continue;
            }
            if ( nodeProperties.getInvisible() ) {
                continue;
            }
            org.json.JSONObject json = new org.json.JSONObject();

            try {
                json.put("displayName", nodeProperties.getDisplayName());
                json.put("name", nodeProperties.getName());
                json.put("viewPosition", nodeProperties.getViewPosition());
            } catch (Exception e) {
                logger.error( "Error generating Current Applications list", e );
            }
            currentApplications.add(json);
        }

        return currentApplications;
    }
    
    public void setReportEntries( List<ReportEntry> newEntries )
    {
        if ( node == null ) {
            throw new RuntimeException("Reporting node not found");
        }

        LinkedList<ReportEntry> newReportEntries = new LinkedList<ReportEntry>(newEntries);
        updateSystemReportEntries( newReportEntries, false );

        ReportingSettings settings = node.getSettings();
        settings.setReportEntries( newReportEntries );
        node.setSettings( settings );
    }

    public List<EventEntry> getEventEntries( String category )
    {
        List<EventEntry> allEventEntries = node.getSettings().getEventEntries();
        LinkedList<EventEntry> entries = new LinkedList<EventEntry>();

        for ( EventEntry entry: allEventEntries ) {
            if ( category == null || category.equals( entry.getCategory() ) )
                 entries.add( entry );
        }
        return entries;
    }

    public EventEntry getEventEntry( String category, String title )
    {
        LinkedList<EventEntry> entries = node.getSettings().getEventEntries();

        if ( category == null || title == null )
            return null;
        
        for ( EventEntry entry : entries ) {
            if ( category.equals(entry.getCategory()) && title.equals(entry.getTitle()) )
                return entry;
        }

        return null;
    }
    
    public void setEventEntries( List<EventEntry> newEntries )
    {
        if ( node == null ) {
            throw new RuntimeException("Reporting node not found");
        }

        LinkedList<EventEntry> newEventEntries = new LinkedList<EventEntry>(newEntries);
        updateSystemEventEntries( newEventEntries, false );

        ReportingSettings settings = node.getSettings();
        settings.setEventEntries( newEventEntries );
        node.setSettings( settings );
    }
    
    public void saveReportEntry( ReportEntry entry )
    {
        String uniqueId = entry.getUniqueId();
        List<ReportEntry> reportEntries = getReportEntries();
        boolean found = false;
        int i = 0;

        if ( uniqueId == null ) {
            throw new RuntimeException("Invalid Entry unique ID: " + uniqueId );
        }
        
        for ( ReportEntry e : reportEntries ) {
            if ( uniqueId.equals( e.getUniqueId() ) ) {
                found = true;
                reportEntries.set( i, entry );
                break;
            }
            i++;
        }

        if (!found)
            reportEntries.add( entry );

        setReportEntries( reportEntries );
        return;
    }
    
    public List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, SqlCondition[] extraConditions, final int limit )
    {
        PreparedStatement sql = entry.toSql( getDbConnection(), startDate, endDate, extraConditions );

        if ( node != null ) 
            node.flushEvents();

        logger.info("Getting Data for : " + entry.getTitle());
        logger.info("SQL              : " + sql);

        long t0 = System.currentTimeMillis();
        ArrayList<JSONObject> results = ReportingNodeImpl.eventReader.getEvents( sql, entry.getTable(), limit );
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
            synchronized( rs ) {
                rs.first();
                do {
                    String columnName = rs.getString(4);
                    //String columnType = rs.getString(6);
                    columnNames.add( columnName );
                } while(rs.next());
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
            synchronized( rs ) {
                rs.first();
                while(rs.next()){
                    String name = rs.getString(4);
                    if ( columnName.equals( name ) ) {
                        return rs.getString(6);
                    }
                }
            }
        } catch ( Exception e ) {
            logger.warn("Failed to retrieve column type", e);
            return null;
        }

        logger.warn("Failed to find column \"" + columnName + "\" in \"" + tableName + "\"");
        return null;
    }

    public boolean tableHasColumn( String tableName, String columnName )
    {
        String type = getColumnType( tableName, columnName );
        if ( type == null )
            return false;
        return true;
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

    public ArrayList<org.json.JSONObject> getEvents(final EventEntry entry, final SqlCondition[] extraConditions, final int limit)
    {
        if (entry == null) {
            logger.warn("Invalid arguments");
            return null;
        }

        int extraConditionsLen = 0;
        if ( extraConditions != null )
            extraConditionsLen = extraConditions.length;
        int conditionsLen = 0;
        if ( entry.getConditions() != null )
            conditionsLen = entry.getConditions().length;
        
        SqlCondition[] conditions = new SqlCondition[ conditionsLen + extraConditionsLen ];
        if ( entry.getConditions() != null )
            System.arraycopy( entry.getConditions(), 0, conditions, 0, conditionsLen );
        if ( extraConditions != null )
            System.arraycopy( extraConditions, 0, conditions, conditionsLen, extraConditionsLen );

        logger.debug( "getEvents(): " + entry.toSqlQuery( extraConditions ) );
        return ReportingNodeImpl.eventReader.getEvents( entry.toSqlQuery( extraConditions ), entry.getTable(), conditions, limit, null, null );
    }

    public ResultSetReader getEventsResultSet(final EventEntry entry, final SqlCondition[] extraConditions, final int limit)
    {
        if (entry == null) {
            logger.warn("Invalid arguments"); 
            return null;
        }
        return getEventsForDateRangeResultSet( entry, extraConditions, limit, null, null );
    }

    public ResultSetReader getEventsForDateRangeResultSet(final EventEntry entry, final SqlCondition[] extraConditions, final int limit, final Date start, final Date end)
    {
        if (entry == null) {
            logger.warn("Invalid arguments");
            return null;
        }
        if ( node != null ) 
            node.flushEvents();

        logger.debug( "getEvents(): " + entry.toSqlQuery( extraConditions ) );

        Date startDate = start;
        Date endDate = end;
        
        if ( endDate == null )
            endDate = new Date(); // now
        if ( startDate == null ) {
            logger.warn("startDate not specified, using 1 day ago");
            startDate = new Date((new Date()).getTime() - (1000 * 60 * 60 * 24));
        }

        int extraConditionsLen = 0;
        if ( extraConditions != null )
            extraConditionsLen = extraConditions.length;
        int conditionsLen = 0;
        if ( entry.getConditions() != null )
            conditionsLen = entry.getConditions().length;
        
        SqlCondition[] conditions = new SqlCondition[ conditionsLen + extraConditionsLen ];
        if ( entry.getConditions() != null )
            System.arraycopy( entry.getConditions(), 0, conditions, 0, conditionsLen );
        if ( extraConditions != null )
            System.arraycopy( extraConditions, 0, conditions, conditionsLen, extraConditionsLen );

        return ReportingNodeImpl.eventReader.getEventsResultSet( entry.toSqlQuery( extraConditions ), entry.getTable(), conditions, limit, startDate, endDate );
    }

    public org.json.JSONObject getConditionQuickAddHints()
    {
        return UvmContextFactory.context().getConditionQuickAddHints();
    }
    
    public Integer getTimeZoneOffset()
    {
        try {
            String tzoffsetStr = UvmContextFactory.context().execManager().execOutput("date +%:z");
        
            if (tzoffsetStr == null) {
                return 0;
            } else {
                String[] tzParts = tzoffsetStr.replaceAll("(\\r|\\n)", "").split(":");
                if (tzParts.length==2) {
                    Integer hours= Integer.valueOf(tzParts[0]);
                    Integer tzoffset = Math.abs(hours)*3600000+Integer.valueOf(tzParts[1])*60000;
                    return hours >= 0 ? tzoffset : -tzoffset;
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to fetch version",e);
        }

        return 0;
    }
    
    public String getSettingsDiff(String fileName) throws SettingsException
    {
        return UvmContextFactory.context().settingsManager().getDiff(fileName);
    }
    
    public List<JSONObject> getPoliciesInfo() {
        ArrayList<JSONObject> policiesInfo = new ArrayList<JSONObject>();
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().nodeManager().node("untangle-node-policy");
        if ( policyManager != null ) {
            policiesInfo = policyManager.getPoliciesInfo();
        }
        return policiesInfo;
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

    protected void updateSystemEventEntries( List<EventEntry> existingEntries, boolean saveIfChanged )
    {
        boolean updates = false;
        if ( existingEntries == null )
            existingEntries = new LinkedList<EventEntry>();
        
        String cmd = "/usr/bin/find " + System.getProperty("uvm.lib.dir") + " -path '*/events/*.js' -print";
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( cmd );
        if (result.getResult() != 0) {
            logger.warn("Failed to find event entries: \"" + cmd + "\" -> "  + result.getResult());
            return;
        }
        try {
            List<String> seenUniqueIds = new LinkedList<String>();
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("Creating Schema: ");
            for ( String line : lines ) {
                logger.info("Reading file: " + line);
                try {
                    EventEntry newEntry = UvmContextFactory.context().settingsManager().load( EventEntry.class, line );

                    /* do some error checking around unique ID */
                    if ( newEntry.getUniqueId() == null ) {
                        logger.error("System Event Entry missing uniqueId: " + line);
                    }
                    if ( seenUniqueIds.contains( newEntry.getUniqueId() ) ) {
                        logger.error("System Event Entry duplicate uniqueId: " + line);
                    } else {
                        seenUniqueIds.add( newEntry.getUniqueId() );
                    }
                    
                    EventEntry oldEntry = findEventEntry( existingEntries, newEntry.getUniqueId() );
                    if ( oldEntry == null ) {
                        logger.info( "Event Entries Update: Adding  \"" + newEntry.getTitle() + "\"");
                        existingEntries.add( newEntry );
                        updates = true;
                    } else {
                        boolean changed = updateEventEntry( existingEntries, newEntry, oldEntry );
                        if ( changed ) {
                            updates = true;
                            logger.info( "event Entries Update: Updated \"" + newEntry.getTitle() + "\"");
                        }
                    }
                } catch (Exception e) {
                    logger.warn( "Failed to read event entry from: " + line, e );
                }
            }
        } catch (Exception e) {
            logger.warn( "Failed to check for new entries.", e );
        }

        if ( updates && saveIfChanged ) {
            setEventEntries( existingEntries );
        }
        
        return;
    }

    protected Connection getDbConnection()
    {
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

    private EventEntry findEventEntry( List<EventEntry> entries, String uniqueId )
    {
        if ( entries == null || uniqueId == null ) {
            logger.warn("Invalid arguments: " + uniqueId, new Exception());
            return null;
        }
        
        for ( EventEntry entry : entries ) {
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

    private boolean updateEventEntry( List<EventEntry> entries, EventEntry newEntry, EventEntry oldEntry )
    {
        String newEntryStr = newEntry.toJSONString();
        String oldEntryStr = oldEntry.toJSONString();

        // no changed are needed if two are identical
        if ( oldEntryStr.equals( newEntryStr ) )
            return false;

        // remove old entry
        if ( ! entries.remove( oldEntry ) ) {
            logger.warn("Failed to update event entry: " + newEntry.getUniqueId());
            return false;
        }

        // copy "changeable" attributes from old settings, replace old entry with new
        //newEntry.setEnabled( oldEntry.getEnabled() );
        entries.add( newEntry );

        return true;
    }
    
    private ResultSet getColumnMetaData( String tableName )
    {
        try {
            ResultSet rs = cacheColumnsResults.get( tableName );
            if ( rs != null ) {
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

    private NodeProperties findNodeProperties( String displayName )
    {
        if ( displayName == null )
            return null;
        
        for ( NodeProperties props : this.nodePropertiesList ) {
            if ( displayName.equals( props.getDisplayName() ) )
                return props;
        }
        
        return null;
    }
}
