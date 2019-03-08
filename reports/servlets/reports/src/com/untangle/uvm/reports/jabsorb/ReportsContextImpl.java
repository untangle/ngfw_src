/**
 * $Id$
 */
package com.untangle.uvm.reports.jabsorb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import com.untangle.app.reports.ReportEntry;
import com.untangle.app.reports.ReportsManager;
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
    private final Logger logger = Logger.getLogger(getClass());

    private final UvmContext context;
    private static final String TIMEZONE_FILE = "/etc/timezone";

    private final SkinManager skinManager = new SkinManagerImpl();
    private final LanguageManager languageManager = new LanguageManagerImpl();
    private final ReportsManager reportsManager = ReportsManagerImpl.getInstance();

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
     */
    public class ReportsManagerImpl extends com.untangle.app.reports.ReportsManagerImpl
    {
        /**
         * Set report entries.
         *
         * @param newEntries
         *  New report entries.
         */
        public void setReportEntries( List<ReportEntry> newEntries ) { throw new RuntimeException("Unable to set the report entries."); }
        /**
         * Save report entry.
         *
         * @param entry
         *  Report entry to save.
         */
        public void saveReportEntry( ReportEntry entry ) { throw new RuntimeException("Unable to set the event entries."); }
    }
}
