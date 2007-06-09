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

package com.untangle.node.webfilter;


import com.untangle.uvm.tapi.TCPSession;
import com.untangle.uvm.node.Node;
import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import org.apache.log4j.Logger;

public class WebFilterHandler extends HttpStateMachine
{
    private static final int SCAN = Node.GENERIC_0_COUNTER;
    private static final int BLOCK = Node.GENERIC_1_COUNTER;
    private static final int PASS = Node.GENERIC_2_COUNTER;

    private final Logger logger = Logger.getLogger(getClass());

    private final WebFilterImpl node;

    // constructors -----------------------------------------------------------

    WebFilterHandler(TCPSession session, WebFilterImpl node)
    {
        super(session);

        this.node = node;
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
        node.incrementCount(SCAN, 1);

        TCPSession sess = getSession();

        String nonce = node.getBlacklist()
            .checkRequest(sess.clientAddr(), getRequestLine(), requestHeader);
        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader
                         + "check request returns: " + nonce);
        }

        if (null == nonce) {
            releaseRequest();
        } else {
            node.incrementCount(BLOCK, 1);
            boolean p = isRequestPersistent();
            String uri = getRequestLine().getRequestUri().toString();
            Token[] response = node.generateResponse(nonce, sess, uri,
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

            String nonce = node.getBlacklist()
                .checkResponse(sess.clientAddr(), getResponseRequest(),
                               responseHeader);
            if (logger.isDebugEnabled()) {
                logger.debug("in doResponseHeader: " + responseHeader
                             + "checkResponse returns: " + nonce);
            }

            if (null == nonce) {
                node.incrementCount(PASS, 1);

                releaseResponse();
            } else {
                node.incrementCount(BLOCK, 1);
                boolean p = isResponsePersistent();
                Token[] response = node.generateResponse(nonce, sess, p);
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
