/**
 * $Id$
 */
package com.untangle.node.ad_blocker;

import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for a blocked request.
 */
@SuppressWarnings("serial")
public class AdBlockerEvent extends LogEvent
{
    private Long requestId;
    private RequestLine requestLine;
    private Action action;
    private String reason;

    public AdBlockerEvent()
    { }

    public AdBlockerEvent(Action action, String reason, RequestLine requestLine)
    {
        super();
        this.action = action;
        this.reason = reason;
        this.requestId = requestLine.getRequestId();
        this.requestLine = requestLine;
    }

    public Long getRequestId() { return requestId; }
    public void setRequestId( Long requestId ) { this.requestId = requestId; }

    public Action getAction() { return action; }
    public void setAction( Action action ) { this.action = action; }

    public String getReason() { return reason; }
    public void setReason( String reason ) { this.reason = reason; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "http_events" + requestLine.getHttpRequestEvent().getPartitionTablePostfix() + " " +
            "SET " +
            "ad_blocker_action = ? " +
            "WHERE " +
            "request_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        
        
        int i = 0;
        pstmt.setString(++i, String.valueOf(getAction().getKey()));
        pstmt.setLong(++i, getRequestId());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        switch (getAction().getKey()) {
        case 'P': action = I18nUtil.marktr("passed"); break;
        case 'B': action = I18nUtil.marktr("blocked"); break;
        default: action = I18nUtil.marktr("scanned"); break;
        }

        String summary = "Ad Blocker" + " " + action + " " + requestLine.getUrl();
        return summary;
    }
}
