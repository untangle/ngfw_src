/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * This event updates the addresses and intf attributes
 * of a session after NAT (or port forward) or some other modification by an application
 * has taken place
 */
@SuppressWarnings("serial")
public class SessionNatEvent extends LogEvent
{
    private SessionEvent sessionEvent;

    private Integer serverIntf;

    private InetAddress sClientAddr;
    private InetAddress sServerAddr;
    private Integer sClientPort;
    private Integer sServerPort;
    
    public SessionNatEvent( SessionEvent sessionEvent, Integer serverIntf, InetAddress sClientAddr, Integer sClientPort, InetAddress sServerAddr, Integer sServerPort )
    {
        super();
        this.sessionEvent = sessionEvent;
        this.serverIntf = serverIntf;
        this.sClientAddr = sClientAddr;
        this.sClientPort = sClientPort;
        this.sServerAddr = sServerAddr;
        this.sServerPort = sServerPort;
    }

    /**
     * Server interface index (at server).
     */
    public Integer getServerIntf() { return serverIntf; }
    public void setServerIntf(Integer serverIntf) { this.serverIntf = serverIntf; }

    /**
     * Client address, at the server side.
     */
    public InetAddress getSClientAddr() { return sClientAddr; }
    public void setSClientAddr(InetAddress sClientAddr) { this.sClientAddr = sClientAddr; }

    /**
     * Server address, at the server side.
     */
    public InetAddress getSServerAddr() { return sServerAddr; }
    public void setSServerAddr(InetAddress sServerAddr) { this.sServerAddr = sServerAddr; }

    /**
     * Client port, at the server side.
     */
    public Integer getSClientPort() { return sClientPort; }
    public void setSClientPort(Integer sClientPort) { this.sClientPort = sClientPort; }

    /**
     * Server port, at the server side.
     */
    public Integer getSServerPort() { return sServerPort; }
    public void setSServerPort(Integer sServerPort) { this.sServerPort = sServerPort; }

    /**
     * The Session Event
     */
    public SessionEvent getSessionEvent() { return this.sessionEvent; }
    public void setSessionEvent( SessionEvent newValue ) { this.sessionEvent = newValue; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "UPDATE " + schemaPrefix() + "sessions" + sessionEvent.getPartitionTablePostfix() + " " +
            "SET server_intf = ?, " +
            "    s_client_addr = ?, " +
            "    s_client_port = ?, " +
            "    s_server_addr = ?, " +
            "    s_server_port = ? " +
            "WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        
 
        int i=0;
        pstmt.setInt(++i, getServerIntf());
        pstmt.setObject(++i, getSClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSClientPort());
        pstmt.setObject(++i, getSServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSServerPort());
        pstmt.setLong(++i, sessionEvent.getSessionId());

        pstmt.addBatch();
        return;
    }
    
    @Override
    public String toSummaryString()
    {
        String clientAddr = (getSClientAddr() != null ? getSClientAddr().getHostAddress() : "null");
        String serverAddr = (getSServerAddr() != null ? getSServerAddr().getHostAddress() : "null");
        String clientPort = (getSClientPort() != null ? getSClientPort().toString() : "null");
        String serverPort = (getSServerPort() != null ? getSServerPort().toString() : "null");

        return I18nUtil.marktr("Session NAT") + " " + clientAddr + ":" + clientPort + " -> " + serverAddr + ":" + serverPort;
    }
}
