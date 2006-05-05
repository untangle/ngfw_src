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

package com.metavize.tran.spyware;

import com.metavize.mvvm.logging.StatisticEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;

/**
 * Log event for a Spyware statistics.
 *
 * @author <a href="mailto:rbscott@metavize.com">rbscott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_SPYWARE_STATISTIC_EVT"
 * mutable="false"
 */
public class SpywareStatisticEvent extends StatisticEvent {
    private int pass = 0; // pass cookie, activeX, URL, or subnet access
    private int cookie = 0; // block cookie
    private int activeX = 0; // block activeX
    private int url = 0; // block URL
    private int subnetAccess = 0; // log subnet access

    // Constructors
    /**
     * Hibernate constructor
     */
    public SpywareStatisticEvent() {}

    public SpywareStatisticEvent(int pass, int cookie, int activeX, int url, int subnetAccess)
    {
        this.pass = pass;
        this.cookie = cookie;
        this.activeX = activeX;
        this.url = url;
        this.subnetAccess = subnetAccess;
    }

    /**
     * Number of passed events (cookie, activeX, URL, or subnet access)
     *
     * @return Number of passed events
     * @hibernate.property
     * column="PASS"
     */
    public int getPass() { return pass; }
    public void setPass( int pass ) { this.pass = pass; }
    public void incrPass() { pass++; }

    /**
     * Number of blocked cookies
     *
     * @return Number of blocked cookies
     * @hibernate.property
     * column="COOKIE"
     */
    public int getCookie() { return cookie; }
    public void setCookie( int cookie ) { this.cookie = cookie; }
    public void incrCookie() { cookie++; }

    /**
     * Number of blocked activeX
     *
     * @return Number of blocked activeX
     * @hibernate.property
     * column="ACTIVEX"
     */
    public int getActiveX() { return activeX; }
    public void setActiveX(int activeX) { this.activeX = activeX; }
    public void incrActiveX() { activeX++; }

    /**
     * Number of blocked urls
     *
     * @return Number of blocked urls
     * @hibernate.property
     * column="URL"
     */
    public int getURL() { return url; }
    public void setURL(int url) { this.url = url; }
    public void incrURL() { url++; }

    /**
     * Number of logged subnet accesses
     *
     * @return Number of logged subnet accesses
     * @hibernate.property
     * column="SUBNET_ACCESS"
     */
    public int getSubnetAccess() { return subnetAccess; }
    public void setSubnetAccess(int subnetAccess) { this.subnetAccess = subnetAccess; }
    public void incrSubnetAccess() { subnetAccess++; }

    /**
     * Returns true if any of the stats are non-zero, whenever all the stats are zero,
     * a new log event is not created.
     */
    public boolean hasStatistics() { return ((pass + cookie + activeX + url + subnetAccess) > 0 ); }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("pass", pass);
        sb.addField("cookie", cookie);
        sb.addField("activeX", activeX);
        sb.addField("url", url);
        sb.addField("subnetAccess", subnetAccess);
    }

    public String getSyslogId()
    {
        return "Statistic";
    }

    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }
}
