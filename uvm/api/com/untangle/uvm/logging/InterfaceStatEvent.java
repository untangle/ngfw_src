/**
 * $Id: InterfaceStatEvent.java 40655 2015-07-08 00:46:02Z dmorris $
 */
package com.untangle.uvm.logging;

import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for system stats.
 *
 */
@SuppressWarnings("serial")
public class InterfaceStatEvent extends LogEvent
{
    private int interfaceId;
    private double rxRate;
    private double txRate;
    private double rxBytes;
    private double txBytes;

    public InterfaceStatEvent() { }

    public int getInterfaceId() { return interfaceId; }
    public void setInterfaceId(int newValue) { this.interfaceId = newValue; }

    public double getRxBytes() { return rxBytes; }
    public void setRxBytes(double newValue) { this.rxBytes = newValue; }

    public double getRxRate() { return rxRate; }
    public void setRxRate(double newValue) { this.rxRate = newValue; }

    public double getTxRate() { return txRate; }
    public void setTxRate(double newValue) { this.txRate = newValue; }

    public double getTxBytes() { return txBytes; }
    public void setTxBytes(double newValue) { this.txBytes = newValue; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "INSERT INTO " + schemaPrefix() + "interface_stat_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, interface_id, rx_rate, rx_bytes, tx_rate, tx_bytes) " +
            " values " +
            "( ?, ?, ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setLong(++i, interfaceId);
        pstmt.setDouble(++i, rxRate);
        pstmt.setDouble(++i, rxBytes);
        pstmt.setDouble(++i, txRate);
        pstmt.setDouble(++i, txBytes);

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = I18nUtil.marktr("Interface") + " " + getInterfaceId() + " " +
            I18nUtil.marktr("state is") + ": " + "[ " +
            I18nUtil.marktr("RX") + ": " + getRxRate() + ", " + 
            I18nUtil.marktr("TX") + ": " + getTxRate() + " ]";

        return summary;
    }
}
