/**
 * $Id$
 */
package com.untangle.node.http;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import com.untangle.node.util.UriUtil;
import com.untangle.uvm.node.SessionEvent;

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
    private Date timeStamp = new Date();
    private long requestId = 0;
    
    private static long nextId = 0;
    
    // constructors -----------------------------------------------------------

    public RequestLine(SessionEvent pe, HttpMethod method, byte[] requestUriBytes)
    {
        this.sessionEvent = pe;
        this.method = method;
        this.requestUriBytes = new byte[requestUriBytes.length];
        System.arraycopy(requestUriBytes, 0, this.requestUriBytes, 0, requestUriBytes.length);

        this.requestUri = getUri(requestUriBytes);

        synchronized(this) {
            if (this.nextId == 0) 
                this.nextId = sessionEvent.getSessionId(); /* borrow the session Id as a starting point */
            this.requestId = ++this.nextId;
        }
    }

    // business methods -------------------------------------------------------

    public URL getUrl()
    {
        // XXX this shouldn't happen in practice
        String host = null == httpRequestEvent ? "" : httpRequestEvent.getHost();

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

    // accessors --------------------------------------------------------------

    /**
     * Get the sessionId
     *
     * @return the SessionEvent.
     */
    public long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    /**
     * Get the sessionId
     *
     * @return the SessionEvent.
     */
    public long getRequestId()
    {
        return this.requestId;
    }

    public void setRequestId( long requestId )
    {
        this.requestId = requestId;
    }
    
    /**
     * Request method.
     *
     * @return the request method.
     */
    public HttpMethod getMethod()
    {
        return method;
    }

    public void setMethod(HttpMethod method)
    {
        this.method = method;
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
     * Time the event was logged, as filled in by logger.
     *
     * @return time logged.
     */
    public Date getTimeStamp()
    {
        return timeStamp;
    }

    /**
     * Don't make Aaron angry!  This should only be set by the event
     * logging system unless you're doing tricky things (with Aaron's
     * approval).
     */
    public void setTimeStamp(Date timeStamp)
    {
        if (timeStamp instanceof Timestamp) {
            this.timeStamp = new Date(timeStamp.getTime());
        } else {
            this.timeStamp = timeStamp;
        }
    }

    /**
     * The HttpRequestEvent that logged this item.
     *
     * @return the HttpRequestEvent.
     */
    public HttpRequestEvent getHttpRequestEvent()
    {
        return httpRequestEvent;
    }

    public void setHttpRequestEvent(HttpRequestEvent httpRequestEvent)
    {
        this.httpRequestEvent = httpRequestEvent;
    }

    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "RequestLine " + " length: "
            + requestUri.toString().length() + " (" + super.toString() + ")";
    }

    // private methods --------------------------------------------------------

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
            Logger.getLogger(getClass()).warn("ignoring bad uri: " + uriStr, exn);
            try {
                return new URI("/");
            } catch (URISyntaxException e) {
                throw new RuntimeException("this should never happen", e);
            }
        }
    }
}
