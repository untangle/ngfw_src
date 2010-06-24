/*
 * $HeadURL: svn://chef/branch/prod/mawk/work/src/webfilter/impl/com/untangle/node/webfilter/Blacklist.java $
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

package com.untangle.node.webfilter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.sleepycat.je.DatabaseException;
import com.untangle.node.util.PrefixUrlList;
import com.untangle.node.util.UrlDatabase;
import com.untangle.node.util.UrlList;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import org.apache.log4j.Logger;

/**
 * Does blacklist lookups in the database.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class UrlBlacklist extends Blacklist
{
    private final Logger logger = Logger.getLogger(getClass());

    private final Pattern GOOGLE_MAPS_PATTERN
        = Pattern.compile("(mt[0-9]*.google.com)|(khm[0-9]*.google.com)|(cbk[0-9]*.google.com)|maps.gstatic.com");

    private boolean unconfigured = true;

    private static final URL BLACKLIST_HOME;
    static {
        try {
            BLACKLIST_HOME = new URL("http://webupdates.untangle.com/diffserver");
        } catch (MalformedURLException exn) {
            throw new RuntimeException(exn);
        }
    }

    private static final File INIT_HOME = new File("/usr/share/untangle-webfilter-init/");

    private final UrlDatabase<String> urlDatabase = new UrlDatabase<String>();

    public UrlBlacklist(WebFilterBase node)
    {
        super(node);
    }

    public void open()
    {
        startUpdateTimer();
    }

    public void close()
    {
        stopUpdateTimer();
    }

    protected boolean getLookupSubdomains()
    {
        return true;
    }

    protected void doReconfigure()
    {
        if (unconfigured) {
            LocalUvmContext uvm = LocalUvmContextFactory.context();
            Map<String,String> m = new HashMap<String,String>();
            m.put("key", uvm.getActivationKey());
            m.put("client-version", uvm.getFullVersion());

            urlDatabase.clear();

            for (BlacklistCategory cat : getSettings().getBlacklistCategories()) {
                String catName = cat.getName();
                if (catName.equals(BlacklistCategory.UNCATEGORIZED)) {
                    continue;
                }
                String dbName = "ubl-" + catName + "-url";
                try {
                    UrlList ul = new PrefixUrlList(BLACKLIST_HOME, "webfilter",
                                                   dbName, m,
                                                   new File(INIT_HOME, dbName));
                    urlDatabase.addBlacklist(dbName, ul);
                } catch (IOException exn) {
                    logger.warn("could not open: " + dbName, exn);
                } catch (DatabaseException exn) {
                    logger.warn("could not open: " + dbName, exn);
                }
            }

            urlDatabase.updateAll(true);
            unconfigured = false;
        }
    }

    protected void updateToCurrentCategories(WebFilterSettings settings)
    {
        Set<BlacklistCategory> curCategories = settings.getBlacklistCategories();

        if (curCategories.size() == 0) {
            /*
             * First time initialization
             */
            BlacklistCategory bc = new BlacklistCategory
                ("porn", "Pornography", "Adult and Sexually Explicit");
            bc.setBlock(true);
            bc.setLog(true);
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("mail", "Web Mail", "Web Mail");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("drugs", "Illegal Drugs", "Illegal Drugs");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("gambling", "Gambling", "Gambling");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("hacking", "Hacking", "Security Cracking");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("aggressive", "Hate and Aggression",
                                       "Hate and Aggression");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("violence", "Violence", "Violence");
            settings.addBlacklistCategory(bc);
        }
        if (curCategories.size() < 8) {
            /*
             * First time or upgrade from 3.0 to 3.1
             */
            BlacklistCategory bc = new BlacklistCategory("sports", "Sports", "Sports");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("jobsearch", "Job Search", "Job Search");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("vacation", "Vacation", "Vacation");
            settings.addBlacklistCategory(bc);
        }
        if (curCategories.size() < 11) {
            /*
             * First time or upgrade from 3.2 to 4.0
             */
            BlacklistCategory bc = new BlacklistCategory("ecommerce", "Shopping", "Online Shopping");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("socialnetworking", "Social Networking", "Social Networking");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("proxy", "Proxy Sites", "Proxy Sites");
            bc.setBlock(true);
            bc.setLog(true);
            settings.addBlacklistCategory(bc);
        }
        if (curCategories.size() < 14) {
            /*
             * First time or upgrade from 4.0 to 4.1
             */
            BlacklistCategory bc = new BlacklistCategory("dating", "Dating", "Online Dating");
            settings.addBlacklistCategory(bc);
        }

        for (BlacklistCategory bc : curCategories) {
            if (bc.getName().equals("proxy")) {
                bc.setDisplayName("Proxy Sites");
                bc.setDescription("Proxy Sites");
            }
        }
    }

    void startUpdateTimer()
    {
        urlDatabase.startUpdateTimer();
    }

    void stopUpdateTimer()
    {
        urlDatabase.stopUpdateTimer();
    }

    // protected methods ------------------------------------------------------

    protected String checkBlacklistDatabase(String dom, int port, String uri)
    {
        // XXX hack attack. should solve the problem with less lookups
        // instead, but no time for that.
        if (GOOGLE_MAPS_PATTERN.matcher(dom).matches()) {
            return null;
        }

        List<String> all = urlDatabase.findAllBlacklisted("http", dom, uri);

        if (null == all || 0 == all.size()) {
            all = Collections.singletonList(BlacklistCategory.UNCATEGORIZED);
        } else {
            for (int ai = 0; ai < all.size(); ai++) {
                String dbName = all.get(ai);

                int i = dbName.indexOf('-');
                if (0 < i) {
                    i++;
                    if (dbName.length() > i) {
                        int j = dbName.indexOf('-', i);
                        if (i < j) {
                            all.set(ai, dbName.substring(i, j));
                        }
                    }
                }
            }

        }

        return mostSpecificCategory(all);
    }
}
