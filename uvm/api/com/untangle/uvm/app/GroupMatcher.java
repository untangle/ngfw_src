/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.util.LinkedList;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.GlobUtil;

public class GroupMatcher
{
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_ANY = "[any]";
    private static final String MARKER_NONE = "[none]";

    private static GroupMatcher ANY_MATCHER = new GroupMatcher(MARKER_ANY);
    
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This stores the string representation of this matcher
     */
    public String matcher;

    /**
     * This is all the available types of group matchers
     */
    private enum GroupMatcherType { ANY, NONE, SINGLE, LIST };

    /**
     * The type of this matcher
     */
    private GroupMatcherType type = GroupMatcherType.NONE;
    
    /**
     * This stores the groupname if this is a single matcher
     */
    public String single = null;
    private String regexValue = null;

    /**
     * if this port matcher is a list of port matchers, this list stores the children
     */
    private LinkedList<GroupMatcher> children = null;
    
    /**
     * Create a group matcher from the given string
     */
    public GroupMatcher( String matcher )
    {
        initialize(matcher);
    }
    
    public boolean isMatch( String value )
    {
        switch (this.type) {

        case ANY:
            return true;

        case NONE:
            return false;
            
        case SINGLE:
            if (value == null)
                return false;
            if (value.equalsIgnoreCase(this.single))
                return true;
            if (Pattern.matches(this.regexValue, value))
                return true;
            return false;
            
        case LIST:
            for (GroupMatcher child : this.children) {
                if (child.isMatch(value))
                    return true;
            }
            return false;

        default:
            logger.warn("Unknown port matcher type: " + this.type);
            return false;
            
        }
    }

    /**
     * return string representation
     */
    public String toString()
    {
        return this.matcher;
    }
    
    public static synchronized GroupMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }

    /**
     * Initialize all the private variables
     */
    private void initialize( String matcher )
    {
        // We used to ';' as a seperator, we now use ','
        matcher = matcher.replaceAll(";",",");
        // only lower case
        matcher = matcher.toLowerCase().trim();
        this.matcher = matcher;

        /**
         * If it contains a comma it must be a list of port matchers
         * if so, go ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR)) {
            this.type = GroupMatcherType.LIST;

            this.children = new LinkedList<GroupMatcher>();

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
        if (MARKER_ANY.equals(matcher))  {
            this.type = GroupMatcherType.ANY;
            return;
        }
        if (MARKER_NONE.equals(matcher))  {
            this.type = GroupMatcherType.ANY;
            return;
        }

        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = GroupMatcherType.SINGLE;
        this.single = matcher;
        this.regexValue = GlobUtil.globToRegex(matcher);

        return;
    }

}
