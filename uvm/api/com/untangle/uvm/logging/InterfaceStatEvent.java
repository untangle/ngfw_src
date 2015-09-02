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
    private float rxRate;
    private float txRate;

    public InterfaceStatEvent() { }

    public int getInterfaceId() { return interfaceId; }
    public void setInterfaceId(int newValue) { this.interfaceId = newValue; }

    public float getRxRate() { return rxRate; }
    public void setRxRate(float newValue) { this.rxRate = newValue; }

    public float getTxRate() { return txRate; }
    public void setTxRate(float newValue) { this.txRate = newValue; }
    
    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        String sql =
            "INSERT INTO reports.interface_stat_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, interface_id, rx_rate, tx_rate) " +
            " values " +
            "( ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setLong(++i, interfaceId);
        pstmt.setFloat(++i, rxRate);
        pstmt.setFloat(++i, txRate);
        return pstmt;
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