/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.http;

import java.net.URI;
import java.nio.ByteBuffer;

import com.untangle.node.token.Token;

/**
 * The in-memory token passed through the pipeline.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class RequestLineToken implements Token
{
    private RequestLine requestLine;
    private String httpVersion;

    public RequestLineToken(RequestLine requestLine, String httpVersion)
    {
        this.requestLine = requestLine;
        this.httpVersion = httpVersion;
    }

    public RequestLine getRequestLine()
    {
        return requestLine;
    }

    public void setRequestLine(RequestLine requestLine)
    {
        this.requestLine = requestLine;
    }

    public HttpMethod getMethod()
    {
        return requestLine.getMethod();
    }

    public void setMethod(HttpMethod httpMethod)
    {
        requestLine.setMethod(httpMethod);
    }

    public URI getRequestUri()
    {
        return requestLine.getRequestUri();
    }

    public void setRequestUri(URI uri)
    {
        requestLine.setRequestUri(uri);
    }

    public String getHttpVersion()
    {
        return httpVersion;
    }

    public void setHttpVersion()
    {
        this.httpVersion = httpVersion;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMethod()).append(" ").append(getRequestUri().toString())
            .append(" ").append(httpVersion).append("\r\n");
        byte[] buf = sb.toString().getBytes();

        return ByteBuffer.wrap(buf);
    }

    public int getEstimatedSize()
    {
        return requestLine.getMethod().toString().length()
            + requestLine.getUrl().toString().length()
            + httpVersion.toString().length();
    }
}
