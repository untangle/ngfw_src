/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.policy;

import java.util.*;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.metavize.mvvm.api.IPSessionDesc;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.ip.IPMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcherFactory;
import org.hibernate.annotations.Type;

/**
 * User Policy Rules.  These are the policy rules that are created by
 * the user.  All of these run before any of the System policy rules.
 *
 * @author
 * @version 1.0
 */
@Entity
@Table(name="user_policy_rule", schema="settings")
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

    public UserPolicyRule(byte clientIntf, byte serverIntf, Policy policy,
                          boolean inbound, ProtocolMatcher protocol,
                          IPMatcher clientAddr, IPMatcher serverAddr,
                          PortMatcher clientPort, PortMatcher serverPort) {
        super(clientIntf, serverIntf, policy, inbound);
        this.protocol = protocol;
        this.clientAddr = clientAddr;
        this.serverAddr = serverAddr;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
    }

    // PolicyRule methods -----------------------------------------------------

    public boolean matches(IPSessionDesc sd)
    {
        return clientIntf == sd.clientIntf()
            && serverIntf == sd.serverIntf()
            && clientAddr.isMatch(sd.clientAddr())
            && serverAddr.isMatch(sd.serverAddr())
            && clientPort.isMatch(sd.clientPort())
            && serverPort.isMatch(sd.serverPort());
    }

    // accessors --------------------------------------------------------------

    /* Hack that sets the ports to zero for Ping sessions */
    public void fixPing() throws ParseException
    {
        PortMatcher pingMatcher = PortMatcherFactory.getInstance().getPingMatcher();

        if ( this.protocol.equals( ProtocolMatcher.MATCHER_PING )) {
            this.clientPort = pingMatcher;
            this.serverPort = pingMatcher;
        } else if ( this.clientPort.equals( pingMatcher ) || this.serverPort.equals( pingMatcher )) {
            throw new ParseException( "Invalid port for a non-ping traffic type" );
        }
    }

    /**
     * Protocol matcher
     *
     * @return the protocol matcher.
     */
    @Column(name="protocol_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.ProtocolMatcherUserType")
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
     */
    @Column(name="client_ip_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.IPMatcherUserType")
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
     */
    @Column(name="server_ip_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.IPMatcherUserType")
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
     */
    @Column(name="client_port_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.PortMatcherUserType")
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
     */
    @Column(name="server_port_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.PortMatcherUserType")
    public PortMatcher getServerPort()
    {
        return serverPort;
    }

    public void setServerPort(PortMatcher serverPort)
    {
        this.serverPort = serverPort;
    }

    @Transient
    public boolean isSameRow(UserPolicyRule pr)
    {
        return getId().equals(pr.getId());
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof UserPolicyRule)) {
            return false;
        } else {
            UserPolicyRule pr = (UserPolicyRule)o;
            return ((policy == null ? pr.policy == null : policy.equals(pr.policy)) &&
                    clientIntf == pr.clientIntf &&
                    serverIntf == pr.serverIntf &&
                    inbound == pr.inbound &&
                    (protocol == null ? pr.protocol == null : protocol.equals(pr.protocol)) &&
                    (clientAddr == null ? pr.clientAddr == null : clientAddr.equals(pr.clientAddr)) &&
                    (serverAddr == null ? pr.serverAddr == null : serverAddr.equals(pr.serverAddr)) &&
                    (clientPort == null ? pr.clientPort == null : clientPort.equals(pr.clientPort)) &&
                    (serverPort == null ? pr.serverPort == null : serverPort.equals(pr.serverPort)));
        }
    }

    public int hashCode()
    {
        // Should be fixed to include other stuff, once those are fixed. XXX
        return (null == policy ? 0 : policy.hashCode()) + clientIntf * 7 + serverIntf * 5;
    }
}
