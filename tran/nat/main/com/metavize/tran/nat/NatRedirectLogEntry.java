/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import java.io.Serializable;
import java.util.Date;

public class NatRedirectLogEntry implements Serializable
{
    private static final long serialVersionUID = 7234639235747894138L;

    private static final String ACTION_DMZ            = "dmz";
    private static final String ACTION_REDIRECT       = "redirect";
    
    private static final String REASON_DMZ            = "Destined to DMZ";
    private static final String REASON_REDIRECT_START = "Redirect Rule #";
    private static final String REASON_REDIRECT_END   = "";
    private static final String FLAG_NATTED           = "(pre-NAT)";

    private final Date      createDate;
    private final String    protocol;
    private final String    clientAddr;
    private final int       clientPort;
    
    private final String    originalServerAddr;
    private final int       originalServerPort;
    private final String    redirectServerAddr;
    private final int       redirectServerPort;
    private final String    clientIntf;
    private final String    serverIntf;
    private final boolean   isDmz;
    private final int       ruleIndex;
    private final boolean   isNatd;

    NatRedirectLogEntry( Date createDate, String protocol,
                         String clientAddr, int clientPort, boolean isNatd,
                         String originalServerAddr, int originalServerPort,
                         String redirectServerAddr, int redirectServerPort,
                         String clientIntf, String serverIntf, 
                         boolean isDmz, int ruleIndex )
    {
        this.createDate         = createDate;
        this.protocol           = protocol;
        this.clientAddr         = clientAddr;
        this.clientPort         = clientPort;
        this.isNatd             = isNatd;
        this.originalServerAddr = originalServerAddr;
        this.originalServerPort = originalServerPort;
        this.redirectServerAddr = redirectServerAddr;
        this.redirectServerPort = redirectServerPort;
        this.clientIntf         = clientIntf;
        this.serverIntf         = serverIntf;
        this.isDmz              = isDmz;
        this.ruleIndex          = ruleIndex;
    }

    // util -------------------------------------------------------------------
    public String getAction()
    {
        return ( isDmz ) ? ACTION_DMZ : ACTION_REDIRECT;
    }

    public String getReason()
    {
        if ( isDmz ) return REASON_DMZ;
        
        return REASON_REDIRECT_START + ruleIndex + REASON_REDIRECT_END;        
    }

    public String getClient()
    {
        return format( this.clientAddr, this.clientPort ) + (( isNatd ) ? ( " " + FLAG_NATTED ) : "" );
    }

    public String getOriginalServer()
    {
        return format( this.originalServerAddr, this.originalServerPort );
    }

    public String getRedirectServer()
    {
        return format( this.redirectServerAddr, this.redirectServerPort );
    }

    private String format( String addr, int port )
    {
        /* XXX another ICMP hack */
        if ( this.protocol.equalsIgnoreCase( "Ping" ) || port == 0 ) {
            return addr.toString();
        }
        
        return addr.toString() + ":" + port;
    }
    
    // accessors --------------------------------------------------------------

    public Date getCreateDate()
    {
        return createDate;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public String getClientAddr()
    {
        return clientAddr;
    }

    public int getClientPort()
    {
        return clientPort;
    }

    public String getOriginalServerAddr()
    {
        return this.originalServerAddr;
    }

    public int getOriginalServerPort()
    {
        return this.originalServerPort;
    }
    
    public String getRedirectServerAddr()
    {
        return this.redirectServerAddr;
    }

    public int getRedirectServerPort()
    {
        return this.redirectServerPort;
    }

    public String getClientIntf()
    {
        return this.clientIntf;
    }
    
    public String getServerIntf()
    {
        return this.serverIntf;
    }
}
