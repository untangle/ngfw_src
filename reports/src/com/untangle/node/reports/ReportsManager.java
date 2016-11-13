/**
 * $Id: ReportsManager.java,v 1.00 2015/03/04 13:45:51 dmorris Exp $
 */
package com.untangle.node.reports;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * The API for interacting/viewing/editing reports
 */
public interface ReportsManager
{
    /**
     * Get all report entries
     */
    List<ReportEntry> getReportEntries();

    /**
     * Get all report entries of the specified category
     */
    List<ReportEntry> getReportEntries( String category );
    
    /**
     * Set all report entries
     */
    void setReportEntries( List<ReportEntry> newEntries );

    /**
     * Get the report entry in the specified category with the specified title
     * This is used in the ATS tests
     */
    ReportEntry getReportEntry( String category, String title );
    
    /**
     * Get the report entry via its unique id
     */
    ReportEntry getReportEntry( String uniqueId );

    /**
     * Save an individual report entry
     * If an entry exists with the same uniqueId in the current entries, it will be overwritten.
     * If an entry does not exist with the same uniqueId, it will be appended to the existing entries
     */
    void saveReportEntry( ReportEntry entry );

    /**
     * Removes a report entry
     * Only if report entry is custom (not ReadOnly).
     */
    void removeReportEntry( ReportEntry entry );

    /**
     * Get the data for a specific report entry with the specified parameters
     */
    List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, final int limit );

    /**
     * Get the data for a specific report entry with the specified parameters  in the last timeframeSec seconds
     */
    List<JSONObject> getDataForReportEntry( ReportEntry entry, final int timeframeSec, final int limit );

    /**
     * Get the data for a specific report entry with the specified parameters
     */
    List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, SqlCondition[] extraConditions, final int limit );

    /**
     * Query events in the reports database
     */
    ArrayList<org.json.JSONObject> getEvents( final ReportEntry entry, final SqlCondition[] extraConditions, final int limit );
    
    /**
     * Query events in the reports database
     */
    ResultSetReader getEventsResultSet( final ReportEntry entry, final SqlCondition[] extraConditions, final int limit );
    
    /**
     * Query events in the reports database in the last timeframeSec seconds
     */
    ResultSetReader getEventsForTimeframeResultSet( final ReportEntry entry, final SqlCondition[] extraConditions, final int timeframeSec, final int limit );
    
    /**
     * Query events in the reports database, within a given date range
     */
    ResultSetReader getEventsForDateRangeResultSet( final ReportEntry entry, final SqlCondition[] extraConditions, final int limit, final Date startDate, final Date endDate );

    /**
     * Get a list of all tables in the database
     */
    String[] getTables();

    /**
     * Get the type of a certain column in a certain table
     */
    String getColumnType( String tableName, String columnName );

    /**
     * Get a list of all columns for a certain table
     */
    String[] getColumnsForTable( String tableName );

    /**
     * Get current application categories that should be displayed
     */
    List<JSONObject> getCurrentApplications();
    
    /**
     * Get the map of unavailable Applications
     */
    Map<String, String> getUnavailableApplicationsMap();
    
    /**
     * Get the metadata hints for the condition quick add function
     */
    JSONObject getConditionQuickAddHints();
    
    /**
     * Tests if reports is enabled.
     * Currently this is the same thing as "is the
     * reports node installed and turned on."
     *
     * @return true if reports is enabled, false otherwise.
     */
    boolean isReportsEnabled();
    
    Integer getTimeZoneOffset();
    
    /**
     * Get current application categories that should be displayed
     */
    List<JSONObject> getPoliciesInfo();
    
    /**
     * Get the list of interfaces ids and names
     */
    List<JSONObject> getInterfacesInfo();
}
