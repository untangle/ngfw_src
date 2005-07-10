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

package com.metavize.tran.firewall;

import com.metavize.mvvm.logging.StatisticEvent;

/**
 * Log event for a Firewall statistics.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_FIREWALL_STATISTIC_EVT"
 * mutable="false"
 */
public class FirewallStatisticEvent extends StatisticEvent
{
    /* Number of outbound firewall sessions */
    /* The total number of sessions examined */
    private int sessions = 0;

    /**
     * Number of TCP Sessions blocked by the default action, 
     * by a rule, passsed by the default, and
     * passed by a rule */
    private int tcpBlockedDefault;
    private int tcpBlockedRule;
    private int tcpPassedDefault;
    private int tcpPassedRule;

    private int udpBlockedDefault;
    private int udpBlockedRule;
    private int udpPassedDefault;
    private int udpPassedRule;

    private int icmpBlockedDefault;
    private int icmpBlockedRule;
    private int icmpPassedDefault;
    private int icmpPassedRule;
    
    // Constructors 
    /**
     * Hibernate constructor 
     */
    public FirewallStatisticEvent()
    {
        this.sessions = 0;

        this.tcpBlockedDefault = 0;
        this.tcpBlockedRule = 0;
        this.tcpPassedDefault = 0;
        this.tcpPassedRule = 0;
        
        this.udpBlockedDefault = 0;
        this.udpBlockedRule = 0;
        this.udpPassedDefault = 0;
        this.udpPassedRule = 0;
        
        this.icmpBlockedDefault = 0;
        this.icmpBlockedRule = 0;
        this.icmpPassedDefault = 0;
        this.icmpPassedRule = 0;
    }

    public FirewallStatisticEvent( int sessions, 
                                   int tcpBlockedDefault, int tcpBlockedRule,
                                   int tcpPasssedDefault, int tcpPassedRule,
                                   int udpBlockedDefault, int udpBlockedRule,
                                   int udpPasssedDefault, int udpPassedRule,
                                   int icmpBlockedDefault, int icmpBlockedRule,
                                   int icmpPasssedDefault, int icmpPassedRule )
    {
        this.sessions           = sessions;
        
        this.tcpBlockedDefault  = tcpBlockedDefault;
        this.tcpBlockedRule     = tcpBlockedRule;
        this.tcpPassedDefault   = tcpPassedDefault;
        this.tcpPassedRule      = tcpPassedRule;

        this.udpBlockedDefault  = udpBlockedDefault;
        this.udpBlockedRule     = udpBlockedRule;
        this.udpPassedDefault   = udpPassedDefault;
        this.udpPassedRule      = udpPassedRule;

        this.icmpBlockedDefault = icmpBlockedDefault;
        this.icmpBlockedRule    = icmpBlockedRule;
        this.icmpPassedDefault  = icmpPassedDefault;
        this.icmpPassedRule     = icmpPassedRule;
    }
    
    /**
     * Number of connections the firewall has examined
     *
     * @return Number of connections the firewall has examined
     * @hibernate.property
     * column="SESSIONS"
     */
    public int getSessions()
    {
        return this.sessions;
    }

    public void setSessions( int sessions )
    {
        this.sessions = sessions;
    }
    
    public void incrSessions()
    {
        this.sessions++;
    }

    /**
     * Number of tcp sessions blocked by the default rule.
     *
     * @return Number of tcp sessions blocked by the default.
     * @hibernate.property
     * column="TCP_BLOCK_DEFAULT"
     */
    public int getTcpBlockedDefault()
    {
        return this.tcpBlockedDefault;
    }

    public void setTcpBlockedDefault( int tcpBlockedDefault )
    {
        this.tcpBlockedDefault = tcpBlockedDefault;
    }

    public void incrTcpBlockedDefault()
    {
        this.tcpBlockedDefault++;
    }

    /**
     * Number of tcp sessions blocked by a rule.
     *
     * @return Number of tcp sessions blocked by a rule.
     * @hibernate.property
     * column="TCP_BLOCK_RULE"
     */
    public int getTcpBlockedRule()
    {
        return this.tcpBlockedRule;
    }

    public void setTcpBlockedRule( int tcpBlockedRule )
    {
        this.tcpBlockedRule = tcpBlockedRule;
    }

    public void incrTcpBlockedRule()
    {
        this.tcpBlockedRule++;
    }

    /**
     * Number of tcp sessions passed by the default rule.
     *
     * @return Number of tcp sessions passed by the default.
     * @hibernate.property
     * column="TCP_PASS_DEFAULT"
     */
    public int getTcpPassedDefault()
    {
        return this.tcpPassedDefault;
    }

    public void setTcpPassedDefault( int tcpPassedDefault )
    {
        this.tcpPassedDefault = tcpPassedDefault;
    }

    public void incrTcpPassedDefault()
    {
        this.tcpPassedDefault++;
    }

    /**
     * Number of tcp sessions passed by a rule.
     *
     * @return Number of tcp sessions passed by a rule.
     * @hibernate.property
     * column="TCP_PASS_RULE"
     */
    public int getTcpPassedRule()
    {
        return this.tcpPassedRule;
    }

    public void setTcpPassedRule( int tcpPassedRule )
    {
        this.tcpPassedRule = tcpPassedRule;
    }

    public void incrTcpPassedRule()
    {
        this.tcpPassedRule++;
    }


    /**
     * Number of udp sessions blocked by the default rule.
     *
     * @return Number of udp sessions blocked by the default.
     * @hibernate.property
     * column="UDP_BLOCK_DEFAULT"
     */
    public int getUdpBlockedDefault()
    {
        return this.udpBlockedDefault;
    }

    public void setUdpBlockedDefault( int udpBlockedDefault )
    {
        this.udpBlockedDefault = udpBlockedDefault;
    }

    public void incrUdpBlockedDefault()
    {
        this.udpBlockedDefault++;
    }

    /**
     * Number of udp sessions blocked by a rule.
     *
     * @return Number of udp sessions blocked by a rule.
     * @hibernate.property
     * column="UDP_BLOCK_RULE"
     */
    public int getUdpBlockedRule()
    {
        return this.udpBlockedRule;
    }

    public void setUdpBlockedRule( int udpBlockedRule )
    {
        this.udpBlockedRule = udpBlockedRule;
    }

    public void incrUdpBlockedRule()
    {
        this.udpBlockedRule++;
    }

    /**
     * Number of udp sessions passed by the default rule.
     *
     * @return Number of udp sessions passed by the default.
     * @hibernate.property
     * column="UDP_PASS_DEFAULT"
     */
    public int getUdpPassedDefault()
    {
        return this.udpPassedDefault;
    }

    public void setUdpPassedDefault( int udpPassedDefault )
    {
        this.udpPassedDefault = udpPassedDefault;
    }

    public void incrUdpPassedDefault()
    {
        this.udpPassedDefault++;
    }

    /**
     * Number of udp sessions passed by a rule.
     *
     * @return Number of udp sessions passed by a rule.
     * @hibernate.property
     * column="UDP_PASS_RULE"
     */
    public int getUdpPassedRule()
    {
        return this.udpPassedRule;
    }

    public void setUdpPassedRule( int udpPassedRule )
    {
        this.udpPassedRule = udpPassedRule;
    }

    public void incrUdpPassedRule()
    {
        this.udpPassedRule++;
    }

    /**
     * Number of icmp sessions blocked by the default rule.
     *
     * @return Number of icmp sessions blocked by the default.
     * @hibernate.property
     * column="ICMP_BLOCK_DEFAULT"
     */
    public int getIcmpBlockedDefault()
    {
        return this.icmpBlockedDefault;
    }

    public void setIcmpBlockedDefault( int icmpBlockedDefault )
    {
        this.icmpBlockedDefault = icmpBlockedDefault;
    }

    public void incrIcmpBlockedDefault()
    {
        this.icmpBlockedDefault++;
    }

    /**
     * Number of icmp sessions blocked by a rule.
     *
     * @return Number of icmp sessions blocked by a rule.
     * @hibernate.property
     * column="ICMP_BLOCK_RULE"
     */
    public int getIcmpBlockedRule()
    {
        return this.icmpBlockedRule;
    }

    public void setIcmpBlockedRule( int icmpBlockedRule )
    {
        this.icmpBlockedRule = icmpBlockedRule;
    }

    public void incrIcmpBlockedRule()
    {
        this.icmpBlockedRule++;
    }

    /**
     * Number of icmp sessions passed by the default rule.
     *
     * @return Number of icmp sessions passed by the default.
     * @hibernate.property
     * column="ICMP_PASS_DEFAULT"
     */
    public int getIcmpPassedDefault()
    {
        return this.icmpPassedDefault;
    }

    public void setIcmpPassedDefault( int icmpPassedDefault )
    {
        this.icmpPassedDefault = icmpPassedDefault;
    }

    public void incrIcmpPassedDefault()
    {
        this.icmpPassedDefault++;
    }

    /**
     * Number of icmp sessions passed by a rule.
     *
     * @return Number of icmp sessions passed by a rule.
     * @hibernate.property
     * column="ICMP_PASS_RULE"
     */
    public int getIcmpPassedRule()
    {
        return this.icmpPassedRule;
    }

    public void setIcmpPassedRule( int icmpPassedRule )
    {
        this.icmpPassedRule = icmpPassedRule;
    }

    public void incrIcmpPassedRule()
    {
        this.icmpPassedRule++;
    }

    /**
     * Returns true if any of the stats are non-zero, whenever all the stats are zero,
     * a new log event is not created.
     */
    public boolean hasStatistics()
    {
        return (( sessions +
                  tcpBlockedDefault  + tcpBlockedRule  + tcpPassedDefault  + tcpPassedRule +
                  udpBlockedDefault  + udpBlockedRule  + udpPassedDefault  + udpPassedRule +
                  icmpBlockedDefault + icmpBlockedRule + icmpPassedDefault + icmpPassedRule ) > 0 );
    }
}
