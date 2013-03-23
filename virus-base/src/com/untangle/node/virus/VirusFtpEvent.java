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
    private String nodeName;

    public VirusFtpEvent() { }

    public VirusFtpEvent(SessionEvent pe, VirusScannerResult result, String nodeName)
    {
        this.sessionEvent = pe;
        this.result = result;
        this.nodeName = nodeName;
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
     * Spam scanner node.
     *
     * @return the node
     */
    public String getNodeName()
    {
        return nodeName;
    }

    public void setNodeName(String nodeName)
    {
        this.nodeName = nodeName;
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
