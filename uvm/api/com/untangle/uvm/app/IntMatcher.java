/**
 * $HeadURL$
 */
package com.untangle.uvm.app;

import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

// THIS IS FOR ECLIPSE - @formatter:off

/**
 * This class manages the "matching" of integers
 *
 * IntMatchers are a string that matches integers
 * "80" matches 80
 * "81,82" matches 81 and 82
 * "80-90" matches 80 through 90
 * "80,90-100" matches 80 and 90-100
 * ">80" matches any int greater than 80
 * "<80" matches any int less than 80
 * "any" matches any int
 * "none" matches nothing
 */

// THIS IS FOR ECLIPSE - @formatter:on

@SuppressWarnings("serial")
public class IntMatcher implements java.io.Serializable
{
    private static final String MARKER_ANY = "any";
    private static final String MARKER_ALL = "all";
    private static final String MARKER_NONE = "none";
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_GREATER_THAN = ">";
    private static final String MARKER_LESS_THAN = "<";
    private static final String MARKER_RANGE = "-";

    private static IntMatcher ANY_MATCHER = new IntMatcher(MARKER_ANY);

    private static Map<String,IntMatcher> MatcherCache;

    static {
        MatcherCache = new ConcurrentHashMap<>();
    }

    private final Logger logger = Logger.getLogger(getClass());

    public String matcher;

    private enum IntMatcherType
    {
        ANY, NONE, SINGLE, GREATER_THAN, LESS_THAN, RANGE, LIST
    };

    /**
     * The type of this matcher
     */
    private IntMatcherType type = IntMatcherType.NONE;

    /**
     * if this port matcher is a list of port matchers, this list stores the
     * children
     */
    private LinkedList<IntMatcher> children = null;

    /**
     * if its a range these two variable store the min and max
     */
    private Number rangeMin = null;
    private Number rangeMax = null;

    /**
     * if its just an int matcher this stores the number
     */
    private Number singleNum = null;

    /**
     * Constructor
     * 
     * @param matcher
     *        The init string
     */
    public IntMatcher(String matcher)
    {
        initialize(matcher);
    }

    /**
     * Get the matcher string
     * 
     * @return The string
     */
    public String getMatcher()
    {
        return this.matcher;
    }

    /**
     * Set the matcher string
     * 
     * @param matcher
     *        The string
     */
    public void setMatcher(String matcher)
    {
        initialize(matcher);
    }

    /**
     * Return true if <param>num</param> matches this matcher.
     * 
     * @param num
     *        The num to test
     * @return True if the <param>num</param> matches.
     */
    public boolean isMatch(long num)
    {
        switch (this.type)
        {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            if (singleNum.longValue() == num) return true;
            return false;

        case GREATER_THAN:
            if (num > singleNum.longValue()) return true;
            return false;

        case LESS_THAN:
            if (num < singleNum.longValue()) return true;
            return false;

        case RANGE:
            if (num >= rangeMin.longValue() && num <= rangeMax.longValue()) return true;
            return false;

        case LIST:
            for (IntMatcher child : this.children) {
                if (child.isMatch(num)) return true;
            }
            return false;

        default:
            logger.warn("Unknown num matcher type: " + this.type);
            return false;
        }
    }

    /**
     * Return true if <param>port</param> matches this matcher.
     * 
     * @param num
     *        The num to test
     * @return True if the <param>port</param> matches.
     */
    public boolean isMatch(double num)
    {
        switch (this.type)
        {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            if (singleNum.doubleValue() == num) return true;
            return false;

        case GREATER_THAN:
            if (num > singleNum.doubleValue()) return true;
            return false;

        case LESS_THAN:
            if (num < singleNum.doubleValue()) return true;
            return false;

        case RANGE:
            if (num >= rangeMin.doubleValue() && num <= rangeMax.doubleValue()) return true;
            return false;

        case LIST:
            for (IntMatcher child : this.children) {
                if (child.isMatch(num)) return true;
            }
            return false;

        default:
            logger.warn("Unknown num matcher type: " + this.type);
            return false;
        }
    }

    /**
     * Maintain cache of matchers.
     *
     * @param  value String to match.
     * @return         Return already defined matcher from cache.  If not found, create new matcher intsance and add to cache.
     */
    public static synchronized IntMatcher getMatcher(String value){
        IntMatcher matcher = MatcherCache.get(value);
        if(matcher == null){
            matcher = new IntMatcher(value);
            MatcherCache.put(value, matcher);
        }
        return matcher;
    }

    /**
     * Return string representation
     * 
     * @return The string
     */
    public String toString()
    {
        return matcher;
    }

    /**
     * Get a matcher that matches any
     * 
     * @return The matcher
     */
    public static synchronized IntMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }

    /**
     * Initialize all the private variables
     * 
     * @param matcher
     *        The init string
     */
    private void initialize(String matcher)
    {
        matcher = matcher.toLowerCase().trim().replaceAll("\\s", "");
        this.matcher = matcher;

        /**
         * If it contains a comma it must be a list of num matchers if so, go
         * ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR)) {
            this.type = IntMatcherType.LIST;

            this.children = new LinkedList<>();

            String[] results = matcher.split(MARKER_SEPERATOR);

            /* check each one */
            for (String childString : results) {
                IntMatcher child = new IntMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * Check the common constants
         */
        if (MARKER_ANY.equals(matcher)) {
            this.type = IntMatcherType.ANY;
            return;
        }
        if (MARKER_ALL.equals(matcher)) {
            this.type = IntMatcherType.ANY;
            return;
        }
        if (MARKER_NONE.equals(matcher)) {
            this.type = IntMatcherType.NONE;
            return;
        }

        /**
         * Check for > and <
         */
        if (matcher.contains(MARKER_GREATER_THAN)) {
            this.type = IntMatcherType.GREATER_THAN;

            int charIdx = matcher.indexOf('>');
            String numStr = matcher.substring(charIdx + 1);

            try {
                this.singleNum = Integer.parseInt(numStr);
                return;
            } catch (NumberFormatException e) {
                try {
                    this.singleNum = Double.parseDouble(numStr);
                    return;
                } catch (NumberFormatException e2) {
                    logger.warn("Unknown format: \"" + numStr + "\"", e);
                    throw new java.lang.IllegalArgumentException("Unknown format: \"" + matcher + "\"", e);
                }
            }
        }

        if (matcher.contains(MARKER_LESS_THAN)) {
            this.type = IntMatcherType.LESS_THAN;

            int charIdx = matcher.indexOf('<');
            String numStr = matcher.substring(charIdx + 1);
            try {
                this.singleNum = Integer.parseInt(numStr);
                return;
            } catch (NumberFormatException e) {
                try {
                    this.singleNum = Double.parseDouble(numStr);
                    return;
                } catch (NumberFormatException e2) {
                    logger.warn("Unknown format: \"" + numStr + "\"", e);
                    throw new java.lang.IllegalArgumentException("Unknown format: \"" + numStr + "\"", e);
                }
            }
        }

        /**
         * If it contains a dash it must be a range
         */
        if (matcher.contains(MARKER_RANGE)) {
            this.type = IntMatcherType.RANGE;

            String[] results = matcher.split(MARKER_RANGE);

            if (results.length != 2) {
                logger.warn("Invalid IntMatcher: Invalid Range: " + matcher);
                throw new java.lang.IllegalArgumentException("Invalid IntMatcher: Invalid Range: " + matcher);
            }

            try {
                this.rangeMin = Integer.parseInt(results[0]);
            } catch (NumberFormatException e) {
                try {
                    this.rangeMin = Double.parseDouble(results[0]);
                    return;
                } catch (NumberFormatException e2) {
                    logger.warn("Unknown format: \"" + results[0] + "\"", e);
                    throw new java.lang.IllegalArgumentException("Unknown format: \"" + results[0] + "\"", e);
                }
            }
            try {
                this.rangeMax = Integer.parseInt(results[1]);
            } catch (NumberFormatException e) {
                try {
                    this.rangeMax = Double.parseDouble(results[1]);
                    return;
                } catch (NumberFormatException e2) {
                    logger.warn("Unknown format: \"" + results[1] + "\"", e);
                    throw new java.lang.IllegalArgumentException("Unknown format: \"" + results[1] + "\"", e);
                }
            }

            return;
        }

        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = IntMatcherType.SINGLE;
        try {
            this.singleNum = Integer.parseInt(matcher);
            return;
        } catch (NumberFormatException e) {
            try {
                this.singleNum = Double.parseDouble(matcher);
                return;
            } catch (NumberFormatException e2) {
                logger.warn("Unknown format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown format: \"" + matcher + "\"", e);
            }
        }
    }
}
