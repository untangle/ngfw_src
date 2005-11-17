/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spyware;

import com.metavize.tran.http.RequestLine;
import com.metavize.mvvm.tran.PipelineEndpoints;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="TR_SPYWARE_EVT_ACTIVEX"
 * mutable="false"
 */
public class SpywareActiveXEvent extends SpywareEvent
{
    private String identification;
    private RequestLine requestLine;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpywareActiveXEvent() { }

    public SpywareActiveXEvent(RequestLine requestLine,
                               String identification)
    {
        this.identification = identification;
        this.requestLine = requestLine;
    }

    // SpywareEvent methods ---------------------------------------------------

    public String getReason()
    {
        return "in ActiveX List";
    }

    public String getLocation()
    {
        return requestLine.getUrl().toString();
    }

    public boolean isBlocked()
    {
        return true;
    }

    public PipelineEndpoints getPipelineEndpoints()
    {
        return requestLine.getPipelineEndpoints();
    }

    // accessors --------------------------------------------------------------

    /**
     * Request line for this HTTP response pair.
     *
     * @return the request line.
     * @hibernate.many-to-one
     * column="REQUEST_ID"
     * cascade="save-update"
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
     * The identification (ActiveX class ID matched)
     *
     * @return the protocl name.
     * @hibernate.property
     * column="IDENT"
     */
    public String getIdentification()
    {
        return identification;
    }

    public void setIdentification(String identification)
    {
        this.identification = identification;
    }
}
