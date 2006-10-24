/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import java.sql.Timestamp;

import com.metavize.mvvm.api.IPSessionDesc;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.protocol.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.protocol.ProtocolMatcherFactory;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcherFactory;
import com.metavize.mvvm.tran.firewall.ip.IPMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcherFactory;
import com.metavize.mvvm.tran.firewall.time.DayOfWeekMatcher;
import com.metavize.mvvm.tran.firewall.time.DayOfWeekMatcherFactory;
import com.metavize.mvvm.tran.firewall.user.UserMatcher;
import com.metavize.mvvm.tran.firewall.user.UserMatcherFactory;
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

    /* True if this matches client interface */
    private IntfMatcher clientIntf = IntfMatcherFactory.getInstance()
        .getExternalMatcher();

    /* True if this matches the server interface */
    private IntfMatcher serverIntf = IntfMatcherFactory.getInstance()
        .getInternalMatcher();

    private IPMatcher   clientAddr;
    private IPMatcher   serverAddr;

    private PortMatcher clientPort;
    private PortMatcher serverPort;

    private Date   startTime;
    private Date   endTime;

    private DayOfWeekMatcher dayOfWeek = DayOfWeekMatcherFactory.getInstance()
        .getAllMatcher();

    private boolean invertEntireDuration;

    private UserMatcher user = UserMatcherFactory.getInstance()
        .getAllMatcher();

    // constructors -----------------------------------------------------------

    public UserPolicyRule() { }

    public UserPolicyRule(IntfMatcher clientIntf, IntfMatcher serverIntf, Policy policy,
                          boolean inbound, ProtocolMatcher protocol,
                          IPMatcher clientAddr, IPMatcher serverAddr,
                          PortMatcher clientPort, PortMatcher serverPort,
                          Date startTime, Date endTime,
                          DayOfWeekMatcher dayOfWeek, UserMatcher user,
                          boolean live, boolean invertEntireDuration) {
        super(live, policy, inbound);
        this.clientIntf = clientIntf;
        this.serverIntf = serverIntf;
        this.protocol = protocol;
        this.clientAddr = clientAddr;
        this.serverAddr = serverAddr;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
	if( startTime instanceof Timestamp )
	    this.startTime = new Date(startTime.getTime());
	else
	    this.startTime = startTime;
	if( endTime instanceof Timestamp )
	    this.endTime = new Date(endTime.getTime());
	else
	    this.endTime = endTime;
        this.dayOfWeek = dayOfWeek;
        this.user = user;
        this.invertEntireDuration = invertEntireDuration;
    }

    // PolicyRule methods -----------------------------------------------------

    public boolean matches(IPSessionDesc sd)
    {
        boolean temp = 
            isLive()
            && clientIntf.isMatch(sd.clientIntf())
            && serverIntf.isMatch(sd.serverIntf())
            && protocol.isMatch(sd.protocol())
            && clientAddr.isMatch(sd.clientAddr())
            && serverAddr.isMatch(sd.serverAddr())
            && clientPort.isMatch(sd.clientPort())
            && serverPort.isMatch(sd.serverPort());

        if (temp == false)
            return temp;

        // Note that we assume we get back something from the database with meaningless
        // fields other than time ones.
        Calendar now = Calendar.getInstance();
        Date dnow = now.getTime();
        int nowhour = now.get(Calendar.HOUR_OF_DAY);
        Calendar start = Calendar.getInstance();
        start.setTime(startTime);
        int starthour = start.get(Calendar.HOUR_OF_DAY);
        boolean outsideDuration =
            (nowhour < starthour ||
             (nowhour == starthour && now.get(Calendar.MINUTE) < start.get(Calendar.MINUTE)));

        // Check the end.
        if (!outsideDuration) {
            Calendar end = Calendar.getInstance();
            end.setTime(endTime);
            int endhour = end.get(Calendar.HOUR_OF_DAY);
            outsideDuration =
                (nowhour > endhour ||
                 (nowhour == endhour && now.get(Calendar.MINUTE) > end.get(Calendar.MINUTE)));
        }
        if (!outsideDuration)
            outsideDuration = !dayOfWeek.isMatch(dnow);

        boolean durationTest = invertEntireDuration ? outsideDuration : !outsideDuration;

        return durationTest
            && user.isMatch(sd.user());
    }

    // accessors --------------------------------------------------------------

    /**
     * source IntfMatcher
     *
     * @return the source IP matcher.
     */
    @Column(name="client_intf_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.IntfMatcherUserType")
    public IntfMatcher getClientIntf()
    {
        return clientIntf;
    }

    public void setClientIntf( IntfMatcher clientIntf )
    {
        this.clientIntf = clientIntf;
    }

    /**
     * destination IntfMatcher
     *
     * @return the destination IP matcher.
     */
    @Column(name="server_intf_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.IntfMatcherUserType")
    public IntfMatcher getServerIntf()
    {
        return serverIntf;
    }

    public void setServerIntf( IntfMatcher serverIntf )
    {
        this.serverIntf = serverIntf;
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
     * User matcher
     *
     * @return the user matcher.
     */
    @Column(name="user_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.UserMatcherUserType")
    public UserMatcher getUser()
    {
        return user;
    }

    public void setUser( UserMatcher user )
    {
        this.user = user;
    }

    /**
     * Time the session may not start before
     *
     * @return the time of day that the session must start after
     */
    @Temporal(TemporalType.TIME)
    @Column(name="start_time")
    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    /**
     * Time the session may not start after
     *
     * @return the time of day that the session must start before
     */
    @Temporal(TemporalType.TIME)
    @Column(name="end_time")
    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    /**
     * Day of Week matcher
     *
     * @return the day of week matcher.
     */
    @Column(name="day_of_week_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.DayOfWeekMatcherUserType")
    public DayOfWeekMatcher getDayOfWeek()
    {
        return dayOfWeek;
    }

    public void setDayOfWeek( DayOfWeekMatcher dayOfWeek )
    {
        this.dayOfWeek = dayOfWeek;
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

    /**
     * Specifies if we should invert sense of the entire duration.
     * So a duration of 0900 -> 1700 M,T,W,R,F when inverted would
     * mean:
     *  all Saturday, all Sunday, and Monday through Friday
     *  from 0000 -> 0859 and 1701 -> 2359
     *
     * @return true if entire duration is inverted 
     */
    @Column(name="invert_entire_duration", nullable=false)
    public boolean isInvertEntireDuration()
    {
        return invertEntireDuration;
    }

    public void setInvertEntireDuration(boolean invertEntireDuration)
    {
        this.invertEntireDuration = invertEntireDuration;
    }


    @Transient
    public boolean isSameRow(UserPolicyRule pr)
    {
        return getId().equals(pr.getId());
    }

    /* Hack that sets the ports to zero for Ping sessions */
    public void fixPing() throws ParseException
    {
        PortMatcher pingMatcher = PortMatcherFactory.getInstance().getPingMatcher();

        if ( this.protocol.equals( ProtocolMatcherFactory.getInstance().getPingMatcher())) {
            this.clientPort = pingMatcher;
            this.serverPort = pingMatcher;
        } else if ( this.clientPort.equals( pingMatcher ) || this.serverPort.equals( pingMatcher )) {
            throw new ParseException( "Invalid port for a non-ping traffic type" );
        }
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof UserPolicyRule)) {
            return false;
        } else {
            UserPolicyRule pr = (UserPolicyRule)o;
            return ((policy == null ? pr.policy == null : policy.equals(pr.policy)) &&
                    (clientIntf == null ? pr.clientIntf == null : clientIntf.equals(pr.clientIntf)) &&
                    (serverIntf == null ? pr.serverIntf == null : serverIntf.equals(pr.serverIntf)) &&
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
        return (null == policy ? 0 : policy.hashCode()) + clientIntf.toDatabaseString().hashCode() * 7 + serverIntf.toDatabaseString().hashCode() * 5;
    }
}
