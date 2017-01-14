/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.util.Date;
import java.io.Serializable;
import java.net.InetAddress;
import java.sql.Timestamp;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

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
    private Timestamp entryTime;
    private Timestamp exitTime;
    private String reason;

    public PenaltyBoxEvent() { }

    public PenaltyBoxEvent(int action, InetAddress address, Date entryTime, Date exitTime, String reason)
    {
        this.action = action;
        this.address = address;
        this.reason = reason;
        this.entryTime = new Timestamp(entryTime.getTime());
        this.exitTime = new Timestamp(exitTime.getTime());
    }
    
    public int getAction() { return action; }
    public void setAction( int action ) { this.action = action; }

    public InetAddress getAddress() { return address; }
    public void setAddress( InetAddress address ) { this.address = address; }

    public void setEntryTime( Timestamp entryTime ) { this.entryTime = entryTime; }
    public Timestamp getEntryTime() { return this.entryTime; }

    public void setExitTime( Timestamp exitTime ) { this.exitTime = exitTime; }
    public Timestamp getExitTime() { return this.exitTime; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        java.sql.PreparedStatement pstmt;
        String sql = "";
        int i=0;
        switch(action) {
        case ACTION_ENTER:
            sql =
                "INSERT INTO " + schemaPrefix() + "penaltybox" + getPartitionTablePostfix( this.entryTime ) + " " +
                "(start_time, end_time, address, time_stamp, reason) " +
                "values " +
                "( ?, ?, ?, ?, ?) ";
            pstmt = getStatementFromCache( sql, statementCache, conn );        
            pstmt.setTimestamp(++i, entryTime);
            pstmt.setTimestamp(++i, exitTime);
            pstmt.setObject(++i, getAddress().getHostAddress(), java.sql.Types.OTHER);
            pstmt.setTimestamp(++i, entryTime);
            pstmt.setString(++i, reason);
            pstmt.addBatch();
            return;
        case ACTION_EXIT:
            sql =
                "UPDATE " + schemaPrefix() + "penaltybox" + getPartitionTablePostfix( this.entryTime ) + " " +
                "SET end_time = ? " +
                "WHERE start_time = ? ";
            pstmt = getStatementFromCache( sql, statementCache, conn );        
            pstmt.setTimestamp(++i, exitTime);
            pstmt.setTimestamp(++i, entryTime);
            pstmt.addBatch();
            return;
        case ACTION_REENTER:
            sql =
                "UPDATE " + schemaPrefix() + "penaltybox" + getPartitionTablePostfix( this.entryTime ) + " " +
                "SET end_time = ? " + 
                "WHERE start_time = ? ";
            pstmt = getStatementFromCache( sql, statementCache, conn );        
            pstmt.setTimestamp(++i, exitTime);
            pstmt.setTimestamp(++i, entryTime);
            pstmt.addBatch();
            return;
        default:
            throw new RuntimeException("Unknown action: " + action);
        }
    }

    @Override
    public String toSummaryString()
    {
        String summary = address.getHostAddress() + " " + I18nUtil.marktr("penalty boxed") + " " + I18nUtil.marktr("because") + " " + reason;
        return summary;
    }
    
}
