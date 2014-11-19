/**
 * $Id: InterestingEvent.java 34225 2013-03-10 20:38:45Z dmorris $
 */
package com.untangle.node.reporting;

import org.json.JSONObject;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for an "interesting" event
 */
@SuppressWarnings("serial")
public class InterestingEvent extends LogEvent
{
    private String description;
    private String summaryText;
    private JSONObject json;
    private LogEvent cause;
    
    public InterestingEvent() { }

    public InterestingEvent( String description, String summaryText, JSONObject json, LogEvent cause )
    {
        this.description = description;
        this.summaryText = summaryText;
        this.json = json;
        this.cause = cause;
        this.setTimeStamp( cause.getTimeStamp() ); /* set the timestamp from the cause */
    }

    public String getDescription() { return description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public String getSummaryText() { return summaryText; }
    public void setSummaryText( String newValue ) { this.summaryText = newValue; }

    public JSONObject getJson() { return json; }
    public void setJson( JSONObject newValue ) { this.json = newValue; }

    public LogEvent getCause() { return cause; }
    public void setCause( LogEvent newValue ) { this.cause = newValue; }
    
    private static String sql = "INSERT INTO reports.interesting " +
        "(time_stamp, description, summary_text, json) " +
        "values " +
        "(?, ?, ?, ?); ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setString(++i, getDescription());
        pstmt.setString(++i, getSummaryText());
        pstmt.setString(++i, json.toString());

        return pstmt;
    }

    @Override
    public String toSummaryString()
    {
        String summary = I18nUtil.marktr("Interesting Event") + " " + ( cause != null ? cause.toSummaryString() : "" );

        return summary;
    }
    
}
