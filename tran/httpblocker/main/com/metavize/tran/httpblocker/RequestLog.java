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

public class RequestLog implements Serializable
{
    private final Long requestEventId;
    private final Long blockEventId;
    private final Date timeStamp;
    private final String url;
    private final Reason reason;
    private final String category;
    private final String contentType;
    private final int contentLength;
    private final String clientAddr;
    private final int clientPort;
    private final String serverAddr;
    private final int serverPort;

    public RequestLog(long requestEventId, long blockEventId, Date timeStamp,
                      String host, String uri, String reasonStr,
                      String category, String contentType, int contentLength,
                      String clientAddr, int clientPort,
                      String serverAddr, int serverPort)
    {
        this.requestEventId = requestEventId;
        this.blockEventId = blockEventId;
        this.timeStamp = timeStamp;
        this.url = "http://" + host + "/" + uri;
        this.reason = null == reasonStr ? null
            : Reason.getInstance(reasonStr.charAt(0));
        this.category = category;
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
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

    // package protected ------------------------------------------------------

    long getRequestEventId()
    {
        return requestEventId;
    }

    long getBlockEventId()
    {
        return blockEventId;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "time: " + timeStamp + " url: " + url + " reason: "
            + reason + " category: " + category;
    }
}
