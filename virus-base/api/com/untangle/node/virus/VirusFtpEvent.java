/**
 * $Id$
 */
package com.untangle.node.virus;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log for FTP virus events.
 */
@SuppressWarnings("serial")
public class VirusFtpEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private VirusScannerResult result;
    private String vendorName;

    public VirusFtpEvent() { }

    public VirusFtpEvent(SessionEvent pe, VirusScannerResult result, String vendorName)
    {
        this.sessionEvent = pe;
        this.result = result;
        this.vendorName = vendorName;
    }

    /**
     * Get the session Id
     *
     * @return the the session Id
     */
    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
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
        /* 
         * FIXME there is currently no table in reports that stores FTP events
         */
        return null;
    }
}
