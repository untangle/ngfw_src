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

package com.untangle.node.phish;

import java.net.InetAddress;
import java.net.URI;

import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.util.UrlDatabaseResult;
import com.untangle.uvm.vnet.TCPSession;

public class PhishHttpHandler extends HttpStateMachine
{
    private final PhishNode node;

    // constructors -----------------------------------------------------------

    PhishHttpHandler(TCPSession session, PhishNode node)
    {
        super(session);

        this.node = node;
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
        //node.incrementScanCount();
        
        RequestLineToken rlToken = getRequestLine();
        URI uri = rlToken.getRequestUri();

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

        UrlDatabaseResult result;
        if (!node.getPhishSettings().getEnableGooglePhishList()
            || node.isWhitelistedDomain(host, getSession().clientAddr())) {
            result = null;
        } else {
            result = node.getUrlDatabase()
                .search(getSession(), uri, requestHeader);
        }

        if (null != result) {
            if (result.blacklisted()) {
                node.incrementBlockCount();
                
                // XXX change this category value
                node.logHttp(new PhishHttpEvent(rlToken.getRequestLine(), Action.BLOCK, "Google Safe Browsing"));

                InetAddress clientIp = getSession().clientAddr();

                PhishBlockDetails bd = new PhishBlockDetails
                    (host, uri.toString(), clientIp);

                Token[] r = node.generateResponse(bd, getSession(),
                                                       isRequestPersistent());

                blockRequest(r);
                return requestHeader;
            }
        }
        
        node.incrementPassCount();
        
        releaseRequest();
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
