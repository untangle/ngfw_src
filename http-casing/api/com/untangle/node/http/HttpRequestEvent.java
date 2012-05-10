/**
 * $Id$
 */
package com.untangle.node.http;

import java.util.Date;
import java.sql.Timestamp;
import java.net.URI;
import java.net.URISyntaxException;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for a request.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class HttpRequestEvent extends LogEvent
{
    private Long requestId;
    private Date timeStamp = new Date();
    private HttpMethod method;
    private URI requestUri;
    private SessionEvent sessionEvent;
    private String host;
    private int contentLength;

    // constructors -----------------------------------------------------------

    public HttpRequestEvent() { }

    public HttpRequestEvent(RequestLine requestLine, String host)
    {
        this.host = host;
        
        this.requestId = requestLine.getRequestId();
        this.timeStamp = requestLine.getTimeStamp();
        this.method = requestLine.getMethod();
        this.requestUri = requestLine.getRequestUri();
        this.sessionEvent = requestLine.getSessionEvent();

        requestLine.setHttpRequestEvent(this); /* XXX hack - this should live elsewhere */
    }

    public HttpRequestEvent(RequestLine requestLine, String host, int contentLength)
    {
        this.host = host;
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
     *
     * @return the host.
     */
    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    /**
     * Content length, as counted by the parser.
     *
     * @return number of octets in the body.
     */
    public int getContentLength()
    {
        return contentLength;
    }

    public void setContentLength(int contentLength)
    {
        this.contentLength = contentLength;
    }

    /**
     * Get the sessionId
     *
     * @return the SessionEvent.
     */
    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    /**
     * Get the requestId
     *
     * @return the RequestEvent.
     */
    public Long getRequestId()
    {
        return this.requestId;
    }

    public void setRequestId( Long requestId )
    {
        this.requestId = requestId;
    }
    
    /**
     * Request method.
     *
     * @return the request method.
     */
    public HttpMethod getMethod()
    {
        return method;
    }

    public void setMethod(HttpMethod method)
    {
        this.method = method;
    }

    /**
     * Request URI.
     *
     * @return the request URI.
     */
    public URI getRequestUri()
    {
        return requestUri;
    }

    public void setRequestUri(URI requestUri)
    {
        this.requestUri = requestUri;
    }

    /**
     * Time the event was logged, as filled in by logger.
     *
     * @return time logged.
     */
    public Date getTimeStamp()
    {
        return timeStamp;
    }

    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }
    
    /**
     * Don't make Aaron angry!  This should only be set by the event
     * logging system unless you're doing tricky things (with Aaron's
     * approval).
     */
    public void setTimeStamp(Date timeStamp)
    {
        if (timeStamp instanceof Timestamp) {
            this.timeStamp = new Date(timeStamp.getTime());
        } else {
            this.timeStamp = timeStamp;
        }
    }
    
    @Override
    public String getDirectEventSql()
    {
        String sql =
            "INSERT INTO reports.n_http_events " +
            "(time_stamp, " +
            "session_id, client_intf, server_intf, " +
            "c_client_addr, c_client_port, c_server_addr, c_server_port, " + 
            "s_client_addr, s_client_port, s_server_addr, s_server_port, " + 
            "policy_id, uid, " + 
            "request_id, method, uri, " + 
            "host, c2s_content_length, " + 
            "hname) " + 
            "values " +
            "( " +
            "timestamp '" + new java.sql.Timestamp(getTimeStamp().getTime()) + "'" + "," +
            getSessionEvent().getSessionId() + "," +
            getSessionEvent().getClientIntf() + "," +
            getSessionEvent().getServerIntf() + "," +
            "'" + getSessionEvent().getCClientAddr().getHostAddress() + "'" + "," +
            getSessionEvent().getCClientPort() + "," +
            "'" + getSessionEvent().getCServerAddr().getHostAddress() + "'" + "," +
            getSessionEvent().getCServerPort() + "," +
            "'" + getSessionEvent().getSClientAddr().getHostAddress() + "'" + "," +
            getSessionEvent().getSClientPort() + "," +
            "'" + getSessionEvent().getSServerAddr().getHostAddress() + "'" + "," +
            getSessionEvent().getSServerPort() + "," +
            getSessionEvent().getPolicyId() + "," +
            "'" + (getSessionEvent().getUsername() == null ? "" : getSessionEvent().getUsername()) + "'" + "," +
            getRequestId()  + "," + 
            "'" + Character.toString(getMethod().getKey()) + "'" + "," +
            "'" + getRequestUri() + "'" + "," +
            "'" + getHost() + "'" + "," +
            "'" + getContentLength() + "'" + "," +
            "'" + (getSessionEvent().getHostname() == null ? "" : getSessionEvent().getHostname()) + "'" + " )" +
            ";";
        return sql;
    }

    public String toString()
    {
        return "HttpRequestEvent length: " + requestUri.toString().length() + " (" + super.toString() + ")";
    }
}
