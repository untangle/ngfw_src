/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareHttpHandler.java 8668 2007-01-29 19:17:09Z amread $
 */

package com.untangle.tran.clamphish;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.http.HttpStateMachine;
import com.untangle.tran.http.RequestLineToken;
import com.untangle.tran.http.StatusLine;
import com.untangle.tran.token.Chunk;
import com.untangle.tran.token.Header;
import com.untangle.tran.util.UrlDatabaseResult;

public class PhishHttpHandler extends HttpStateMachine
{
    private final ClamPhishTransform transform;


    // constructors -----------------------------------------------------------

    PhishHttpHandler(TCPSession session, ClamPhishTransform transform)
    {
        super(session);

        this.transform = transform;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine)
    {
        String path = requestLine.getRequestUri().getPath();

        return requestLine;
    }

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        UrlDatabaseResult result = transform.getUrlDatabase()
            .search(getSession(), getRequestLine().getRequestUri(),
                    requestHeader);

        if (null != result) {
            // XXX fire off event
            if (result.blacklisted()) {
//                 Token[] r = transform.generateResponse(bd, getSession(), uri,
//                                                        isRequestPersistent());
//                 blockRequest(r);
            }
        }
        return requestHeader;

    }

    @Override
    protected Chunk doRequestBody(Chunk chunk)
    {
        return chunk;
    }

    @Override
    protected void doRequestBodyEnd() { }

    @Override
    protected StatusLine doStatusLine(StatusLine statusLine)
    {
        releaseResponse();
        return statusLine;
    }

    @Override
    protected Header doResponseHeader(Header header)
    {
        return header;
    }

    @Override
    protected Chunk doResponseBody(Chunk chunk)
    {
        return chunk;
    }

    @Override
    protected void doResponseBodyEnd()
    {
    }
}
