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

import java.net.InetAddress;
import java.net.URI;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.http.HttpStateMachine;
import com.untangle.tran.http.RequestLineToken;
import com.untangle.tran.http.StatusLine;
import com.untangle.tran.token.Chunk;
import com.untangle.tran.token.Header;
import com.untangle.tran.token.Token;
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
        URI uri = getRequestLine().getRequestUri();

        // XXX this code should be factored out
        String host = uri.getHost();
        if (null == host) {
            host = requestHeader.getValue("host");
            if (null == host) {
                InetAddress clientIp = getSession().clientAddr();
                host = clientIp.getHostAddress();
            }
        }
        host = host.toLowerCase();

        // XXX yuck
        UrlDatabaseResult result;
        if (transform.isWhitelistedDomain(host, getSession().clientAddr())) {
            result = null;
        } else {
            result = transform.getUrlDatabase()
                .search(getSession(), uri, requestHeader);
        }

        if (null != result) {
            // XXX fire off event
            if (result.blacklisted()) {
                InetAddress clientIp = getSession().clientAddr();

                ClamPhishBlockDetails bd = new ClamPhishBlockDetails
                    (host, uri.toString(), clientIp);

                Token[] r = transform.generateResponse(bd, getSession(),
                                                       isRequestPersistent());

                blockRequest(r);
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
