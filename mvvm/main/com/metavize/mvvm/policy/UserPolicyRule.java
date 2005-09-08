/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.policy;

import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import java.io.Serializable;
import java.util.*;

/**
 * User Policy Rules.  These are the policy rules that are created by the user.  All of
 * these run before any of the System policy rules.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="USER_POLICY_RULE"
 */
public class UserPolicyRule extends PolicyRule
{
    /* settings */
    private ProtocolMatcher protocol;

    private IPMatcher   clientAddr;
    private IPMatcher   serverAddr;

    private PortMatcher clientPort;
    private PortMatcher serverPort;

    // constructors -----------------------------------------------------------

    public UserPolicyRule() { }

    public UserPolicyRule(byte clientIntf, byte serverIntf, Policy policy, boolean inbound,
                          ProtocolMatcher protocol, IPMatcher clientAddr, IPMatcher serverAddr,
                          PortMatcher clientPort, PortMatcher serverPort) {
        super(clientIntf, serverIntf, policy, inbound);
        this.protocol = protocol;
        this.clientAddr = clientAddr;
        this.serverAddr = serverAddr;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
    }


    // accessors -----------------------------------------------------------

    /* Hack that sets the ports to zero for Ping sessions */
    public void fixPing() throws ParseException
    {
        if ( this.protocol.equals( ProtocolMatcher.MATCHER_PING )) {
            this.clientPort = PortMatcher.MATCHER_PING;
            this.serverPort = PortMatcher.MATCHER_PING;
        } else if ( this.clientPort.equals( PortMatcher.MATCHER_PING ) || 
                    this.serverPort.equals( PortMatcher.MATCHER_PING )) {
            throw new ParseException( "Invalid port for a non-ping traffic type" );
        }
    }

    /**
     * Protocol matcher
     *
     * @return the protocol matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.ProtocolMatcherUserType"
     * @hibernate.column
     * name="PROTOCOL_MATCHER"
     */
    public ProtocolMatcher getProtocol()
    {
        return protocol;
    }

    public void setProtocol( ProtocolMatcher protocol )
    {
        this.protocol = protocol;
    }
        
    /**
     * client address IPMatcher
     *
     * @return the client address IP matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IPMatcherUserType"
     * @hibernate.column
     * name="CLIENT_IP_MATCHER"
     */
    public IPMatcher getClientAddr()
    {
        return clientAddr;
    }

    public void setClientAddr(IPMatcher clientAddr)
    {
        this.clientAddr = clientAddr;
    }

    /**
     * server address IPMatcher
     *
     * @return the server address IP matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IPMatcherUserType"
     * @hibernate.column
     * name="SERVER_IP_MATCHER"
     */
    public IPMatcher getServerAddr()
    {
        return serverAddr;
    }

    public void setServerAddr(IPMatcher serverAddr)
    {
        this.serverAddr = serverAddr;
    }
    
    /**
     * client port PortMatcher
     *
     * @return the client port matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.PortMatcherUserType"
     * @hibernate.column
     * name="CLIENT_PORT_MATCHER"
     */
    public PortMatcher getClientPort()
    {
        return clientPort;
    }

    public void setClientPort(PortMatcher clientPort)
    {
        this.clientPort = clientPort;
    }

    /**
     * server port PortMatcher
     *
     * @return the server port matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.PortMatcherUserType"
     * @hibernate.column
     * name="SERVER_PORT_MATCHER"
     */
    public PortMatcher getServerPort()
    {
        return serverPort;
    }

    public void setServerPort(PortMatcher serverPort)
    {
        this.serverPort = serverPort;
    }
}
