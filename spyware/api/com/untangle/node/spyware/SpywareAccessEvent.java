/*
 * $Id$
 */
package com.untangle.node.spyware;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.SessionEvent;
import org.hibernate.annotations.Type;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_spyware_evt_access", schema="events")
@SuppressWarnings("serial")
public class SpywareAccessEvent extends SpywareEvent
{
    private SessionEvent sessionEvent;
    private String identification;
    private IPMaskedAddress ipMaddr; // location
    private Boolean blocked;

    // constructors -----------------------------------------------------------

    public SpywareAccessEvent() { }

    public SpywareAccessEvent(SessionEvent pe, String identification, IPMaskedAddress ipMaddr, Boolean blocked)
    {
        this.sessionEvent = pe;
        this.identification = identification;
        this.ipMaddr = ipMaddr;
        this.blocked = blocked;
    }

    // SpywareEvent methods ---------------------------------------------------

    @Transient
    public String getType()
    {
        return "Access";
    }

    @Transient
    public String getReason()
    {
        return "in Subnet List";
    }

    @Transient
    public String getLocation()
    {
        return ipMaddr.toString();
    }

    // accessors --------------------------------------------------------------

    /**
     * Get the SessionId.
     *
     * @return the SessionEvent.
     */
    @Column(name="session_id", nullable=false)
    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    @Transient
    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }

    /**
     * An address or subnet.
     *
     * @return the IPMaskedAddress.
     */
    @Type(type="com.untangle.uvm.type.IPMaskedAddressUserType")
    public IPMaskedAddress getIpMaddr()
    {
        return ipMaddr;
    }

    public void setIpMaddr(IPMaskedAddress ipMaddr)
    {
        this.ipMaddr = ipMaddr;
    }

    /**
     * The identification (domain matched)
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
     * Whether or not we blocked it.
     *
     * @return whether or not the session was blocked (closed)
     */
    @Column(nullable=false)
    public Boolean isBlocked()
    {
        return blocked;
    }

    public void setBlocked(Boolean blocked)
    {
        this.blocked = blocked;
    }

    // Syslog methods ---------------------------------------------------------

    // use SpywareEvent appendSyslog, getSyslogId and getSyslogPriority
}
