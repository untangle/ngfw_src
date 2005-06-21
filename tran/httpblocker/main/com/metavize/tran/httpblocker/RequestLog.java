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

import com.metavize.mvvm.tran.PipelineInfo;
import com.metavize.tran.http.HttpRequestEvent;
import com.metavize.tran.http.HttpResponseEvent;

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

    public HttpRequestEvent getHttpRequestEvent()
    {
        return httpRequestEvent;
    }

    public HttpResponseEvent getHttpResponseEvent()
    {
        return httpResponseEvent;
    }

    public PipelineInfo getPipelineInfo()
    {
        return pipelineInfo;
    }

    public HttpBlockerEvent getHttpBlockerEvent()
    {
        return httpBlockerEvent;
    }
}
