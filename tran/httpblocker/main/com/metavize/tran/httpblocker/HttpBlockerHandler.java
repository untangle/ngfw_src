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

package com.metavize.tran.httpblocker;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.http.HttpStateMachine;
import com.metavize.tran.http.RequestLine;
import com.metavize.tran.http.StatusLine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Header;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenResult;
import org.apache.log4j.Logger;

public class HttpBlockerHandler extends HttpStateMachine
{
    private static final int SCAN = Transform.GENERIC_0_COUNTER;
    private static final int BLOCK = Transform.GENERIC_1_COUNTER;
    private static final int PASS = Transform.GENERIC_2_COUNTER;

    private static final Logger logger = Logger
        .getLogger(HttpBlockerHandler.class);

    private final HttpBlockerImpl transform;

    /* Holds pipelined responses XXX factor out*/
    private List responseQueue = new LinkedList();
    private List hostQueue = new LinkedList();
    private List requests = new LinkedList();

    private RequestLine requestLine;
    private StatusLine statusLine;

    private RequestLine responseRequest;

    private String c2sReplacement;
    private String s2cReplacement;

    private boolean c2sPersistent;
    private boolean s2cPersistent;

    // constructors -----------------------------------------------------------

    HttpBlockerHandler(TCPSession session, HttpBlockerImpl transform)
    {
        super(session);

        this.transform = transform;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
        protected TokenResult doRequestLine(RequestLine requestLine)
    {
        logger.debug("in doRequestLine: " + requestLine);

        this.requestLine = requestLine;
        requests.add(requestLine);

        return TokenResult.NONE;
    }

    @Override
        protected TokenResult doRequestHeader(Header requestHeader)
    {
        logger.debug("in doRequestHeader(): " + requestHeader);

        c2sPersistent = isPersistent(requestHeader);

        transform.incrementCount(SCAN, 1);
        c2sReplacement = transform.getBlacklist()
            .checkRequest(getSession().clientAddr(),
                          requestLine, requestHeader);
        logger.debug("check request returns: " + c2sReplacement);

        if (null == c2sReplacement) {
            responseQueue.add(null);
            Token[] c2s = new Token[] { requestLine, requestHeader };
            return new TokenResult(null, c2s);
        } else {
            transform.incrementCount(BLOCK, 1);
            requests.remove(requests.size() - 1); // dequeue request line
            return TokenResult.NONE;
        }
    }

    @Override
        protected TokenResult doRequestBody(Chunk c)
    {
        logger.debug("in doRequestBody(): " + c);

        if (null == c2sReplacement) {
            return new TokenResult(null, new Token[] { c });
        } else { /* blocking */
            return TokenResult.NONE;
        }
    }

    @Override
        protected TokenResult doRequestBodyEnd(EndMarker endMarker)
    {
        logger.debug("in doRequestBodyEnd: " + endMarker);

        if (null == c2sReplacement) {
            return new TokenResult(null, new Token[] { endMarker });
        } else { /* blocking */
            if (0 == responseQueue.size()) {
                List l = generateResponse(c2sReplacement, c2sPersistent);
                Token[] resp = (Token[])l.toArray(new Token[l.size()]);
                return new TokenResult(resp, null);
            } else {
                responseQueue.add(generateResponse(c2sReplacement,
                                                   c2sPersistent));
                return TokenResult.NONE;
            }
        }
    }

    @Override
        protected TokenResult doStatusLine(StatusLine statusLine)
    {
        logger.debug("in doStatusLine: " + statusLine);

        if (100 != statusLine.getStatusCode()) {
            responseRequest = (RequestLine)requests.remove(0);
        }

        this.statusLine = statusLine;

        return TokenResult.NONE;
    }

    @Override
        protected TokenResult doResponseHeader(Header responseHeader)
    {
        logger.debug("in doResponseHeader: " + responseHeader);

        s2cReplacement = transform.getBlacklist()
            .checkResponse(getSession().clientAddr(), responseRequest,
                           responseHeader);
        logger.debug("chekResponse returns: " + s2cReplacement);

        if (null == s2cReplacement) {
            transform.incrementCount(PASS, 1);

            Token[] response = new Token[] { statusLine, responseHeader };

            return new TokenResult(response, null);
        } else { /* block */
            s2cPersistent = isPersistent(responseHeader);
            transform.incrementCount(BLOCK, 1);

            return TokenResult.NONE;
        }
    }

    @Override
        protected TokenResult doResponseBody(Chunk c)
    {
        logger.debug("in doResponseBody: " + c);

        if (null == s2cReplacement) {
            return new TokenResult(new Token[] { c }, null);
        } else { /* block */
            return TokenResult.NONE;
        }
    }

    @Override
        protected TokenResult doResponseBodyEnd(EndMarker em)
    {
        logger.debug("in doResponseBodyEnd: " + em);

        if (100 == statusLine.getStatusCode()) {
            return new TokenResult(new Token[] { em }, null);
        } else {
            List l = new LinkedList();

            if (null == s2cReplacement) {
                l.add(em);
                dequeueDeferred(l);
            } else { /* block */
                l = generateResponse(s2cReplacement, s2cPersistent);
                dequeueDeferred(l);
            }

            Token[] r = (Token[])l.toArray(new Token[l.size()]);
            return new TokenResult(r, null);
        }
    }

    @Override
        public TokenResult releaseFlush()
    {
        // XXX we do not even attempt to deal with outstanding block pages
        if (ClientState.REQ_LINE_STATE == getClientState()) {
            return new TokenResult(null, new Token[] { requestLine });
        } else {
            return TokenResult.NONE;
        }
    }

    // private methods --------------------------------------------------------

    private List generateResponse(String replacement, boolean persistent)
    {
        List response = new LinkedList();

        // XXX make canned responses in constructor
        // XXX Do template replacement
        ByteBuffer buf = ByteBuffer.allocate(replacement.length());
        buf.put(replacement.getBytes()).flip();

        StatusLine sl = new StatusLine("HTTP/1.1", 403, "Forbidden");
        response.add(sl);

        Header h = new Header();
        h.addField("Content-Length", Integer.toString(buf.remaining()));
        h.addField("Content-Type", "text/html");
        h.addField("Connection", persistent ? "Keep-Alive" : "Close");
        response.add(h);

        Chunk c = new Chunk(buf);
        response.add(c);

        response.add(EndMarker.MARKER);

        return response;
    }

    private void dequeueDeferred(List rl)
    {
        // this response:
        Object o = responseQueue.remove(0);
        assert null == o;
        // deferred responses:
        while (0 < responseQueue.size() && null != responseQueue.get(0)) {
            logger.debug("dequeuing deferred response");
            List l = (List)responseQueue.remove(0);
            rl.addAll(l);
        }
    }

    private boolean isPersistent(Header header)
    {
        String con = header.getValue("connection");
        return null == con ? false : con.equalsIgnoreCase("keep-alive");
    }
}
