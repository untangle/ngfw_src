/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.reports.jabsorb;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.LocaleInfo;
import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.SkinManager;
import com.untangle.uvm.SkinInfo;
import com.untangle.uvm.SkinSettings;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.RemoteUvmContextFactory;
import com.untangle.uvm.RemoteUvmContext;
import com.untangle.uvm.reports.ApplicationData;
import com.untangle.uvm.reports.DateItem;
import com.untangle.uvm.reports.Highlight;
import com.untangle.uvm.reports.RemoteReportingManager;
import com.untangle.uvm.reports.TableOfContents;
import org.apache.commons.fileupload.FileItem;


public class ReportsContextImpl implements UtJsonRpcServlet.ReportsContext
{
    private final RemoteUvmContext context;

    private final SkinManager skinManager = new SkinManagerImpl();
    private final RemoteReportingManager reportingManager = new RemoteReportingManagerImpl();
    private final LanguageManager languageManager = new LanguageManagerImpl();


    private ReportsContextImpl( RemoteUvmContext context )
    {
        this.context = context;
    }

    public RemoteReportingManager reportingManager()
    {
        return this.reportingManager;
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
        RemoteUvmContext uvm = RemoteUvmContextFactory.context();
        return new ReportsContextImpl( uvm );
    }

    private class SkinManagerImpl implements SkinManager
    {
        /**
         * Get the settings.
         *
         * @return the settings.
         */
        public SkinSettings getSkinSettings()
        {
            return context.skinManager().getSkinSettings();
        }

        /**
         * Set the settings.
         *
         * @param skinSettings the settings.
         */
        public void setSkinSettings(SkinSettings skinSettings)
        {
            throw new RuntimeException("Unable to change the skin settings.");
        }

        /**
         * Upload a new skin
         */
        public void uploadSkin(FileItem item) throws UvmException
        {
            throw new RuntimeException("Unable to change the skin settings.");
        }


        public List<SkinInfo> getSkinsList(boolean fetchAdminSkins,
                                           boolean fetchUserFacingSkins)
        {
            return context.skinManager().getSkinsList(fetchAdminSkins,
                                                      fetchUserFacingSkins);
        }
    }

    private class LanguageManagerImpl implements LanguageManager
    {
        /**
         * Get the settings.
         *
         * @return the settings.
         */
    public LanguageSettings getLanguageSettings()
        {
            return context.languageManager().getLanguageSettings();
        }

        /**
         * Set the settings.
         *
         * @param langSettings the settings.
         */
        public void setLanguageSettings(LanguageSettings langSettings)
        {
            throw new RuntimeException("Unable to change the language settings.");
        }

        /**
         * Upload New Language Pack
         *
         * @return true if the uploaded language pack was processed
         * with no errors; otherwise returns false.
         */
        public boolean uploadLanguagePack(FileItem item) throws UvmException
        {
            throw new RuntimeException("Unable to upload a language pack.");
        }

        /**
         * Get list of available languages
         */
        public List<LocaleInfo> getLanguagesList()
        {
            return context.languageManager().getLanguagesList();
        }

        /**
         * Return the map of translations for a module, for the
         * current language.
         *
         * @param module  the name of the module
         * @return map of translations
         */
        public Map<String, String> getTranslations(String module)
        {
            return context.languageManager().getTranslations(module);
        }
    }

    private class RemoteReportingManagerImpl implements RemoteReportingManager
    {
        public List<DateItem> getDates()
        {
            return context.reportingManager().getDates();
        }

        public List<Highlight> getHighlights(Date d, int numDays)
        {
            return context.reportingManager().getHighlights(d, numDays);
        }

        public Date getReportsCutoff()
        {
            return context.reportingManager().getReportsCutoff();
        }

        public TableOfContents getTableOfContents(Date d, int numDays)
        {
            return context.reportingManager().getTableOfContents(d, numDays);
        }

        public TableOfContents getTableOfContentsForHost(Date d, int numDays,
                                                         String hostname)
        {
            return context.reportingManager().getTableOfContentsForHost(d, numDays, hostname);
        }

        public TableOfContents getTableOfContentsForUser(Date d, int numDays,
                                                         String username)
        {
            return context.reportingManager().getTableOfContentsForUser(d, numDays, username);
        }

        public TableOfContents getTableOfContentsForEmail(Date d, int numDays,
                                                          String email)
        {
            return context.reportingManager().getTableOfContentsForEmail(d, numDays, email);
        }

        public ApplicationData getApplicationData(Date d, int numDays,
                                                  String appName, String type,
                                                  String value)
        {
            return context.reportingManager().getApplicationData(d, numDays, appName, type, value);
        }

        public ApplicationData getApplicationData(Date d, int numDays,
                                                  String appName)
        {
            return context.reportingManager().getApplicationData(d, numDays, appName);
        }

        public ApplicationData getApplicationDataForUser(Date d, int numDays,
                                                         String appName,
                                                         String username)
        {
            return context.reportingManager().getApplicationDataForUser(d, numDays, appName, username);
        }


        public ApplicationData getApplicationDataForEmail(Date d, int numDays,
                                                          String appName,
                                                          String emailAddr)
        {
            return context.reportingManager().getApplicationDataForEmail(d, numDays, appName, emailAddr);
        }

        public ApplicationData getApplicationDataForHost(Date d, int numDays,
                                                         String appName,
                                                         String hostname)
        {
            return context.reportingManager().getApplicationDataForHost(d, numDays, appName, hostname);
        }

        public List<List<Object>> getDetailData(Date d, int numDays, String appName,
                                        String detailName, String type,
                                        String value)
        {
            return context.reportingManager().getDetailData(d, numDays, appName, detailName, type, value);
        }


        public List<List<Object>> getAllDetailData(Date d, int numDays, String appName,
                                           String detailName, String type,
                                           String value)
        {
            return context.reportingManager().getAllDetailData(d, numDays, appName, detailName, type, value);
        }

        /**
         *  Tests if reporting is enabled, that is if reports will be
         * generated nightly.  Currently this is the same thing as "is the
         * reporting node installed and turned on."
         *
         * @return true if reporting is enabled, false otherwise.
         */
        public boolean isReportingEnabled()
        {
            return context.reportingManager().isReportingEnabled();
        }

        /**
         * Tests if reporting is enabled and reports have been generated
         * and are ready to view.  Currently this is the same thing as
         * "does the current symlink exist and contain a valid
         * reporting-node/sum-daily.html file."
         *
         * @return true if reports are available
         */
        public boolean isReportsAvailable()
        {
            return context.reportingManager().isReportsAvailable();
        }
    }
}
