/**
 * $Id: CriticalAlertEvent.java $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;

/**
 * Critical alert event. Originally created to manage the alert we generate
 * when we transition to/from discard mode for logger events when the amout
 * of free disk space gets too low.
 */
@SuppressWarnings("serial")
public class CriticalAlertEvent extends LogEvent
{
    private String component = "component";
    private String message = "message";
    private String problem = "problem";

    public CriticalAlertEvent( String argComponent, String argMessage, String argProblem )
    {
        if( argComponent != null ){
            this.component = argComponent;
        }
        if( argMessage != null ){
            this.message = argMessage;
        }
        if( argProblem != null){
            this.problem = argProblem;
        }
    }

    public String getComponent() { return this.component; }
    public void setComponent(String newValue ) { this.component = newValue; }

    public String getMessage() { return this.message; }
    public void setMessage( String newValue ) { this.message = newValue; }

    public String getProblem() { return this.problem; }
    public void getProblem( String newValue ) { this.problem = newValue; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "critical_alerts" + getPartitionTablePostfix() + " " +
            "( time_stamp, component, message, problem)" +
            " values " +
            "( ?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getSqlTimeStamp());
        pstmt.setString(++i, this.component);
        pstmt.setString(++i, this.message);
        pstmt.setString(++i, this.problem);

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "CriticalAlertEvent" + " " + this.component + "/" + this.message;
        return summary;
    }
}
