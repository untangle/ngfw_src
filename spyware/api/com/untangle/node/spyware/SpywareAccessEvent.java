/*
 * $Id$
 */
package com.untangle.node.spyware;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for a spyware hit.
 */
@SuppressWarnings("serial")
public class SpywareAccessEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private String identification;
    private IPMaskedAddress ipMaddr; // location
    private Boolean blocked;

    // constructors -----------------------------------------------------------

    public SpywareAccessEvent() { }

    public SpywareAccessEvent(SessionEvent pe, String identification, IPMaskedAddress ipMaddr, Boolean blocked)
    {
        this.sessionEvent = pe;
        this.identification = identification;
        this.ipMaddr = ipMaddr;
        this.blocked = blocked;
    }


    // accessors --------------------------------------------------------------

    /**
     * Get the SessionId.
     */
    public Long getSessionId() { return sessionEvent.getSessionId(); }
    public void setSessionId(  Long sessionId  ) { this.sessionEvent.setSessionId(sessionId); }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent( SessionEvent sessionEvent ) { this.sessionEvent = sessionEvent; }

    /**
     * An address or subnet.
     */
    public IPMaskedAddress getIpMaddr() { return ipMaddr; }
    public void setIpMaddr( IPMaskedAddress ipMaddr ) { this.ipMaddr = ipMaddr; }

    /**
     * The identification (domain matched)
     */
    public String getIdentification() { return identification; }
    public void setIdentification( String identification ) { this.identification = identification; }

    /**
     * Whether or not we blocked it.
     */
    public Boolean isBlocked() { return blocked; }
    public void setBlocked( Boolean blocked ) { this.blocked = blocked; }

    @Override
    public String getDirectEventSql()
    {
        String sql =
            "UPDATE reports.sessions " + 
            "SET sw_access_ident = '" + getIdentification() + "' " +
            "WHERE session_id = '" + sessionEvent.getSessionId() + "'";
        return sql;
    }
}
