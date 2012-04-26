/* $HeadURL$ */
package com.untangle.uvm.node;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.Protocol;

/**
 * An matcher for protcols
 *
 * Examples:
 * "any"
 * "TCP"
 * "TCP,UDP"
 *
 * ProtocolMatcher it is case insensitive
 *
 * @author <a href="mailto:dmorris@untangle.com">Dirk Morris</a>
 */
public class ProtocolMatcher
{
    private static final String MARKER_ANY = "any";
    private static final String MARKER_ALL = "all";
    private static final String MARKER_NONE = "none";
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_SEPERATOR2 = "&"; 
    private static final String MARKER_TCP = "tcp";
    private static final String MARKER_UDP = "udp";

    private static ProtocolMatcher ANY_MATCHER = new ProtocolMatcher(MARKER_ANY);
    private static ProtocolMatcher TCP_MATCHER = new ProtocolMatcher(MARKER_TCP);
    private static ProtocolMatcher UDP_MATCHER = new ProtocolMatcher(MARKER_UDP);

    private final Logger logger = Logger.getLogger(getClass());
    
    private enum ProtocolMatcherType { ANY, NONE, SINGLE, LIST };
    
    /**
     * The type of this matcher
     */
    private ProtocolMatcherType type = ProtocolMatcherType.NONE;

    /**
     * This stores the string representation of this matcher
     */
    private String matcher;

    /**
     * If this matcher is a single matcher this stores the protocol to match
     */
    private int single;
    
    /**
     * if this port matcher is a list of port matchers, this list stores the children
     */
    private LinkedList<ProtocolMatcher> children = null;

    
    /**
     * Create a protocol matcher from the given string
     */
    public ProtocolMatcher( String matcher )
    {
        initialize(matcher);
    }
    
    /**
     * Return true if <param>protocol</param> matches this matcher.
     *
     * @param protocol The protocol to test
     * @return True if the <param>protocol</param> matches.
     */
    public boolean isMatch( Protocol protocol )
    {
        return isMatch(protocol.getId());
    }

    /**
     * Return true if <param>protocol</param> matches this matcher.
     *
     * @param protocol The protocol to test
     * @return True if the <param>protocol</param> matches.
     */
    public boolean isMatch( int protocol )
    {
       switch (this.type) {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            if (this.single == protocol)
                return true;
            return false;

        case LIST:
            for (ProtocolMatcher child : this.children) {
                if (child.isMatch(protocol))
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

    public static ProtocolMatcher getTCPAndUDPMatcher()
    {
        return ANY_MATCHER;
    }
    
    public static ProtocolMatcher getTCPMatcher()
    {
        return TCP_MATCHER;
    }

    public static ProtocolMatcher getUDPMatcher()
    {
        return UDP_MATCHER;
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
            this.type = ProtocolMatcherType.LIST;

            this.children = new LinkedList<ProtocolMatcher>();

            String[] results = matcher.split(MARKER_SEPERATOR);
            
            /* check each one */
            for (String childString : results) {
                ProtocolMatcher child = new ProtocolMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * If it contains a ampersand it must be a list of protocol matchers
         * if so, go ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR2)) {
            this.type = ProtocolMatcherType.LIST;

            this.children = new LinkedList<ProtocolMatcher>();

            String[] results = matcher.split(MARKER_SEPERATOR2);
            
            /* check each one */
            for (String childString : results) {
                ProtocolMatcher child = new ProtocolMatcher(childString);
                this.children.add(child);
            }

            return;
        }
        
        /**
         * Check the common constants
         */
        if (MARKER_ANY.equals(matcher))  {
            this.type = ProtocolMatcherType.ANY;
            return;
        }
        if (MARKER_ALL.equals(matcher)) {
            this.type = ProtocolMatcherType.ANY;
            return;
        }
        if (MARKER_NONE.equals(matcher)) {
            this.type = ProtocolMatcherType.NONE;
            return;
        }
        if (MARKER_TCP.equals(matcher)) {
            this.type = ProtocolMatcherType.SINGLE;
            this.single = Protocol.TCP.getId();
            return;
        }
        if (MARKER_UDP.equals(matcher)) {
            this.type = ProtocolMatcherType.SINGLE;
            this.single = Protocol.UDP.getId();
            return;
        }

        logger.error("Invalid Protocol matcher: \"" + matcher + "\"");
        throw new IllegalArgumentException("Invalid Protocol matcher: \"" + matcher + "\"");
    }

}
