/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.app.reports;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Reviewed callable operations for SafeReportsManager.
 */
public interface SafeReportsManager
{
    /**
     * fixedReportsAllowGraphs through the reviewed RPC contract.
     * @return The delegated result
     */
    Boolean fixedReportsAllowGraphs();

    /**
     * getAllAppProperties through the reviewed RPC contract.
     * @return The delegated result
     */
    List<JSONObject> getAllAppProperties();

    /**
     * getColumnsForTable through the reviewed RPC contract.
     * @param tableName The tableName argument
     * @return The delegated result
     */
    String[] getColumnsForTable(String tableName);

    /**
     * getCurrentApplications through the reviewed RPC contract.
     * @return The delegated result
     */
    List<JSONObject> getCurrentApplications();

    /**
     * getDataForReportEntry through the reviewed RPC contract.
     * @param entry The entry argument
     * @param startDate The startDate argument
     * @param endDate The endDate argument
     * @param extraSelects The extraSelects argument
     * @param extraConditions The extraConditions argument
     * @param fromType The fromType argument
     * @param limit The limit argument
     * @return The delegated result
     */
    List<JSONObject> getDataForReportEntry(ReportEntry entry, Date startDate, Date endDate, String[] extraSelects, SqlCondition[] extraConditions, SqlFrom fromType, int limit);

    /**
     * getEvents through the reviewed RPC contract.
     * @param entry The entry argument
     * @param extraConditions The extraConditions argument
     * @param limit The limit argument
     * @return The delegated result
     */
    ArrayList<JSONObject> getEvents(ReportEntry entry, SqlCondition[] extraConditions, int limit);

    /**
     * getEventsForDateRangeResultSet through the reviewed RPC contract.
     * @param entry The entry argument
     * @param extraConditions The extraConditions argument
     * @param limit The limit argument
     * @param start The start argument
     * @param endDate The endDate argument
     * @return The delegated result
     */
    SafeResultSetReader getEventsForDateRangeResultSet(ReportEntry entry, SqlCondition[] extraConditions, int limit, Date start, Date endDate);

    /**
     * getEventsForTimeframeResultSet through the reviewed RPC contract.
     * @param entry The entry argument
     * @param extraConditions The extraConditions argument
     * @param timeframeSec The timeframeSec argument
     * @param limit The limit argument
     * @return The delegated result
     */
    SafeResultSetReader getEventsForTimeframeResultSet(ReportEntry entry, SqlCondition[] extraConditions, int timeframeSec, int limit);

    /**
     * getEventsResultSet through the reviewed RPC contract.
     * @param entry The entry argument
     * @param extraConditions The extraConditions argument
     * @param limit The limit argument
     * @return The delegated result
     */
    SafeResultSetReader getEventsResultSet(ReportEntry entry, SqlCondition[] extraConditions, int limit);

    /**
     * getInterfacesInfo through the reviewed RPC contract.
     * @return The delegated result
     */
    List<JSONObject> getInterfacesInfo();

    /**
     * getPoliciesInfo through the reviewed RPC contract.
     * @return The delegated result
     */
    ArrayList<JSONObject> getPoliciesInfo();

    /**
     * getRecommendedReportIds through the reviewed RPC contract.
     * @return The delegated result
     */
    List<String> getRecommendedReportIds();

    /**
     * getReportEntries through the reviewed RPC contract.
     * @return The delegated result
     */
    List<ReportEntry> getReportEntries();

    /**
     * getReportEntry through the reviewed RPC contract.
     * @param uniqueId The uniqueId argument
     * @return The delegated result
     */
    ReportEntry getReportEntry(String uniqueId);

    /**
     * getReportInfo through the reviewed RPC contract.
     * @param appName The appName argument
     * @param policyId The policyId argument
     * @param key The key argument
     * @return The delegated result
     */
    JSONArray getReportInfo(String appName, Integer policyId, String key);

    /**
     * getReportInfo through the reviewed RPC contract.
     * @param appName The appName argument
     * @param policyId The policyId argument
     * @param key The key argument
     * @param arguments The arguments argument
     * @return The delegated result
     */
    JSONArray getReportInfo(String appName, Integer policyId, String key, String... arguments);

    /**
     * getTables through the reviewed RPC contract.
     * @return The delegated result
     */
    String[] getTables();

    /**
     * reinitializeDatabase through the reviewed RPC contract.
     */
    void reinitializeDatabase();

    /**
     * removeReportEntry through the reviewed RPC contract.
     * @param entry The entry argument
     */
    void removeReportEntry(ReportEntry entry);

    /**
     * saveReportEntry through the reviewed RPC contract.
     * @param entry The entry argument
     */
    void saveReportEntry(ReportEntry entry);
}
