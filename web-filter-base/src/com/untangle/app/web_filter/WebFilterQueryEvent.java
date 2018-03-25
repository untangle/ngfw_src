/**
 * $Id: WebFilterQueryEvent.java 34220 2013-03-10 17:41:01Z dmorris $
 */
package com.untangle.app.web_filter;

import java.net.URI;

import com.untangle.app.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.app.http.HttpMethod;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for a web filter search engine query event
 */
@SuppressWarnings("serial")
public class WebFilterQueryEvent extends LogEvent
{
    private Long requestId;
    private HttpMethod method;
    private String  term;
    private String  appName;
    private SessionEvent sessionEvent;
    private URI requestUri;
    private String host;
    private long contentLength;

    public WebFilterQueryEvent() { }

    public WebFilterQueryEvent(RequestLine requestLine, String host, String term, String appName)
    {
        this.host = host;
        this.requestId = requestLine.getRequestId();
        this.method = requestLine.getMethod();
        this.term = term;
        this.appName = appName;
        this.sessionEvent = requestLine.getSessionEvent();
        this.requestUri = requestLine.getRequestUri();
    }

    public String getHost() { return host; }
    public void setHost( String host ) { this.host = host; }

    public Long getRequestId() { return requestId; }
    public void setRequestId( Long requestId ) { this.requestId = requestId; }

    public HttpMethod getMethod() { return method; }
    public void setMethod( HttpMethod method ) { this.method = method; }

    public String getTerm() { return term; }
    public void setTerm( String term ) { this.term = term; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent( SessionEvent sessionEvent ) { this.sessionEvent = sessionEvent; }
    
    public URI getRequestUri() { return requestUri; }
    public void setRequestUri( URI requestUri ) { this.requestUri = requestUri; }

    public long getContentLength() { return contentLength; }
    public void setContentLength( long contentLength ) { this.contentLength = contentLength; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
        "INSERT INTO " + schemaPrefix() + "http_query_events" + getPartitionTablePostfix() + " " +
        "(" + 
            "time_stamp, " +
            "session_id, " + 
            "client_intf, " + 
            "server_intf, " +
            "c_client_addr, " +
            "c_client_port, " + 
            "c_server_addr, " + 
            "c_server_port, " + 
            "s_client_addr, " + 
            "s_client_port, " + 
            "s_server_addr, " + 
            "s_server_port, " + 
            "policy_id, " + 
            "username, " + 
            "request_id, " + 
            "method, " + 
            "uri, " + 
            "host, " +
            "c2s_content_length, " + 
            "hostname, " +
            "term" + 
        ") " + 
        "values ( " + 
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + 
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + 
            "?" +
        ")";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setLong(++i, getSessionEvent().getSessionId());
        pstmt.setInt(++i, getSessionEvent().getClientIntf());
        pstmt.setInt(++i, getSessionEvent().getServerIntf());
        pstmt.setObject(++i, getSessionEvent().getCClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getCClientPort());
        pstmt.setObject(++i, getSessionEvent().getCServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getCServerPort());
        pstmt.setObject(++i, getSessionEvent().getSClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getSClientPort());
        pstmt.setObject(++i, getSessionEvent().getSServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSessionEvent().getSServerPort());
        pstmt.setLong(++i, getSessionEvent().getPolicyId());
        pstmt.setString(++i, getSessionEvent().getUsername());
        pstmt.setLong(++i, getRequestId());
        pstmt.setString(++i, Character.toString(getMethod().getKey()));
        pstmt.setString(++i, getRequestUri().toString());
        pstmt.setString(++i, getHost());
        pstmt.setLong(++i, getContentLength());
        pstmt.setString(++i, getSessionEvent().getHostname());
        pstmt.setString(++i, getTerm());

        pstmt.addBatch();
        return;
    }


    @Override
    public String toSummaryString()
    {
        String summary = getSessionEvent().getCClientAddr().getHostAddress() + " " + I18nUtil.marktr("searched for") + " \"" + getTerm() + "\" " + I18nUtil.marktr("on") + " " + getHost();
        return summary;
    }
}
