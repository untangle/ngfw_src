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

package com.untangle.node.spyware;


import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.PipelineEndpoints;

@SuppressWarnings("serial")
public abstract class SpywareEvent extends LogEvent
{
    // constructors -----------------------------------------------------------

    public SpywareEvent() { }

    // abstract methods -------------------------------------------------------

    public abstract PipelineEndpoints getPipelineEndpoints();
    public abstract String getType();
    public abstract String getReason();
    public abstract String getIdentification();
    public abstract String getLocation();
    public abstract boolean isBlocked();

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getPipelineEndpoints().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("ident", getIdentification());
        sb.addField("loc", getLocation());
        sb.addField("blocked", isBlocked());
    }

    public String getSyslogId()
    {
        return getType();
    }

    public SyslogPriority getSyslogPriority()
    {
        // NOTICE = spyware (access, activeX, blacklist, cookie) event logged
        // WARNING = traffic altered
        return false == isBlocked() ? SyslogPriority.NOTICE : SyslogPriority.WARNING;
    }
}
