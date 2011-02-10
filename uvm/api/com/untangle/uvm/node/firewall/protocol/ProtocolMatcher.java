/* $HeadURL$ */
package com.untangle.uvm.node.firewall.protocol;

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
 * @version 1.0
 */
public class ProtocolMatcher
{
    private static ProtocolMatcher ANY_MATCHER = new ProtocolMatcher("any");
    private static ProtocolMatcher TCP_MATCHER = new ProtocolMatcher("tcp");
    private static ProtocolMatcher UDP_MATCHER = new ProtocolMatcher("udp");

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
    
    /**
     * Retrieve the database representation of this protocol matcher.
     *
     * @return The database representation of this protocol matcher.
     */
    public String toDatabaseString()
    {
        return this.matcher;
    }

    public String toString()
    {
        return toDatabaseString();
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
        this.matcher = matcher.toLowerCase();

        /**
         * If it contains a comma it must be a list of protocol matchers
         * if so, go ahead and initialize the children
         */
        if (matcher.contains(",")) {
            this.type = ProtocolMatcherType.LIST;

            this.children = new LinkedList<ProtocolMatcher>();

            String[] results = matcher.split(",");
            
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
        if ("any".equals(matcher))  {
            this.type = ProtocolMatcherType.ANY;
            return;
        }
        if ("all".equals(matcher)) {
            this.type = ProtocolMatcherType.ANY;
            return;
        }
        if ("none".equals(matcher)) {
            this.type = ProtocolMatcherType.NONE;
            return;
        }
        if ("tcp".equals(matcher)) {
            this.type = ProtocolMatcherType.SINGLE;
            this.single = Protocol.TCP.getId();
            return;
        }
        if ("udp".equals(matcher)) {
            this.type = ProtocolMatcherType.SINGLE;
            this.single = Protocol.UDP.getId();
            return;
        }
    }

}
