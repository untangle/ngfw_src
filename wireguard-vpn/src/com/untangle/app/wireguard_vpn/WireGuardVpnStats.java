/**
 * $Id: WireGuardVpnStats.java 39740 2015-02-26 20:46:19Z dmorris $
 */

package com.untangle.app.wireguard_vpn;

import java.io.Serializable;
import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Class to manage a tunnel status event
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class WireGuardVpnStats extends LogEvent implements Serializable
{
    private InetAddress peerAddress;
    private String tunnelName;
    private long inBytes;
    private long outBytes;

// THIS IS FOR ECLIPSE - @formatter:off

    public WireGuardVpnStats()
    {
        tunnelName = "unknown";
        inBytes = outBytes = 0;
    }

    public WireGuardVpnStats(String tunnelName,InetAddress peerAddress,long inBytes,long outBytes)
    {
        this.peerAddress = peerAddress;
        this.tunnelName = tunnelName;
        this.inBytes = inBytes;
        this.outBytes = outBytes;
    }

    public InetAddress getPeerAddress() { return(peerAddress); }
    public void setPeerAddress(InetAddress peerAddress) { this.peerAddress = peerAddress; }

    public String getTunnelName() { return(tunnelName); }
    public void setTunnelName( String tunnelName ) { this.tunnelName = tunnelName; }

    public long getInBytes() { return(inBytes); }
    public void setInBytes( long inBytes ) { this.inBytes = inBytes; }

    public long getOutBytes() { return(outBytes); }
    public void setOutBytes( long outBytes ) { this.outBytes = outBytes; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "wireguard_vpn_stats" + getPartitionTablePostfix() + " " +
            "(time_stamp, tunnel_name, peer_address, in_bytes, out_bytes) " +
            "values " +
            "( ?, ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setString(++i, getTunnelName());
        pstmt.setObject(++i, getPeerAddress().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setLong(++i, getInBytes());
        pstmt.setLong(++i, getOutBytes());

        pstmt.addBatch();
        return;
    }

// THIS IS FOR ECLIPSE - @formatter:on

    public String toString()
    {
        String detail = new String();
        detail += ("WireGuardVpnStats(");
        detail += (" tunnelName:" + tunnelName);
        detail += (" inBytes:" + inBytes);
        detail += (" outBytes:" + outBytes);
        detail += (" )");
        return detail;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "WireGuard Tunnel " + getTunnelName() + " " + I18nUtil.marktr("sent") + " " + getOutBytes() + " " + I18nUtil.marktr("bytes and received") + " " + getInBytes() + " " + I18nUtil.marktr("bytes");
        return summary;
    }
}
