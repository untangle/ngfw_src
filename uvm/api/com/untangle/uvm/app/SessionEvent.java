/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionTuple;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTableEntry;

/**
 * Used to record the Session endpoints at session end time.
 * SessionStatsEvent and SessionEvent used to be the PiplineInfo
 * object.
 */
@SuppressWarnings("serial")
public class SessionEvent extends LogEvent
{
    private Long sessionId = -1L;
    private boolean bypassed = false;
    private boolean entitled = true;
    private Short protocol;
    private Short icmpType = null;
    private Integer clientIntf;
    private Integer serverIntf;
    private String clientCountry;
    private Double clientLatitude;
    private Double clientLongitude;
    private String serverCountry;
    private Double serverLatitude;
    private Double serverLongitude;
    private InetAddress localAddr;
    private InetAddress remoteAddr;
    private InetAddress cClientAddr;
    private InetAddress sClientAddr;
    private InetAddress cServerAddr;
    private InetAddress sServerAddr;
    private Integer cClientPort;
    private Integer sClientPort;
    private Integer cServerPort;
    private Integer sServerPort;
    private Integer policyId;
    private Integer policyRuleId;
    private String username;
    private String hostname;
    private String filterPrefix;
    private String tagsString;
    
    public SessionEvent()
    {
        super();
    }

    /**
     * Session id.
     */
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    /**
     * If the session is bypassed
     */
    public boolean getBypassed() { return bypassed; }
    public void setBypassed(boolean newValue) { this.bypassed = newValue; }

    /**
     * If the session is entitled to paid apps
     */
    public boolean getEntitled() { return entitled; }
    public void setEntitled(boolean newValue) { this.entitled = newValue; }
    
    /**
     * Protocol.  Currently always either 6 (TCP) or 17 (UDP).
     */
    public Short getProtocol() { return protocol; }
    public void setProtocol(Short protocol) { this.protocol = protocol; }

    /**
     * ICMP type. Only used for bypassde ICMP sessions (required by ICSA)
     */
    public Short getIcmpType() { return icmpType; }
    public void setIcmpType(Short newValue) { this.icmpType = newValue; }
    
    /**
     * Client interface number (at client).
     */
    public Integer getClientIntf() { return clientIntf; }
    public void setClientIntf(Integer clientIntf) { this.clientIntf = clientIntf; }

    /**
     * Server interface number (at server).
     */
    public Integer getServerIntf() { return serverIntf; }
    public void setServerIntf(Integer serverIntf) { this.serverIntf = serverIntf; }

    /**
     * Country, latitude, and longitude for WAN clients 
     */
    public String getClientCountry() { return clientCountry; }
    public void setClientCountry(String clientCountry) { this.clientCountry = clientCountry; }
    
    public Double getClientLatitude() { return clientLatitude; }
    public void setClientLatitude(Double clientLatitude) { this.clientLatitude = clientLatitude; }
    
    public Double getClientLongitude() { return clientLongitude; }
    public void setClientLongitude(Double clientLongitude) { this.clientLongitude = clientLongitude; }
    
    /**
     * Country, latitude, and longitude for WAN servers 
     */
    public String getServerCountry() { return serverCountry; }
    public void setServerCountry(String serverCountry) { this.serverCountry = serverCountry; }
    
    public Double getServerLatitude() { return serverLatitude; }
    public void setServerLatitude(Double serverLatitude) { this.serverLatitude = serverLatitude; }
    
    public Double getServerLongitude() { return serverLongitude; }
    public void setServerLongitude(Double serverLongitude) { this.serverLongitude = serverLongitude; }

    /**
     * The local (non-WAN address) address - the client address if ambigiuous
     */
    public InetAddress getLocalAddr() { return localAddr; }
    public void setLocalAddr(InetAddress newValue) { this.localAddr = newValue; }

    /**
     * The remote (WAN address) address - the server address if ambigiuous
     */
    public InetAddress getRemoteAddr() { return remoteAddr; }
    public void setRemoteAddr(InetAddress newValue) { this.remoteAddr = newValue; }

    /**
     * Client address, at the client side.
     */
    public InetAddress getCClientAddr() { return cClientAddr; }
    public void setCClientAddr(InetAddress cClientAddr) { this.cClientAddr = cClientAddr; }

    /**
     * Client address, at the server side.
     */
    public InetAddress getSClientAddr() { return sClientAddr; }
    public void setSClientAddr(InetAddress sClientAddr) { this.sClientAddr = sClientAddr; }

    /**
     * Server address, at the client side.
     */
    public InetAddress getCServerAddr() { return cServerAddr; }
    public void setCServerAddr(InetAddress cServerAddr) { this.cServerAddr = cServerAddr; }

    /**
     * Server address, at the server side.
     */
    public InetAddress getSServerAddr() { return sServerAddr; }
    public void setSServerAddr(InetAddress sServerAddr) { this.sServerAddr = sServerAddr; }

    /**
     * Client port, at the client side.
     */
    public Integer getCClientPort() { return cClientPort; }
    public void setCClientPort(Integer cClientPort) { this.cClientPort = cClientPort; }

    /**
     * Client port, at the server side.
     */
    public Integer getSClientPort() { return sClientPort; }
    public void setSClientPort(Integer sClientPort) { this.sClientPort = sClientPort; }

    /**
     * Server port, at the client side.
     */
    public Integer getCServerPort() { return cServerPort; }
    public void setCServerPort(Integer cServerPort) { this.cServerPort = cServerPort; }

    /**
     * Server port, at the server side.
     */
    public Integer getSServerPort() { return sServerPort; }
    public void setSServerPort(Integer sServerPort) { this.sServerPort = sServerPort; }

    /**
     * Policy that was applied for this pipeline.
     */
    public Integer getPolicyId() { return policyId; }
    public void setPolicyId(Integer policyId) { this.policyId = policyId; }

    /**
     * Policy rule ID that matched and set the policy (if any).
     */
    public Integer getPolicyRuleId() { return policyRuleId; }
    public void setPolicyRuleId(Integer newValue) { this.policyRuleId = newValue; }
    
    /**
     * The username associated with this session
     */
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    /**
     * The iptables NFLOG prefix associated with this session
     */
    public String getFilterPrefix() { return filterPrefix; }
    public void setFilterPrefix(String filterPrefix) { this.filterPrefix = filterPrefix; }

    /**
     * The hostname associated with this session
     */
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    /**
     * The tags associated with this session
     */
    public String getTagsString() { return this.tagsString; }
    public void setTagsString(String newValue) { this.tagsString = newValue; }
    
    public String getProtocolName()
    {
        switch (protocol) {
        case SessionTuple.PROTO_TCP: return "TCP";
        case SessionTuple.PROTO_UDP: return "UDP";
        default: return "unknown";
        }
    }
    
    public static String determineBestHostname( InetAddress clientAddr, int clientIntf, InetAddress serverAddr, int serverIntf )
    {
        try {

            /**
             * 1) If the host table entry for the client exists and already knows the hostname use it
             */
            HostTableEntry clientEntry = null;
            if ( clientAddr != null )
                clientEntry = UvmContextFactory.context().hostTable().getHostTableEntry( clientAddr );
            if ( clientEntry != null && clientEntry.hostnameKnown() ) {
                return clientEntry.getHostname();
            }

            /**
             * 2) If the client is on a WAN - check for the hostname of the server (the local address)
             */
            if ( clientIntf != 0 && UvmContextFactory.context().networkManager().isWanInterface( clientIntf ) ) {
                HostTableEntry serverEntry = null;
                if ( serverAddr != null )
                    serverEntry = UvmContextFactory.context().hostTable().getHostTableEntry( serverAddr );
                if ( serverEntry != null && serverEntry.hostnameKnown() ) {
                    return serverEntry.getHostname();
                }
            }

            /**
             * 3) If neither is known just use the address if fallbackToIp otherwise null
             */
            if ( clientIntf != 0 && UvmContextFactory.context().networkManager().isWanInterface( clientIntf ) ) {
                return serverAddr.getHostAddress();
            } else {
                return clientAddr.getHostAddress();
            }
        } catch (Exception e) {
            logger.warn( "Exception determing hostname", e );
            return null;
        }
    }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "sessions" + getPartitionTablePostfix() + " " +
            "(session_id, bypassed, entitled, time_stamp, protocol, icmp_type, end_time, hostname, username, filter_prefix, policy_id, policy_rule_id, local_addr, remote_addr, c_client_addr, c_client_port, c_server_addr, c_server_port, s_client_addr, s_client_port, s_server_addr, s_server_port, client_intf, server_intf, client_country, client_latitude, client_longitude, server_country, server_latitude, server_longitude, tags) " +
            "values " +
            "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        
        
        int i=0;
        pstmt.setLong(++i,getSessionId());
        pstmt.setBoolean(++i,getBypassed());
        pstmt.setBoolean(++i,getEntitled());
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setInt(++i,getProtocol());
        pstmt.setObject(++i,getIcmpType(), java.sql.Types.OTHER);
        pstmt.setTimestamp(++i,timeStampPlusSeconds(1)); // default end_time
        pstmt.setString(++i, getHostname());
        pstmt.setString(++i, getUsername());
        pstmt.setString(++i, getFilterPrefix());
        pstmt.setInt(++i, (getPolicyId() == null ? 1 : getPolicyId() ));
        pstmt.setInt(++i, (getPolicyRuleId() == null ? 0 : getPolicyRuleId() ));
        pstmt.setObject(++i, getLocalAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setObject(++i, getRemoteAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setObject(++i, getCClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getCClientPort());
        pstmt.setObject(++i, getCServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getCServerPort());
        pstmt.setObject(++i, getSClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSClientPort());
        pstmt.setObject(++i, getSServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSServerPort());
        pstmt.setInt(++i, getClientIntf());
        pstmt.setInt(++i, getServerIntf());
        pstmt.setString(++i, getClientCountry());
        pstmt.setDouble(++i, (getClientLatitude() == null ? 0 : getClientLatitude()) );
        pstmt.setDouble(++i, (getClientLongitude() == null ? 0 : getClientLongitude()) );
        pstmt.setString(++i, getServerCountry());
        pstmt.setDouble(++i, (getServerLatitude() == null ? 0 : getServerLatitude()) );
        pstmt.setDouble(++i, (getServerLongitude() == null ? 0 : getServerLongitude()) );
        pstmt.setString(++i, getTagsString());
        pstmt.addBatch();
        return;
    }
    
    public boolean equals(Object o)
    {
        if (o instanceof SessionEvent) {
            SessionEvent pe = (SessionEvent)o;
            return getSessionId().equals(pe.getSessionId());
        } else {
            return false;
        }
    }

    public String toString()
    {
        String clientAddr = (getCClientAddr() != null ? getCClientAddr().getHostAddress() : "null");
        String serverAddr = (getSServerAddr() != null ? getSServerAddr().getHostAddress() : "null");
        String clientPort = (getCClientPort() != null ? getCClientPort().toString() : "null");
        String serverPort = (getSServerPort() != null ? getSServerPort().toString() : "null");
        String protocol  = getProtocolName();
        
        return "SessionEvent: [" + protocol + "] " + clientAddr + ":" + clientPort + " -> " + serverAddr + ":" + serverPort;
    }

    @Override
    public String toSummaryString()
    {
        String clientAddr = (getCClientAddr() != null ? getCClientAddr().getHostAddress() : "null");
        String serverAddr = (getSServerAddr() != null ? getSServerAddr().getHostAddress() : "null");
        String clientPort = (getCClientPort() != null ? getCClientPort().toString() : "null");
        String serverPort = (getSServerPort() != null ? getSServerPort().toString() : "null");
        String protocol  = getProtocolName();

        return I18nUtil.marktr("Session") + " " + "[" + protocol + "] " + clientAddr + ":" + clientPort + " -> " + serverAddr + ":" + serverPort;
    }
    
    public int hashCode()
    {
        return getSessionId().hashCode();
    }
}
