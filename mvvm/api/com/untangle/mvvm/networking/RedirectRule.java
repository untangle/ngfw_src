/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.networking;

import java.net.UnknownHostException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolDBMatcher;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcherFactory;
import com.untangle.mvvm.tran.firewall.TrafficIntfRule;
import com.untangle.mvvm.tran.firewall.intf.IntfDBMatcher;
import com.untangle.mvvm.tran.firewall.ip.IPDBMatcher;
import com.untangle.mvvm.tran.firewall.port.PortDBMatcher;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_redirect_rule", schema="settings")
public class RedirectRule extends TrafficIntfRule
{
    // !!! private static final long serialVersionUID = -7898386470255279187L;

    private static final String REDIRECT_PORT_PING         = "n/a";
    private static final String REDIRECT_PORT_UNCHANGED    = "unchanged";
    private static final String REDIRECT_ADDRESS_UNCHANGED = "unchanged";

    private static final String ACTION_REDIRECT     = "Redirect";
    private static final String ACTION_REDIRECT_LOG = "Redirect & Log";

    private static final String[] ACTION_ENUMERATION
        = { ACTION_REDIRECT_LOG, ACTION_REDIRECT };

    private boolean isDstRedirect;

    /* A local redirect is special case where many of the parameters
     * are fixed to reasonable defaults for virtual servers */
    private boolean isLocalRedirect;

    private int redirectPort;
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

    /* Hack that sets the ports to zero for Ping sessions */
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

    public void setDstRedirect( boolean isDstRedirect )
    {
        this.isDstRedirect = isDstRedirect;
    }

    /**
     * Is this is a local redirect.
     *
     * @return If this is a local redirect.
     */
    @Column(name="is_local_redirect", nullable=false)
    public boolean isLocalRedirect()
    {
        return isLocalRedirect;
    }

    public void setLocalRedirect( boolean newValue )
    {
        this.isLocalRedirect = newValue;
    }

    /**
     * Redirect port. -1 to not modify
     *
     * @return the port to redirect to.
     */
    @Column(name="redirect_port")
    public int getRedirectPort()
    {
        return redirectPort;
    }

    public void setRedirectPort( int port ) throws ParseException
    {
        if ( port < -1 || port > 65535 ) {
            throw new ParseException( "Redirect port must be in the range 0 to 65535: " + port );
        }

        this.redirectPort = port;
    }

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
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getRedirectAddress()
    {
        return redirectAddress;
    }

    public void setRedirectAddress( IPaddr host )
    {
        this.redirectAddress = host;
    }

    public void setRedirectAddress( String host ) throws UnknownHostException, ParseException
    {
        if ( host.equalsIgnoreCase( REDIRECT_ADDRESS_UNCHANGED )) {
            this.redirectAddress = null;
        } else {
            setRedirectAddress( IPaddr.parse( host ));
        }
    }

    @Transient
    public String getRedirectAddressString()
    {
        if ( redirectAddress == null || redirectAddress.isEmpty()) {
            return REDIRECT_ADDRESS_UNCHANGED;
        }
        return redirectAddress.toString();
    }

    @Transient
    public String getAction()
    {
        return ( getLog()) ? ACTION_REDIRECT_LOG : ACTION_REDIRECT;
    }

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

    public static String[] getActionEnumeration()
    {
        return ACTION_ENUMERATION;
    }

    public static String getActionDefault()
    {
        return ACTION_ENUMERATION[0];
    }
}
