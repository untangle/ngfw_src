/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.io.Serializable;
import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Quota event for the bandwidth control.  
 */
@SuppressWarnings("serial")
public class QuotaEvent extends LogEvent implements Serializable
{
    public static final int ACTION_GIVEN = 1; /* address was given a quota */
    public static final int ACTION_EXCEEDED = 2; /* address exceeded quata */
    
    private int action;

    private InetAddress address;

    private String reason; 

    private long quotaSize;

    // constructors -----------------------------------------------------------

    public QuotaEvent() { }

    public QuotaEvent(int action, InetAddress address, String reason, long quotaSize )
    {
        this.action = action;
        this.address = address;
        this.reason = reason;
        this.quotaSize = quotaSize;
    }
    
    
    // accessors --------------------------------------------------------------

    public int getAction() { return action; }
    public void setAction( int action ) { this.action = action; }

    public String getReason() { return reason; }
    public void setReason( String reason ) { this.reason = reason; }

    public InetAddress getAddress() { return address; }
    public void setAddress( InetAddress address ) { this.address = address; }

    public long getQuotaSize() { return quotaSize; }
    public void setQuotaSize( long quotaSize ) { this.quotaSize = quotaSize; }
    
    private static String sql = "INSERT INTO reports.quotas " +
        "(time_stamp, address, action, reason, size ) " + 
        "values " +
        "( ?, ?, ?, ?, ? )";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setObject(++i, getAddress().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getAction());
        pstmt.setString(++i, getReason());
        pstmt.setLong(++i, getQuotaSize());
        return pstmt;
    }

    @Override
    public String toSummaryString()
    {
        String actionStr;
        switch ( getAction() ) {
        case 1: actionStr = I18nUtil.marktr("given quota of"); break;
        case 2: actionStr = I18nUtil.marktr("exceeded quota of"); break;
        default: actionStr = I18nUtil.marktr("unknown"); break;
            
        }
            
        String summary = address.getHostAddress() + " " + action + " " + (quotaSize/(1024*1024)) + " " + "MB";
        return summary;
    }
    

}
