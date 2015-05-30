/**
 * $Id$
 */
package com.untangle.node.http;

import java.sql.Timestamp;
import java.net.URI;
import java.net.URISyntaxException;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for a request.
 *
 */
@SuppressWarnings("serial")
public class HttpRequestEvent extends LogEvent
{
    private Long requestId;
    private HttpMethod method;
    private URI requestUri;
    private SessionEvent sessionEvent;
    private String host;
    private String domain;
    private long contentLength;

    // constructors -----------------------------------------------------------

    public HttpRequestEvent() { }

    public HttpRequestEvent(RequestLine requestLine, String host )
    {
        this.host = host;
        this.domain = getDomainForHost( host );
        this.requestId = requestLine.getRequestId();
        this.timeStamp = requestLine.getTimeStamp();
        this.method = requestLine.getMethod();
        this.requestUri = requestLine.getRequestUri();
        this.sessionEvent = requestLine.getSessionEvent();

        requestLine.setHttpRequestEvent(this); /* XXX hack - this should live elsewhere */
    }

    public HttpRequestEvent(RequestLine requestLine, String host, long contentLength)
    {
        this.host = host;
        this.domain = getDomainForHost( host );
        this.contentLength = contentLength;
        this.requestId = requestLine.getRequestId();
        this.timeStamp = requestLine.getTimeStamp();
        this.method = requestLine.getMethod();
        this.requestUri = requestLine.getRequestUri();
        this.sessionEvent = requestLine.getSessionEvent();

        requestLine.setHttpRequestEvent(this); /* XXX hack - this should live elsewhere */
    }

    // accessors --------------------------------------------------------------

    /**
     * The host, as specified by the request header.
     */
    public String getHost() { return host; }
    public void setHost( String newValue )
    {
        this.host = newValue;
        this.domain = getDomainForHost( host );
    }

    /**
     * The host, as specified by the request header.
     */
    public String getDomain() { return domain; }
    public void setDomain( String newValue ) { this.domain = newValue; }
    
    /**
     * Content length, as counted by the parser.
     */
    public long getContentLength() { return contentLength; }
    public void setContentLength( long contentLength ) { this.contentLength = contentLength; }

    /**
     * Get the sessionId
     */
    public Long getSessionId() { return sessionEvent.getSessionId(); }
    public void setSessionId( Long  sessionId ) { this .sessionEvent.setSessionId(sessionId); }

    /**
     * Get the requestId
     */
    public Long getRequestId() { return this.requestId; }
    public void setRequestId(  Long requestId  ) { this.requestId = requestId; }
    
    /**
     * Request method.
     */
    public HttpMethod getMethod() { return method; }
    public void setMethod( HttpMethod method ) { this.method = method; }

    /**
     * Request URI.
     */
    public URI getRequestUri() { return requestUri; }
    public void setRequestUri( URI requestUri ) { this.requestUri = requestUri; }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent( SessionEvent sessionEvent ) { this.sessionEvent = sessionEvent; }
    
    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        String sql = "INSERT INTO reports.http_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, " +
            "session_id, client_intf, server_intf, " +
            "c_client_addr, c_client_port, c_server_addr, c_server_port, " + 
            "s_client_addr, s_client_port, s_server_addr, s_server_port, " + 
            "policy_id, username, " + 
            "request_id, method, uri, " + 
            "host, domain, c2s_content_length, " + 
            "hostname) " + 
            "values " +
            "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

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
        pstmt.setString(++i, getDomain());
        pstmt.setLong(++i, getContentLength());
        pstmt.setString(++i, getSessionEvent().getHostname());

        return pstmt;
    }

    public String toString()
    {
        return "HttpRequestEvent length: " + requestUri.toString().length() + " (" + super.toString() + ")";
    }

    @Override
    public String toSummaryString()
    {
        String summary = sessionEvent.getCClientAddr().getHostAddress() + " " + I18nUtil.marktr("requested") +
            " (" + getMethod().toString() + ") " + 
            ( sessionEvent.getSServerPort() == 443 ? "https" : "http" ) + "://" +
            getHost() + getRequestUri();
        return summary;
    }


    // translates a host to a "domain"
    // foo.bar.yahoo.com -> yahoo.com
    // foor.bar.co.uk -> bar.co.uk
    private String getDomainForHost( String host )
    {
        if ( host == null )
            return null;
        
        String[] parts = host.split("\\.");
        int len = parts.length;
        
        if (parts.length <= 2) {
            return host;
        }
        
        String lastPart = parts[len-1];

        switch ( lastPart ) {
        case "au":
        case "za":
        case "uk":
        if ( parts.length > 2 )
            return parts[len-3] + "." + parts[len-2] + "." + parts[len-1];
        else
            return host;
        
        default:
            return parts[len-2] + "." + parts[len-1];
        }
    }
    
}
