/**
 * $Id: ReportsManager.java,v 1.00 2015/03/04 13:45:51 dmorris Exp $
 */
package com.untangle.app.reports;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The API for interacting/viewing/editing reports
 */
public interface ReportsManager
{
    /**
     * Get all report entries
     *
     * @return
     *  List of all report entries.
     */
    List<ReportEntry> getReportEntries();

    /**
     * Get all report entries of the specified category
     *
     * @param category
     *  Name of category to match.
     * @return
     *  List of all report entries.
     */
    List<ReportEntry> getReportEntries( String category );
    
    /**
     * Set all report entries
     *
     * @param newEntries
     *  New report entries to set.
     */
    void setReportEntries( List<ReportEntry> newEntries );

    /**
     * Get the report entry in the specified category with the specified title
     * This is used in the ATS tests
     *
     * @param category
     *  Category to match.
     * @param title
     *  Title to match.
     * @return
     *  Matching ReportEntry.
     */
    ReportEntry getReportEntry( String category, String title );
    
    /**
     * Get the report entry via its unique id
     *
     * @param uniqueId
     *  Report id to match.
     * @return
     *  Matching ReportEntry.
     */
    ReportEntry getReportEntry( String uniqueId );

    /**
     * Save an individual report entry
     * If an entry exists with the same uniqueId in the current entries, it will be overwritten.
     * If an entry does not exist with the same uniqueId, it will be appended to the existing entries
     *
     * @param entry
     *  ReportEntry to save.
     */
    void saveReportEntry( ReportEntry entry );

    /**
     * Removes a report entry
     * Only if report entry is custom (not ReadOnly).
     *
     * @param entry
     *  ReportEntry to remove.
     */
    void removeReportEntry( ReportEntry entry );

    /**
     * Get the data for a specific report entry with the specified parameters
     *
     * @param entry
     *  ReportEntry to query.
     * @param startDate
     *  Start date of query.
     * @param endDate
     *  End date of query.
     * @param limit
     *  Maximum number of results to return.
     * @return
     *  List of JSONObject of results.
     */
    List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, final int limit );

    /**
     * Get the data for a specific report entry with the specified parameters  in the last timeframeSec seconds
     *
     * @param entry
     *  ReportEntry to query.
     * @param timeframeSec
     *  Seconds from current time to return.
     * @param limit
     *  Maximum number of results to return.
     * @return
     *  List of JSONObject of results.
     */
    List<JSONObject> getDataForReportEntry( ReportEntry entry, final int timeframeSec, final int limit );

    /**
     * Get the data for a specific report entry with the specified parameters
     *
     * @param entry
     *  ReportEntry to query.
     * @param startDate
     *  Start date of query.
     * @param endDate
     *  End date of query.
     * @param extraSelects
     *  Extra selects
     * @param extraConditions
     *  Additional SQL conditions to use in query. 
     * @param fromType
     *  If null, use the table, otherwise construct using information in object.
     * @param limit
     *  Maximum number of results to return.
     * @return
     *  List of JSONObject of results.
     */
    List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, String[] extraSelects, SqlCondition[] extraConditions, SqlFrom fromType, final int limit );

    /**
     * Query events in the reports database
     *
     * @param entry
     *  ReportEntry to query.
     * @param extraConditions
     *  Additional SQL conditions to use in query. 
     * @param limit
     *  Maximum number of results to return.
     * @return
     *  List of JSONObject of results.
     */
    ArrayList<org.json.JSONObject> getEvents( final ReportEntry entry, final SqlCondition[] extraConditions, final int limit );
    
    /**
     * Query events in the reports database
     *
     * @param entry
     *  ReportEntry to query.
     * @param extraConditions
     *  Additional SQL conditions to use in query. 
     * @param limit
     *  Maximum number of results to return.
     * @return
     *  ResultSet reader for the query.
     */
    ResultSetReader getEventsResultSet( final ReportEntry entry, final SqlCondition[] extraConditions, final int limit );
    
    /**
     * Query events in the reports database in the last timeframeSec seconds
     *
     * @param entry
     *  ReportEntry to query.
     * @param extraConditions
     *  Additional SQL conditions to use in query. 
     * @param timeframeSec
     *  Seconds from current time to return.
     * @param limit
     *  Maximum number of results to return.
     * @return
     *  ResultSet reader for the query.
     */
    ResultSetReader getEventsForTimeframeResultSet( final ReportEntry entry, final SqlCondition[] extraConditions, final int timeframeSec, final int limit );
    
    /**
     * Query events in the reports database, within a given date range
     *
     * @param entry
     *  ReportEntry to query.
     * @param extraConditions
     *  Additional SQL conditions to use in query. 
     * @param limit
     *  Maximum number of results to return.
     * @param startDate
     *  Start of query date.
     * @param endDate
     *  End of query date.
     * @return
     *  ResultSet reader for the query.
     */
    ResultSetReader getEventsForDateRangeResultSet( final ReportEntry entry, final SqlCondition[] extraConditions, final int limit, final Date startDate, final Date endDate );

    /**
     * Get a list of all tables in the database
     *
     * @return
     *  Array of table names.
     */
    String[] getTables();

    /**
     * Get the type of a certain column in a certain table
     *
     * @param tableName
     *  Table to search.
     * @param columnName
     *  Column to query.
     * @return
     *  Type of column.
     */
    String getColumnType( String tableName, String columnName );

    /**
     * Get a list of all columns for a certain table
     *
     * @param tableName
     *  Name of table to query.
     * @return
     *  Array of column names.
     */
    String[] getColumnsForTable( String tableName );

    /**
     * Get current application categories that should be displayed
     *
     * @return
     *  List of JSONObject containing application information.
     */
    List<JSONObject> getCurrentApplications();
    
    /**
     * Get the map of unavailable Applications
     *
     * @return
     *  Map of all unavailable application names.
     */
    Map<String, String> getUnavailableApplicationsMap();
    
    /**
     * Tests if reports is enabled.
     * Currently this is the same thing as "is the
     * reports app installed and turned on."
     *
     * @return true if reports is enabled, false otherwise.
     */
    boolean isReportsEnabled();
    
    /**
     * Get the timezone offset in seconds.
     *
     * @return
     *  Timezone offset in seconds.
     */
    Integer getTimeZoneOffset();
    
    /**
     * Get the list of interfaces ids and names
     *
     * @return
     *  List of JSONObjects containing interfaceId and name fields.
     */
    List<JSONObject> getInterfacesInfo();

    /**
     * Check if system allows graphs for fixed reports.
     *
     * @return
     *  true if system can generate reports via browser, false otherwise.
     */
    Boolean fixedReportsAllowGraphs();

    /**
     * Return administrator email addresses.
     *
     * @return
     *  List of administrator email addresses.
     */
    List<String> getAdminEmailAddresses();

    /**
     * Return list of recommended report identifiers.
     *
     * @return
     *  List of report identifiers.
     */
    List<String> getRecommendedReportIds();

    /**
     * Return application specific list of values throught
     * the app's getReportInfo method, ensuring that the app explictly
     * is allowing this informaiton.
     *
     * @param  appName  Name of app.
     * @param  policyId Policy id.
     * @param  key Key of information to retrieve.
     * @param arguments Array of String arguments to pass.
     * @return          JSONArray of result.
     */
    public JSONArray getReportInfo( String appName, Integer policyId, String key, String...arguments);

}
