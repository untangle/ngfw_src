/**
 * $Id$
 */
package com.untangle.node.shield;

import java.io.Serializable;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for the shield rejection.
 */
@SuppressWarnings("serial")
public class ShieldStatisticEvent extends LogEvent implements Serializable
{
    private int accepted;
    private int limited;
    private int dropped;
    private int rejected;
    private int relaxed;
    private int lax;
    private int tight;
    private int closed;
        
    // Constructors
    public ShieldStatisticEvent()
    {
        this.accepted = 0;
        this.limited  = 0;
        this.dropped  = 0;
        this.rejected = 0;
        this.relaxed  = 0;
        this.lax      = 0;
        this.tight    = 0;
        this.closed   = 0;
    }

    public ShieldStatisticEvent( int accepted, int limited, int dropped, int rejected, int relaxed,
                                 int lax, int tight, int closed )
    {
        this.accepted = accepted;
        this.limited  = limited;
        this.dropped  = dropped;
        this.rejected = rejected;
        this.relaxed  = relaxed;
        this.lax      = lax;
        this.tight    = tight;
        this.closed   = closed;
    }

    /**
     * Number of accepted connections since the last log event
     */
    public int getAccepted() { return accepted; }
    public void setAccepted(  int accepted  ) { this.accepted = accepted; }

    /**
     * Number of limited sessions since the last time the user generated an event.
     */
    public int getLimited() { return limited; }
    public void setLimited(  int limited  ) { this.limited = limited; }

    /**
     * Number of dropped sessions since the last time the user generated an event.
     */
    public int getDropped() { return dropped; }
    public void setDropped(  int dropped  ) { this.dropped = dropped; }

    /**
     * Number of rejected connections since the last log event
     */
    public int getRejected() { return rejected; }
    public void setRejected(  int rejected  ) { this.rejected = rejected; }

    /**
     * Number of ticks the shield spent in relaxed mode since the last log event.
     */
    public int getRelaxed() { return relaxed; }
    public void setRelaxed(  int relaxed  ) { this.relaxed = relaxed; }

    /**
     * Number of ticks the shield spent in lax mode since the last log event.
     */
    public int getLax() { return lax; }
    public void setLax(  int lax  ) { this.lax = lax; }

    /**
     * Number of ticks the shield spent in tight mode since the last log event.
     */
    public int getTight() { return tight; }
    public void setTight(  int tight  ) { this.tight = tight; }

    /**
     * Number of ticks the shield spent in closed mode since the last log event.
     */
    public int getClosed() { return closed; }
    public void setClosed(  int closed  ) { this.closed = closed; }

    @Override
    public String getDirectEventSql()
    {
        String sql = "INSERT INTO reports.n_shield_totals " +
            "(time_stamp, accepted, limited, dropped, rejected) " + 
            "values " +
            "( " +
            "timestamp '" + new java.sql.Timestamp(getTimeStamp().getTime()) + "'" + "," +
            "'" + getAccepted() + "'" + "," +
            "'" + getLimited() + "'" + "," +
            "'" + getDropped() + "'" + "," +
            "'" + getRejected() + "'" + ") " +
            ";";
            return sql;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("accepted", accepted);
        sb.addField("limited", limited);
        sb.addField("dropped", dropped);
        sb.addField("rejected", rejected);
        sb.addField("relaxed", relaxed);
        sb.addField("lax", lax);
        sb.addField("tight", tight);
        sb.addField("closed", closed);
    }

    public String getSyslogId()
    {
        return "Shield_Statistic";
    }

    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }
}
