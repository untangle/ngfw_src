/**
 * $Id: ReportingManagerNew.java,v 1.00 2015/03/04 13:45:51 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;

import org.json.JSONObject;

import com.untangle.uvm.SettingsManager.SettingsException;

/**
 * The API for interacting/viewing/editing reports
 */
public interface ReportingManagerNew
{
    List<ReportEntry> getReportEntries();

    List<ReportEntry> getReportEntries( String category );
    
    void setReportEntries( List<ReportEntry> newEntries );

    /**
     * Get the event entries for a category 
     */
    List<EventEntry> getEventEntries( String category );

    /**
     * Get the event entry in the specified category with the specified title
     * This is used in the ATS tests
     */
    EventEntry getEventEntry( String category, String title );
    
    /**
     * Save an individual report entry
     * If an entry exists with the same uniqueId in the current entries, it will be overwritten.
     * If an entry does not exist with the same uniqueId, it will be appended to the existing entries
     */
    void saveReportEntry( ReportEntry entry );

    /**
     * Get the data for a specific report entry with the specified parameters
     */
    List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, final int limit );

    /**
     * Get the data for a specific report entry with the specified parameters
     */
    List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, SqlCondition[] extraConditions, final int limit );

    /**
     * Query events in the reports database
     */
    ArrayList<org.json.JSONObject> getEvents( final EventEntry entry, final SqlCondition[] extraConditions, final int limit );
    
    /**
     * Query events in the reports database
     */
    ResultSetReader getEventsResultSet( final EventEntry entry, final SqlCondition[] extraConditions, final int limit );
    
    /**
     * Query events in the reports database, within a given date range
     */
    ResultSetReader getEventsForDateRangeResultSet( final EventEntry entry, final SqlCondition[] extraConditions, final int limit, final Date startDate, final Date endDate );

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
     * Get the metadata hints for the condition quick add function
     */
    org.json.JSONObject getConditionQuickAddHints();
    
    /**
     * Tests if reporting is enabled, that is if reports will be
     * generated nightly.  Currently this is the same thing as "is the
     * reporting node installed and turned on."
     *
     * @return true if reporting is enabled, false otherwise.
     */
    boolean isReportingEnabled();
    
    Integer getTimeZoneOffset();
    
    public String getSettingsDiff(String fileName) throws SettingsException;
}
