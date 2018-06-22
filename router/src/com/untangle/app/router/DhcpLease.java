/**
 * $Id$
 */
package com.untangle.app.router;

import java.util.Date;
import java.net.InetAddress;

import org.apache.log4j.Logger;

/**
 * Object that represents a DhcpLease
 */
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

    /**
     * constructor
     */
    public DhcpLease()
    {
    }

    /**
     * DhcpLease constructor
     * @param endOfLease
     * @param mac
     * @param ip
     * @param hostname
     * @param now
     */
    public DhcpLease( Date endOfLease, String mac, InetAddress ip, String hostname, Date now )
    {
        this.endOfLease = endOfLease;
        this.mac        = mac;
        this.ip         = ip;
        this.hostname   = hostname;
        updateState( now );
    }

    /**
     * hasChanged
     * @param endOfLease
     * @param mac
     * @param ip
     * @param hostname
     * @param now
     * @return true if the passed in parameters are different from the
     *        current parameters
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
     * isRenewal
     * Returns true if these new values represent a lease renewal
     * @param mac
     * @param hostname
     * @return
     */
    boolean isRenewal( String mac, String hostname )
    {
        /* renewal if the previous lease was active, and mac and hostname have not changed */
        return isActive() && this.mac.equals( mac ) && this.hostname.equals( hostname );
    }

    /**
     * isActive
     * @return true if the lease was active when this object was
     *        created or last updated.
     */
    boolean isActive()
    {
        return ( state == ACTIVE ) ? true : false;
    }

    /**
     * set
     * @param endOfLease
     * @param mac
     * @param ip
     * @param hostname
     * @param now
     */
    void set( Date endOfLease, String mac, InetAddress ip, String hostname, Date now )
    {
        this.endOfLease = endOfLease;
        this.mac        = mac;
        this.ip         = ip;
        this.hostname   = hostname;
        updateState( now );
    }

    /**
     * updateState
     * @param now
     */
    void updateState( Date now )
    {
        this.state = ( now.before( endOfLease )) ? ACTIVE : EXPIRED;
    }

    /**
     * getMac
     * @return
     */
    public String getMac()
    {
        return mac;
    }

    /**
     * setMac
     * @param mac
     */
    public void setMac( String mac )
    {
        this.mac = mac;
    }

    /**
     * getHostname
     * @return
     */
    public String getHostname()
    {
        return hostname;
    }

    /**
     * setHostname
     * @param hostname
     */
    public void setHostname( String hostname )
    {
        this.hostname = hostname;
    }

    /**
     * getIP
     * @return
     */
    public InetAddress getIP()
    {
        return this.ip;
    }

    /**
     * setIP
     * @param ip
     */
    public void setIP( InetAddress ip )
    {
        this.ip = ip;
    }

    /**
     * getEndOfLease
     * @return
     */
    public Date getEndOfLease()
    {
        return endOfLease;
    }

    /**
     * setEndOfLease
     * @param endOfLease
     */
    public void setEndOfLease( Date endOfLease )
    {
        this.endOfLease = endOfLease;
    }
}
