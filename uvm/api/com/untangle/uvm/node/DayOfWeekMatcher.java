/* $HeadURL$ */
package com.untangle.uvm.node;

import java.util.LinkedList;
import java.util.Date;
import java.util.Calendar;

import org.apache.log4j.Logger;

public class DayOfWeekMatcher
{
    private static DayOfWeekMatcher ANY_MATCHER = new DayOfWeekMatcher("any");

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


    public DayOfWeekMatcher( String matcher )
    {
        initialize(matcher);
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
                return (this.single.equals("monday"));
            case Calendar.TUESDAY:
                return (this.single.equals("tuesday"));
            case Calendar.WEDNESDAY:
                return (this.single.equals("wednesday"));
            case Calendar.THURSDAY:
                return (this.single.equals("thursday"));
            case Calendar.FRIDAY:
                return (this.single.equals("friday"));
            case Calendar.SATURDAY:
                return (this.single.equals("saturday"));
            case Calendar.SUNDAY:
                return (this.single.equals("sunday"));
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

    public String toDatabaseString()
    {
        return this.matcher;
    }

    public String toString()
    {
        return toDatabaseString();
    }

    public static DayOfWeekMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }
    
    private void initialize( String matcher )
    {
        this.matcher = matcher.toLowerCase();

        /**
         * If it contains a comma it must be a list of protocol matchers
         * if so, go ahead and initialize the children
         */
        if (matcher.contains(",")) {
            this.type = DayOfWeekMatcherType.LIST;

            this.children = new LinkedList<DayOfWeekMatcher>();

            String[] results = matcher.split(",");
            
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
        if ("any".equals(matcher))  {
            this.type = DayOfWeekMatcherType.ANY;
            return;
        }
        if ("all".equals(matcher)) {
            this.type = DayOfWeekMatcherType.ANY;
            return;
        }
        if ("none".equals(matcher)) {
            this.type = DayOfWeekMatcherType.NONE;
            return;
        }
        if ("sunday".equals(matcher) ||
            "monday".equals(matcher) ||
            "tuesday".equals(matcher) ||
            "wednesday".equals(matcher) ||
            "thursday".equals(matcher) ||
            "friday".equals(matcher) ||
            "saturday".equals(matcher)) {
            this.type = DayOfWeekMatcherType.SINGLE;
            this.single = matcher;
            return;
        }
    }
}
