/**
 * $Id: UserTableEvent.java 40333 2015-05-20 06:32:20Z dmorris $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for an update to the user table
 */
@SuppressWarnings("serial")
public class UserTableEvent extends LogEvent
{
    private String username;
    private String key;
    private String value;
    
    public UserTableEvent() { }

    public UserTableEvent( String username, String key, String value )
    {
        this.username = username;
        this.key = key;
        this.value = value;
    }

    public String getUsername() { return username; }
    public void setUsername( String newValue ) { this.username = newValue; }

    public String getKey() { return key; }
    public void setKey( String newValue ) { this.key = newValue; }

    public String getValue() { return value; }
    public void setValue( String newValue ) { this.value = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "user_table_updates" + getPartitionTablePostfix() + " " +
            "(time_stamp, username, key, value) " +
            "values " +
            "(?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i = 0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setString(++i, getUsername());
        pstmt.setString(++i, getKey());
        pstmt.setString(++i, getValue());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        return I18nUtil.marktr("User Table Update") + " " + "[" + username + "] " + key + " -> " + value;
    }
    
}
