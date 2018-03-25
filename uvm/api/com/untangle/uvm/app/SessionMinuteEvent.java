/**
 * $Id: SessionMinuteEvent.java,v 1.00 2016/05/30 19:57:23 dmorris Exp $
 */
package com.untangle.uvm.app;

import com.untangle.uvm.logging.LogEvent;

/**
 * This event stores the activity of a session on a given minute
 */
@SuppressWarnings("serial")
public class SessionMinuteEvent extends LogEvent
{
    private final long sessionId;
    private final long c2sBytes;
    private final long s2cBytes;
    private long sessionStartTime = 0;
    
    public SessionMinuteEvent( long sessionId, long c2sBytes, long s2cBytes, long sessionStartTime )
    {
        this.sessionId = sessionId;
        this.c2sBytes = c2sBytes;
        this.s2cBytes = s2cBytes;
        this.sessionStartTime = sessionStartTime;
    }

    /**
     * Session id.
     */
    public long getSessionId() { return sessionId; }

    /**
     * The number of bytes sent by the client during this minute
     */
    public long getC2sBytes() { return c2sBytes; }

    /**
     * The number of bytes sent by the client during this minute
     */
    public long getS2cBytes() { return s2cBytes; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "session_minutes" + getPartitionTablePostfix() + " " +
            "(time_stamp,c2s_bytes,s2c_bytes," + 
            "session_id, " + 
            "start_time, " + 
            "end_time, " + 
            "bypassed, " + 
            "entitled, " + 
            "protocol, " + 
            "icmp_type, " + 
            "hostname, " + 
            "username, " + 
            "policy_id, " + 
            "policy_rule_id, " + 
            "local_addr, " + 
            "remote_addr, " + 
            "c_client_addr, " + 
            "c_server_addr, " + 
            "c_server_port, " + 
            "c_client_port, " + 
            "s_client_addr, " + 
            "s_server_addr, " + 
            "s_server_port, " + 
            "s_client_port, " + 
            "client_intf, " + 
            "server_intf, " + 
            "client_country, " + 
            "client_latitude, " + 
            "client_longitude, " + 
            "server_country, " + 
            "server_latitude, " + 
            "server_longitude, " + 
            "filter_prefix, " + 
            "firewall_blocked, " + 
            "firewall_flagged, " + 
            "firewall_rule_index, " + 
            "application_control_lite_protocol, " + 
            "application_control_lite_blocked, " + 
            "captive_portal_blocked, " + 
            "captive_portal_rule_index, " + 
            "application_control_application, " + 
            "application_control_protochain, " + 
            "application_control_category, " + 
            "application_control_blocked, " + 
            "application_control_flagged, " + 
            "application_control_confidence, " + 
            "application_control_ruleid, " + 
            "application_control_detail, " + 
            "bandwidth_control_priority, " + 
            "bandwidth_control_rule, " + 
            "ssl_inspector_ruleid, " + 
            "ssl_inspector_status, " + 
            "ssl_inspector_detail, " +
            "tags) " +
            "SELECT " + 
            "?, ?, ?, " +
            "session_id, " + 
            "time_stamp as start_time, " + 
            "end_time, " + 
            "bypassed, " + 
            "entitled, " + 
            "protocol, " + 
            "icmp_type, " + 
            "hostname, " + 
            "username, " + 
            "policy_id, " + 
            "policy_rule_id, " + 
            "local_addr, " + 
            "remote_addr, " + 
            "c_client_addr, " + 
            "c_server_addr, " + 
            "c_server_port, " + 
            "c_client_port, " + 
            "s_client_addr, " + 
            "s_server_addr, " + 
            "s_server_port, " + 
            "s_client_port, " + 
            "client_intf, " + 
            "server_intf, " + 
            "client_country, " + 
            "client_latitude, " + 
            "client_longitude, " + 
            "server_country, " + 
            "server_latitude, " + 
            "server_longitude, " + 
            "filter_prefix, " + 
            "firewall_blocked, " + 
            "firewall_flagged, " + 
            "firewall_rule_index, " + 
            "application_control_lite_protocol, " + 
            "application_control_lite_blocked, " + 
            "captive_portal_blocked, " + 
            "captive_portal_rule_index, " + 
            "application_control_application, " + 
            "application_control_protochain, " + 
            "application_control_category, " + 
            "application_control_blocked, " + 
            "application_control_flagged, " + 
            "application_control_confidence, " + 
            "application_control_ruleid, " + 
            "application_control_detail, " + 
            "bandwidth_control_priority, " + 
            "bandwidth_control_rule, " + 
            "ssl_inspector_ruleid, " + 
            "ssl_inspector_status, " + 
            "ssl_inspector_detail, " +
            "tags " +
            "FROM " + schemaPrefix() + "sessions" + getSessionTablePostfix() + " as s WHERE s.session_id = ?";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setLong(++i,getC2sBytes());
        pstmt.setLong(++i,getS2cBytes());
        pstmt.setLong(++i,getSessionId());
        pstmt.addBatch();

        // java.io.PrintWriter out = new java.io.PrintWriter("/tmp/query.txt");
        // out.println(pstmt.toString());
        // out.close();
        return;
    }
    
    public boolean equals(Object o)
    {
        if (o instanceof SessionMinuteEvent) {
            SessionMinuteEvent pe = (SessionMinuteEvent)o;
            if (pe.getSessionId() != getSessionId() ||
                pe.getTimeStamp() != getTimeStamp() ||
                pe.getC2sBytes() != getC2sBytes() ||
                pe.getS2cBytes() != getS2cBytes())
                return false;
            return true;
        } else {
            return false;
        }
    }

    public String toString()
    {
        return "SessionMinuteEvent: [" + sessionId + "] c2sBytes:" + c2sBytes + " s2cBytes:" + s2cBytes;
    }

    @Override
    public String toSummaryString()
    {
        return "SessionMinuteEvent: [" + sessionId + "] c2sBytes:" + c2sBytes + " s2cBytes:" + s2cBytes;
    }
    
    public int hashCode()
    {
        return ((int)sessionId) + getTimeStamp().hashCode();
    }

    private String getSessionTablePostfix()
    {
        if ( sessionStartTime != 0 )
            return getPartitionTablePostfix( new java.sql.Timestamp ( this.sessionStartTime ) );

        logger.warn("Unknown start time in event: " + this);
        return getPartitionTablePostfix( new java.sql.Timestamp ( System.currentTimeMillis() ) );
    }
}
