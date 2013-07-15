/**
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/uvm/node/IntMatcher.java $
 */
package com.untangle.uvm.node;

import java.util.LinkedList;

import org.apache.log4j.Logger;

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
     
    private final Logger logger = Logger.getLogger(getClass());

    
    public String matcher;

    private enum IntMatcherType { ANY, NONE, SINGLE, GREATER_THAN, LESS_THAN, RANGE, LIST };
    
    /**
     * The type of this matcher
     */
    private IntMatcherType type = IntMatcherType.NONE;

    /**
     * if this port matcher is a list of port matchers, this list stores the children
     */
    private LinkedList<IntMatcher> children = null;

    /**
     * if its a range these two variable store the min and max
     */
    private long rangeMin = -1;
    private long rangeMax = -1;

    /**
     * if its just an int matcher this stores the number
     */
    private long singleInt = -1;


    public IntMatcher(String matcher)
    {
        initialize(matcher);
    }

    public String getMatcher()
    {
        return this.matcher;
    }

    public void setMatcher( String matcher )
    {
        initialize(matcher);
    }
    
    /**
     * Return true if <param>port</param> matches this matcher.
     *
     * @param port The port to test
     * @return True if the <param>port</param> matches.
     */
    public boolean isMatch( long port )
    {
        switch (this.type) {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            if (singleInt == port)
                return true;
            return false;

        case GREATER_THAN:
            if ( port > singleInt )
                return true;
            return false;

        case LESS_THAN:
            if ( port < singleInt )
                return true;
            return false;
            
        case RANGE:
            if (port >= rangeMin && port <= rangeMax)
                return true;
            return false;

        case LIST:
            for (IntMatcher child : this.children) {
                if (child.isMatch(port))
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
        return matcher;
    }

    public static synchronized IntMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }
    
    /**
     * Initialize all the private variables
     */
    private void initialize( String matcher )
    {
        matcher = matcher.toLowerCase().trim().replaceAll("\\s","");
        this.matcher = matcher;

        /**
         * If it contains a comma it must be a list of port matchers
         * if so, go ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR)) {
            this.type = IntMatcherType.LIST;

            this.children = new LinkedList<IntMatcher>();

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
        if (MARKER_ANY.equals(matcher))  {
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
            String intStr = matcher.substring( charIdx + 1 );
            
            try {
                this.singleInt = Integer.parseInt( intStr );
            } catch (NumberFormatException e) {
                logger.warn("Unknown IntMatcher format: \"" + intStr + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IntMatcher format: \"" + matcher + "\"", e);
            }
            return;
        }
        if (matcher.contains(MARKER_LESS_THAN)) {
            this.type = IntMatcherType.LESS_THAN;

            int charIdx = matcher.indexOf('<');
            String intStr = matcher.substring( charIdx + 1 );
            try {
                this.singleInt = Integer.parseInt( intStr );
            } catch (NumberFormatException e) {
                logger.warn("Unknown IntMatcher format: \"" + intStr + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IntMatcher format: \"" + matcher + "\"", e);
            }
            return;
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
                this.rangeMax = Integer.parseInt(results[1]);
            } catch (NumberFormatException e) {
                logger.warn("Unknown IntMatcher format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IntMatcher format: \"" + matcher + "\"", e);
            }

            return;
        }
            
        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = IntMatcherType.SINGLE;
        try {
            this.singleInt = Integer.parseInt(matcher);
        } catch (NumberFormatException e) {
            logger.warn("Unknown IntMatcher format: \"" + matcher + "\"", e);
            throw new java.lang.IllegalArgumentException("Unknown IntMatcher format: \"" + matcher + "\"", e);

        }

        return;
    }
    
}
