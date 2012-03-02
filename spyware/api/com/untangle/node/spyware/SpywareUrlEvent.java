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
import javax.persistence.Column;

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
    private Long requestId;
    private RequestLine requestLine; // pipeline endpoints & location

    // constructors -----------------------------------------------------------

    public SpywareUrlEvent() { }

    public SpywareUrlEvent(RequestLine requestLine)
    {
        this.requestId = requestLine.getRequestId();
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

    @Column(name="request_id")
    public Long getRequestId()
    {
        return requestId;
    }

    public void setRequestId(Long requestId)
    {
        this.requestId = requestId;
    }

}
