/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpCasing.java,v 1.7 2005/03/17 02:47:47 amread Exp $
 */

package com.metavize.tran.http;

import com.metavize.mvvm.tapi.event.TCPSessionEvent;
import com.metavize.tran.token.AbstractCasing;
import com.metavize.tran.token.Parser;
import com.metavize.tran.token.Unparser;
import org.apache.log4j.Logger;

class HttpCasing extends AbstractCasing
{
    private final HttpParser parser;
    private final HttpUnparser unparser;
    private final String insideStr;
    private final Logger logger = Logger.getLogger(HttpCasing.class);

    private RequestLine request;

    // constructors -----------------------------------------------------------

    HttpCasing(boolean inside)
    {
        insideStr = inside ? "inside " : "outside";
        parser = new HttpParser(this);
        unparser = new HttpUnparser(this);
    }

    // Casing methods ---------------------------------------------------------

    public Unparser unparser()
    {
        return unparser;
    }

    public Parser parser()
    {
        return parser;
    }

    // package private methods ------------------------------------------------

    void queueRequest(RequestLine request)
    {
        if (logger.isDebugEnabled())
            logger.debug(insideStr + " set request: " + request);
        this.request = request;
    }

    RequestLine dequeueRequest()
    {
        if (logger.isDebugEnabled())
            logger.debug(insideStr + " got request: " + request);
        return request;
    }

    // private methods --------------------------------------------------------

    private String sessStr(TCPSessionEvent e)
    {
        return insideStr + e.session().id() + " ";
    }
}
