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

    /**
     * Create a RequestLineToken.
     * @param requestLine
     * @param httpVersion
     */
    public RequestLineToken(RequestLine requestLine, String httpVersion)
    {
        this.requestLine = requestLine;
        this.httpVersion = httpVersion;
    }

    /**
     * Get the request line object
     * @return the RequestLine
     */
    public RequestLine getRequestLine()
    {
        return requestLine;
    }

    /**
     * Set the request line object
     * @param requestLine
     */
    public void setRequestLine(RequestLine requestLine)
    {
        this.requestLine = requestLine;
    }

    /**
     * Get the HTTP method of the request
     * @return the HttpMethod
     */
    public HttpMethod getMethod()
    {
        return requestLine.getMethod();
    }

    /**
     * Set the method of the RequestLine
     * @param httpMethod
     */
    public void setMethod(HttpMethod httpMethod)
    {
        requestLine.setMethod(httpMethod);
    }

    /**
     * Get the Request URI of the request
     * @return the URI
     */
    public URI getRequestUri()
    {
        return requestLine.getRequestUri();
    }

    /**
     * Set the Request URI of the request
     * @param uri
     */
    public void setRequestUri(URI uri)
    {
        requestLine.setRequestUri(uri);
    }

    /**
     * Get the HTTP Version
     * @return The HTTP Version
     */
    public String getHttpVersion()
    {
        return httpVersion;
    }

    /**
     * Set the HTTP Version
     * @param httpVersion
     */
    public void setHttpVersion(String httpVersion)
    {
        this.httpVersion = httpVersion;
    }

    /**
     * Get the ByteBuffer equivalent of this RequestLineToken
     * @return ByteBuffer
     */
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
