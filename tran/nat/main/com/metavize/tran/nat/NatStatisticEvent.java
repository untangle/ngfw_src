/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import com.metavize.mvvm.logging.StatisticEvent;
import com.metavize.mvvm.logging.SyslogBuilder;

/**
 * Log event for a Nat statistics.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_NAT_STATISTIC_EVT"
 * mutable="false"
 */
public class NatStatisticEvent extends StatisticEvent
{
    /* Number of outbound nat sessions */
    private int natSessions           = 0;
    private int tcpIncomingRedirects  = 0;
    private int tcpOutgoingRedirects  = 0;
    private int udpIncomingRedirects  = 0;
    private int udpOutgoingRedirects  = 0;
    private int icmpIncomingRedirects = 0;
    private int icmpOutgoingRedirects = 0;
    private int dmzSessions           = 0;

    // Constructors
    /**
     * Hibernate constructor
     */
    public NatStatisticEvent()
    {
    }

    public NatStatisticEvent( int natSessions, int tcpIncomingRedirects, int tcpOutgoingRedirects,
                              int udpIncomingRedirects,  int udpOutgoingRedirects,
                              int icmpIncomingRedirects, int icmpOutgoingRedirects,
                              int dmzSessions )
    {
        this.natSessions           = natSessions;
        this.tcpIncomingRedirects  = tcpIncomingRedirects;
        this.tcpOutgoingRedirects  = tcpOutgoingRedirects;
        this.udpIncomingRedirects  = udpIncomingRedirects;
        this.udpOutgoingRedirects  = udpOutgoingRedirects;
        this.icmpIncomingRedirects = icmpIncomingRedirects;
        this.icmpOutgoingRedirects = icmpOutgoingRedirects;
        this.dmzSessions           = dmzSessions;
    }

    /**
     * Number of natted connections since the last log event
     *
     * @return Number of natted connections since the last log event
     * @hibernate.property
     * column="NAT_SESSIONS"
     */
    public int getNatSessions()
    {
        return natSessions;
    }

    public void setNatSessions( int natSessions )
    {
        this.natSessions = natSessions;
    }

    public void incrNatSessions()
    {
        this.natSessions++;
    }

    /**
     * Number of tcp incoming redirects since the last log event
     *
     * @return Number of tcp incoming redirects since the last log event
     * @hibernate.property
     * column="TCP_INCOMING"
     */
    public int getTcpIncomingRedirects()
    {
        return tcpIncomingRedirects;
    }

    public void setTcpIncomingRedirects( int tcpIncomingRedirects )
    {
        this.tcpIncomingRedirects = tcpIncomingRedirects;
    }

    public void incrTcpIncomingRedirects()
    {
        this.tcpIncomingRedirects++;
    }

    /**
     * Number of tcp outgoing redirects since the last log event
     *
     * @return Number of tcp outgoing redirects since the last log event
     * @hibernate.property
     * column="TCP_OUTGOING"
     */
    public int getTcpOutgoingRedirects()
    {
        return tcpOutgoingRedirects;
    }

    public void setTcpOutgoingRedirects( int tcpOutgoingRedirects )
    {
        this.tcpOutgoingRedirects = tcpOutgoingRedirects;
    }

    public void incrTcpOutgoingRedirects()
    {
        this.tcpOutgoingRedirects++;
    }


    /**
     * Number of udp incoming redirects since the last log event
     *
     * @return Number of udp incoming redirects since the last log event
     * @hibernate.property
     * column="UDP_INCOMING"
     */
    public int getUdpIncomingRedirects()
    {
        return udpIncomingRedirects;
    }

    public void setUdpIncomingRedirects( int udpIncomingRedirects )
    {
        this.udpIncomingRedirects = udpIncomingRedirects;
    }

    public void incrUdpIncomingRedirects()
    {
        this.udpIncomingRedirects++;
    }

    /**
     * Number of udp outgoing redirects since the last log event
     *
     * @return Number of udp outgoing redirects since the last log event
     * @hibernate.property
     * column="UDP_OUTGOING"
     */
    public int getUdpOutgoingRedirects()
    {
        return udpOutgoingRedirects;
    }

    public void setUdpOutgoingRedirects( int udpOutgoingRedirects )
    {
        this.udpOutgoingRedirects = udpOutgoingRedirects;
    }

    public void incrUdpOutgoingRedirects()
    {
        this.udpOutgoingRedirects++;
    }

    /**
     * Number of icmp incoming redirects since the last log event
     *
     * @return Number of icmp incoming redirects since the last log event
     * @hibernate.property
     * column="ICMP_INCOMING"
     */
    public int getIcmpIncomingRedirects()
    {
        return icmpIncomingRedirects;
    }

    public void setIcmpIncomingRedirects( int icmpIncomingRedirects )
    {
        this.icmpIncomingRedirects = icmpIncomingRedirects;
    }

    public void incrIcmpIncomingRedirects()
    {
        this.icmpIncomingRedirects++;
    }

    /**
     * Number of icmp outgoing redirects since the last log event
     *
     * @return Number of icmp outgoing redirects since the last log event
     * @hibernate.property
     * column="ICMP_OUTGOING"
     */
    public int getIcmpOutgoingRedirects()
    {
        return icmpOutgoingRedirects;
    }

    public void setIcmpOutgoingRedirects( int icmpOutgoingRedirects )
    {
        this.icmpOutgoingRedirects = icmpOutgoingRedirects;
    }

    public void incrIcmpOutgoingRedirects()
    {
        this.icmpOutgoingRedirects++;
    }

    /**
     * Number of DMZd sessions since the last log event
     *
     * @return Number of DMZd sessions since the last log event
     * @hibernate.property
     * column="DMZ_SESSIONS"
     */
    public int getDmzSessions()
    {
        return dmzSessions;
    }

    public void setDmzSessions( int dmzSessions )
    {
        this.dmzSessions = dmzSessions;
    }

    public void incrDmzSessions()
    {
        this.dmzSessions++;
    }

    /**
     * Returns true if any of the stats are non-zero, whenever all the stats are zero,
     * a new log event is not created.
     */
    public boolean hasStatistics()
    {
        return (( natSessions + dmzSessions +
                  tcpIncomingRedirects  + tcpOutgoingRedirects +
                  udpIncomingRedirects  + udpOutgoingRedirects +
                  icmpIncomingRedirects + icmpOutgoingRedirects ) > 0 );
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.addField("nat-sessions", natSessions);
        sb.addField("tcp-incoming-redirects", tcpIncomingRedirects);
        sb.addField("tcp-outgoing-redirects", tcpOutgoingRedirects);
        sb.addField("udp-incoming-redirects", udpIncomingRedirects);
        sb.addField("udp-outgoing-redirects", udpOutgoingRedirects);
        sb.addField("icmp-incoming-redirects", icmpIncomingRedirects);
        sb.addField("icmp-outgoing-redirects", icmpOutgoingRedirects);
        sb.addField("dmz-sessions", dmzSessions);
    }
}
