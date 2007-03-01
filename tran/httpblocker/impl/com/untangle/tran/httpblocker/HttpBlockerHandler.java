/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.httpblocker;


import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.http.HttpStateMachine;
import com.untangle.tran.http.RequestLineToken;
import com.untangle.tran.http.StatusLine;
import com.untangle.tran.token.Chunk;
import com.untangle.tran.token.Header;
import com.untangle.tran.token.Token;
import org.apache.log4j.Logger;

public class HttpBlockerHandler extends HttpStateMachine
{
    private static final int SCAN = Transform.GENERIC_0_COUNTER;
    private static final int BLOCK = Transform.GENERIC_1_COUNTER;
    private static final int PASS = Transform.GENERIC_2_COUNTER;

    private final Logger logger = Logger.getLogger(getClass());

    private final HttpBlockerImpl transform;

    // constructors -----------------------------------------------------------

    HttpBlockerHandler(TCPSession session, HttpBlockerImpl transform)
    {
        super(session);

        this.transform = transform;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine)
    {
        return requestLine;
    }

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        transform.incrementCount(SCAN, 1);

        TCPSession sess = getSession();

        String nonce = transform.getBlacklist()
            .checkRequest(sess.clientAddr(), getRequestLine(), requestHeader);
        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader
                         + "check request returns: " + nonce);
        }

        if (null == nonce) {
            releaseRequest();
        } else {
            transform.incrementCount(BLOCK, 1);
            boolean p = isRequestPersistent();
            String uri = getRequestLine().getRequestUri().toString();
            Token[] response = transform.generateResponse(nonce, sess, uri,
                                                          requestHeader, p);
            blockRequest(response);
        }

        return requestHeader;
    }

    @Override
    protected Chunk doRequestBody(Chunk c)
    {
        return c;
    }

    @Override
    protected void doRequestBodyEnd() { }

    @Override
    protected StatusLine doStatusLine(StatusLine statusLine)
    {
        return statusLine;
    }

    @Override
    protected Header doResponseHeader(Header responseHeader)
    {
        if (100 == getStatusLine().getStatusCode()) {
            releaseResponse();
        } else {
            TCPSession sess = getSession();

            String nonce = transform.getBlacklist()
                .checkResponse(sess.clientAddr(), getResponseRequest(),
                               responseHeader);
            if (logger.isDebugEnabled()) {
                logger.debug("in doResponseHeader: " + responseHeader
                             + "checkResponse returns: " + nonce);
            }

            if (null == nonce) {
                transform.incrementCount(PASS, 1);

                releaseResponse();
            } else {
                transform.incrementCount(BLOCK, 1);
                boolean p = isResponsePersistent();
                Token[] response = transform.generateResponse(nonce, sess, p);
                blockResponse(response);
            }
        }

        return responseHeader;
    }

    @Override
    protected Chunk doResponseBody(Chunk c)
    {
        return c;
    }

    @Override
    protected void doResponseBodyEnd() { }
}
