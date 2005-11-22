/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: NatStatisticEvent.java 1283 2005-07-10 01:10:23Z rbscott $
 */

package com.metavize.tran.ids;

import com.metavize.mvvm.logging.StatisticEvent;
import com.metavize.mvvm.logging.SyslogBuilder;

/**
 * Log event for a IDS statistics.
 *
 * @author <a href="mailto:nchilders@metavize.com">nchilders</a>
 * @stolen from rbscott yo
 * @version 1.0
 * @hibernate.class
 * table="TR_IDS_STATISTIC_EVT"
 * mutable="false"
 */
public class IDSStatisticEvent extends StatisticEvent {
    private int dnc = 0; // did-not-care
    private int logged = 0; // logged or alerted
    private int blocked = 0;

    // Constructors
    /**
     * Hibernate constructor
     */
    public IDSStatisticEvent() {}

    public IDSStatisticEvent( int dnc, int logged, int blocked )
    {
        this.dnc = dnc;
        this.logged  = logged;
        this.blocked = blocked;
    }

    /**
     * Number of dnc chunks (did-not-cares are not logged or blocked)
     *
     * @return Number of dnc chunks
     * @hibernate.property
     * column="DNC"
     */
    public int getDNC() { return dnc; }
    public void setDNC( int dnc ) { this.dnc = dnc; }
    public void incrDNC() { dnc++; }

    /**
     * Number of logged chunks
     *
     * @return Number of logged chunks
     * @hibernate.property
     * column="LOGGED"
     */
    public int getLogged() { return logged; }
    public void setLogged(int logged) { this.logged = logged; }
    public void incrLogged() { logged++; }

    /**
     * Number of blocked chunks
     *
     * @return Number of blocked chunks
     * @hibernate.property
     * column="BLOCKED"
     */
    public int getBlocked() { return blocked; }
    public void setBlocked(int blocked) { this.blocked = blocked; }
    public void incrBlocked() { blocked++; }

    /**
     * Returns true if any of the stats are non-zero, whenever all the stats are zero,
     * a new log event is not created.
     */
    public boolean hasStatistics() { return ((dnc + logged + blocked) > 0 ); }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("did-not-care", dnc);
        sb.addField("logged", logged);
        sb.addField("blocked", blocked);
    }
}
