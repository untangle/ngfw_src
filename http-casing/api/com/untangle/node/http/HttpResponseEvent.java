/**
 * $Id$
 */
package com.untangle.node.http;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for response.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class HttpResponseEvent extends LogEvent
{
    private RequestLine requestLine;
    private String contentType;
    private int contentLength;

    // constructors -----------------------------------------------------------

    public HttpResponseEvent() { }

    public HttpResponseEvent(RequestLine requestLine, String contentType, int contentLength)
    {
        this.requestLine = requestLine;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    // accessors --------------------------------------------------------------

    /**
     * Request line for this HTTP response pair.
     */
    public RequestLine getRequestLine()
    {
        return requestLine;
    }

    public void setRequestLine(RequestLine requestLine)
    {
        this.requestLine = requestLine;
    }

    /**
     * The base Content-Type, without any encodings or other useless
     * nonsense.
     */
    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        if (contentType != null && contentType.length() > DEFAULT_STRING_SIZE) contentType = contentType.substring(0, DEFAULT_STRING_SIZE);
        this.contentType = contentType;
    }

    /**
     * Content length, as counted by the parser.
     */
    public int getContentLength()
    {
        return contentLength;
    }

    public void setContentLength(int contentLength)
    {
        this.contentLength = contentLength;
    }

    @Override
    public boolean isDirectEvent()
    {
        return true;
    }

    @Override
    public String getDirectEventSql()
    {
        String sql =
            "UPDATE reports.n_http_events " +
            "SET " +
            "s2c_content_length = " + "'" + getContentLength() + "'" + ", " +
            "s2c_content_type = " + "'" + getContentType() + "'" + " " +
            "WHERE " +
            "request_id = " + getRequestLine().getRequestId() +
            ";";
        return sql;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        requestLine.getSessionEvent().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("url", requestLine.getUrl().toString());
        sb.addField("content-type", contentType);
        sb.addField("content-length", contentLength);
    }

    public String getSyslogId()
    {
        return "Response";
    }

    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }
}
