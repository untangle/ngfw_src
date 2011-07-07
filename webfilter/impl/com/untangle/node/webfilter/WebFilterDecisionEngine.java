/*
 * $Id: WebFilterDecisionEngine.java,v 1.00 2011/07/07 12:12:27 dmorris Exp $
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

import org.apache.log4j.Logger;

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

    private final HashMap<String,List<String>> urlDatabase = new HashMap<String,List<String>>();

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
        String url = "http://" + dom + "/" + uri;
        logger.error("LOOKUP: " + url); // FIXME for debugging
        List<String> all = urlDatabase.get(url);
        logger.error("LOOKUP: " + url + " = " + all); // FIXME for debugging

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
