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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import com.untangle.node.util.UriUtil;
import com.untangle.uvm.node.PipelineEndpoints;

/**
 * Holds a RFC 2616 request-line.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_http_req_line", schema="events")
@SuppressWarnings("serial")
public class RequestLine implements Serializable
{

    private Long id;
    private HttpMethod method;
    private byte[] requestUriBytes;
    private URI requestUri;
    private PipelineEndpoints pipelineEndpoints;
    private HttpRequestEvent httpRequestEvent; // Filled in after creation time.
    private Date timeStamp = new Date();

    // constructors -----------------------------------------------------------

    public RequestLine() { }

    public RequestLine(PipelineEndpoints pe, HttpMethod method,
                       byte[] requestUriBytes)
    {
        this.pipelineEndpoints = pe;
        this.method = method;

        this.requestUriBytes = new byte[requestUriBytes.length];
        System.arraycopy(requestUriBytes, 0, this.requestUriBytes, 0,
                         requestUriBytes.length);

        this.requestUri = getUri(requestUriBytes);
    }

    // business methods -------------------------------------------------------

    @Transient
    public URL getUrl()
    {
        // XXX this shouldn't happen in practice
        String host = null == httpRequestEvent ? ""
            : httpRequestEvent.getHost();

        URL url;
        try {
            url = new URL("http", host, getRequestUri().toString());
        } catch (MalformedURLException exn) {
            throw new RuntimeException(exn); // should never happen
        }

        return url;
    }

    @Transient
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

    @SuppressWarnings("unused")
	@Id
    @Column(name="request_id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    @SuppressWarnings("unused")
	private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Get the sessionId
     *
     * @return the PipelineEndpoints.
     */
    @Column(name="session_id", nullable=false)
    public Long getSessionId()
    {
        return pipelineEndpoints.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.pipelineEndpoints.setSessionId(sessionId);
    }

    /**
     * Request method.
     *
     * @return the request method.
     */
    @Type(type="com.untangle.node.http.HttpMethodUserType")
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
    @Column(name="uri")
    @Type(type="com.untangle.uvm.type.UriUserType")
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
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="time_stamp")
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
    @OneToOne(mappedBy="requestLine")
    public HttpRequestEvent getHttpRequestEvent()
    {
        return httpRequestEvent;
    }

    public void setHttpRequestEvent(HttpRequestEvent httpRequestEvent)
    {
        this.httpRequestEvent = httpRequestEvent;
    }

    @Transient
    public PipelineEndpoints getPipelineEndpoints()
    {
        return pipelineEndpoints;
    }

    public void setPipelineEndpoints(PipelineEndpoints pipelineEndpoints)
    {
        this.pipelineEndpoints = pipelineEndpoints;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "RequestLine id: " + id + " length: "
            + requestUri.toString().length() + " (" + super.toString() + ")";
    }

    // private methods --------------------------------------------------------

    @Transient
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
