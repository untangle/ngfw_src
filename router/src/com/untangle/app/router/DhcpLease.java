/**
 * $Id$
 */
package com.untangle.app.router;

import java.util.Date;
import java.net.InetAddress;

import org.apache.log4j.Logger;

/* XXX Probably should be an inner class for DhcpMonitor */
public class DhcpLease
{
    private static final int EXPIRED = 0;
    private static final int ACTIVE  = 1;

    private final Logger logger = Logger.getLogger(getClass());

    private String mac        = null;
    private String     hostname   = "";
    private InetAddress     ip         = null;
    private Date       endOfLease = null;
    private int        state      = EXPIRED;

    // Constructors
    /**
     * Hibernate constructor
     */
    public DhcpLease()
    {
    }

    public DhcpLease( Date endOfLease, String mac, InetAddress ip, String hostname, Date now )
    {
        this.endOfLease = endOfLease;
        this.mac        = mac;
        this.ip         = ip;
        this.hostname   = hostname;
        updateState( now );
    }

    /**
     * @return true if the passed in parameters are different from the current parameters
     */
    boolean hasChanged( Date endOfLease, String mac, InetAddress ip, String hostname, Date now )
    {
        int state = this.state;
        updateState( now );

        /**
         * A DhcpLease is suppose to track the lease on a specific IP
         */
        if ( !this.ip.equals( ip )) {
            logger.warn( "hasChanged with different ip: " + this.ip.toString() + " ->" + ip.toString());
            return true;
        }

        if ( this.state != state || !this.endOfLease.equals( endOfLease ) || !this.mac.equals( mac ) ||
             !this.hostname.equals( hostname )) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if these new values represent a lease renewal
     */
    boolean isRenewal( String mac, String hostname )
    {
        /* renewal if the previous lease was active, and mac and hostname have not changed */
        return isActive() && this.mac.equals( mac ) && this.hostname.equals( hostname );
    }

    /**
     * @return true if the lease was active when this object was created or last updated.
     */
    boolean isActive()
    {
        return ( state == ACTIVE ) ? true : false;
    }

    void set( Date endOfLease, String mac, InetAddress ip, String hostname, Date now )
    {
        this.endOfLease = endOfLease;
        this.mac        = mac;
        this.ip         = ip;
        this.hostname   = hostname;
        updateState( now );
    }

    void updateState( Date now )
    {
        this.state = ( now.before( endOfLease )) ? ACTIVE : EXPIRED;
    }

    public String getMac()
    {
        return mac;
    }

    public void setMac( String mac )
    {
        this.mac = mac;
    }

    public String getHostname()
    {
        return hostname;
    }

    public void setHostname( String hostname )
    {
        this.hostname = hostname;
    }

    public InetAddress getIP()
    {
        return this.ip;
    }

    public void setIP( InetAddress ip )
    {
        this.ip = ip;
    }

    public Date getEndOfLease()
    {
        return endOfLease;
    }

    public void setEndOfLease( Date endOfLease )
    {
        this.endOfLease = endOfLease;
    }
}
