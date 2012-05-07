/**
 * $Id$
 */
package com.untangle.node.phish;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
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
     *
     * @return the action.
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

    @Override
    public String getDirectEventSql()
    {
        String sql =
            "UPDATE reports.n_http_events " +
            "SET " +
            "phish_action = " + "'" + getAction().getKey() + "'" + " " +
            "WHERE " +
            "request_id = " + getRequestId() +
            ";";
        return sql;
    }

    public void appendSyslog(SyslogBuilder sb)
    {
        requestLine.getSessionEvent().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("url", requestLine.getUrl().toString());
        sb.addField("action", null == action ? "none" : action.getName());
        sb.addField("category", null == category ? "none" : category);
    }

    public String getSyslogId()
    {
        return "Block";
    }

    public SyslogPriority getSyslogPriority()
    {
        switch(getActionType())
            {
            case PASSED:
                // statistics or normal operation
                return SyslogPriority.INFORMATIONAL;

            default:
            case BLOCKED:
                return SyslogPriority.WARNING; // traffic altered
            }
    }

    public String toString()
    {
        return "PhishHttpEvent id: " + getRequestId() + " RequestLine: " + requestLine;
    }
}
