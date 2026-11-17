/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.app.reports;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeReportsManagerImpl implements SafeReportsManager
{
    private final ReportsManagerImpl delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeReportsManagerImpl(ReportsManagerImpl delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * fixedReportsAllowGraphs through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public Boolean fixedReportsAllowGraphs()
    {
        return delegate.fixedReportsAllowGraphs();
    }

    /**
     * getAllAppProperties through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<JSONObject> getAllAppProperties()
    {
        return delegate.getAllAppProperties();
    }

    /**
     * getColumnsForTable through the reviewed RPC contract.
     * @param tableName The tableName argument
     * @return The delegated result
     */
    @Override
    public String[] getColumnsForTable(String tableName)
    {
        return delegate.getColumnsForTable(tableName);
    }

    /**
     * getCurrentApplications through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<JSONObject> getCurrentApplications()
    {
        return delegate.getCurrentApplications();
    }

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
    @Override
    public List<JSONObject> getDataForReportEntry(ReportEntry entry, Date startDate, Date endDate, String[] extraSelects, SqlCondition[] extraConditions, SqlFrom fromType, int limit)
    {
        return delegate.getDataForReportEntry(entry, startDate, endDate, extraSelects, extraConditions, fromType, limit);
    }

    /**
     * getEvents through the reviewed RPC contract.
     * @param entry The entry argument
     * @param extraConditions The extraConditions argument
     * @param limit The limit argument
     * @return The delegated result
     */
    @Override
    public ArrayList<JSONObject> getEvents(ReportEntry entry, SqlCondition[] extraConditions, int limit)
    {
        return delegate.getEvents(entry, extraConditions, limit);
    }

    /**
     * getEventsForDateRangeResultSet through the reviewed RPC contract.
     * @param entry The entry argument
     * @param extraConditions The extraConditions argument
     * @param limit The limit argument
     * @param start The start argument
     * @param endDate The endDate argument
     * @return The delegated result
     */
    @Override
    public SafeResultSetReader getEventsForDateRangeResultSet(ReportEntry entry, SqlCondition[] extraConditions, int limit, Date start, Date endDate)
    {
        ResultSetReader reader = delegate.getEventsForDateRangeResultSet(entry, extraConditions, limit, start, endDate);
        return reader == null ? null : new SafeResultSetReaderImpl(reader);
    }

    /**
     * getEventsForTimeframeResultSet through the reviewed RPC contract.
     * @param entry The entry argument
     * @param extraConditions The extraConditions argument
     * @param timeframeSec The timeframeSec argument
     * @param limit The limit argument
     * @return The delegated result
     */
    @Override
    public SafeResultSetReader getEventsForTimeframeResultSet(ReportEntry entry, SqlCondition[] extraConditions, int timeframeSec, int limit)
    {
        ResultSetReader reader = delegate.getEventsForTimeframeResultSet(entry, extraConditions, timeframeSec, limit);
        return reader == null ? null : new SafeResultSetReaderImpl(reader);
    }

    /**
     * getEventsResultSet through the reviewed RPC contract.
     * @param entry The entry argument
     * @param extraConditions The extraConditions argument
     * @param limit The limit argument
     * @return The delegated result
     */
    @Override
    public SafeResultSetReader getEventsResultSet(ReportEntry entry, SqlCondition[] extraConditions, int limit)
    {
        ResultSetReader reader = delegate.getEventsResultSet(entry, extraConditions, limit);
        return reader == null ? null : new SafeResultSetReaderImpl(reader);
    }

    /**
     * getInterfacesInfo through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<JSONObject> getInterfacesInfo()
    {
        return delegate.getInterfacesInfo();
    }

    /**
     * getPoliciesInfo through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public ArrayList<JSONObject> getPoliciesInfo()
    {
        return delegate.getPoliciesInfo();
    }

    /**
     * getRecommendedReportIds through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<String> getRecommendedReportIds()
    {
        return delegate.getRecommendedReportIds();
    }

    /**
     * getReportEntries through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<ReportEntry> getReportEntries()
    {
        return delegate.getReportEntries();
    }

    /**
     * getReportEntry through the reviewed RPC contract.
     * @param uniqueId The uniqueId argument
     * @return The delegated result
     */
    @Override
    public ReportEntry getReportEntry(String uniqueId)
    {
        return delegate.getReportEntry(uniqueId);
    }

    /**
     * getReportInfo through the reviewed RPC contract.
     * @param appName The appName argument
     * @param policyId The policyId argument
     * @param key The key argument
     * @return The delegated result
     */
    @Override
    public JSONArray getReportInfo(String appName, Integer policyId, String key)
    {
        return delegate.getReportInfo(appName, policyId, key);
    }

    /**
     * getReportInfo through the reviewed RPC contract.
     * @param appName The appName argument
     * @param policyId The policyId argument
     * @param key The key argument
     * @param arguments The arguments argument
     * @return The delegated result
     */
    @Override
    public JSONArray getReportInfo(String appName, Integer policyId, String key, String... arguments)
    {
        return delegate.getReportInfo(appName, policyId, key, arguments);
    }

    /**
     * getTables through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String[] getTables()
    {
        return delegate.getTables();
    }

    /**
     * reinitializeDatabase through the reviewed RPC contract.
     */
    @Override
    public void reinitializeDatabase()
    {
        delegate.reinitializeDatabase();
    }

    /**
     * removeReportEntry through the reviewed RPC contract.
     * @param entry The entry argument
     */
    @Override
    public void removeReportEntry(ReportEntry entry)
    {
        delegate.removeReportEntry(entry);
    }

    /**
     * saveReportEntry through the reviewed RPC contract.
     * @param entry The entry argument
     */
    @Override
    public void saveReportEntry(ReportEntry entry)
    {
        delegate.saveReportEntry(entry);
    }
}
