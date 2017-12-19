/**
 * $Id: TunnelStatusEvent.java 39740 2015-02-26 20:46:19Z dmorris $
 */
package com.untangle.app.ipsec_vpn;

import java.io.Serializable;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * This class is used to periodically log IPsec tunnel status events which
 * capture the amount of data sent and received across each enabled IPsec
 * tunnel.
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class TunnelStatusEvent extends LogEvent implements Serializable
{
    private String tunnelName;
    private long inBytes;
    private long outBytes;

    public TunnelStatusEvent()
    {
        tunnelName = "unknown";
        inBytes = outBytes = 0;
    }

    public TunnelStatusEvent(String tunnelName, long inBytes, long outBytes)
    {
        this.tunnelName = tunnelName;
        this.inBytes = inBytes;
        this.outBytes = outBytes;
    }

    public String getTunnelName()
    {
        return (tunnelName);
    }

    public void setTunnelName(String tunnelName)
    {
        this.tunnelName = tunnelName;
    }

    public long getInBytes()
    {
        return (inBytes);
    }

    public void setInBytes(long inBytes)
    {
        this.inBytes = inBytes;
    }

    public long getOutBytes()
    {
        return (outBytes);
    }

    public void setOutBytes(long outBytes)
    {
        this.outBytes = outBytes;
    }

    @Override
    public void compileStatements(java.sql.Connection conn, java.util.Map<String, java.sql.PreparedStatement> statementCache) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "ipsec_tunnel_stats" + getPartitionTablePostfix() + " " + "(time_stamp, tunnel_name, in_bytes, out_bytes) " + "values " + "( ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = getStatementFromCache(sql, statementCache, conn);

        int i = 0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setString(++i, getTunnelName());
        pstmt.setLong(++i, getInBytes());
        pstmt.setLong(++i, getOutBytes());

        pstmt.addBatch();
        return;
    }

    public String toString()
    {
        String detail = new String();
        detail += ("TunnelStatusEvent(");
        detail += (" tunnelName:" + tunnelName);
        detail += (" inBytes:" + inBytes);
        detail += (" outBytes:" + outBytes);
        detail += (" )");
        return detail;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "IPse Tunnel " + getTunnelName() + " " + I18nUtil.marktr("sent") + " " + getOutBytes() + " " + I18nUtil.marktr("bytes and received") + " " + getInBytes() + " " + I18nUtil.marktr("bytes");
        return summary;
    }
}
