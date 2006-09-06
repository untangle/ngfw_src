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

import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;
import com.metavize.mvvm.tran.PipelineEndpoints;
import com.metavize.tran.http.RequestLine;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="TR_SPYWARE_EVT_COOKIE"
 * mutable="false"
 */
public class SpywareCookieEvent extends SpywareEvent
{
    private String identification;
    private RequestLine requestLine; // pipeline endpoints & location
    private boolean toServer;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpywareCookieEvent() { }

    public SpywareCookieEvent(RequestLine requestLine,
                              String identification,
                              boolean toServer)
    {
        this.identification = identification;
        this.requestLine = requestLine;
        this.toServer = toServer;
    }

    // SpywareEvent methods ---------------------------------------------------

    public String getType()
    {
        return "Cookie";
    }

    public String getReason()
    {
        return "in Cookie List";
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
     * The identification (name of IP address range matched)
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

    /**
     * Whether or not the cookie is to the server (otherwise to the client)
     *
     * @return if true the cookie was zeroed going to the server,
     * otherwise it was removed going to the client
     * @hibernate.property
     * column="TO_SERVER"
     */
    public boolean isToServer()
    {
        return toServer;
    }

    public void setToServer(boolean toServer)
    {
        this.toServer = toServer;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getPipelineEndpoints().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("info", getIdentification());
        sb.addField("loc", getLocation());
        sb.addField("blocked", isBlocked());
        sb.addField("toServer", isToServer());
    }

    // use SpywareEvent getSyslogId and getSyslogPriority
}
