/**
 * $Id$
 */
package com.untangle.node.webfilter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a web filter cation
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_webfilter_evt", schema="events")
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
    @Column(name="request_id")
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
    @Type(type="com.untangle.node.webfilter.ReasonUserType")
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
    @Column(name="vendor_name")
    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }


    // LogEvent methods ----------------------------------------------------

    @Transient
    public boolean isPersistent()
    {
        return true;
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

    @Transient
    public String getSyslogId()
    {
        return "Block";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.WARNING; 
    }

    // Object methods ------------------------------------------------------

    public String toString()
    {
        return "WebFilterEvent id: " + getId();
    }
}
