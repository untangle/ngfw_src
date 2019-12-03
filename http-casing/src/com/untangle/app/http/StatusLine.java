/**
 * $Id$
 */
package com.untangle.app.http;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.Token;

/**
 * Holds a RFC 2616 status-line.
 */
public class StatusLine implements Token
{
    private final String httpVersion;
    private final int statusCode;
    private final String reasonPhrase;

    /**
     * Create a StatusLine
     * @param httpVersion
     * @param statusCode
     * @param reasonPhrase
     */
    public StatusLine(String httpVersion, int statusCode, String reasonPhrase)
    {
        this.httpVersion = httpVersion;
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    /**
     * Get the HTTP version
     * @return string of http version
     */
    public String getHttpVersion()
    {
        return httpVersion;
    }

    /**
     * Get the HTTP status code
     * @return status code
     */
    public int getStatusCode()
    {
        return statusCode;
    }

    /**
     * Get the reason
     * @return string reason
     */
    public String getReasonPhrase()
    {
        return reasonPhrase;
    }

    /**
     * Get the ByteBuffer equivalent of the HeaderToken
     * @return the ByteBuffer
     */
    public String getString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(httpVersion).append(" ").append(statusCode)
            .append(" ").append(reasonPhrase).append("\r\n");
        return sb.toString();
    }

    /**
     * Get the ByteBuffer equivalent of this StatusLine
     * @return the ByteBuffer
     */
    @Override
    public ByteBuffer getBytes()
    {
        byte[] buf = getString().getBytes();

        return ByteBuffer.wrap(buf);
    }

}
