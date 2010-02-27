/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.firewall;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.PipelineEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.PipelineEndpoints;

/**
 * Log event for the firewall.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_firewall_evt", schema="events")
public class FirewallEvent extends PipelineEvent implements Serializable
{
    private static final long serialVersionUID = -6784469969222582381L;
    private int     ruleIndex;
    private boolean wasBlocked;

    // Constructors
    public FirewallEvent() { }

    public FirewallEvent( PipelineEndpoints pe, boolean wasBlocked, int ruleIndex )
    {
        super(pe);

        this.wasBlocked = wasBlocked;
        this.ruleIndex  = ruleIndex;
    }

    /**
     * Whether or not the session was blocked.
     *
     * @return If the session was passed or blocked.
     */
    @Column(name="was_blocked", nullable=false)
    public boolean getWasBlocked()
    {
        return wasBlocked;
    }

    public void setWasBlocked( boolean wasBlocked )
    {
        this.wasBlocked = wasBlocked;
    }

    /**
     * Rule index, when this event was triggered.
     *
     * @return current rule index for the rule that triggered this event.
     */
    @Column(name="rule_index", nullable=false)
    public int getRuleIndex()
    {
        return ruleIndex;
    }

    public void setRuleIndex( int ruleIndex )
    {
        this.ruleIndex = ruleIndex;
    }

    // Syslog methods -----------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getPipelineEndpoints().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("reason-rule#", getRuleIndex());
        sb.addField("blocked", getWasBlocked());
    }

    @Transient
    public String getSyslogId()
    {
        return ""; // XXX
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        // INFORMATIONAL = statistics or normal operation
        // WARNING = traffic altered
        return false == getWasBlocked() ? SyslogPriority.INFORMATIONAL : SyslogPriority.WARNING;
    }
}
