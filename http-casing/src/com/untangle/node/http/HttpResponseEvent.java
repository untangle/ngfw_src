/**
 * $Id$
 */
package com.untangle.node.http;

import com.untangle.uvm.logging.LogEvent;

/**
 * Log event for response.
 *
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

    private static String sql =
        "UPDATE reports.http_events " +
        "SET " +
        "s2c_content_length = ?, " +
        "s2c_content_type = ? " +
        "WHERE " +
        "request_id = ? ";
    
    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setInt(++i, getContentLength());
        pstmt.setString(++i, getContentType());
        pstmt.setLong(++i, getRequestLine().getRequestId());

        return pstmt;
    }
}
