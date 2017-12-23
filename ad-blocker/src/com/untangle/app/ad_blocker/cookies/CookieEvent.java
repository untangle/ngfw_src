/**
 * $Id$
 */
package com.untangle.app.ad_blocker.cookies;

import com.untangle.uvm.app.SessionEvent;
import com.untangle.app.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for a blocked cookie
 */
@SuppressWarnings("serial")
public class CookieEvent extends LogEvent
{
    private RequestLine requestLine;
    private Long requestId;
    private String identification;

    public CookieEvent() { }

    public CookieEvent(RequestLine requestLine, String identification)
    {
        this.requestLine = requestLine;
        this.identification = identification;
        this.requestLine = requestLine;
    }

    public SessionEvent getSessionEvent()
    {
        return requestLine.getSessionEvent();
    }

    public Long getRequestId() { return requestId; }
    public void setRequestId( Long requestId ) { this.requestId = requestId; }

    public String getIdentification() { return identification; }
    public void setIdentification( String identification ) { this.identification = identification; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "http_events" + requestLine.getHttpRequestEvent().getPartitionTablePostfix() + " " +
            "SET " +
            "ad_blocker_cookie_ident = ? " +
            "WHERE " +
            "request_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        
        
        int i = 0;
        pstmt.setString(++i, getIdentification());
        pstmt.setLong(++i, requestLine.getRequestId());
        
        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "Ad Blocker" + " " + I18nUtil.marktr("blocked") + " " + I18nUtil.marktr("cookie") + " " +
            getIdentification() + " " + I18nUtil.marktr("on") + " " + requestLine.getUrl();
        return summary;
    }
}
