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

package com.untangle.tran.spyware;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.tran.PipelineEndpoints;
import com.untangle.tran.http.RequestLine;
import javax.persistence.Entity;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="tr_spyware_evt_activex", schema="events")
public class SpywareActiveXEvent extends SpywareEvent
{
    private String identification;
    private RequestLine requestLine; // pipeline endpoints & location

    // constructors -----------------------------------------------------------

    public SpywareActiveXEvent() { }

    public SpywareActiveXEvent(RequestLine requestLine,
                               String identification)
    {
        this.identification = identification;
        this.requestLine = requestLine;
    }

    // SpywareEvent methods ---------------------------------------------------

    @Transient
    public String getType()
    {
        return "ActiveX";
    }

    @Transient
    public String getReason()
    {
        return "in ActiveX List";
    }

    @Transient
    public String getLocation()
    {
        return requestLine.getUrl().toString();
    }

    @Transient
    public boolean isBlocked()
    {
        return true;
    }

    @Transient
    public PipelineEndpoints getPipelineEndpoints()
    {
        return requestLine.getPipelineEndpoints();
    }

    // accessors --------------------------------------------------------------

    /**
     * Request line for this HTTP response pair.
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
     * The identification (ActiveX class ID matched)
     *
     * @return the protocl name.
     */
    @Column(name="ident")
    public String getIdentification()
    {
        return identification;
    }

    public void setIdentification(String identification)
    {
        this.identification = identification;
    }

    // Syslog methods ---------------------------------------------------------

    // use SpywareEvent appendSyslog, getSyslogId and getSyslogPriority
}
