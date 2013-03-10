/**
 * $Id$
 */
package com.untangle.node.virus;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.node.http.RequestLine;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log for HTTP Virus events.
 */
@SuppressWarnings("serial")
public class VirusHttpEvent extends LogEvent
{
    private Long requestId;
    private RequestLine requestLine;
    private VirusScannerResult result;
    private String vendorName;

    // constructors -------------------------------------------------------

    public VirusHttpEvent() { }

    public VirusHttpEvent(RequestLine requestLine, VirusScannerResult result, String vendorName)
    {
        this.requestId = requestLine.getRequestId();
        this.requestLine = requestLine;
        this.result = result;
        this.vendorName = vendorName;
    }

    // accessors ----------------------------------------------------------

    /**
     * Corresponding request line.
     *
     * @return the request line.
     */
    public Long getRequestId() { return requestId; }
    public void setRequestId( Long requestId ) { this.requestId = requestId; }

    /**
     * Virus scan result.
     */
    public VirusScannerResult getResult() { return result; }
    public void setResult( VirusScannerResult result ) { this.result = result; }

    /**
     * Virus vendor.
     */
    public String getVendorName() { return vendorName; }
    public void setVendorName( String vendorName ) { this.vendorName = vendorName; }

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        String sql =
            "UPDATE reports.http_events " +
            "SET " +
            "virus_" + getVendorName().toLowerCase() + "_clean = ?, " + 
            "virus_" + getVendorName().toLowerCase() + "_name = ? "  + 
            "WHERE " +
            "request_id = ? ";
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );
        
        int i = 0;
        pstmt.setBoolean(++i,getResult().isClean());
        pstmt.setString(++i, getResult().getVirusName());
        pstmt.setLong(++i, getRequestId());
        return pstmt;
    }
}
