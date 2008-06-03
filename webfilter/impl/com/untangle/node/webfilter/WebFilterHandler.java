/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.webfilter;

import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.TCPSession;
import org.apache.log4j.Logger;

/**
 * Blocks HTTP traffic that is on an active block list.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
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
        //node.incrementCount(SCAN, 1);

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
            //node.incrementCount(BLOCK, 1);
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
                //node.incrementCount(PASS, 1);

                releaseResponse();
            } else {
                //node.incrementCount(BLOCK, 1);
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
