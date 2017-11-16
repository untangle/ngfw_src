/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.app.http.RequestLine;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log for HTTP Virus events.
 */
@SuppressWarnings("serial")
public class VirusHttpEvent extends LogEvent
{
    private RequestLine requestLine;
    private SessionEvent sessionEvent;
    private boolean clean;
    private String virusName;
    private String appName;

    public VirusHttpEvent() { }

    public VirusHttpEvent(RequestLine requestLine, SessionEvent sessionEvent, boolean clean, String virusName, String appName)
    {
        this.requestLine = requestLine;
        this.sessionEvent = sessionEvent;
        this.clean = clean;
        this.virusName = virusName;
        this.appName = appName;
    }

    public boolean getClean() { return clean; }
    public void setClean(boolean clean) { this.clean = clean; }

    public String getVirusName() { return virusName; }
    public void SetVirusName(String newValue) { this.virusName = newValue; }

    public String getAppName() { return appName; }
    public void setAppName( String appName ) { this.appName = appName; }

    public RequestLine getRequestLine() { return requestLine; }
    public void setRequestLine(RequestLine newValue) { this.requestLine = newValue; }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent newValue) { this.sessionEvent = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "http_events" + requestLine.getHttpRequestEvent().getPartitionTablePostfix() + " " +
            "SET " +
            getAppName().toLowerCase() + "_clean = ?, " + 
            getAppName().toLowerCase() + "_name = ? "  + 
            "WHERE " +
            "request_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        
        
        int i = 0;
        pstmt.setBoolean(++i, getClean());
        pstmt.setString(++i, getVirusName());
        pstmt.setLong(++i, getRequestLine().getRequestId());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String appName;
        switch ( getAppName().toLowerCase() ) {
        case "virus_blocker_lite": appName = "Virus Blocker Lite"; break;
        case "virus_blocker": appName = "Virus Blocker"; break;
        default: appName = "Virus Blocker"; break;
        }

        String actionStr;
        if ( getClean() )
            actionStr = I18nUtil.marktr("scanned");
        else
            actionStr = I18nUtil.marktr("found virus") + " [" + getVirusName() + "]";
        
        String summary = appName + " " + actionStr + " " + requestLine.getUrl();
        return summary;
    }

}
