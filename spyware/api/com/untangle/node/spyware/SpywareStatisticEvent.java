/*
 * $Id$
 */
package com.untangle.node.spyware;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a Spyware statistics.
 *
 * @author <a href="mailto:rbscott@untangle.com">rbscott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_spyware_statistic_evt", schema="events")
@SuppressWarnings("serial")
public class SpywareStatisticEvent extends StatisticEvent
{
    private int pass = 0; // pass cookie, URL, or subnet access
    private int cookie = 0; // block cookie
    private int url = 0; // block URL
    private int subnetAccess = 0; // log subnet access

    // Constructors
    public SpywareStatisticEvent() { }

    public SpywareStatisticEvent(int pass, int cookie, int url, int subnetAccess)
    {
        this.pass = pass;
        this.cookie = cookie;
        this.url = url;
        this.subnetAccess = subnetAccess;
    }

    /**
     * Number of passed events (cookie, URL, or subnet access)
     *
     * @return Number of passed events
     */
    @Column(nullable=false)
    public int getPass() { return pass; }
    public void setPass( int pass ) { this.pass = pass; }
    public void incrPass() { pass++; }

    /**
     * Number of blocked cookies
     *
     * @return Number of blocked cookies
     */
    @Column(nullable=false)
    public int getCookie() { return cookie; }
    public void setCookie( int cookie ) { this.cookie = cookie; }
    public void incrCookie() { cookie++; }

    /**
     * Number of blocked urls
     *
     * @return Number of blocked urls
     */
    @Column(nullable=false)
    public int getURL() { return url; }
    public void setURL(int url) { this.url = url; }
    public void incrURL() { url++; }

    /**
     * Number of logged subnet accesses
     *
     * @return Number of logged subnet accesses
     */
    @Column(name="subnet_access", nullable=false)
    public int getSubnetAccess() { return subnetAccess; }
    public void setSubnetAccess(int subnetAccess) { this.subnetAccess = subnetAccess; }
    public void incrSubnetAccess() { subnetAccess++; }

    /**
     * Returns true if any of the stats are non-zero, whenever all the stats are zero,
     * a new log event is not created.
     */
    public boolean hasStatistics() { return ((pass + cookie + url + subnetAccess) > 0 ); }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("pass", pass);
        sb.addField("cookie", cookie);
        sb.addField("url", url);
        sb.addField("subnetAccess", subnetAccess);
    }

    @Transient
    public String getSyslogId()
    {
        return "Statistic";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }
}
