/*
 * $Id: GlobUtil.java,v 1.00 2011/10/05 11:49:32 dmorris Exp $
 */
package com.untangle.node.util;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Utilities for escaping URIs.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class GlobUtil
{
    public static String globToRegex(String glob)
    {
        if (glob == null)
            return null;
        
        if ("".equals(glob))
            return "^$";
        
        /**
         * transform globbing operators into regex ones
         */
        String re = glob;
        re = re.replaceAll(Pattern.quote("."), "\\.");
        re = re.replaceAll(Pattern.quote("*"), ".*");
        re = re.replaceAll(Pattern.quote("?"), ".");

        re = "^" + re + "$";
        
        return re;
    }

    public static String urlGlobToRegex(String glob)
    {
        if (glob == null)
            return null;
        
        if ("".equals(glob))
            return "^$";

        String re = glob;

        /**
         * remove potential '\*\.?' or 'www.' at the beginning
         * for examlpe "*.foo.com" becomes just "foo.com" because "foo.com" matches "*.foo.com" AND "foo.com"
         */
        re = re.replaceAll("^"+Pattern.quote("*."), "");
        re = re.replaceAll("^"+Pattern.quote("www."), "");

        /**
         * transform globbing operators into regex ones
         */
        re = re.replaceAll(Pattern.quote("."), "\\.");
        re = re.replaceAll(Pattern.quote("*"), ".*");
        re = re.replaceAll(Pattern.quote("?"), ".");

        /**
         * possibly some path after a domain name... People
         * specifying 'google.com' certainly want to block
         * '"google.com/whatever"
         *
         * if the URL already ends in '/' just add .*
         * if it does not end in '/' add /.*
         * we do this because "foo.com/test" should match foo.com/test/bar and foo.com/test BUT NOT foo.com/testbar
         */
        if ( re.charAt(re.length()-1) == '/' )
            re = re + "(.*)?";
        else
            re = re + "(/.*)?";

        return re;
    }
}
