/* $HeadURL$ */
package com.untangle.uvm.node.firewall.intf;

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
public class IntfMatcher 
{
    private final Logger logger = Logger.getLogger(getClass());

    public String matcher;

    public IntfMatcher(String matcher)
    {
        this.matcher = matcher;
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
        if ("any".equals(matcher))
            return true;
        if ("all".equals(matcher))
            return true;
        if ("none".equals(matcher))
            return false;
        if ("wan".equals(matcher))
            return intfConf.isWAN();
        if ("non_wan".equals(matcher))
            return !intfConf.isWAN();

        int index = intfConf.getInterfaceId();
        
        if (matcher.contains(",")) {
            /* must be a comma separated list */
            String[] results = matcher.split(",");

            /* check each one */
            for (String intString : results) {
                try {
                    if (index == Integer.parseInt(intString))
                        return true;
                } catch (NumberFormatException e) {
                    logger.warn("Unknown interface format: \"" + matcher + "\" specifically: \"" + intString + "\"", e);
                }
            }

            return false; /* didn't match any of the above */
        }

        /* if it isn't of the above it must just be any integer */
        try {
            if (index == Integer.parseInt(matcher))
                return true;
        } catch (NumberFormatException e) {
            logger.warn("Unknown interface format: " + matcher, e);
        }

        /* If it didn't match anything at this point it doesn't match */
        return false;
    }
    

    /**
     * Retrieve the database representation of this interface matcher.
     * "1" matches interface 1
     * "1,2" matches 1 OR 2
     * "any" matches any interface
     * "all" matches any interface
     * "wan" matches any wan interface
     * "non_wan" matches any non_wan interface
     * "none" matches nothing
     *
     *
     * @return The database representation of this interface matcher.
     */
    public String toDatabaseString()
    {
        return matcher;
    }

    public String toString()
    {
        return matcher;
    }

}
