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
import com.metavize.tran.token.Tokenizer;
import com.metavize.tran.token.Untokenizer;

class HttpCasing extends AbstractCasing
{
    private final HttpTokenizer tokenizer;
    private final HttpUntokenizer untokenizer;

    private RequestLine request;

    // constructors -----------------------------------------------------------

    HttpCasing(TCPSession session, boolean clientSide)
    {
        tokenizer = new HttpTokenizer(session, clientSide, this);
        untokenizer = new HttpUntokenizer(session, clientSide, this);
    }

    // Casing methods ---------------------------------------------------------

    public Untokenizer untokenizer()
    {
        return untokenizer;
    }

    public Tokenizer tokenizer()
    {
        return tokenizer;
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
