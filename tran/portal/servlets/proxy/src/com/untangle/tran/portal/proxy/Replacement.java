/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: UrlRewriter.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.tran.portal.proxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

class Replacement
{
    private final Pattern pattern;
    private final String replacement;
    private final boolean global;

    private static final Logger logger = Logger.getLogger(Replacement.class);

    // constructors -----------------------------------------------------------

    private Replacement(String regexp, String replacement, boolean global)
    {
        this.pattern = Pattern.compile(regexp);
        this.replacement = replacement;
        this.global = global;
    }

    // static factories -------------------------------------------------------

    static List<Replacement> getReplacements(InputStream is)
        throws IOException
    {
        List<Replacement> l = new ArrayList<Replacement>();

        Reader r = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(r);

        String line;
        while (null != (line = br.readLine())) {
            line = line.trim();
            Replacement repl = getReplacement(line);
            if (null != repl) {
                l.add(repl);
            }
        }

        return l;
    }

    static Replacement getReplacement(String expr)
    {
        int i = expr.indexOf('#');
        if (0 <= i) {
            expr = expr.substring(0, i);
        }

        if (4 > expr.length()) { // "s///"
            if (0 < expr.length()) {
                logger.warn("bad replacement: " + expr);
            }
            return null;
        } else if ('s' != expr.charAt(0)) {
            logger.warn("bad command: " + expr);
            return null;
        } else {
            char delim = expr.charAt(1);

            StringBuilder regexp = new StringBuilder();
            StringBuilder repl = new StringBuilder();
            StringBuilder suffix = new StringBuilder();
            StringBuilder[] b = new StringBuilder[] { regexp, repl, suffix };
            int j = 0;

            for (i = 2; i < expr.length(); i++) {
                char c = expr.charAt(i);
                if ('\\' == c) {
                    if (expr.length() - 1 == i) {
                        logger.warn("unterminated escape: " + expr);
                    } else {
                        b[j].append(c).append(expr.charAt(++i));
                    }
                } else {
                    if (j < 2 && delim == c) {
                        j++;
                    } else {
                        b[j].append(c);
                    }
                }
            }

            boolean global = 0 <= suffix.indexOf("g");

            return new Replacement(regexp.toString(), repl.toString(), global);
        }
    }

    // package protected methods ----------------------------------------------

    String doReplacement(String l)
    {
        String r = replacement; // XXX do subs

        Matcher m = pattern.matcher(l);

        return global ? m.replaceAll(r) : m.replaceFirst(r);
    }
}
