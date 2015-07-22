/**
 * $Id: SettingsChangeEvent.java 33539 2012-12-03 23:45:01Z dmorris $
 */
package com.untangle.uvm;

import java.sql.Timestamp;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Settings change event
 */
@SuppressWarnings("serial")
public class SettingsChangesEvent extends LogEvent
{
    private String settings_file;
    private String username = "localadmin";
    private String hostname = "127.0.0.1";

    public SettingsChangesEvent( String settings_file, String username, String hostname )
    {
        this.settings_file = settings_file;
        if( username != null ){
            this.username = username;
        }
        if( hostname != null ){
            this.hostname = hostname;
        }
    }

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        String sql = "INSERT INTO reports.settings_changes" + getPartitionTablePostfix() + " " +
            "( time_stamp, settings_file, username, hostname)" +
            " values " +
            "( ?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;

        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setString(++i, this.settings_file);
        pstmt.setString(++i, this.username);
        pstmt.setString(++i, this.hostname);

        return pstmt;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "SettingsChangesEvent" + " " + this.settings_file;
        return summary;
    }
    
    
}
