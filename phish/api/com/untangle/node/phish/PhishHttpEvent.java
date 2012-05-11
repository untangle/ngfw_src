/**
 * $Id$
 */
package com.untangle.node.phish;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.node.http.RequestLine;

/**
 * Log event for a blocked request.
 */
@SuppressWarnings("serial")
public class PhishHttpEvent extends LogEvent
{
    // action types
    private static final int PASSED = 0;
    private static final int BLOCKED = 1;

    private Long requestId;
    private RequestLine requestLine;
    private Action action;
    private String category;

    public PhishHttpEvent() { }

    public PhishHttpEvent(RequestLine requestLine, Action action, String category)
    {
        this.requestId = requestLine.getRequestId();
        this.requestLine = requestLine;
        this.action = action;
        this.category = category;
    }

    public Long getRequestId() { return requestId; }
    public void setRequestId( Long requestId ) { this.requestId = requestId; }

    /**
     * The action taken.
     */
    public Action getAction() { return action; }
    public void setAction( Action action ) { this.action = action; }

    /**
     * A string associated with the block reason.
     */
    public String getCategory() { return category; }
    public void setCategory( String category ) { this.category = category; }

    public int getActionType()
    {
        if (null == action || Action.PASS_KEY == action.getKey()) {
            return PASSED;
        } else {
            return BLOCKED;
        }
    }

    private static String sql =
        "UPDATE reports.n_http_events " +
        "SET " +
        "phish_action = ? " +
        "WHERE " +
        "request_id = ? ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );
        
        int i = 0;
        pstmt.setString(++i, String.valueOf(getAction().getKey()));
        pstmt.setLong(++i, getRequestId());
        return pstmt;
    }
}
