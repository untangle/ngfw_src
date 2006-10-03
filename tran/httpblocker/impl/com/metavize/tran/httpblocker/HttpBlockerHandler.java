/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.httpblocker;

import java.nio.ByteBuffer;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.http.HttpStateMachine;
import com.metavize.tran.http.RequestLineToken;
import com.metavize.tran.http.StatusLine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Header;
import com.metavize.tran.token.Token;
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

        BlockDetails c2sReplacement = transform.getBlacklist()
            .checkRequest(getSession().clientAddr(), getRequestLine(),
                          requestHeader);
        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader
                         + "check request returns: " + c2sReplacement);
        }

        if (null == c2sReplacement) {
            releaseRequest();
        } else {
            transform.incrementCount(BLOCK, 1);
            blockRequest(generateResponse(c2sReplacement, isRequestPersistent()));
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
            BlockDetails s2cReplacement = transform.getBlacklist()
                .checkResponse(getSession().clientAddr(), getResponseRequest(),
                               responseHeader);
            if (logger.isDebugEnabled()) {
                logger.debug("in doResponseHeader: " + responseHeader
                             + "checkResponse returns: " + s2cReplacement);
            }

            if (null == s2cReplacement) {
                transform.incrementCount(PASS, 1);

                releaseResponse();
            } else {
                transform.incrementCount(BLOCK, 1);
                blockResponse(generateResponse(s2cReplacement,
                                               isResponsePersistent()));
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

    // private methods --------------------------------------------------------

    private Token[] generateResponse(BlockDetails details, boolean persistent)
    {
        String hostname = MvvmContextFactory.context().networkManager()
            .getPublicAddress();
        String blockPageUrl = "http://" + hostname
            + "/httpblocker/blockpage.jsp?nonce=" + details.getNonce()
            + "&tid=" + transform.getTid();

        Token response[] = new Token[4];

        StatusLine sl = new StatusLine("HTTP/1.1", 307, "Temporary Redirect");
        response[0] = sl;

        Header h = new Header();
        h.addField("Location", blockPageUrl);
        h.addField("Content-Type", "text/plain");
        h.addField("Content-Length", "0");
        h.addField("Connection", persistent ? "Keep-Alive" : "Close");
        response[1] = h;

        response[2] = Chunk.EMPTY;

        response[3] = EndMarker.MARKER;

        return response;
    }
}
