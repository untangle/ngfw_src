/**
 * $Id$
 */
package com.untangle.uvm.reports.jabsorb;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.LocaleInfo;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.SkinManager;
import com.untangle.uvm.SkinInfo;
import com.untangle.uvm.SkinSettings;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;
import com.untangle.node.reporting.ReportingNode;
import com.untangle.node.reporting.ReportingManager;
import com.untangle.node.reporting.items.ApplicationData;
import com.untangle.node.reporting.items.DateItem;
import com.untangle.node.reporting.items.Highlight;
import com.untangle.node.reporting.items.TableOfContents;
import org.apache.commons.fileupload.FileItem;


public class ReportsContextImpl implements UtJsonRpcServlet.ReportsContext
{
    private final Logger logger = Logger.getLogger(getClass());

    private final UvmContext context;

    private final SkinManager skinManager = new SkinManagerImpl();
    private final LanguageManager languageManager = new LanguageManagerImpl();

    private ReportsContextImpl( UvmContext context )
    {
        this.context = context;
    }

    public ReportingManager reportingManager()
    {
        ReportingNode reporting = (ReportingNode) UvmContextFactory.context().nodeManager().node("untangle-node-reporting");
        if (reporting == null) {
            logger.warn("reporting node not found");
            return null;
        }
        return reporting.getReportingManager();
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
     * This proxy object is used so the reporting servlet does not have access to setSettings and related methods
     */
    public class SkinManagerImpl implements SkinManager
    {
        public SkinSettings getSettings() { return context.skinManager().getSettings(); }
        public void setSettings( SkinSettings skinSettings ) { throw new RuntimeException("Unable to change the skin settings."); }
        public void uploadSkin(FileItem item) { throw new RuntimeException("Unable to change the skin settings."); }
        public List<SkinInfo> getSkinsList( ) { return context.skinManager().getSkinsList(); }
    }

    /**
     * This proxy object is used so the reporting servlet does not have access to setSettings and related methods
     */
    public class LanguageManagerImpl implements LanguageManager
    {
        public LanguageSettings getLanguageSettings() { return context.languageManager().getLanguageSettings(); }
        public void setLanguageSettings(LanguageSettings langSettings) { throw new RuntimeException("Unable to change the language settings."); }
        public boolean uploadLanguagePack(FileItem item) throws UvmException { throw new RuntimeException("Unable to upload a language pack."); }
        public List<LocaleInfo> getLanguagesList() { return context.languageManager().getLanguagesList(); }
        public Map<String, String> getTranslations(String module) { return context.languageManager().getTranslations(module); }
    }
}
