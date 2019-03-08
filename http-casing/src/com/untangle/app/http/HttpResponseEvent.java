/**
 * $Id$
 */
package com.untangle.app.http;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for response.
 * This modifies the http_events table with all the metadata in the response
 */
@SuppressWarnings("serial")
public class HttpResponseEvent extends LogEvent
{
    private RequestLine requestLine;
    private String contentType;
    private String contentFilename;
    private long contentLength;

    /**
     * Create an empty HttpResponseEvent.
     */
    public HttpResponseEvent() { }

    /**
     * Create an HttpResponseEvent.
     * @param requestLine
     * @param contentType
     * @param contentFilename
     * @param contentLength
     */
    public HttpResponseEvent(RequestLine requestLine, String contentType, String contentFilename, long contentLength)
    {
        this.requestLine = requestLine;
        this.contentType = contentType;
        this.contentFilename = contentFilename;
        this.contentLength = contentLength;
    }

    /**
     * getRequestLine.
     * @return requestLine
     */
    public RequestLine getRequestLine() { return requestLine; }

    /**
     * setRequestLine.
     * @param newValue 
     */
    public void setRequestLine(RequestLine newValue) { this.requestLine = newValue; }

    /**
     * The base Content-Type, without any encodings or other useless
     * nonsense.
     * @return contentType
     */
    public String getContentType() { return contentType; }

    /**
     * setContentType.
     * @param newValue 
     */
    public void setContentType(String newValue) { this.contentType = contentType; }

    /**
     * The filename as specified in the content-dispition if specified
     * @return contentFilename
     */
    public String getContentFilename() { return contentFilename; }

    /**
     * setContentFilename.
     * @param newValue 
     */
    public void setContentFilename(String newValue) { this.contentFilename = contentFilename; }

    /**
     * Content length, as counted by the parser.
     * @return contentLength
     */
    public long getContentLength() { return contentLength; }

    /**
     * setContentLength.
     * @param newValue
     */
    public void setContentLength(long newValue) { this.contentLength = newValue; }

    /**
     * getHttpRequestEvent.
     * @return
     */
    public HttpRequestEvent getHttpRequestEvent()
    {
        if ( requestLine != null )
            return requestLine.getHttpRequestEvent();
        return null;
    }
    
    /**
     * Compile SQL statements
     * @param conn
     * @param statementCache
     * @throws Exception
     */
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "http_events" + requestLine.getHttpRequestEvent().getPartitionTablePostfix() + " " +
            "SET " +
            "s2c_content_length = ?, " +
            "s2c_content_type = ?, " +
            "s2c_content_filename = ? " +
            "WHERE " +
            "request_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setLong(++i, getContentLength());
        pstmt.setString(++i, getContentType());
        pstmt.setString(++i, getContentFilename());
        pstmt.setLong(++i, getRequestLine().getRequestId());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = requestLine.getHttpRequestEvent().getSessionEvent().getCClientAddr().getHostAddress() + " " + I18nUtil.marktr("downloaded") + " " + requestLine.getUrl();
        return summary;
    }

}
