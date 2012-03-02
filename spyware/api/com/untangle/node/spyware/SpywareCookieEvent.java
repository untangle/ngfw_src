/**
 * $Id$
 */
package com.untangle.node.spyware;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.node.http.RequestLine;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_spyware_evt_cookie", schema="events")
@SuppressWarnings("serial")
public class SpywareCookieEvent extends SpywareEvent
{
    private String identification;
    private RequestLine requestLine; // pipeline endpoints & location
    private Boolean toServer;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpywareCookieEvent() { }

    public SpywareCookieEvent(RequestLine requestLine, String identification, Boolean toServer)
    {
        this.identification = identification;
        this.requestLine = requestLine;
        this.toServer = toServer;
    }

    // SpywareEvent methods ---------------------------------------------------

    @Transient
    public String getType()
    {
        return "Cookie";
    }

    @Transient
    public String getReason()
    {
        return "in Cookie List";
    }

    @Transient
    public String getLocation()
    {
        return requestLine.getUrl().toString();
    }

    @Transient
    public Boolean isBlocked()
    {
        return true;
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

    /**
     * The identification (name of IP address range matched)
     *
     * @return the protocl name.
     */
    @Column(name="ident")
    public String getIdentification()
    {
        return identification;
    }

    public void setIdentification(String identification)
    {
        this.identification = identification;
    }

    /**
     * Whether or not the cookie is to the server (otherwise to the client)
     *
     * @return if true the cookie was zeroed going to the server,
     * otherwise it was removed going to the client
     */
    @Column(name="to_server", nullable=false)
    public Boolean isToServer()
    {
        return toServer;
    }

    public void setToServer(Boolean toServer)
    {
        this.toServer = toServer;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getSessionEvent().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("info", getIdentification());
        sb.addField("loc", getLocation());
        sb.addField("blocked", isBlocked());
        sb.addField("toServer", isToServer());
    }

    // use SpywareEvent getSyslogId and getSyslogPriority
}
