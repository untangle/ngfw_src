/**
 * $Id: OperationsEvent.java 33539 2024-04-23 12:45:01Z rohitsingh $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;

/**
 * Settings change event
 */
@SuppressWarnings("serial")
public class OperationsEvent extends LogEvent
{
    private String operation;
    private String username = "localadmin";
    private String hostname = "127.0.0.1";

    public OperationsEvent( String operation, String username, String hostname )
    {
        this.operation = operation;
        if( username != null ){
            this.username = username;
        }
        if( hostname != null ){
            this.hostname = hostname;
        }
    }

    public String getOperation() { return this.operation; }
    public void setOperation( String newValue ) { this.operation = newValue; }

    public String getUsername() { return this.username; }
    public void setUsername( String newValue ) { this.username = newValue; }

    public String getHostname() { return this.hostname; }
    public void setHostname( String newValue ) { this.hostname = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "system_operations" + getPartitionTablePostfix() + " " +
            "( time_stamp, operation, username, hostname)" +
            " values " +
            "( ?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getSqlTimeStamp());
        pstmt.setString(++i, this.operation);
        pstmt.setString(++i, this.username);
        pstmt.setString(++i, this.hostname);

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "OperationsEvent" + " " + this.operation;
        return summary;
    }
    
    
}
