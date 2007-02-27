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

import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.http.HttpStateMachine;
import com.untangle.tran.http.RequestLineToken;
import com.untangle.tran.http.StatusLine;
import com.untangle.tran.token.Chunk;
import com.untangle.tran.token.EndMarker;
import com.untangle.tran.token.Header;
import com.untangle.tran.token.Token;
import org.apache.log4j.Logger;

public class HttpBlockerHandler extends HttpStateMachine
{
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This site blocked because of inappropriate content</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Category: %s</p>"
        + "<p>Please contact %s</p>"
        + "<HR>"
        + "<ADDRESS>Untangle</ADDRESS>"
        + "</BODY></HTML>";

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

        String nonce = transform.getBlacklist()
            .checkRequest(getSession().clientAddr(), getRequestLine(),
                          requestHeader);
        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader
                         + "check request returns: " + nonce);
        }

        if (null == nonce) {
            releaseRequest();
        } else {
            transform.incrementCount(BLOCK, 1);
            blockRequest(generateResponse(nonce, isRequestPersistent()));
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
            String nonce = transform.getBlacklist()
                .checkResponse(getSession().clientAddr(), getResponseRequest(),
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
                blockResponse(generateResponse(nonce, isResponsePersistent()));
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

    private Token[] generateResponse(String nonce, boolean persistent)
    {
        InetAddress addr = MvvmContextFactory.context().networkManager()
            .getInternalHttpAddress(getSession());
        if (null == addr) {
            return generateSimplePage(nonce, persistent);
        } else {
            String host = addr.getHostAddress();
            return generateRedirect(host, nonce, persistent);
        }
    }

    private Token[] generateSimplePage(String nonce, boolean persistent)
    {
        Token response[] = new Token[4];

        BlockDetails details = transform.getDetails(nonce);

        String replacement = String.format(BLOCK_TEMPLATE, details.getHeader(),
                                           details.getHost(), details.getUri(),
                                           details.getReason(),
                                           details.getContact());

        ByteBuffer buf = ByteBuffer.allocate(replacement.length());
        buf.put(replacement.getBytes()).flip();

        StatusLine sl = new StatusLine("HTTP/1.1", 403, "Forbidden");
        response[0] = sl;

        Header h = new Header();
        h.addField("Content-Length", Integer.toString(buf.remaining()));
        h.addField("Content-Type", "text/html");
        h.addField("Connection", persistent ? "Keep-Alive" : "Close");
        response[1] = h;

        Chunk c = new Chunk(buf);
        response[2] = c;

        response[3] = EndMarker.MARKER;

        return response;
    }

    private Token[] generateRedirect(String host, String nonce,
                                     boolean persistent)
    {
        String blockPageUrl = "http://" + host
            + "/httpblocker/blockpage.jsp?nonce=" + nonce
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
