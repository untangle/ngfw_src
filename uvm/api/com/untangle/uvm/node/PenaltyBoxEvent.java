/**
 * $Id: PenaltyBoxEvent.java,v 1.00 2012/05/02 10:18:59 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.util.Date;
import java.io.Serializable;
import java.net.InetAddress;
import java.sql.Timestamp;

import com.untangle.uvm.logging.LogEvent;

/**
 * PenaltyBox event
 */
@SuppressWarnings("serial")
public class PenaltyBoxEvent extends LogEvent implements Serializable
{
    public static final int ACTION_ENTER = 1; /* client was put in penalty box */
    public static final int ACTION_EXIT = 2; /* client was released from penalty box */
    public static final int ACTION_REENTER = 3; /* client had penalty box time reset */
    
    private int action;

    private InetAddress address;
    private int priority; /* 0 if action = EXIT */
    private Timestamp entryTime;
    private Timestamp exitTime;
    private String reason;

    // constructors -----------------------------------------------------------

    public PenaltyBoxEvent() { }

    public PenaltyBoxEvent(int action, InetAddress address, int priority, Date entryTime, Date exitTime, String reason)
    {
        this.action = action;
        this.address = address;
        this.priority = priority;
        this.reason = reason;
        this.entryTime = new Timestamp(entryTime.getTime());
        this.exitTime = new Timestamp(exitTime.getTime());
    }
    
    // accessors --------------------------------------------------------------

    public int getAction() { return action; }
    public void setAction( int action ) { this.action = action; }

    public int getPriority() { return priority; }
    public void setPriority( int priority ) { this.priority = priority; }
    
    public InetAddress getAddress() { return address; }
    public void setAddress( InetAddress address ) { this.address = address; }

    public void setEntryTime( Timestamp entryTime ) { this.entryTime = entryTime; }
    public Timestamp getEntryTime() { return this.entryTime; }

    public void setExitTime( Timestamp exitTime ) { this.exitTime = exitTime; }
    public Timestamp getExitTime() { return this.exitTime; }
    
    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt;
        String sql = "";
        int i=0;
        switch(action) {
        case ACTION_ENTER:
            sql =
                "INSERT INTO reports.penaltybox " +
                "(start_time, end_time, address, time_stamp, reason) " +
                "values " +
                "( ?, ?, ?, ?, ?) ";
            pstmt = conn.prepareStatement( sql );
            pstmt.setTimestamp(++i, entryTime);
            pstmt.setTimestamp(++i, exitTime);
            pstmt.setObject(++i, getAddress().getHostAddress(), java.sql.Types.OTHER);
            pstmt.setTimestamp(++i, entryTime);
            pstmt.setString(++i, reason);
            return pstmt;
        case ACTION_EXIT:
            sql =
                "UPDATE reports.penaltybox " + 
                "SET end_time = ? " +
                "WHERE start_time = ? ";
            pstmt = conn.prepareStatement( sql );
            pstmt.setTimestamp(++i, exitTime);
            pstmt.setTimestamp(++i, entryTime);
            return pstmt;
        case ACTION_REENTER:
            sql =
                "UPDATE reports.penaltybox " + 
                "SET end_time = ? " + 
                "WHERE start_time = ? ";
            pstmt = conn.prepareStatement( sql );
            pstmt.setTimestamp(++i, exitTime);
            pstmt.setTimestamp(++i, entryTime);
            return pstmt;
        default:
            throw new RuntimeException("Unknown action: " + action);
        }
    }
}
