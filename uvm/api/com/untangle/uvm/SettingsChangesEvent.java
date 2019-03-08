/**
 * $Id: SettingsChangeEvent.java 33539 2012-12-03 23:45:01Z dmorris $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;

/**
 * Settings change event
 */
@SuppressWarnings("serial")
public class SettingsChangesEvent extends LogEvent
{
    private String settingsFile;
    private String username = "localadmin";
    private String hostname = "127.0.0.1";

    public SettingsChangesEvent( String settingsFile, String username, String hostname )
    {
        this.settingsFile = settingsFile;
        if( username != null ){
            this.username = username;
        }
        if( hostname != null ){
            this.hostname = hostname;
        }
    }

    public String getSettingsFile() { return this.settingsFile; }
    public void setSettingsFile( String newValue ) { this.settingsFile = newValue; }

    public String getUsername() { return this.username; }
    public void setUsername( String newValue ) { this.username = newValue; }

    public String getHostname() { return this.hostname; }
    public void setHostname( String newValue ) { this.hostname = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "settings_changes" + getPartitionTablePostfix() + " " +
            "( time_stamp, settings_file, username, hostname)" +
            " values " +
            "( ?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setString(++i, this.settingsFile);
        pstmt.setString(++i, this.username);
        pstmt.setString(++i, this.hostname);

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "SettingsChangesEvent" + " " + this.settingsFile;
        return summary;
    }
    
    
}
