/**
 * $Id$
 */
package com.untangle.uvm.reports.jabsorb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;

import com.untangle.app.reports.ReportEntry;
import com.untangle.app.reports.ReportsApp;
import com.untangle.app.reports.ReportsManager;
import com.untangle.app.reports.ResultSetReader;
import com.untangle.app.reports.SqlCondition;
import com.untangle.app.reports.SqlFrom;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.LocaleInfo;
import com.untangle.uvm.SkinInfo;
import com.untangle.uvm.SkinManager;
import com.untangle.uvm.SkinSettings;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

/**
 * Reports context for reports servlet.
 */
public class ReportsContextImpl implements UtJsonRpcServlet.ReportsContext
{
    private final Logger logger = LogManager.getLogger(getClass());

    private final UvmContext context;
    private static final String TIMEZONE_FILE = "/etc/timezone";

    private final SkinManager skinManager = new SkinManagerImpl();
    private final LanguageManager languageManager = new LanguageManagerImpl();
    private final ReportsManager reportsManager = this.new ReportsManagerImpl();

    /**
     * Initialize reportscontext with UVM context.
     *
     * @param context
     *  Current UVM context.
     */
    private ReportsContextImpl( UvmContext context )
    {
        this.context = context;
    }

    /**
     * Return timezone.
     *
     * @return
     *  Timezone value.
     */
    private TimeZone getTimeZone()
    {
        TimeZone current = TimeZone.getDefault();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(TIMEZONE_FILE));
            String str = in.readLine();
            str = str.trim();
            current = TimeZone.getTimeZone(str);
        } catch (Exception x) {
            logger.warn("Unable to get timezone, using java default:" , x);
        } finally {
            if (in != null){
                try{
                    in.close();
                } catch (Exception x) {
                    logger.warn("Unable to get timezone, using java default:" , x);
                }
            }
        }
        return current;
    }

    /**
     * Return reports manager.
     *
     * @return
     *  Reports manager.
     */
    public ReportsManager reportsManager()
    {
        return this.reportsManager;
    }

    /**
     * Return skin manager.
     * 
     * @return
     *  Skin manager.
     */
    public SkinManager skinManager()
    {
        return this.skinManager;
    }

    /**
     * Return language manager.
     * 
     * @return
     *  Language manager.
     */
    public LanguageManager languageManager()
    {
        return this.languageManager;
    }

    /**
     * Return current time in milliseconds.
     * 
     * @return
     *  Current time in milliseconds.
     */
    public long getMilliseconds()
    {
        return System.currentTimeMillis();
    }

    /**
     * Return timezone offset.
     * 
     * @return
     *  Time zone in seconds.
     */
    public Integer getTimeZoneOffset()
    {
        TimeZone tz = getTimeZone();
        Calendar cal = Calendar.getInstance(tz);
        Integer offset = tz.getOffset(cal.getTimeInMillis());
        logger.info("getTimeZoneOffset calculated value = " + offset);
        return(offset);
    }

    /**
     * Return new ReportsContext.
     * 
     * @return
     *  New reports context.
     */
    static UtJsonRpcServlet.ReportsContext makeReportsContext()
    {
        UvmContext uvm = UvmContextFactory.context();
        return new ReportsContextImpl( uvm );
    }

    /**
     * This proxy object is used so the reports servlet does not have access to setSettings and related methods
     */
    public class SkinManagerImpl implements SkinManager
    {
        /**
         * Proxy return skin settings.
         *
         * @return
         *  SkinSettings.
         */
        public SkinSettings getSettings() { return context.skinManager().getSettings(); }
        /**
         * Proxy save skin settings.
         *
         * @param skinSettings
         *  New SkinSettings to save.
         */
        public void setSettings( SkinSettings skinSettings ) { throw new RuntimeException("Unable to change the skin settings."); }
        /**
         * Proxy upload skin
         *
         * @param item
         *  File item containing new skin.
         */
        public void uploadSkin(FileItem item) { throw new RuntimeException("Unable to change the skin settings."); }
        /**
         * Proxy Return available skin list.
         *
         * @return
         *  List of SkinInfo.
         */
        public List<SkinInfo> getSkinsList( ) { return context.skinManager().getSkinsList(); }
        /**
         * Proxy return skin information.
         *
         * @return
         *  SkinInfo object.
         */
        public SkinInfo getSkinInfo() { return context.skinManager().getSkinInfo(); }
    }

    /**
     * This proxy object is used so the reports servlet does not have access to setSettings and related methods
     */
    public class LanguageManagerImpl implements LanguageManager
    {
        /**
         * Proxy return language settings.
         *
         * @return
         *  Language Settings.
         */
        public LanguageSettings getLanguageSettings() { return context.languageManager().getLanguageSettings(); }
        /**
         * Proxy save language settings.
         *
         * @param langSettings
         *  New language Settings.
         */
        public void setLanguageSettings(LanguageSettings langSettings) { throw new RuntimeException("Unable to change the language settings."); }
        /**
         * Proxy synchronize curent language - inactive
         */
        public void synchronizeLanguage() { throw new RuntimeException("Unable to synchronize language"); }
        /**
         * Proxy return language list.
         *
         * @return
         *  List of LocaleInfo.
         */
        public List<LocaleInfo> getLanguagesList() { return context.languageManager().getLanguagesList(); }
        /**
         * Proxy return translations for a module.
         *
         * @param module
         *  module to query.
         * @return
         *  Map of languages.
         */
        public Map<String, String> getTranslations(String module) { return context.languageManager().getTranslations(module); }
    }

    /**
     * This class is used extend ReportsManagerImpl and overwrite some methods that changes settings so reports servlet does not have access to them.
     * Query methods resolve client-provided entries against server-side definitions to prevent
     * injection of malicious entry fields (pieGroupColumn, conditions, etc.)
     */
    public class ReportsManagerImpl extends com.untangle.app.reports.ReportsManagerImpl
    {
        /**
         * Resolve a client-provided report entry against server-side definitions.
         *
         * @param clientEntry the entry provided by the client.
         * @return the server-side entry if found, otherwise the original client entry.
         */
        private ReportEntry resolveEntry(ReportEntry clientEntry)
        {
            if (clientEntry == null) return null;
            String uniqueId = clientEntry.getUniqueId();
            if (uniqueId != null && !uniqueId.isEmpty()) {
                ReportEntry serverEntry = super.getReportEntry(uniqueId);
                if (serverEntry != null) return serverEntry;
            }
            throw new RuntimeException("No server-side report entry found for uniqueId: " + uniqueId);
        }

        /**
         * Get report data for the given entry and date range with extra options.
         *
         * @param entry the report entry.
         * @param startDate the start date.
         * @param endDate the end date.
         * @param extraSelects additional select columns.
         * @param extraConditions additional SQL conditions.
         * @param fromType the SQL from type.
         * @param limit maximum number of results.
         * @return list of JSON result objects.
         */
        @Override
        public List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, String[] extraSelects, SqlCondition[] extraConditions, SqlFrom fromType, final int limit )
        {
            return super.getDataForReportEntry(resolveEntry(entry), startDate, endDate, null, extraConditions, fromType, limit);
        }

        /**
         * Get report data for the given entry and date range.
         *
         * @param entry the report entry.
         * @param startDate the start date.
         * @param endDate the end date.
         * @param limit maximum number of results.
         * @return list of JSON result objects.
         */
        @Override
        public List<JSONObject> getDataForReportEntry( ReportEntry entry, final Date startDate, final Date endDate, final int limit )
        {
            return super.getDataForReportEntry(resolveEntry(entry), startDate, endDate, limit);
        }

        /**
         * Get report data for the given entry and timeframe.
         *
         * @param entry the report entry.
         * @param timeframeSec the timeframe in seconds.
         * @param limit maximum number of results.
         * @return list of JSON result objects.
         */
        @Override
        public List<JSONObject> getDataForReportEntry( ReportEntry entry, final int timeframeSec, final int limit )
        {
            return super.getDataForReportEntry(resolveEntry(entry), timeframeSec, limit);
        }

        /**
         * Get events for the given report entry.
         *
         * @param entry the report entry.
         * @param extraConditions additional SQL conditions.
         * @param limit maximum number of results.
         * @return list of JSON event objects.
         */
        @Override
        public ArrayList<JSONObject> getEvents( final ReportEntry entry, final SqlCondition[] extraConditions, final int limit )
        {
            return super.getEvents(resolveEntry(entry), extraConditions, limit);
        }

        /**
         * Get an events result set for the given report entry.
         *
         * @param entry the report entry.
         * @param extraConditions additional SQL conditions.
         * @param limit maximum number of results.
         * @return the result set reader.
         */
        @Override
        public ResultSetReader getEventsResultSet( final ReportEntry entry, final SqlCondition[] extraConditions, final int limit )
        {
            return super.getEventsResultSet(resolveEntry(entry), extraConditions, limit);
        }

        /**
         * Get an events result set for the given report entry and timeframe.
         *
         * @param entry the report entry.
         * @param extraConditions additional SQL conditions.
         * @param timeframeSec the timeframe in seconds.
         * @param limit maximum number of results.
         * @return the result set reader.
         */
        @Override
        public ResultSetReader getEventsForTimeframeResultSet( final ReportEntry entry, final SqlCondition[] extraConditions, final int timeframeSec, final int limit )
        {
            return super.getEventsForTimeframeResultSet(resolveEntry(entry), extraConditions, timeframeSec, limit);
        }

        /**
         * Get an events result set for the given report entry and date range.
         *
         * @param entry the report entry.
         * @param extraConditions additional SQL conditions.
         * @param limit maximum number of results.
         * @param startDate the start date.
         * @param endDate the end date.
         * @return the result set reader.
         */
        @Override
        public ResultSetReader getEventsForDateRangeResultSet( final ReportEntry entry, final SqlCondition[] extraConditions, final int limit, final Date startDate, final Date endDate )
        {
            return super.getEventsForDateRangeResultSet(resolveEntry(entry), extraConditions, limit, startDate, endDate);
        }

        /**
         * Set report entries. Not supported in this context.
         *
         * @param newEntries the report entries to set.
         */
        @Override
        public void setReportEntries( List<ReportEntry> newEntries ) { throw new RuntimeException("Unable to set the report entries."); }

        /**
         * Save report entry.
         *
         * @param entry
         *  Report entry to save.
         */
        @Override
        public void saveReportEntry( ReportEntry entry ) { throw new RuntimeException("Unable to save the report entry."); }
        /**
         * Remove report entry.
         *
         * @param entry
         *  Report entry to remove.
         */
        @Override
        public void removeReportEntry( ReportEntry entry ) { throw new RuntimeException("Unable to remove the report entry."); }
        /**
         * Set reports app.
         *
         * @param app
         *  Reports app to set.
         */
        @Override
        public void setReportsApp( ReportsApp app ) { throw new RuntimeException("Unable to set the reports app."); }
        /**
         * Reinitialize database.
         */
        @Override
        public void reinitializeDatabase() { throw new RuntimeException("Unable to reinitialize the database."); }
    }
}
