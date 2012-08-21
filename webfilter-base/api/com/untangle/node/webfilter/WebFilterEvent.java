/**
 * $Id$
 */
package com.untangle.node.webfilter;

import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;

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
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        String sql =
            "UPDATE reports.n_http_events " +
            "SET " +
            "wf_" + getVendorName().toLowerCase() + "_blocked  = ?, " + 
            "wf_" + getVendorName().toLowerCase() + "_flagged  = ?, " +
            "wf_" + getVendorName().toLowerCase() + "_reason   = ?, " +
            "wf_" + getVendorName().toLowerCase() + "_category = ? " +
            "WHERE " +
            "request_id = ? ";
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i = 0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setBoolean(++i, getFlagged());
        pstmt.setString(++i, ((getReason() == null) ? "" : Character.toString(getReason().getKey())));
        pstmt.setString(++i, getCategory());
        pstmt.setLong(++i, getRequestId());
        return pstmt;
    }
}
