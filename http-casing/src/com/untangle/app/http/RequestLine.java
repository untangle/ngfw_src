/**
 * $Id$
 */
package com.untangle.app.http;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.untangle.uvm.util.UriUtil;
import com.untangle.uvm.app.SessionEvent;

/**
 * Holds a RFC 2616 request-line.
 */
@SuppressWarnings("serial")
public class RequestLine implements Serializable
{
    private HttpMethod method;
    private byte[] requestUriBytes;
    private URI requestUri;
    private SessionEvent sessionEvent;
    private HttpRequestEvent httpRequestEvent; // Filled in after creation time.
    protected Timestamp timeStamp = new Timestamp((new java.util.Date()).getTime());
    private long requestId = 0;

    private static long nextId = 0;

    public RequestLine(SessionEvent pe, HttpMethod method, byte[] requestUriBytes)
    {
        this.sessionEvent = pe;
        this.method = method;
        this.requestUriBytes = new byte[requestUriBytes.length];
        System.arraycopy(requestUriBytes, 0, this.requestUriBytes, 0, requestUriBytes.length);

        this.requestUri = getUri(requestUriBytes);

        synchronized(RequestLine.class) {
            if (RequestLine.nextId == 0)
                RequestLine.nextId = sessionEvent.getSessionId(); /* borrow the session Id as a starting point */
            this.requestId = RequestLine.nextId++;
        }
    }

    public URL getUrl()
    {
        String host = ( httpRequestEvent == null ? "" : httpRequestEvent.getHost() );

        URL url;
        try {
            url = new URL("http", host, getRequestUri().toString());
        } catch (MalformedURLException exn) {
            throw new RuntimeException(exn); // should never happen
        }

        return url;
    }

    public byte[] getUriBytes()
    {
        byte[] b = null;
        if (requestUriBytes != null) {
            b = new byte[requestUriBytes.length];
            System.arraycopy(requestUriBytes, 0, b, 0,
                             requestUriBytes.length);
        }
        return b;
    }

    /**
     * Request URI.
     *
     * @return the request URI.
     */
    public URI getRequestUri()
    {
        return requestUri;
    }

    public void setRequestUri(URI requestUri)
    {
        this.requestUri = requestUri;
        if (requestUri != null) {
            this.requestUriBytes = requestUri.toString().getBytes();
        }
    }

    /**
     * Get the sessionId
     *
     * @return the SessionEvent.
     */
    public long getRequestId() { return this.requestId; }
    public void setRequestId( long newValue ) { this.requestId = requestId; }

    /**
     * Request method.
     *
     * @return the request method.
     */
    public HttpMethod getMethod() { return method; }
    public void setMethod(HttpMethod newValue) { this.method = newValue; }

    /**
     * Time the event was logged, as filled in by logger.
     *
     * @return time logged.
     */
    public Timestamp getTimeStamp() { return timeStamp; }
    public void setTimeStamp(Timestamp newValue) { this.timeStamp = newValue; }

    /**
     * The HttpRequestEvent that logged this item.
     *
     * @return the HttpRequestEvent.
     */
    public HttpRequestEvent getHttpRequestEvent() { return httpRequestEvent; }
    public void setHttpRequestEvent(HttpRequestEvent newValue) { this.httpRequestEvent = newValue; }

    /**
     * The SessionEvent that logged this item.
     *
     * @return the SessionEvent.
     */
    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent newValue) { this.sessionEvent = newValue; }

    public String toString()
    {
        return getMethod() + " " + getUrl().toString();
    }

    private URI getUri(byte[] b)
    {
        String uriStr;

        try {
            uriStr = new String(b, "UTF-8");
        } catch (UnsupportedEncodingException exn) {
            Logger.getLogger(getClass()).warn("Could not decode URI", exn);
            uriStr = new String(b);
        }

        uriStr = UriUtil.escapeUri(uriStr);

        try {
            return new URI(uriStr);
        } catch (URISyntaxException exn) {
            Logger.getLogger(getClass()).warn( "ignoring bad uri: " + uriStr );
            try {
                return new URI("/");
            } catch (URISyntaxException e) {
                throw new RuntimeException("this should never happen", e);
            }
        }
    }
}
