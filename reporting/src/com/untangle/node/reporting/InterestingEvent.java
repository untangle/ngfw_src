/**
 * $Id: InterestingEvent.java 34225 2013-03-10 20:38:45Z dmorris $
 */
package com.untangle.node.reporting;

import org.json.JSONObject;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for a proto filter match.
 */
@SuppressWarnings("serial")
public class InterestingEvent extends LogEvent
{
    private String text;
    private JSONObject json;
    private LogEvent cause;
    
    public InterestingEvent() { }

    public InterestingEvent( String text, JSONObject json, LogEvent cause )
    {
        this.text = text;
        this.json = json;
        this.cause = cause;
        this.setTimeStamp( cause.getTimeStamp() ); /* set the timestamp from the cause */
    }

    public String getText() { return text; }
    public void setText( String newValue ) { this.text = newValue; }

    public JSONObject getJson() { return json; }
    public void setJson( JSONObject newValue ) { this.json = newValue; }

    public LogEvent getCause() { return cause; }
    public void setCause( LogEvent newValue ) { this.cause = newValue; }
    
    private static String sql = "INSERT INTO reports.interesting " +
        "(time_stamp, text, json) " +
        "values " +
        "(?, ?, ?); ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setString(++i, getText());
        pstmt.setString(++i, json.toString());

        return pstmt;
    }
}
