/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.clamphish;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.SyslogBuilder;
import com.untangle.mvvm.logging.SyslogPriority;
import com.untangle.tran.http.RequestLine;
import org.hibernate.annotations.Type;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="tr_phishhttp_evt", schema="events")
public class PhishHttpEvent extends LogEvent
{
    // action types
    private static final int PASSED = 0;
    private static final int BLOCKED = 1;

    private RequestLine requestLine;
    private Action action;
    private String category;

    // non-persistent fields --------------------------------------------------
    private boolean nonEvent = false;

    // constructors -----------------------------------------------------------
    public PhishHttpEvent() { }

    public PhishHttpEvent(RequestLine requestLine, Action action,
                          String category, boolean nonEvent)
    {
        this.requestLine = requestLine;
        this.action = action;
        this.category = category;

        this.nonEvent = nonEvent;
    }

    public PhishHttpEvent(RequestLine requestLine, Action action,
                          String category)
    {
        this.requestLine = requestLine;
        this.action = action;
        this.category = category;
    }

    // public methods ---------------------------------------------------------

    @Transient
    public boolean isNonEvent()
    {
        return nonEvent;
    }

    // accessors --------------------------------------------------------------

    /**
     * Request line for this Phish HTTP response pair.
     *
     * @return the request line.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="request_id")
    public RequestLine getRequestLine()
    {
        return requestLine;
    }

    public void setRequestLine(RequestLine requestLine)
    {
        this.requestLine = requestLine;
    }

    /**
     * The action taken.
     *
     * @return the action.
     */
    @Type(type="com.untangle.tran.clamphish.ActionUserType")
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

    // PhishHttpEvent methods -------------------------------------------------

    @Transient
    private int getActionType()
    {
        if (null == action ||
            Action.PASS_KEY == action.getKey()) {
            return PASSED;
        } else {
            return BLOCKED;
        }
    }

    // LogEvent methods -------------------------------------------------------

    @Transient
    public boolean isPersistent()
    {
        return !nonEvent;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        requestLine.getPipelineEndpoints().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("url", requestLine.getUrl().toString());
        sb.addField("action", null == action ? "none" : action.toString());
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

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "PhishHttpEvent id: " + getId() + " RequestLine: "
            + requestLine;
    }
}
