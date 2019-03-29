/**
 * $Id$
 */

package com.untangle.uvm.app;

import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.uvm.util.GlobUtil;

/**
 * Glob matcher
 */
public class GlobMatcher
{
    private static final String MARKER_SEPERATOR = ",";

    private final Logger logger = Logger.getLogger(getClass());

    private static Map<String,GlobMatcher> MatcherCache;
    static {
        MatcherCache = new ConcurrentHashMap<>();
    }

    /**
     * This stores the string representation of this matcher
     */
    public String matcher;

    /**
     * This is all the available types of str matchers
     */
    private enum GlobMatcherType
    {
        NONE, SINGLE, LIST
    };

    /**
     * The type of this matcher
     */
    private GlobMatcherType type = GlobMatcherType.NONE;

    /**
     * This stores the string if this is a single matcher
     */
    private String single = null;

    /**
     * This stores the regex string if the single matcher is a glob
     */
    private Pattern regex = null;

    /**
     * if this port matcher is a list of port matchers, this list stores the
     * children
     */
    private LinkedList<GlobMatcher> children = null;

    /**
     * Create a str matcher from the given string
     * 
     * @param matcher
     *        The string
     */
    public GlobMatcher(String matcher)
    {
        initialize(matcher);
    }

    /**
     * Check for a match
     * 
     * @param str
     *        The string to check
     * @return True for match, otherwise false
     */
    public boolean isMatch(String str)
    {
        if (str != null) str = str.toLowerCase();

        switch (this.type)
        {

        case NONE:
            return false;

        case SINGLE:
            if (str == null) {
                /* "" matches null */
                if ("".equals(single)) return true;
                return false;
            }
            if (str.equalsIgnoreCase(this.single)) return true;
            if (this.regex.matcher(str).matches()) return true;
            return false;

        case LIST:
            for (GlobMatcher child : this.children) {
                if (child.isMatch(str)) return true;
            }
            return false;

        default:
            logger.warn("Unknown port matcher type: " + this.type);
            return false;

        }
    }

    /**
     * Maintain cache of matchers.
     *
     * @param  matcher String to match.
     * @return         Return already defined matcher from cache.  If not found, create new matcher intsance and add to cache.
     */
    public static synchronized GlobMatcher getMatcher(String matcher){
        GlobMatcher globMatcher = MatcherCache.get(matcher);
        if(globMatcher == null){
            globMatcher = new GlobMatcher(matcher);
            MatcherCache.put(matcher, globMatcher);
        }
        return globMatcher;
    }

    /**
     * Return string representation
     * 
     * @return The string representation
     */
    public String toString()
    {
        return this.matcher;
    }

    /**
     * Initialize all the private variables
     * 
     * @param matcher
     *        The matcher for initialization
     */
    private void initialize(String matcher)
    {
        // only lower case
        matcher = matcher.toLowerCase().trim();
        this.matcher = matcher;

        /**
         * If it contains a comma it must be a list of port matchers if so, go
         * ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR)) {
            this.type = GlobMatcherType.LIST;

            this.children = new LinkedList<>();

            String[] results = matcher.split(MARKER_SEPERATOR);

            /* check each one */
            for (String childString : results) {
                GlobMatcher child = new GlobMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = GlobMatcherType.SINGLE;
        this.single = matcher;
        this.regex = Pattern.compile(GlobUtil.globToRegex(matcher));

        return;
    }
}
