/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MimeTypeRule.java 229 2005-04-07 22:25:00Z amread $
 */

package com.metavize.tran.firewall;

import com.metavize.mvvm.tran.ParseException;

import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.TrafficRule;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="FIREWALL_RULE"
 */
public class FirewallRule extends TrafficRule
{
    /* XXX The varchar probably should just be chars */

    private static final long serialVersionUID = 1886689190345445284L;
    
    private static final String ACTION_BLOCK     = "Block";
    private static final String ACTION_BLOCK_LOG = "Block & Log";
    private static final String ACTION_PASS      = "Pass";
    private static final String ACTION_PASS_LOG  = "Pass & Log";
    
    private static final String[] ACTION_ENUMERATION = { ACTION_BLOCK, ACTION_BLOCK_LOG,
                                                         ACTION_PASS,  ACTION_PASS_LOG };
    
    private boolean isTrafficBlocker;
    
    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public FirewallRule() { }

    public FirewallRule( boolean     isLive,     ProtocolMatcher protocol, 
                         IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                         IPMatcher   srcAddress, IPMatcher       dstAddress,
                         PortMatcher srcPort,    PortMatcher     dstPort,
                         boolean isTrafficBlocker )
    {
        super( isLive, protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );
        
        /* Attributes of the firewall */
        this.isTrafficBlocker = isTrafficBlocker;
    }


    // accessors --------------------------------------------------------------

    /**
     * Does this rule block traffic or let it pass.
     *
     * @return if this rule blocks traffic.
     * @hibernate.property
     * column="IS_TRAFFIC_BLOCKER"
     */
    public boolean isTrafficBlocker()
    {
        return isTrafficBlocker;
    }

    public void setTrafficBlocker( boolean isTrafficBlocker )
    {
        this.isTrafficBlocker = isTrafficBlocker;
    }

    public  String getAction()
    {
        if ( isTrafficBlocker ) {
            return ( getLog()) ? ACTION_BLOCK_LOG : ACTION_BLOCK;
        }

        return ( getLog()) ? ACTION_PASS_LOG : ACTION_PASS;
    }
    
    public  void setAction( String action ) throws ParseException
    {
        if ( action.equalsIgnoreCase( ACTION_BLOCK )) {
            isTrafficBlocker = true;
            setLog( false );
        } else if ( action.equalsIgnoreCase( ACTION_BLOCK_LOG )) {
            isTrafficBlocker = true;
            setLog( true );
        } else if ( action.equalsIgnoreCase( ACTION_PASS )) {
            isTrafficBlocker = false;
            setLog( false );
        } else if ( action.equalsIgnoreCase( ACTION_PASS_LOG )) {
            isTrafficBlocker = false;
            setLog( true );
        } else {
            throw new ParseException( "Invalid action: " + action );
        }
    }

    public static String[] getActionEnumeration()
    {
        return ACTION_ENUMERATION;
    }

    public static String getActionDefault()
    {
        return ACTION_ENUMERATION[0];
    }
}
