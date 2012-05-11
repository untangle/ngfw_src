/**
 * $Id$
 */
package com.untangle.node.protofilter;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for a proto filter match.
 */
@SuppressWarnings("serial")
public class ProtoFilterLogEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private String protocol;
    private boolean blocked;

    // constructors -----------------------------------------------------------

    public ProtoFilterLogEvent() { }

    public ProtoFilterLogEvent(SessionEvent sessionEvent, String protocol, boolean blocked)
    {
        this.sessionEvent = sessionEvent;
        this.protocol = protocol;
        this.blocked = blocked;
    }

    // accessors --------------------------------------------------------------

    /**
     * The protocol, as determined by the protocol filter.
     *
     * @return the protocol name.
     */
    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    /**
     * Whether or not we blocked it.
     *
     * @return whether or not the session was blocked (closed)
     */
    public boolean getBlocked()
    {
        return blocked;
    }

    public void setBlocked(boolean blocked)
    {
        this.blocked = blocked;
    }

    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    private static String sql =
        "UPDATE reports.sessions " + 
        "SET pf_protocol = ?, " + 
        "    pf_blocked = ? " +
        "WHERE session_id = ? ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setString(++i, getProtocol());
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setLong(++i, sessionEvent.getSessionId());

        return pstmt;
    }
}
