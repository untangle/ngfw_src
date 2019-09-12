/**
 * $Id$
 */

package com.untangle.uvm.app;

import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.util.GlobUtil;

/**
 * A matcher for domain names
 */
public class BitmaskMatcher
{
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_ANY = "[any]";
    private static final String MARKER_NONE = "[none]";

    private static BitmaskMatcher ANY_MATCHER = new BitmaskMatcher(MARKER_ANY);
    private static BitmaskMatcher NONE_MATCHER = new BitmaskMatcher(MARKER_NONE);

    private static Map<String,BitmaskMatcher> MatcherCache;
    static {
        MatcherCache = new ConcurrentHashMap<>();
        MatcherCache.put(MARKER_ANY, new BitmaskMatcher(MARKER_ANY));
        MatcherCache.put(MARKER_NONE, new BitmaskMatcher(MARKER_NONE));
    }

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This stores the string representation of this matcher
     */
    public String matcher;

    /**
     * This is all the available types of domain matchers
     */
    private enum BitmaskMatcherType
    {
        ANY, NONE, SINGLE, LIST
    };

    /**
     * The type of this matcher
     */
    private BitmaskMatcherType type = BitmaskMatcherType.NONE;

    /**
     * This stores the domain name if this is a single matcher
     */
    public String single = null;
    Integer bit = null;

    /**
     * if this port matcher is a list of port matchers, this list stores the
     * children
     */
    private LinkedList<BitmaskMatcher> children = null;

    /**
     * Create a domain matcher from the given string
     * 
     * @param matcher
     *        The string
     */
    public BitmaskMatcher(String matcher)
    {
        initialize(matcher);
    }

    /**
     * Check for a match using the argumented value
     * 
     * @param value
     *        The value
     * @return True for match, otherwise false
     */
    public boolean isMatch(Integer value)
    {
        switch (this.type)
        {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            if (value == null) return false;
            return ( ( this.bit & value ) >  0 ) ? true : false;

        case LIST:
            for (BitmaskMatcher child : this.children) {
                if (child.isMatch(value)) return true;
            }
            return false;

        default:
            logger.warn("Unknown bitmask matcher type: " + this.type);
            return false;

        }
    }

    /**
     * Return string representation
     * 
     * @return The string
     */
    public String toString()
    {
        return this.matcher;
    }

    /**
     * Return a matcher that will match any domain
     *
     * @return The matcher
     */
    public static synchronized BitmaskMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }

    /**
     * Get a matcher that will not match any domain.
     *
     * @return The matcher
     */
    public static synchronized BitmaskMatcher getNoneMatcher()
    {
        return NONE_MATCHER;
    }

    /**
     * Maintain cache of matchers.
     *
     * @param  matcher String to match.
     * @return         Return already defined matcher from cache.  If not found, create new matcher intsance and add to cache.
     */
    public static synchronized BitmaskMatcher getMatcher(String matcher){
        BitmaskMatcher groupMatcher = MatcherCache.get(matcher);
        if(groupMatcher == null){
            groupMatcher = new BitmaskMatcher(matcher);
            MatcherCache.put(matcher, groupMatcher);
        }
        return groupMatcher;
    }

    /**
     * Determine if value is a string that would be a matcher or not.
     *
     * @param  candidate String to check.
     * @return       Boolean true if would be matcher or set, false otherwise.
     */
    public static Boolean isMatchable(String candidate){
        return (candidate.indexOf(';') > -1 ||
            candidate.indexOf(',') > -1 ||
            candidate.indexOf('*') > -1  ||
            candidate.indexOf(MARKER_SEPERATOR) > -1  ||
            candidate.indexOf(MARKER_ANY) > -1  ||
            candidate.indexOf(MARKER_NONE) > -1 );
    }

    /**
     * Initialize all the private variables
     * 
     * @param matcher
     *        The matcher used to initialize
     */
    private void initialize(String matcher)
    {
        // We used to ';' as a seperator, we now use ','
        matcher = matcher.replaceAll(";", ",");
        // only lower case
        matcher = matcher.toLowerCase().trim();
        this.matcher = matcher;

        /**
         * If it contains a comma it must be a list of port matchers if so, go
         * ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR)) {
            this.type = BitmaskMatcherType.LIST;

            this.children = new LinkedList<>();

            String[] results = matcher.split(MARKER_SEPERATOR);

            /* check each one */
            for (String childString : results) {
                BitmaskMatcher child = new BitmaskMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * Check the common constants
         */
        if (MARKER_ANY.equals(matcher)) {
            this.type = BitmaskMatcherType.ANY;
            return;
        }
        if (MARKER_NONE.equals(matcher)) {
            this.type = BitmaskMatcherType.NONE;
            return;
        }

        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = BitmaskMatcherType.SINGLE;
        this.single = matcher;
        if(matcher.equals("0")){
            this.type = BitmaskMatcherType.ANY;
            this.bit = 0;
        }else{
            this.type = BitmaskMatcherType.SINGLE;
            this.bit = (int) Math.pow(2, Integer.parseInt(matcher) - 1);
        }

        return;
    }
}
