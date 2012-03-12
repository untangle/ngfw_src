/**
 * $Id$
 */
package com.untangle.node.virus;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
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

    // VirusEvent methods -------------------------------------------------

    // accessors ----------------------------------------------------------

    /**
     * Corresponding request line.
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
     * Virus scan result.
     *
     * @return the scan result.
     */
    public VirusScannerResult getResult()
    {
        return result;
    }

    public void setResult(VirusScannerResult result)
    {
        this.result = result;
    }

    /**
     * Virus vendor.
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
    public boolean isDirectEvent()
    {
        return true;
    }

    @Override
    public String getDirectEventSql()
    {
        String sql =
            "UPDATE reports.n_http_events " +
            "SET " +
            "virus_" + getVendorName().toLowerCase() + "_clean = " + "'" + getResult().isClean() + "'" + ", " +
            "virus_" + getVendorName().toLowerCase() + "_name = "  + "'" + getResult().getVirusName() + "'" + " " +
            "WHERE " +
            "request_id = " + getRequestId() +
            ";";
        return sql;
    }

    public void appendSyslog(SyslogBuilder sb)
    {
        SessionEvent pe = requestLine.getSessionEvent();
        if (null != pe) {
            pe.appendSyslog(sb);
        }

        sb.startSection("info");
        sb.addField("location", (null == requestLine ? "" : requestLine.getUrl().toString()));
        sb.addField("infected", !result.isClean());
        sb.addField("virus-name", (null == result.getVirusName() ? "" : result.getVirusName()));
    }
}
