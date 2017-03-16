/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.util.LinkedList;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.GlobUtil;

public class UserMatcher
{
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_UNAUTHENTICATED = "[unauthenticated]";
    private static final String MARKER_AUTHENTICATED = "[authenticated]";

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This stores the string representation of this matcher
     */
    public String matcher;

    /**
     * This is all the available types of user matchers
     */
    private enum UserMatcherType { SINGLE, AUTHENTICATED, UNAUTHENTICATED, LIST };

    /**
     * The type of this matcher
     */
    private UserMatcherType type;
    
    /**
     * This stores the username if this is a single matcher
     */
    private String single = null;
    private String regexValue = null;

    /**
     * if this user matcher is a list of user matchers, this list stores the children
     */
    private LinkedList<UserMatcher> children = null;
    
    /**
     * Create a user matcher from the given string
     */
    public UserMatcher( String matcher )
    {
        initialize(matcher);
    }
    
    public boolean isMatch( String value )
    {
        switch (this.type) {

        case SINGLE:
            if (value == null) {
                /* "" matches null user */
                if ("".equals( single ))
                    return true;
                return false;
            }
            if (value.equalsIgnoreCase(this.single))
                return true;
            if (Pattern.matches(this.regexValue, value.toLowerCase()))
                return true;
            return false;
            
        case AUTHENTICATED:
            return (value != null && !"".equals(value)); 

        case UNAUTHENTICATED:
            return ("".equals(value) || value == null); 

        case LIST:
            for (UserMatcher child : this.children) {
                if (child.isMatch(value))
                    return true;
            }
            return false;

        default:
            logger.warn("Unknown user matcher type: " + this.type);
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
         * If it contains a comma it must be a list of user matchers
         * if so, go ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR)) {
            this.type = UserMatcherType.LIST;

            this.children = new LinkedList<UserMatcher>();

            String[] results = matcher.split(MARKER_SEPERATOR);
            
            /* check each one */
            for (String childString : results) {
                UserMatcher child = new UserMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * Check the common constants
         */

        if (MARKER_AUTHENTICATED.equals(matcher)) {
            this.type = UserMatcherType.AUTHENTICATED;
            return;
        }
        if (MARKER_UNAUTHENTICATED.equals(matcher)) {
            this.type = UserMatcherType.UNAUTHENTICATED;
            return;
        }

        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = UserMatcherType.SINGLE;
        this.single = matcher;
        this.regexValue = GlobUtil.globToRegex(matcher);

        return;
    }

}
