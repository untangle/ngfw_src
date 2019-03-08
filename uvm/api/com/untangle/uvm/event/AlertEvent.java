/**
 * $Id: Event.java 34225 2013-03-10 20:38:45Z dmorris $
 */
package com.untangle.uvm.event;

import org.json.JSONObject;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

import com.untangle.uvm.event.EventRule;

/**
 * Log event for an event
 */
@SuppressWarnings("serial")
public class AlertEvent extends LogEvent
{
    private String description;
    private String summaryText;
    private JSONObject json;
    private LogEvent cause;
    private Boolean eventSent;
    private EventRule causalRule;
    
    public AlertEvent() { }

    public AlertEvent( String description, String summaryText, JSONObject json, LogEvent cause, EventRule causalRule, Boolean eventSent )
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

    public LogEvent getCause() { return cause; }
    public void setCause( LogEvent newValue ) { this.cause = newValue; }

    public Boolean getEventSent() { return eventSent; }
    public void setEventSent( Boolean newValue ) { this.eventSent = newValue; }

    public EventRule getCausalRule() { return causalRule; }
    public void setCausalRule( EventRule newValue ) { this.causalRule = newValue; }

    public void setJson( JSONObject newValue )
    {
        this.json = newValue;
    }

    public String getJson()
    {
        String jsonString;
        try {
            jsonString = json.toString(4);
        } catch (org.json.JSONException e) {
            logger.warn("Failed to pretty print.",e);
            jsonString = json.toString();
        }

        return jsonString;
    }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        // !!! rename alerts to events
        String sql = "INSERT INTO " + schemaPrefix() + "alerts" + getPartitionTablePostfix() + " " +
            "(time_stamp, description, summary_text, json) " +
            "values " +
            "(?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setString(++i, getDescription());
        pstmt.setString(++i, getSummaryText());
        pstmt.setString(++i, json.toString());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = I18nUtil.marktr("Event") + " " + ( cause != null ? cause.toSummaryString() : "" );

        return summary;
    }
    
}
