/*
 * $Id$
 */
package com.untangle.node.spyware;


import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.PipelineEndpoints;

@SuppressWarnings("serial")
public abstract class SpywareEvent extends LogEvent
{
    public SpywareEvent() { }

    public abstract PipelineEndpoints getPipelineEndpoints();
    public abstract String getType();
    public abstract String getReason();
    public abstract String getIdentification();
    public abstract String getLocation();
    public abstract Boolean isBlocked();

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
        // NOTICE = spyware (access, blacklist, cookie) event logged
        // WARNING = traffic altered
        return false == isBlocked() ? SyslogPriority.NOTICE : SyslogPriority.WARNING;
    }
}
