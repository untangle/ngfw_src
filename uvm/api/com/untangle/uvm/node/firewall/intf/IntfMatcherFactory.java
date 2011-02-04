/* $HeadURL$ */
package com.untangle.uvm.node.firewall.intf;

import com.untangle.uvm.networking.InterfaceConfiguration;
import com.untangle.uvm.networking.NetworkConfiguration;
import com.untangle.uvm.RemoteUvmContextFactory;
import com.untangle.uvm.RemoteUvmContext;

/**
 * A factory for interface matchers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class IntfMatcherFactory
{
    private static final IntfMatcherFactory INSTANCE = new IntfMatcherFactory();

    private static final IntfMatcher ANY_MATCHER = new IntfMatcher("any");
    private static final IntfMatcher NONE_MATCHER = new IntfMatcher("none");
    private static final IntfMatcher WAN_MATCHER = new IntfMatcher("wan");
    private static final IntfMatcher NONWAN_MATCHER = new IntfMatcher("non_wan");
    
    private IntfMatcherFactory() {}

    public IntfMatcher getAnyMatcher()
    {
        return this.ANY_MATCHER;
    }
    
    public IntfMatcher getNilMatcher()
    {
        return this.NONE_MATCHER;
    }

    public IntfMatcher getWanMatcher()
    {
        return this.WAN_MATCHER;
    }

    public IntfMatcher getNonWanMatcher()
    {
        return this.NONWAN_MATCHER;
    }

    public static IntfMatcher parse(String value) 
    {
        return new IntfMatcher(value);
    }
    
    public static IntfMatcherFactory getInstance()
    {
        return INSTANCE;
    }
}
