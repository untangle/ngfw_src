/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MimeTypeRule.java 229 2005-04-07 22:25:00Z amread $
 */

package com.metavize.mvvm.tran.firewall;

import com.metavize.mvvm.tran.IPaddr;

import com.metavize.mvvm.security.Tid;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:rbscott@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="REDIRECT_RULE"
 */
public class RedirectRule extends TrafficRule
{
    /* XXX The varchar probably should just be chars */

    private static final long serialVersionUID =   7015498486016408054L;

    private boolean isDstRedirect;

    private int redirectPort;
    private IPaddr redirectAddress;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public RedirectRule() { }

    public RedirectRule( boolean     isLive,  ProtocolMatcher protocol, 
                         IntfMatcher srcIntf,    IntfMatcher  dstIntf, 
                         IPMatcher   srcAddress, IPMatcher    dstAddress,
                         PortMatcher srcPort,    PortMatcher  dstPort,
                         boolean isDstRedirect,  IPaddr redirectAddress, int redirectPort )
    {
        super( isLive, protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );
        
        /* Attributes of the redirect */
        this.isDstRedirect   = isDstRedirect;
        this.redirectAddress = redirectAddress;
        this.redirectPort    = redirectPort;
    }


    // accessors --------------------------------------------------------------

    /**
     * Is this a destination redirect or a source redirect
     *
     * @return If this is a destinatino redirect.
     * @hibernate.property
     * column="IS_DST_REDIRECT"
     */
    public boolean isDstRedirect()
    {
        return isDstRedirect;
    }

    public void setDstRedirect( boolean isDstRedirect )
    {
        this.isDstRedirect = isDstRedirect;
    }

    /**
     * Redirect port. -1 to not modify
     *
     * @return the port to redirect to.
     * @hibernate.property
     * column="REDIRECT_PORT"
     */
    public int getRedirectPort()
    {
        return redirectPort;
    }

    public void setRedirectPort( int port )
    {
        this.redirectPort = port;
    }

    /**
     * Redirect host. -1 to not modify
     *
     * @return the host to redirect to.
     *
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="REDIRECT_ADDR"
     * sql-type="inet"
     */
    public IPaddr getRedirectAddress()
    {
        return redirectAddress;
    }

    public void setRedirectAddress( IPaddr host )
    {
        this.redirectAddress = host;
    }
}
