/**
 * $Id$
 */
package com.untangle.node.spyware;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.node.http.HttpRequestEvent;
import com.untangle.node.http.RequestLine;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_spyware_evt_url", schema="events")
@SuppressWarnings("serial")
public class SpywareUrlEvent extends SpywareEvent
{
    private RequestLine requestLine; // pipeline endpoints & location

    // constructors -----------------------------------------------------------

    public SpywareUrlEvent() { }

    public SpywareUrlEvent(RequestLine requestLine)
    {
        this.requestLine = requestLine;
    }

    // SpywareEvent methods ---------------------------------------------------

    @Transient
    public String getType()
    {
        return "Blacklist";
    }

    @Transient
    public String getReason()
    {
        return "in URL List";
    }

    @Transient
    public String getIdentification()
    {
        HttpRequestEvent hre = requestLine.getHttpRequestEvent();
        String host = null == hre
            ? getSessionEvent().getSServerAddr().toString()
            : hre.getHost();
        return "http://" + host + requestLine.getRequestUri().toString();
    }

    @Transient
    public Boolean isBlocked()
    {
        return true;
    }

    @Transient
    public String getLocation()
    {
        return requestLine.getUrl().toString();
    }

    @Transient
    public SessionEvent getSessionEvent()
    {
        return requestLine.getSessionEvent();
    }

    // accessors --------------------------------------------------------------

    /**
     * Request line for this HTTP response pair.
     *
     * @return the request line.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="request_id")
    public RequestLine getRequestLine()
    {
        return requestLine;
    }

    public void setRequestLine(RequestLine requestLine)
    {
        this.requestLine = requestLine;
    }

    // Syslog methods ---------------------------------------------------------

    // use SpywareEvent appendSyslog, getSyslogId and getSyslogPriority
}
