/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.networking;

import java.net.UnknownHostException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.TrafficIntfRule;
import com.untangle.uvm.node.firewall.intf.IntfDBMatcher;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.port.PortDBMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolDBMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcherFactory;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_redirect_rule", schema="settings")
public class RedirectRule extends TrafficIntfRule
{

    /* User string for the the port redirect when doing a ping redirect. */
    private static final String REDIRECT_PORT_PING         = "n/a";

    /* User string for when the port is unchanged. */
    private static final String REDIRECT_PORT_UNCHANGED    = "unchanged";

    /* User string for when the address is unchanged. */
    private static final String REDIRECT_ADDRESS_UNCHANGED = "unchanged";

    /* User string for the action to redirect without logging. */
    private static final String ACTION_REDIRECT     = "Redirect";

    /* User string for the action redirect and log. */
    private static final String ACTION_REDIRECT_LOG = "Redirect & Log";

    /* An enumeration of all of the possible actions */
    private static final String[] ACTION_ENUMERATION
        = { ACTION_REDIRECT_LOG, ACTION_REDIRECT };

    /* unused: does the redirect apply to the destination or source */
    private boolean isDstRedirect;

    /* A local redirect is special case where many of the parameters
     * are fixed to reasonable defaults for virtual servers */
    private boolean isLocalRedirect;

    /* The port to redirect to, or 0 if the port shouldn't be
     * changed */
    private int redirectPort;

    /* The address to redirect to, or null if the address shouldn't be
     * changed */
    private IPaddr redirectAddress;

    // constructors -----------------------------------------------------------

    public RedirectRule() { }

    public RedirectRule( boolean       isLive,        ProtocolDBMatcher protocol,
                         IntfDBMatcher srcIntf,       IntfDBMatcher  dstIntf,
                         IPDBMatcher   srcAddress,    IPDBMatcher    dstAddress,
                         PortDBMatcher srcPort,       PortDBMatcher  dstPort,
                         boolean       isDstRedirect, IPaddr redirectAddress, int redirectPort )
    {
        super( isLive, protocol, srcIntf, dstIntf, srcAddress, dstAddress,
               srcPort, dstPort );

        /* Attributes of the redirect */
        this.isDstRedirect   = isDstRedirect;
        this.redirectAddress = redirectAddress;
        this.redirectPort    = redirectPort;
        this.isLocalRedirect = false;
    }

    // accessors --------------------------------------------------------------

    /**
     * A function thats sets the ports to zero for Ping sessions.
     *
     * @exception ParseException When the redirect port is not valid
     * for this type of matcher.
     */
    public final void fixPing() throws ParseException
    {
        super.fixPing();

        if ( getProtocol().equals( ProtocolMatcherFactory.getInstance().getPingMatcher())) {
            /* Indicate to use the redirect port for ping */
            this.redirectPort = -1;
        } else {
            /* The redirect port can't be -1 */
            if ( this.redirectPort == - 1 ) {
                throw new ParseException( "The Redirect port must be set for non-ping sessions" );
            }
        }
    }

    /**
     * Is this a destination redirect or a source redirect
     *
     * @return If this is a destination redirect.
     */
    @Column(name="is_dst_redirect", nullable=false)
    public boolean isDstRedirect()
    {
        return isDstRedirect;
    }

    /**
     * Set if this is a destination redirect or a source redirect
     *
     * @param isDstRedirect If this is a destination redirect.
     */
    public void setDstRedirect( boolean isDstRedirect )
    {
        this.isDstRedirect = isDstRedirect;
    }

    /**
     * Is this is a local redirect. (AKA a port forward).
     *
     * @return If this is a local redirect.
     */
    @Column(name="is_local_redirect", nullable=false)
    public boolean isLocalRedirect()
    {
        return isLocalRedirect;
    }

    /**
     * Set if this is a port forward.
     *
     * @param newValue If this is a port forward.
     */
    public void setLocalRedirect( boolean newValue )
    {
        this.isLocalRedirect = newValue;
    }

    /**
     * Redirect port. 0 to not modify, -1 for ping.
     *
     * @return the port to redirect to.
     */
    @Column(name="redirect_port")
    public int getRedirectPort()
    {
        return redirectPort;
    }

    /**
     * Set the redirect port.  0 to not modify, -1 for ping.
     *
     * @param port The redirect port.
     * @exception ParseException If <code>port</code> is invalid.
     */
    public void setRedirectPort( int port ) throws ParseException
    {
        if ( port < -1 || port > 65535 ) {
            throw new ParseException( "Redirect port must be in the range 0 to 65535: " + port );
        }

        this.redirectPort = port;
    }

    /**
     * Set the redirect port.  This will parse
     * <code>REDIRECT_PORT_UNCHANGED</code> and
     * <code>REDIRECT_PORT_PING</code>.
     *
     * @param port The redirect port.
     * @exception ParseException If <code>port</code> is invalid.
     */
    public void setRedirectPort( String port ) throws ParseException
    {
        if ( port.equalsIgnoreCase( REDIRECT_PORT_UNCHANGED )) {
            setRedirectPort( 0 );
        } else if ( port.equalsIgnoreCase( REDIRECT_PORT_PING )) {
            setRedirectPort( -1 );
        } else {
            setRedirectPort( Integer.parseInt( port ));
        }
    }


    /**
     * User representation of the port to reeirect to.
     *
     * @return User representation of the port to reeirect to.
     */
    @Transient
    public String getRedirectPortString()
    {
        if ( redirectPort == 0 ) return REDIRECT_PORT_UNCHANGED;
        else if ( redirectPort == -1 ) return REDIRECT_PORT_PING;

        return "" + redirectPort;
    }

    /**
     * Redirect host. null to not modify
     *
     * @return the host to redirect to.
     */
    @Column(name="redirect_addr")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getRedirectAddress()
    {
        return redirectAddress;
    }

    /**
     * Set the redirect host. null to not modify
     *
     * @param host The host to redirect to.
     */
    public void setRedirectAddress( IPaddr host )
    {
        this.redirectAddress = host;
    }

    /**
     * Set the redirect host. This will attempt to parse
     * <code>host</code>.  This must be an IP address and not a
     * hostname.
     *
     * @param host The host string to parse.
     */
    public void setRedirectAddress( String host ) throws UnknownHostException, ParseException
    {
        if ( host.equalsIgnoreCase( REDIRECT_ADDRESS_UNCHANGED )) {
            this.redirectAddress = null;
        } else {
            setRedirectAddress( IPaddr.parse( host ));
        }
    }

    /**
     * Get the user representation of the redirect address.
     *
     * @return User string for the action.
     */
    @Transient
    public String getRedirectAddressString()
    {
        if ( redirectAddress == null || redirectAddress.isEmpty()) {
            return REDIRECT_ADDRESS_UNCHANGED;
        }
        return redirectAddress.toString();
    }

    /**
     * Get the user representation of the current action.
     *
     * @return User string for the action.
     */
    @Transient
    public String getAction()
    {
        return ( getLog()) ? ACTION_REDIRECT_LOG : ACTION_REDIRECT;
    }

    /**
     * Set the current action.
     *
     * @param action User string for the action.
     * @exception ParseException if <code>action</code> is not valid. 
     */
    public void setAction( String action ) throws ParseException
    {
        if ( action.equalsIgnoreCase( ACTION_REDIRECT )) {
            setLog( false );
        } else if ( action.equalsIgnoreCase( ACTION_REDIRECT_LOG )) {
            setLog( true );
        } else {
            throw new ParseException( "Invalid action: " + action );
        }
    }

    /**
     * Retrieve all of the possible actions.
     *
     * @return All of the possible actions.
     */
    public static String[] getActionEnumeration()
    {
        return ACTION_ENUMERATION;
    }

    /**
     * Retrieve the default action.
     *
     * @return The default action.
     */
    public static String getActionDefault()
    {
        return ACTION_ENUMERATION[0];
    }
}
