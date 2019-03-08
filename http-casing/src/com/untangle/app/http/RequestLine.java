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

    /**
     * Create a RequestLine
     * @param sessionEvent
     * @param method
     * @param requestUriBytes
     */
    public RequestLine(SessionEvent sessionEvent, HttpMethod method, byte[] requestUriBytes)
    {
        this.sessionEvent = sessionEvent;
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

    /**
     * Get the URL for the RequestLine
     * @return URL
     */
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

    /**
     * Get the URI as a byte array
     * @return URI
     */
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
     * @return the request URI.
     */
    public URI getRequestUri()
    {
        return requestUri;
    }

    /**
     * Set the URI
     * @param requestUri
     */
    public void setRequestUri(URI requestUri)
    {
        this.requestUri = requestUri;
        if (requestUri != null) {
            this.requestUriBytes = requestUri.toString().getBytes();
        }
    }

    /**
     * Get the requestId
     * @return the requestId.
     */
    public long getRequestId() { return this.requestId; }

    /**
     * Set the requestId
     * @param newValue
     */
    public void setRequestId( long newValue ) { this.requestId = requestId; }

    /**
     * Request method.
     * @return the request method.
     */
    public HttpMethod getMethod() { return method; }

    /**
     * Set the HTTP Method
     * @param newValue
     */
    public void setMethod(HttpMethod newValue) { this.method = newValue; }

    /**
     * Time the event was logged, as filled in by logger.
     * @return time logged.
     */
    public Timestamp getTimeStamp() { return timeStamp; }

    /**
     * Set the timestamp
     * @param newValue
     */
    public void setTimeStamp(Timestamp newValue) { this.timeStamp = newValue; }

    /**
     * The HttpRequestEvent that logged this item.
     * @return the HttpRequestEvent.
     */
    public HttpRequestEvent getHttpRequestEvent() { return httpRequestEvent; }

    /**
     * Set the HttpRequestEvent
     * @param newValue
     */
    public void setHttpRequestEvent(HttpRequestEvent newValue) { this.httpRequestEvent = newValue; }

    /**
     * The SessionEvent that logged this item.
     * @return the SessionEvent.
     */
    public SessionEvent getSessionEvent() { return sessionEvent; }

    /**
     * Set the SessionEvent
     * @param newValue
     */
    public void setSessionEvent(SessionEvent newValue) { this.sessionEvent = newValue; }

    /**
     * Get the String version of this request line
     * @return string representation
     */
    public String toString()
    {
        return getMethod() + " " + getUrl().toString();
    }

    /**
     * Return the URI as a byte array as a URI object
     * @param b - the byte array of the URI
     * @return the URI
     */
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
