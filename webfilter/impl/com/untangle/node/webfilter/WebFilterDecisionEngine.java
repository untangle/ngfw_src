/*
 * $HeadURL: svn://chef/branch/prod/mawk/work/src/webfilter/impl/com/untangle/node/webfilter/WebFilterDecisionEngine.java $
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
import com.untangle.uvm.vnet.TCPSession;

/**
 * This class has the Web Filter Lite (webfilter) specifics for the decision engine
 *
 * @version 1.0
 */
class WebFilterDecisionEngine extends DecisionEngine
{
    private final Logger logger = Logger.getLogger(getClass());

    private boolean unconfigured = true;

    private static final File INIT_HOME = new File("/usr/share/untangle-webfilter-init/");

    private final UrlDatabase<String> urlDatabase = new UrlDatabase<String>();

    public WebFilterDecisionEngine(WebFilterBase node)
    {
        super(node);
    }

    protected boolean getLookupSubdomains()
    {
        return true;
    }

    // protected methods ------------------------------------------------------

    protected List<String> categorizeSite(String dom, int port, String uri)
    {
        List<String> all = urlDatabase.findAllBlacklisted("http", dom, uri);

        if (null == all || 0 == all.size()) {
            all = Collections.singletonList("Uncategorized");
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

        return all;
    }
}
