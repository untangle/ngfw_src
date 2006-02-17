/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import com.metavize.mvvm.tran.IPaddr;

public interface RemoteSettings
{
    /* Get the script to run once the box is configured */
    public String getPostConfigurationScript();
    
    /* Set the script to run once the box is configured */
    public void setPostConfigurationScript( String newValue );
    
    /* Get whether or not ssh is enabled. */
    public boolean isSshEnabled();
    
    /* Set whether or not ssh is enabled. */
    public void isSshEnabled( boolean newValue );
    
    /* Get whether or not exception reporting is enabled */
    public boolean isExceptionReportingEnabled();

    /* Set whether or not exception reporting is enabled */
    public void isExceptionReportingEnabled( boolean newValue );

    /* Get whether tcp window scaling is enabled */
    public boolean isTcpWindowScalingEnabled();

    /* Set whether tcp window scaling is enabled */
    public void isTcpWindowScalingEnabled( boolean newValue );
    
    /** True if insecure access from the inside is enabled. */
    public boolean isInsideInsecureEnabled();

    public void isInsideInsecureEnabled( boolean newValue );
    
    /** True if outside (secure) access is enabled. */
    public boolean isOutsideAccessEnabled();

    public void isOutsideAccessEnabled( boolean isEnabled );

    /** True if outside (secure) access is restricted. */
    public boolean isOutsideAccessRestricted();

    public void isOutsideAccessRestricted( boolean isRestricted );

    /**
     * The netmask of the network/host that is allowed to administer the box from outside
     * This is ignored if outside access is not enabled, null for just
     * one host.
     */

    /** The restricted network of machines allowed to connect to the box. */
    public IPaddr outsideNetwork();

    public void outsideNetwork( IPaddr network );
    
    /** The restricted netmask of machines allowed to connect to the box. */
    public IPaddr outsideNetmask();

    public void outsideNetmask( IPaddr netmask );

    /* Get the port to run HTTPs on in addition to port 443. */
    public int httpsPort();

    /* Set the port to run HTTPs on in addition to port 443. */
    public void httpsPort( int httpsPort );

    /** The hostname for the box(this is the hostname that goes into certificates). */
    public String getHostname();

    public void setHostname( String newValue );

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public String getPublicAddress();

    public void setPublicAddress( String newValue );

    /* Return true if the current settings have a public address */
    public boolean hasPublicAddress();
}