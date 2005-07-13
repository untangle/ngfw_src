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
import java.util.Date;

import com.metavize.mvvm.tran.Direction;

public class HttpRequestLog implements Serializable
{
    private static final long serialVersionUID = -4671603809489005943L;

    private final Date timeStamp;
    private final String url;
    private final Action action;
    private final Reason reason;
    private final String category;
    private final String contentType;
    private final int contentLength;
    private final String clientAddr;
    private final int clientPort;
    private final String serverAddr;
    private final int serverPort;
    private final Direction direction;

    public HttpRequestLog(Date timeStamp, String host, String uri,
                          String actionStr, String reasonStr, String category,
                          String contentType, int contentLength,
                          String clientAddr, int clientPort, String serverAddr,
                          int serverPort, Direction direction)
    {
        this.timeStamp = timeStamp;
        this.url = "http://" + host + uri;
        this.action = null == actionStr ? Action.PASS
            : Action.getInstance(actionStr.charAt(0));
        this.reason = null == reasonStr ? Reason.DEFAULT
            : Reason.getInstance(reasonStr.charAt(0));
        this.category = category;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.direction = direction;
    }

    // accessors --------------------------------------------------------------

    public Date timeStamp()
    {
        return timeStamp;
    }

    public String getUrl()
    {
        return url;
    }

    public Action getAction()
    {
        return action;
    }

    public Reason getReason()
    {
        return reason;
    }

    public String getCategory()
    {
        return category;
    }

    public String getContentType()
    {
        return contentType;
    }

    public int getContentLength()
    {
        return contentLength;
    }

    public String getClientAddr()
    {
        return clientAddr;
    }

    public int getCClientPort()
    {
        return clientPort;
    }

    public String getServerAddr()
    {
        return serverAddr;
    }

    public int getSServerPort()
    {
        return serverPort;
    }

    public Direction getDirection()
    {
        return direction;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "time: " + timeStamp + " url: " + url +
            " action: " + action + " reason: " + reason
            + " category: " + category;
    }
}
