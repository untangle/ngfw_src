/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.util.LinkedList;
import java.util.Date;
import java.util.Calendar;

import org.apache.log4j.Logger;

/**
 * An matcher for days of the week
 *
 * Examples:
 * "any"
 * "Monday"
 * "Monday,Tuesday"
 *
 * DayOfWeekMatcher it is case insensitive
 *
 * @author <a href="mailto:dmorris@untangle.com">Dirk Morris</a>
 */
public class DayOfWeekMatcher
{
    private static final String MARKER_ANY = "any";
    private static final String MARKER_ALL = "all";
    private static final String MARKER_NONE = "none";
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_MONDAY    = "monday";
    private static final String MARKER_TUESDAY   = "tuesday";
    private static final String MARKER_WEDNESDAY = "wednesday";
    private static final String MARKER_THURSDAY  = "thursday";
    private static final String MARKER_FRIDAY    = "friday";
    private static final String MARKER_SATURDAY  = "saturday";
    private static final String MARKER_SUNDAY    = "sunday";

    private static DayOfWeekMatcher ANY_MATCHER = new DayOfWeekMatcher(MARKER_ANY);

    private final Logger logger = Logger.getLogger(getClass());

    private enum DayOfWeekMatcherType { ANY, NONE, SINGLE, LIST };

    /**
     * The type of this matcher
     */
    private DayOfWeekMatcherType type = DayOfWeekMatcherType.NONE;

    /**
     * This stores the string representation of this matcher
     */
    private String matcher;

    /**
     * This stores the string of this representation of this single matcher
     */
    private String single;
    
    /**
     * if this port matcher is a list of port matchers, this list stores the children
     */
    private LinkedList<DayOfWeekMatcher> children = null;
    
    /**
     * Construct a day of week matcher from the given string
     */
    public DayOfWeekMatcher( String matcher )
    {
        initialize(matcher);
    }

    /**
     * returns isMatch(now())
     */
    public boolean isMatch()
    {
        return isMatch(new Date());
    }
    
    public boolean isMatch( Date when )
    {
       switch (this.type) {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            Calendar cal = Calendar.getInstance();
            cal.setTime(when);
            int calDay = cal.get(Calendar.DAY_OF_WEEK);
            switch (calDay) {
            case Calendar.MONDAY:
                return (this.single.equals(MARKER_MONDAY));
            case Calendar.TUESDAY:
                return (this.single.equals(MARKER_TUESDAY));
            case Calendar.WEDNESDAY:
                return (this.single.equals(MARKER_WEDNESDAY));
            case Calendar.THURSDAY:
                return (this.single.equals(MARKER_THURSDAY));
            case Calendar.FRIDAY:
                return (this.single.equals(MARKER_FRIDAY));
            case Calendar.SATURDAY:
                return (this.single.equals(MARKER_SATURDAY));
            case Calendar.SUNDAY:
                return (this.single.equals(MARKER_SUNDAY));
            default:
                return false;
            }

        case LIST:
            for (DayOfWeekMatcher child : this.children) {
                if (child.isMatch(when))
                    return true;
            }
            return false;

        default:
            logger.warn("Unknown port matcher type: " + this.type);
            return false;
        }
    }

    public String toString()
    {
        return this.matcher;
    }

    public static DayOfWeekMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }
    
    private void initialize( String matcher )
    {
        matcher = matcher.toLowerCase().trim().replaceAll("\\s","");
        this.matcher = matcher;

        /**
         * If it contains a comma it must be a list of protocol matchers
         * if so, go ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR)) {
            this.type = DayOfWeekMatcherType.LIST;

            this.children = new LinkedList<DayOfWeekMatcher>();

            String[] results = matcher.split(MARKER_SEPERATOR);
            
            /* check each one */
            for (String childString : results) {
                DayOfWeekMatcher child = new DayOfWeekMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * Check the common constants
         */
        if (MARKER_ANY.equals(matcher))  {
            this.type = DayOfWeekMatcherType.ANY;
            return;
        }
        if (MARKER_ALL.equals(matcher)) {
            this.type = DayOfWeekMatcherType.ANY;
            return;
        }
        if (MARKER_NONE.equals(matcher)) {
            this.type = DayOfWeekMatcherType.NONE;
            return;
        }
        if (MARKER_SUNDAY.equals(matcher) || MARKER_MONDAY.equals(matcher) ||
            MARKER_TUESDAY.equals(matcher) || MARKER_WEDNESDAY.equals(matcher) ||
            MARKER_THURSDAY.equals(matcher) || MARKER_FRIDAY.equals(matcher) ||
            MARKER_SATURDAY.equals(matcher)) {
            this.type = DayOfWeekMatcherType.SINGLE;
            this.single = matcher;
            return;
        }
    }
}
