/**
 * $Id$
 */
package com.untangle.app.http;

import java.net.URI;
import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.Token;

/**
 * The in-memory token passed through the pipeline.
 *
 */
public class RequestLineToken implements Token
{
    private static final byte[] SPACE_BYTES = " ".getBytes();
    private static final byte[] CRLF_BYTES = "\r\n".getBytes();

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

    public void setHttpVersion(String httpVersion)
    {
        this.httpVersion = httpVersion;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        byte[][] e = new byte[][]{ requestLine.getMethod().toString().getBytes(),
                                   SPACE_BYTES,
                                   requestLine.getUriBytes(),
                                   SPACE_BYTES,
                                   httpVersion.getBytes(),
                                   CRLF_BYTES };

        int l = 0;
        for (byte[] ba : e) {
            l += ba.length;
        }

        byte[] buf = new byte[l];

        int i = 0;
        for (byte[] ba : e) {
            for (int j = 0; j < ba.length; j++) {
                buf[i++] = ba[j];
            }
        }

        return ByteBuffer.wrap(buf);
    }
}
