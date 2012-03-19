/**
 * $Id$
 */
package com.untangle.node.phish;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Column;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.node.http.RequestLine;
import org.hibernate.annotations.Type;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_phish_http_evt", schema="events")
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

    @Column(name="request_id")
    public Long getRequestId()
    {
        return requestId;
    }

    public void setRequestId(Long requestId)
    {
        this.requestId = requestId;
    }

    /**
     * The action taken.
     *
     * @return the action.
     */
    @Type(type="com.untangle.node.phish.ActionUserType")
    public Action getAction()
    {
        return action;
    }

    public void setAction(Action action)
    {
        this.action = action;
    }

    /**
     * A string associated with the block reason.
     */
    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    @Transient
    public int getActionType()
    {
        if (null == action || Action.PASS_KEY == action.getKey()) {
            return PASSED;
        } else {
            return BLOCKED;
        }
    }

    public void appendSyslog(SyslogBuilder sb)
    {
        requestLine.getSessionEvent().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("url", requestLine.getUrl().toString());
        sb.addField("action", null == action ? "none" : action.getName());
        sb.addField("category", null == category ? "none" : category);
    }

    @Transient
    public String getSyslogId()
    {
        return "Block";
    }

    @Transient
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
        return "PhishHttpEvent id: " + getId() + " RequestLine: " + requestLine;
    }
}
