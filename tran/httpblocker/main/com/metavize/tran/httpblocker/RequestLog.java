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

package com.metavize.tran.httpblocker;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import com.metavize.mvvm.tran.PipelineInfo;
import com.metavize.tran.http.HttpRequestEvent;
import com.metavize.tran.http.HttpResponseEvent;
import org.apache.log4j.Logger;

public class RequestLog implements Serializable
{
    private final HttpBlockerEvent httpBlockerEvent;
    private final HttpRequestEvent httpRequestEvent;
    private final HttpResponseEvent httpResponseEvent;
    private final PipelineInfo pipelineInfo;

    public RequestLog(HttpBlockerEvent httpBlockerEvent,
                      HttpRequestEvent httpRequestEvent,
                      HttpResponseEvent httpResponseEvent,
                      PipelineInfo pipelineInfo)
    {
        this.httpBlockerEvent = httpBlockerEvent;
        this.httpRequestEvent = httpRequestEvent;
        this.httpResponseEvent = httpResponseEvent;
        this.pipelineInfo = pipelineInfo;
    }

    // accessors --------------------------------------------------------------

    public Date timeStamp()
    {
        return httpRequestEvent.getTimeStamp();
    }

    public String getUrl()
    {
        String h = httpRequestEvent.getHost();
        URI u = httpRequestEvent.getRequestLine().getRequestUri();

        try {
            URI host = new URI("http://" + h);
            return host.relativize(u).toString();
        } catch (URISyntaxException exn) {
            Logger l = Logger.getLogger(RequestLog.class);
            l.warn("could not create host URI: " + u);
            return "http://" + h + "/" + u;
        }
    }

    public Reason getReason()
    {
        return null == httpBlockerEvent ? null : httpBlockerEvent.getReason();
    }

    public String getCategory()
    {
        return null == httpBlockerEvent ? null : httpBlockerEvent.getCategory();
    }

    public String getContentType()
    {
        return null == httpResponseEvent ? null : httpResponseEvent.getContentType();
    }

    public int getContentLength()
    {
        return null == httpResponseEvent ? null : httpResponseEvent.getContentLength();
    }

    public InetAddress getClientAddr()
    {
        return pipelineInfo.getCClientAddr();
    }

    public int getCClientPort()
    {
        return pipelineInfo.getCClientPort();
    }

    public InetAddress getServerAddr()
    {
        return pipelineInfo.getSServerAddr();
    }

    public int getSServerPort()
    {
        return pipelineInfo.getSServerPort();
    }

    // package protected ------------------------------------------------------

    long getRequestEventId()
    {
        return httpRequestEvent.getId();
    }

    long getBlockEventId()
    {
        return httpBlockerEvent.getId();
    }
}
