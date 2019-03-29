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
 * Group matcher
 */
public class GroupMatcher
{
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_ANY = "[any]";
    private static final String MARKER_NONE = "[none]";

    private static GroupMatcher ANY_MATCHER = new GroupMatcher(MARKER_ANY);
    private static GroupMatcher NONE_MATCHER = new GroupMatcher(MARKER_NONE);

    private static Map<String,GroupMatcher> MatcherCache;
    static {
        MatcherCache = new ConcurrentHashMap<>();
        MatcherCache.put(MARKER_ANY, new GroupMatcher(MARKER_ANY));
        MatcherCache.put(MARKER_NONE, new GroupMatcher(MARKER_NONE));
    }

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This stores the string representation of this matcher
     */
    public String matcher;

    /**
     * This is all the available types of group matchers
     */
    private enum GroupMatcherType
    {
        ANY, NONE, SINGLE, LIST
    };

    /**
     * The type of this matcher
     */
    private GroupMatcherType type = GroupMatcherType.NONE;

    /**
     * This stores the groupname if this is a single matcher
     */
    public String single = null;
    Pattern regex = null;

    /**
     * if this port matcher is a list of port matchers, this list stores the
     * children
     */
    private LinkedList<GroupMatcher> children = null;

    /**
     * Create a group matcher from the given string
     * 
     * @param matcher
     *        The matcher string
     */
    public GroupMatcher(String matcher)
    {
        initialize(matcher);
    }

    /**
     * Check for a match
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
            for (GroupMatcher child : this.children) {
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
     * @return The string representation
     */
    public String toString()
    {
        return this.matcher;
    }

    /**
     * Get a matcher that will match any group
     *
     * @return The matcher
     */
    public static synchronized GroupMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }

    /**
     * Get a matcher that will not match any group.
     *
     * @return The matcher
     */
    public static synchronized GroupMatcher getNoneMatcher()
    {
        return NONE_MATCHER;
    }

    /**
     * Maintain cache of matchers.
     *
     * @param  matcher String to match.
     * @return         Return already defined matcher from cache.  If not found, create new matcher intsance and add to cache.
     */
    public static synchronized GroupMatcher getMatcher(String matcher){
        GroupMatcher groupMatcher = MatcherCache.get(matcher);
        if(groupMatcher == null){
            groupMatcher = new GroupMatcher(matcher);
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
     *        The string for initialization
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
            this.type = GroupMatcherType.LIST;

            this.children = new LinkedList<>();

            String[] results = matcher.split(MARKER_SEPERATOR);

            /* check each one */
            for (String childString : results) {
                GroupMatcher child = new GroupMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * Check the common constants
         */
        if (MARKER_ANY.equals(matcher)) {
            this.type = GroupMatcherType.ANY;
            return;
        }
        if (MARKER_NONE.equals(matcher)) {
            this.type = GroupMatcherType.NONE;
            return;
        }

        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = GroupMatcherType.SINGLE;
        this.single = matcher;
        this.regex = Pattern.compile(GlobUtil.globToRegex(matcher));

        return;
    }
}
