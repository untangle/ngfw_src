/**
 * $Id$
 */
package com.untangle.uvm.reports.jabsorb;

import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import com.untangle.app.reports.ReportEntry;
import com.untangle.app.reports.ReportsManager;
import com.untangle.app.reports.ReportsApp;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.LocaleInfo;
import com.untangle.uvm.SkinInfo;
import com.untangle.uvm.SkinManager;
import com.untangle.uvm.SkinSettings;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmException;


public class ReportsContextImpl implements UtJsonRpcServlet.ReportsContext
{
    private final Logger logger = Logger.getLogger(getClass());

    private final UvmContext context;

    private final SkinManager skinManager = new SkinManagerImpl();
    private final LanguageManager languageManager = new LanguageManagerImpl();
    private final ReportsManager reportsManager = ReportsManagerImpl.getInstance();

    private ReportsContextImpl( UvmContext context )
    {
        this.context = context;
    }

    public ReportsManager reportsManager()
    {
        return this.reportsManager;
    }

    public SkinManager skinManager()
    {
        return this.skinManager;
    }

    public LanguageManager languageManager()
    {
        return this.languageManager;
    }

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
        public SkinSettings getSettings() { return context.skinManager().getSettings(); }
        public void setSettings( SkinSettings skinSettings ) { throw new RuntimeException("Unable to change the skin settings."); }
        public void uploadSkin(FileItem item) { throw new RuntimeException("Unable to change the skin settings."); }
        public List<SkinInfo> getSkinsList( ) { return context.skinManager().getSkinsList(); }
        public SkinInfo getSkinInfo() { return context.skinManager().getSkinInfo(); }
    }

    /**
     * This proxy object is used so the reports servlet does not have access to setSettings and related methods
     */
    public class LanguageManagerImpl implements LanguageManager
    {
        public LanguageSettings getLanguageSettings() { return context.languageManager().getLanguageSettings(); }
        public void setLanguageSettings(LanguageSettings langSettings) { throw new RuntimeException("Unable to change the language settings."); }
        public void synchronizeLanguage() { throw new RuntimeException("Unable to synchronize language"); }
        public List<LocaleInfo> getLanguagesList() { return context.languageManager().getLanguagesList(); }
        public Map<String, String> getTranslations(String module) { return context.languageManager().getTranslations(module); }
    }
    
    /**
     * This class is used extend ReportsManagerImpl and overwrite some methods that changes settings so reports servlet does not have access to them.
     */
    public class ReportsManagerImpl extends com.untangle.app.reports.ReportsManagerImpl
    {
        public void setReportEntries( List<ReportEntry> newEntries ) { throw new RuntimeException("Unable to set the report entries."); }
        public void saveReportEntry( ReportEntry entry ) { throw new RuntimeException("Unable to set the event entries."); }
    }
}
