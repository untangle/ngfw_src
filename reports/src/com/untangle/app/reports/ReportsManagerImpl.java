/**
 * $Id: ReportsManagerImpl.java,v 1.00 2015/03/04 13:59:12 dmorris Exp $
 */
package com.untangle.app.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.AdminUserSettings;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.WebBrowser;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.app.PolicyManager;

/**
 * Reports manager implementation for reports manager API
 */
public class ReportsManagerImpl implements ReportsManager
{
    private static final Logger logger = Logger.getLogger(ReportsManagerImpl.class);

    private static ReportsManagerImpl instance = null;

    private static ReportsApp app = null;

    /**
     * This stores the table column metadata lookup results so we don't have to frequently lookup metadata
     * which is slow
     */
    private static HashMap<String,HashMap<String,String>> cacheColumnsResults = new HashMap<>();

    /**
     * This stores the tables metadata lookup results so we don't have to frequently lookup metadata
     * which is slow
     */
    private static ResultSet cacheTablesResults = null;

    /**
     * This stores all the app properties. It is used to reference information about the different apps/categories
     */
    private List<AppProperties> appPropertiesList = null;

    /** 
     * Initialize reports manager
     */
    protected ReportsManagerImpl()
    {
        this.appPropertiesList = UvmContextFactory.context().appManager().getAllAppProperties();

        // sort by view position
        Collections.sort(this.appPropertiesList, new Comparator<AppProperties>() {
            /**
             * Based on view position and name, determine proper comparision order.
             * 
             * @param np1
             *  Application 1
             * @param np2
             *  Application 2
             * @return
             *  -1 if app 1 comes before app 2, 1 otherwise.
             */
            public int compare(AppProperties np1, AppProperties np2) {
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

    /** 
     * Return the existing reports manager instance
     *
     * @return
     *  ReportManager instance.
     */
    public static ReportsManagerImpl getInstance()
    {
        synchronized ( ReportsManagerImpl.class ) {
            if ( instance == null ) {
                instance = new ReportsManagerImpl();
            }
        }

        return instance;
    }

    /** 
     * Set the reports application
     *
     * @param app
     *   Reports application.
     */
    public void setReportsApp( ReportsApp app )
    {
        ReportsManagerImpl.app = app;
    }

    /** 
     * Check if reports applicaton is enabled
     *
     * @return
     *  true if reports application is enabled, false if not.
     */
    public boolean isReportsEnabled()
    {
        return app != null && AppSettings.AppState.RUNNING.equals(app.getRunState());
    }

    /** 
     * Get all report entries.
     *
     * @return
     *  List of all ReportEntry objects.
     */
    public List<ReportEntry> getReportEntries()
    {
        if ( app == null ) {
            throw new RuntimeException("Reports app not found");
        }

        LinkedList<ReportEntry> allReportEntries = new LinkedList<>( app.getSettings().getReportEntries() );

        Collections.sort( allReportEntries, new ReportEntryDisplayOrderComparator() );

        return allReportEntries;
    }

    /** 
     * Get all report entries for a specified category.
     *
     * @param category
     *  Category to search for
     * @return
     *  List of all ReportEntry objects containing the category.
     */
    public List<ReportEntry> getReportEntries( String category )
    {
        List<ReportEntry> allReportEntries = getReportEntries();
        LinkedList<ReportEntry> entries = new LinkedList<>();

        /**
         * If fetching the reports for an app, check that it is installed and has a valid license
         * If the license is not valid, return an empty list
         * If the category isnt an app name, just continue.
         */
        AppProperties appProperties = findAppProperties( category );
        if ( appProperties != null ) {
            App app = UvmContextFactory.context().appManager().app( appProperties.getName() );
            if ( app == null ) {
                logger.warn("Not showing report entries for \"" + category + "\" because it isnt installed.");
                return entries;
            }
            if ( !app.isLicenseValid() ) {
                logger.warn("Not showing report entries for \"" + category + "\" because of invalid license.");
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

    /** 
     * Get all installed and license-active NGFW applications
     *
     * @return
     *  List of JSON object for each application containing fields for application displayName, name, viewPosition.
     */
    public List<JSONObject> getCurrentApplications()
    {
        ArrayList<JSONObject> currentApplications = new ArrayList<>();

        for ( AppProperties appProperties : this.appPropertiesList ) {
            if ( appProperties.getInvisible()) {
                continue;
            }
            App app = UvmContextFactory.context().appManager().app( appProperties.getName() );
            if ( app == null ) {
                continue;
            }
            if ( ! app.isLicenseValid() ) {
                continue;
            }
            org.json.JSONObject json = new org.json.JSONObject();

            try {
                json.put("displayName", appProperties.getDisplayName());
                json.put("name", appProperties.getName());
                json.put("viewPosition", appProperties.getViewPosition());
            } catch (Exception e) {
                logger.error( "Error generating Current Applications list", e );
            }
            currentApplications.add(json);
        }

        return currentApplications;
    }

    /** 
     * Get all installed but not license-active NGFW applications
     *
     * @return
     *  Map of unavailable applications using the application fields displayName and name.
     */
    public Map<String, String> getUnavailableApplicationsMap()
    {
        Map<String, String> unavailableApplicationsMap = new HashMap<>();

        for ( AppProperties appProperties : this.appPropertiesList ) {
            if("shield".equals(appProperties.getName())){
                continue;
            }
            if ( appProperties.getInvisible() ||
                    UvmContextFactory.context().appManager().app( appProperties.getName() ) == null ||
                    !UvmContextFactory.context().licenseManager().isLicenseValid( appProperties.getName() ) ) {
                unavailableApplicationsMap.put(appProperties.getDisplayName(), appProperties.getName());
            }
        }

        return unavailableApplicationsMap;
    }

    /**
     * Write new set of report entries, overwriting existing.
     *
     * @param newEntries
     *  List of ReportEntry objectss.
     */
    public void setReportEntries( List<ReportEntry> newEntries )
    {
        if ( app == null ) {
            throw new RuntimeException("Reports app not found");
        }

        LinkedList<ReportEntry> newReportEntries = new LinkedList<>(newEntries);
        updateSystemReportEntries( newReportEntries, false );

        ReportsSettings settings = app.getSettings();
        settings.setReportEntries( newReportEntries );
        app.setSettings( settings, true );
    }

    /**
     * Get report entry by category and title.
     *
     * @param category
     *  String of category to match.
     * @param title
     *  Strng of title to match.
     * @return
     *  Matching ReportEntry object.
     */
    public ReportEntry getReportEntry( String category, String title )
    {
        LinkedList<ReportEntry> entries = app.getSettings().getReportEntries();

        if ( category == null || title == null )
            return null;

        for ( ReportEntry entry : entries ) {
            if ( category.equals(entry.getCategory()) && title.equals(entry.getTitle()) )
                return entry;
        }

        return null;
    }

    /**
     * Get report entry by its unique id.
     *
     * @param uniqueId
     *  String of uniqueId
     * @return
     *  Matching ReportEntry object.
     */
    public ReportEntry getReportEntry( String uniqueId )
    {
        LinkedList<ReportEntry> entries = app.getSettings().getReportEntries();

        if ( uniqueId == null)
            return null;

        for ( ReportEntry entry : entries ) {
            if ( uniqueId.equals(entry.getUniqueId()) )
                return entry;
        }

        return null;
    }

    /**
     * Write a single report entry to to entries set.  
     * An existing entry will be replaced if the uniqueId fields match.
     * Otherwise, the entry will be consdered new and added.
     *
     * @param entry
     *  ReportEntry to write.
     */
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

    /** 
     * Remove the specific report entry from the entries set.
     * 
     * @param entry
     *  Entry to remove.
     */
    public void removeReportEntry( ReportEntry entry )
    {
        String uniqueId = entry.getUniqueId();
        List<ReportEntry> reportEntries = getReportEntries();
        boolean found = false;
        int i = 0;

        if ( uniqueId == null ) {
            throw new RuntimeException("Invalid Entry unique ID: " + uniqueId );
        }

        if ( entry.getReadOnly() ) {
            throw new RuntimeException("Readonly entries cannot be removed!");
        }

        for ( ReportEntry e : reportEntries ) {
            if ( uniqueId.equals( e.getUniqueId() ) ) {
                found = true;
                reportEntries.set( i, entry );
                break;
            }
            i++;
        }

        if ( !found ) {
            throw new RuntimeException("Report entry: " + uniqueId + " not found!");
        }

        // remove entry
        if ( !reportEntries.remove( entry ) ) {
            throw new RuntimeException("Failed to remove report entry: " + uniqueId);
        }

        setReportEntries( reportEntries );
        return;
    }

    /**
     * Query report data.
     *
     * @param entry
     *  ReportEntry to use.
     * @param startDate
     *  Beginning date.
     * @param endDate
     *  Ending date.
     * @param extraSelects
     *  Extra selects
     * @param extraConditions
     *  Additional SQL query options.
     * @param fromType
     *  If null, use the table, otherwise construct using information in object.
     * @param limit
     *  Restrict number of results to this number.
     * @return
     *  Results of query as a List of JSONObjects.
     */
    public List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, String[] extraSelects, SqlCondition[] extraConditions, SqlFrom fromType, final int limit )
    {
        Connection conn = app.getDbConnection();
        PreparedStatement statement = entry.toSql( conn, startDate, endDate, extraSelects, extraConditions, fromType);

        if ( app != null ) {
            // only flush if there are less than 10k events pending
            // if there are more than 10k then events are currently being flushed and the queue can be quite long
            // as such, just return the results for the events already in the DB instead of waiting up to several minutes
            if (app.getEventsPendingCount() < 10000)
                app.flushEvents();
        }

        logger.info("Getting Data for : (" + entry.getCategory() + ") " + entry.getTitle());
        logger.info("Statement        : " + statement);

        long t0 = System.currentTimeMillis();
        ArrayList<JSONObject> results = ReportsApp.eventReader.getEvents( conn, statement, entry.getTable(), limit );
        long t1 = System.currentTimeMillis();

        logger.info("Query Time       : " + String.format("%5d",(t1 - t0)) + " ms");

        return results;
    }

    /**
     * Query report data.
     *
     * @param entry
     *  ReportEntry to use.
     * @param startDate
     *  Beginning date.
     * @param endDate
     *  Ending date.
     * @param limit
     *  Restrict number of results to this number.
     * @return
     *  Results of query as a List of JSONObjects.
     */
    public List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, final int limit )
    {
        return getDataForReportEntry( entry, startDate, endDate, null, null, null, limit );
    }

    /**
     * Query report data.
     *
     * @param entry
     *  ReportEntry to use.
     * @param timeframeSec
     *  Last seconds to query (e.g.,300 to pull last 5 minutes)
     * @param limit
     *  Restrict number of results to this number.
     * @return
     *  Results of query as a List of JSONObjects.
     */
    public List<JSONObject> getDataForReportEntry( ReportEntry entry, final int timeframeSec, final int limit )
    {
        Date startDate = null;
        if(timeframeSec > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, -timeframeSec);
            startDate = cal.getTime();
        }
        return getDataForReportEntry( entry, startDate, null, null, null, null, limit );
    }

    /**
     * Get column table names for the specified table.
     *
     * @param tableName
     *  Name of table.
     * @return
     *  Array of table's column names.
     */
    public String[] getColumnsForTable( String tableName )
    {
        String[] array = null;
        ArrayList<String> columnNames = new ArrayList<>();
        HashMap<String,String> metadata = getColumnMetaData( tableName );

        if(metadata == null){
            return array;
        }
        
        Set<String> keys = metadata.keySet();
        array = new String[keys.size()];
        array = keys.toArray(array);
        return array;
    }

    /**
     * Get column type.
     *
     * @param tableName
     *  Name of table.
     * @param columnName
     *  Name of column.
     * @return
     *  String of the column type.
     */
    public String getColumnType( String tableName, String columnName )
    {
        HashMap<String,String> metadata = getColumnMetaData( tableName );

        return (metadata == null ? null : metadata.get( columnName ));
    }

    /**
     * Check to see if table has the named column.
     *
     * @param tableName
     *  Name of table.
     * @param columnName
     *  Name of column.
     * @return
     *  true if table contains column, false otherwise.
     */
    public boolean tableHasColumn( String tableName, String columnName )
    {
        String type = getColumnType( tableName, columnName );
        if ( type == null )
            return false;
        return true;
    }

    /**
     * Get names of all tables.
     *
     * @return
     *  Array of all table names.
     */
    public String[] getTables()
    {
        ArrayList<String> tableNames = new ArrayList<>();
        Connection conn = app.getDbConnection();
        try {
            ResultSet rs = cacheTablesResults;
            if ( rs == null ) {
                if (ReportsApp.dbDriver.equals("sqlite")) {
                    // don't cache sqlite results
                    // the result is FORWARD_ONLY
                    rs = conn.getMetaData().getTables( null, null, null, null );
                } else {
                    cacheTablesResults = conn.getMetaData().getTables( null, "reports", null, null );
                    rs = cacheTablesResults;
                }
            } else {
                rs.beforeFirst();
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
        } finally {
            try { conn.close(); } catch (Exception e) {
                logger.warn("Close Exception",e);
            }
        }

        String[] array = new String[tableNames.size()];
        array = tableNames.toArray(array);
        return array;
    }

    /**
     * Return events for an EVENT_LIST report.
     *
     * @param entry
     *  ReportEntry object.
     * @param extraConditions
     *  Additional SQL conditions.
     * @param limit
     *  Maximum number of records to return.
     * @return
     *  ArrayList containing JSONObject of event result.
     */
    public ArrayList<org.json.JSONObject> getEvents(final ReportEntry entry, final SqlCondition[] extraConditions, final int limit)
    {
        ArrayList<org.json.JSONObject> results = null;
        if (entry == null) {
            logger.warn("Invalid arguments");
            return results;
        }
        if ( entry.getType() != ReportEntry.ReportEntryType.EVENT_LIST )
            throw new RuntimeException("Can only retrieve events for an EVENT_LIST type report entry");
        if ( app == null ) {
            return results;
        }
        // only flush if there are less than 10k events pending
        // if there are more than 10k then events are currently being flushed and the queue can be quite long
        // as such, just return the results for the events already in the DB instead of waiting up to several minutes
        if (app.getEventsPendingCount() < 10000) {
                app.flushEvents();
        }

        Connection conn = app.getDbConnection();
        PreparedStatement statement = entry.toSql( conn, null, null, null, extraConditions, null, limit );

        logger.info("Getting Events for : (" + entry.getCategory() + ") " + entry.getTitle());
        logger.info("Statement          : " + statement);

        long t0 = System.currentTimeMillis();
        results =  ReportsApp.eventReader.getEvents( conn, statement, entry.getTable(), limit );
        long t1 = System.currentTimeMillis();

        logger.info("Query Time         : " + String.format("%5d",(t1 - t0)) + " ms");

        return results;
    }

    /**
     * Return result set reader for an EVENT_LIST report.
     *
     * @param entry
     *  ReportEntry object.
     * @param extraConditions
     *  Additional SQL conditions.
     * @param limit
     *  Maximum number of records to return.
     * @return
     *  ResultSetReader for the query.
     */
    public ResultSetReader getEventsResultSet(final ReportEntry entry, final SqlCondition[] extraConditions, final int limit)
    {
        if (entry == null) {
            logger.warn("Invalid arguments");
            return null;
        }
        return getEventsForDateRangeResultSet( entry, extraConditions, limit, null, null );
    }

    /**
     * Return result set reader for report based on most recent seconds from current time.
     *
     * @param entry
     *  ReportEntry object.
     * @param extraConditions
     *  Additional SQL conditions.
     * @param timeframeSec
     *  Number of seconds "revent" to query.
     * @param limit
     *  Maximum number of records to return.
     * @return
     *  ResultSetReader for the query.
     */
    public ResultSetReader getEventsForTimeframeResultSet(final ReportEntry entry, final SqlCondition[] extraConditions, final int timeframeSec, final int limit)
    {
        if (entry == null) {
            logger.warn("Invalid arguments");
            return null;
        }
        Date startDate = null;
        if(timeframeSec > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, -timeframeSec);
            startDate = cal.getTime();
        }
        return getEventsForDateRangeResultSet( entry, extraConditions, limit, startDate, null );
    }

    /**
     * Return result set reader for report based on date range.
     *
     * @param entry
     *  ReportEntry object.
     * @param extraConditions
     *  Additional SQL conditions.
     * @param limit
     *  Maximum number of records to return.
     * @param start
     *  Beginning date.
     * @param endDate
     *  Beginning endDate.
     * @return
     *  ResultSetReader for the query.
     */
    public ResultSetReader getEventsForDateRangeResultSet(final ReportEntry entry, final SqlCondition[] extraConditions, final int limit, final Date start, final Date endDate)
    {
        ResultSetReader result = null;
        if (entry == null) {
            logger.warn("Invalid arguments");
            return result;
        }
        if ( entry.getType() != ReportEntry.ReportEntryType.EVENT_LIST )
            throw new RuntimeException("Can only retrieve events for an EVENT_LIST type report entry");
        if ( app == null ) {
            return result;
        }
        // only flush if there are less than 10k events pending
        // if there are more than 10k then events are currently being flushed and the queue can be quite long
        // as such, just return the results for the events already in the DB instead of waiting up to several minutes
        if (app.getEventsPendingCount() < 10000) {
            app.flushEvents();
        }

        Date startDate = start;
        if ( startDate == null ) {
            startDate = new Date((new Date()).getTime() - (1000 * 60 * 60 * 24));
            logger.info("startDate not specified, using 1 day ago: " + startDate);
        }

        Connection conn = app.getDbConnection();
        PreparedStatement statement = entry.toSql( conn, startDate, endDate, null, extraConditions, null, limit );

        logger.info("Getting Events for : (" + entry.getCategory() + ") " + entry.getTitle());
        logger.info("Statement          : " + statement);

        long t0 = System.currentTimeMillis();
        result = ReportsApp.eventReader.getEventsResultSet( conn, statement, limit);
        long t1 = System.currentTimeMillis();

        logger.info("Query Time         : " + String.format("%5d",(t1 - t0)) + " ms");

        return result;
    }

    /**
     * Return system timezone offset.
     *
     * @return
     *  Timezone offset in seconds.
     */
    public Integer getTimeZoneOffset()
    {
        return UvmContextFactory.context().systemManager().getTimeZoneOffset();
    }

    /**
     * Return all interface information.
     *
     * @return
     *  List of JSONObjects containing interfaceId and name fields.
     */
    public List<JSONObject> getInterfacesInfo()
    {
        ArrayList<JSONObject> interfacesInfo = new ArrayList<>();
        for( InterfaceSettings interfaceSettings : UvmContextFactory.context().networkManager().getNetworkSettings().getInterfaces() ){
            try {
                JSONObject json = new org.json.JSONObject();
                json.put("interfaceId", interfaceSettings.getInterfaceId());
                json.put("name", interfaceSettings.getName() );
                interfacesInfo.add(json);
            } catch (Exception e) {
                logger.warn("Error generating interfaces list",e);
            }
        }
        for( InterfaceSettings interfaceSettings : UvmContextFactory.context().networkManager().getNetworkSettings().getVirtualInterfaces() ){
            try {
                JSONObject json = new org.json.JSONObject();
                json.put("interfaceId", interfaceSettings.getInterfaceId());
                json.put("name", interfaceSettings.getName() );
                interfacesInfo.add(json);
            } catch (Exception e) {
                logger.warn("Error generating interfaces list",e);
            }
        }
        return interfacesInfo;
    }

    /** 
     * Check if system allows graphs for fixed reports.
     *
     * @return
     *  true if system can generate reports via browser, false otherwise.
     */
    public Boolean fixedReportsAllowGraphs()
    {
        return WebBrowser.exists();
    }

    /**
     * Return administrator email addresses.
     *
     * @return
     *  List of administrator email addresses.
     */
    public List<String> getAdminEmailAddresses()
    {
        LinkedList<String> adminEmailAddresses = new LinkedList<>();

        LinkedList<ReportsUser> reportsUsers = app.getSettings().getReportsUsers();
        Boolean reportsUserFound;
        LinkedList<AdminUserSettings> adminUsers = UvmContextFactory.context().adminManager().getSettings().getUsers();
        if((reportsUsers != null) && (adminUsers != null)){
            for(AdminUserSettings adminUser : adminUsers ){
                if( (adminUser == null) ||
                    (adminUser.getEmailAddress() == null) ||
                    (adminUser.getEmailAddress().isEmpty()) ){
                    // Ignore if admin email address is empty.
                    continue;
                }
                reportsUserFound = false;
                for(ReportsUser reportUser: reportsUsers){
                    if( (reportUser != null) &&
                        (reportUser.getEmailAddress() != null) &&
                        reportUser.getEmailAddress().equals( adminUser.getEmailAddress() ) ){
                        reportsUserFound = true;
                    }
                }
                if(reportsUserFound == false){
                    adminEmailAddresses.add(adminUser.getEmailAddress());
                }
            }
        }

        return adminEmailAddresses;
    }

    /**
     * Return list of recommended report identifiers.
     *
     * @return
     *  List of report identifiers.
     */
    public List<String> getRecommendedReportIds()
    {
        return FixedReports.ReservedReports;
    }

    /**
     * Return app-specific list of values.
     *
     * @param  appName  Name of app.
     * @param  policyId Policy id.
     * @param  key      Name of list to return.
     * @return          List of JSONObjects
     */
    public List<JSONObject> getReportInfo( String appName, Integer policyId, String key){
        List<App> apps = null;
        if(policyId == -1){
            apps = UvmContextFactory.context().appManager().appInstances(appName);
        }else{
            apps = UvmContextFactory.context().appManager().appInstances(appName, policyId, false);
        }
        if(apps != null && apps.size() > 0){
            return ((AppBase)apps.get(0)).getReportInfo(key);
        }

        return null;
    }


    /**
     * Return list of policies if defined.
     * @return List of polciies
     */
    public ArrayList<JSONObject> getPoliciesInfo(){

        ArrayList<JSONObject> policies = null;
        PolicyManager policyManager = (PolicyManager)UvmContextFactory.context().appManager().app( "policy-manager");
        if (policyManager != null) {
            return policyManager.getPoliciesInfo();
        }
        return policies;
    }

    /** 
     * Synchronize system report enrtries.
     *
     * @param existingEntries
     *  List of entries to update.
     * @param saveIfChanged
     *  If true, save the updated entries.
     */
    protected void updateSystemReportEntries( List<ReportEntry> existingEntries, boolean saveIfChanged )
    {
        boolean updates = false;
        if ( existingEntries == null )
            existingEntries = new LinkedList<>();

        String cmd = "/usr/bin/find " + System.getProperty("uvm.lib.dir") + " -path '*/lib/*/reports/*.json' -print";
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( cmd );
        if (result.getResult() != 0) {
            logger.warn("Failed to find report entries: \"" + cmd + "\" -> "  + result.getResult());
            return;
        }
        try {
            List<String> seenUniqueIds = new LinkedList<>();
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("Creating Schema: ");
            for ( String line : lines ) {
                logger.debug("Reading file: " + line);
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
                        logger.info( "Report Entries Update: Adding  \"" + newEntry.getTitle() + "\" [" + line + "]");
                        existingEntries.add( newEntry );
                        updates = true;
                    } else {
                        boolean changed = updateReportEntry( existingEntries, newEntry, oldEntry );
                        if ( changed ) {
                            updates = true;
                            logger.info( "Report Entries Update: Updated \"" + newEntry.getTitle() + "\" [" + line + "]");
                        }
                    }
                } catch (Exception e) {
                    logger.warn( "Failed to read report entry from: " + line, e );
                }
            }

            /**
             * Remove any obsolete entries
             */
            for (Iterator<ReportEntry> i = existingEntries.iterator(); i.hasNext(); ) {
                ReportEntry entry = i.next();
                if ( entry.getUniqueId().startsWith("reporting-") ) {
                    i.remove();
                    updates = true;
                }
                if ( entry.getUniqueId().startsWith("syslog-") ) {
                    i.remove();
                    updates = true;
                }
                if ( "Web Filter Lite".equals(entry.getCategory()) ) {
                    i.remove();
                    updates = true;
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

    /** 
     * Search a list of reports by uniqueId.
     *
     * @param entries
     *  List of report entries.
     * @param uniqueId
     *  Id o report to find.
     * @return
     *  ReportEntry if found, null otherwise.
     */
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

    /** 
     * Update a report entry from a list.
     *
     * @param entries
     *  List of report entries.
     * @param newEntry
     *  New report entry.
     * @param oldEntry
     *  Entry to replace.
     * @return
     *  true if report entry was changed.
     */
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

    /**
    * Return hash of column metadata.
    *
    * @param tableName
    *   Name of table to retrive columsnfrom.
    * @return
    *   Hash of column name and type.
     */
    private HashMap<String,String> getColumnMetaData( String tableName )
    {
        Connection conn = app.getDbConnection();
        if ( conn == null ) {
            logger.warn("Failed to get DB Connection");
            return null;
        }

        try {
            HashMap<String,String> results = cacheColumnsResults.get( tableName );
            if ( results != null ) {
                return results;
            }
            results = new HashMap<>();

            ResultSet rs;
            if (ReportsApp.dbDriver.equals("sqlite"))
                rs = conn.getMetaData().getColumns( null, null, tableName, null );
            else
                rs = conn.getMetaData().getColumns( null, "reports", tableName, null );

            synchronized( rs ) {
                while(rs.next()) {
                    String columnName = rs.getString(4);
                    String columnType = rs.getString(6).toLowerCase();
                    results.put(columnName, columnType);
                }
            }

            cacheColumnsResults.put( tableName, results );
            return results;
        } catch ( Exception e ) {
            logger.warn("Failed to fetch column meta data", e);
            return null;
        } finally {
            try { conn.close(); } catch (Exception e) {
                logger.warn("Close Exception",e);
            }
        }
    }

    /**
     * Report Entry order display.
     */
    private class ReportEntryDisplayOrderComparator implements Comparator<ReportEntry>
    {
        /**
         * Compare based on report entry display order or title.
         *
         * @param entry1
         *  First report entry to compare.
         * @param entry2
         *  Second report entry to compare.
         * @return
         *  Comparision indicating which should be displayed first.
         */
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

    /**
     * Find an application by its displayName.
     *
     * @param displayName
     *  Application name to find.
     * @return
     *  AppProperties of matching application.
     */
    private AppProperties findAppProperties( String displayName )
    {
        if ( displayName == null )
            return null;

        for ( AppProperties props : this.appPropertiesList ) {
            if ( displayName.equals( props.getDisplayName() ) )
                return props;
        }

        return null;
    }
}
