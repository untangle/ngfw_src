/**
 * $Id$
 */
package com.untangle.node.webfilter;

import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a web filter cation
 */
@SuppressWarnings("serial")
public class WebFilterEvent extends LogEvent
{
    private Long requestId;
    private Boolean blocked;
    private Boolean flagged;
    private Reason  reason;
    private String  category;
    private String  vendorName;

    public WebFilterEvent() { }

    public WebFilterEvent(RequestLine requestLine, Boolean blocked, Boolean flagged, Reason reason, String category, String vendorName)
    {
        this.requestId = requestLine.getRequestId();
        this.blocked = blocked;
        this.flagged = flagged;
        this.reason = reason;
        this.category = category;
        this.vendorName = vendorName;
    }

    /**
     * Request line for this HTTP response pair.
     *
     * @return the request line.
     */
    public Long getRequestId()
    {
        return requestId;
    }

    public void setRequestId(Long requestId)
    {
        this.requestId = requestId;
    }

    /**
     * True if this event was blocked
     */
    public Boolean getBlocked()
    {
        return blocked;
    }

    public void setBlocked(Boolean blocked)
    {
        this.blocked = blocked;
    }

    /**
     * True if this event flagged as a violation
     */
    public Boolean getFlagged()
    {
        return flagged;
    }

    public void setFlagged(Boolean flagged)
    {
        this.flagged = flagged;
    }

    /**
     * Reason for blocking.
     *
     * @return the reason.
     */
    public Reason getReason()
    {
        return reason;
    }

    public void setReason(Reason reason)
    {
        this.reason = reason;
    }

    /**
     * A string associated with the block reason.
     */
    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    /**
     * Spam scanner vendor.
     *
     * @return the vendor
     */
    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }

    @Override
    public String getDirectEventSql()
    {
        String reasonKey = ((getReason() == null) ? "" : Character.toString(getReason().getKey()));
        
        String sql =
            "UPDATE reports.n_http_events " +
            "SET " +
            "wf_" + getVendorName().toLowerCase() + "_blocked = " + "'" + getBlocked() + "'" + ", " +
            "wf_" + getVendorName().toLowerCase() + "_flagged = "  + "'" + getFlagged() + "'" + " " + ", " +
            "wf_" + getVendorName().toLowerCase() + "_reason = "  + "'" + reasonKey + "'" + " " + ", " +
            "wf_" + getVendorName().toLowerCase() + "_category = "  + "'" + getCategory() + "'" + " " +
            "WHERE " +
            "request_id = " + getRequestId() +
            ";";
        return sql;
    }

    // Syslog methods ------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("id", requestId);
        sb.addField("blocked", blocked);
        sb.addField("flagged", flagged);
        sb.addField("reason", null == reason ? "none" : reason.toString());
        sb.addField("category", null == category ? "none" : category);
    }

    public String getSyslogId()
    {
        return "Block";
    }

    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.WARNING; 
    }

    // Object methods ------------------------------------------------------

    public String toString()
    {
        return "WebFilterEvent id: " + getRequestId();
    }
}
