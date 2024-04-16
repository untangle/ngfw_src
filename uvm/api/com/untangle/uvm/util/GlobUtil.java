/**
 * $Id$
 */
package com.untangle.uvm.util;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Utilities for transforming globs to regexs
 */
public class GlobUtil
{
    private static final Logger logger = Logger.getLogger(GlobUtil.class);

    /**
     * Return regular expression from glob.
     * @param  glob String of glob.
     * @return      String converted to regular expression.
     */
    public static String globToRegex(String glob)
    {
        if (glob == null)
            return null;
        
        if ("".equals(glob))
            return "^$";
        
        /**
         * transform globbing operators into regex ones
         */
        StringBuilder re = new StringBuilder();
        re.append("^");
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*':
                    re.append(".*");
                    break;
                case '?':
                    re.append(".");
                    break;
                case '.':
                case '(':
                case ')':
                case '{':
                case '}':
                case '+':
                case '|':
                case '^':
                case '[':
                case ']':
                case '@':
                    re.append("\\").append(c);
                    break;
                case '$':  // Handle $ as a literal character
                    re.append("\\$");
                    break;
                default:
                    re.append(c);
                    break;
            }
        }
        re.append("$");
        return re.toString();
    }

    /**
     * Return regular expression from URL glob.
     * @param  glob String of URL glob.
     * @return      String converted to regular expression.
     */
    public static String urlGlobToRegex(String glob)
    {
        if (glob == null)
            return null;
        if ("".equals(glob))
            return "^$";

        String re = glob;

        /**
         * remove potential '\*\.?' or 'www.' at the beginning
         * for example "*.foo.com" becomes just "foo.com" because "foo.com" matches "*.foo.com" AND "foo.com"
         */
        re = re.replaceAll("^"+Pattern.quote("*."), "");
        re = re.replaceAll("^"+Pattern.quote("www."), "");

        /**
         * transform unescaped globbing operators into regex ones
         */
        re = re.replaceAll("(?<!\\\\)" + Pattern.quote("."), "\\.");
        re = re.replaceAll("(?<!\\\\)" + Pattern.quote("*"), ".*");
        re = re.replaceAll("(?<!\\\\)" + Pattern.quote("?"), ".");

        /**
         * transform escaped globbing operators into regex ones
         */
        re = re.replaceAll("\\\\" + Pattern.quote("."), ".");
        re = re.replaceAll("\\\\" + Pattern.quote("*"), "*");
        re = re.replaceAll("\\\\" + Pattern.quote("?"), "?");

        /**
         * Add the right side anchor (if not explicitly denied with $)
         * This is so google.com blocks google.com/whatever
         */
        if (re.charAt(re.length()-1) != '$')  {
            re = re + ".*";
        }
        
        return re;
    }
}
