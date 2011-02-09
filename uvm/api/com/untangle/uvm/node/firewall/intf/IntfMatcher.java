/* $HeadURL$ */
package com.untangle.uvm.node.firewall.intf;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.networking.InterfaceConfiguration;
import com.untangle.uvm.networking.NetworkConfiguration;
import com.untangle.uvm.RemoteUvmContextFactory;
import com.untangle.uvm.RemoteUvmContext;


/**
 * An interface to test for particular interfaces.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class IntfMatcher implements java.io.Serializable
{
    private static final IntfMatcher ANY_MATCHER = new IntfMatcher("any");
    private static final IntfMatcher NONE_MATCHER = new IntfMatcher("none");
    private static final IntfMatcher WAN_MATCHER = new IntfMatcher("wan");
    private static final IntfMatcher NONWAN_MATCHER = new IntfMatcher("non_wan");

    private final Logger logger = Logger.getLogger(getClass());
    
    public String matcher;

    private enum IntfMatcherType { ANY, NONE, ANY_WAN, ANY_NON_WAN, SINGLE, LIST };
    
    /**
     * The type of this matcher
     */
    private IntfMatcherType type = IntfMatcherType.NONE;

    /**
     * if this intf matcher is a list of intf matchers, this list stores the children
     */
    private LinkedList<IntfMatcher> children = null;

    /**
     * If this intf matcher is a single this store the single interface ID
     */
    private int singleInt = -1;
        
    public IntfMatcher(String matcher)
    {
        initialize(matcher);
    }
    
    /**
     * Return true if <param>interfaceId</param> matches this matcher.
     *
     * @param intf The interface to test
     * @return True if the <param>interfaceId</param> matches.
     */
    public boolean isMatch(int interfaceId)
    {
        NetworkConfiguration netConf = RemoteUvmContextFactory.context().networkManager().getNetworkConfiguration();
        
        if (netConf == null) {
            logger.warn("Failed to match interface: null network configuration");
            return false;
        }

        InterfaceConfiguration intfConf = netConf.findById(interfaceId);

        if (intfConf == null) {
            logger.warn("Failed to match interface: Cant find interface " + interfaceId);
            return false;
        }

        return isMatch(intfConf);
    }

    /**
     * Return true if <param>interfaceId</param> matches this matcher.
     *
     * @param intf The interface to test
     * @return True if the <param>intf</param> matches.
     */
    public boolean isMatch(InterfaceConfiguration intfConf)
    {
        switch (this.type) {

        case ANY:
            return true;

        case NONE:
            return false;

        case ANY_WAN:
            return intfConf.isWAN();

        case ANY_NON_WAN:
            return !intfConf.isWAN();
            
        case SINGLE:
            if (singleInt == intfConf.getInterfaceId())
                return true;
            return false;

        case LIST:
            for (IntfMatcher child : this.children) {
                if (child.isMatch(intfConf))
                    return true;
            }
            return false;

        default:
            logger.warn("Unknown port matcher type: " + this.type);
            return false;
        }
    }
    

    /**
     * Retrieve the database representation of this interface matcher.
     *
     * @return The database representation of this interface matcher.
     */
    public String toDatabaseString()
    {
        return matcher;
    }

    /**
     * return toDatabaseString()
     */
    public String toString()
    {
        return toDatabaseString();
    }

    public static IntfMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }
    
    public static IntfMatcher getNilMatcher()
    {
        return NONE_MATCHER;
    }

    public static IntfMatcher getWanMatcher()
    {
        return WAN_MATCHER;
    }

    public static IntfMatcher getNonWanMatcher()
    {
        return NONWAN_MATCHER;
    }

    private void initialize( String matcher )
    {
        this.matcher = matcher;

        /**
         * if it contains a comma it must be a list
         */
        if (matcher.contains(",")) {
            this.type = IntfMatcherType.LIST;

            this.children = new LinkedList<IntfMatcher>();

            String[] results = matcher.split(",");
            
            /* check each one */
            for (String childString : results) {
                IntfMatcher child = new IntfMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * check the common constants
         */
        if ("any".equals(matcher)) {
            this.type = IntfMatcherType.ANY;
            return;
        }
        if ("all".equals(matcher)) {
            this.type = IntfMatcherType.ANY;
            return;
        }
        if ("none".equals(matcher)) {
            this.type = IntfMatcherType.NONE;
            return;
        }
        if ("wan".equals(matcher)) {
            this.type = IntfMatcherType.ANY_WAN;
            return;
        }
        if ("non_wan".equals(matcher)) {
            this.type = IntfMatcherType.ANY_NON_WAN;
            return;
        }

        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = IntfMatcherType.SINGLE;
        try {
            this.singleInt = Integer.parseInt(matcher);
        } catch (NumberFormatException e) {
            logger.warn("Unknown IntfMatcher format: \"" + matcher + "\"", e);
            throw new java.lang.IllegalArgumentException("Unknown IntfMatcher format: \"" + matcher + "\"", e);

        }

        return;
    }        
}
