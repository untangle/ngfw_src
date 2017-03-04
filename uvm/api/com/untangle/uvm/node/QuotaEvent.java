/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.io.Serializable;

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

    private String entity;

    private String reason; 

    private long quotaSize;

    public QuotaEvent() { }

    public QuotaEvent(int action, String entity, String reason, long quotaSize )
    {
        this.action = action;
        this.entity = entity;
        this.reason = reason;
        this.quotaSize = quotaSize;
    }
    
    public int getAction() { return action; }
    public void setAction( int action ) { this.action = action; }

    public String getReason() { return reason; }
    public void setReason( String reason ) { this.reason = reason; }

    public String getEntity() { return entity; }
    public void setEntity( String entity ) { this.entity = entity; }

    public long getQuotaSize() { return quotaSize; }
    public void setQuotaSize( long quotaSize ) { this.quotaSize = quotaSize; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "quotas" + getPartitionTablePostfix() + " " +
            "(time_stamp, entity, action, reason, size ) " + 
            "values " +
            "( ?, ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setString(++i, getEntity());
        pstmt.setInt(++i, getAction());
        pstmt.setString(++i, getReason());
        pstmt.setLong(++i, getQuotaSize());

        pstmt.addBatch();
        return;
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
            
        String summary = entity + " " + action + " " + (quotaSize/(1024*1024)) + " " + "MB";
        return summary;
    }
    

}
