/**
 * $Id$
 */

package com.untangle.uvm.app;

import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import com.untangle.uvm.util.GlobUtil;

/**
 * A matcher for domain names
 */
public class DomainMatcher
{
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_ANY = "[any]";
    private static final String MARKER_NONE = "[none]";

    private static DomainMatcher ANY_MATCHER = new DomainMatcher(MARKER_ANY);
    private static DomainMatcher NONE_MATCHER = new DomainMatcher(MARKER_NONE);

    private static Map<String,DomainMatcher> MatcherCache;
    static {
        MatcherCache = new ConcurrentHashMap<>();
        MatcherCache.put(MARKER_ANY, new DomainMatcher(MARKER_ANY));
        MatcherCache.put(MARKER_NONE, new DomainMatcher(MARKER_NONE));
    }

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This stores the string representation of this matcher
     */
    public String matcher;

    /**
     * This is all the available types of domain matchers
     */
    private enum DomainMatcherType
    {
        ANY, NONE, SINGLE, LIST
    };

    /**
     * The type of this matcher
     */
    private DomainMatcherType type = DomainMatcherType.NONE;

    /**
     * This stores the domain name if this is a single matcher
     */
    public String single = null;
    Pattern regex = null;

    /**
     * if this port matcher is a list of port matchers, this list stores the
     * children
     */
    private LinkedList<DomainMatcher> children = null;

    /**
     * Create a domain matcher from the given string
     * 
     * @param matcher
     *        The string
     */
    public DomainMatcher(String matcher)
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
    public boolean isMatch(String value)
    {
        switch (this.type)
        {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            if (value == null) return false;
            if (value.equalsIgnoreCase(this.single)) return true;
            return this.regex.matcher(value).matches();

        case LIST:
            for (DomainMatcher child : this.children) {
                if (child.isMatch(value)) return true;
            }
            return false;

        default:
            logger.warn("Unknown port matcher type: " + this.type);
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
    public static synchronized DomainMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }

    /**
     * Get a matcher that will not match any domain.
     *
     * @return The matcher
     */
    public static synchronized DomainMatcher getNoneMatcher()
    {
        return NONE_MATCHER;
    }

    /**
     * Maintain cache of matchers.
     *
     * @param  matcher String to match.
     * @return         Return already defined matcher from cache.  If not found, create new matcher intsance and add to cache.
     */
    public static synchronized DomainMatcher getMatcher(String matcher){
        DomainMatcher groupMatcher = MatcherCache.get(matcher);
        if(groupMatcher == null){
            groupMatcher = new DomainMatcher(matcher);
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
            this.type = DomainMatcherType.LIST;

            this.children = new LinkedList<DomainMatcher>();

            String[] results = matcher.split(MARKER_SEPERATOR);

            /* check each one */
            for (String childString : results) {
                DomainMatcher child = new DomainMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * Check the common constants
         */
        if (MARKER_ANY.equals(matcher)) {
            this.type = DomainMatcherType.ANY;
            return;
        }
        if (MARKER_NONE.equals(matcher)) {
            this.type = DomainMatcherType.NONE;
            return;
        }

        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = DomainMatcherType.SINGLE;
        this.single = matcher;
        this.regex = Pattern.compile(GlobUtil.globToRegex(matcher));

        return;
    }
}
