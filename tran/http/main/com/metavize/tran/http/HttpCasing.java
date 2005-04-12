/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.http;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractCasing;
import com.metavize.tran.token.Parser;
import com.metavize.tran.token.Unparser;

class HttpCasing extends AbstractCasing
{
    private final HttpParser parser;
    private final HttpUnparser unparser;

    private RequestLine request;

    // constructors -----------------------------------------------------------

    HttpCasing(TCPSession session, boolean clientSide)
    {
        parser = new HttpParser(session, clientSide, this);
        unparser = new HttpUnparser(session, clientSide, this);
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
        this.request = request;
    }

    RequestLine dequeueRequest()
    {
        return request;
    }
}
