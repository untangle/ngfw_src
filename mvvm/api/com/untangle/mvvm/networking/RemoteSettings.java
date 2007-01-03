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

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.ParseException;

public interface RemoteSettings
{
    /* Get the script to run once the box is configured */
    public String getPostConfigurationScript();
    
    /* Set the script to run once the box is configured */
    public void setPostConfigurationScript( String newValue );

    /* Get the post configuration script */
    public String getCustomRules();
    
    /* XXXX This should be validated */
    public void setCustomRules( String newValue );
    
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
    public HostName getHostname();

    public void setHostname( HostName newValue );

    /* Returns if the hostname for this box is publicly resolvable to this box */
    public boolean getIsHostnamePublic();

    /* Set if the hostname for this box is publicly resolvable to this box */
    public void setIsHostnamePublic( boolean newValue );

    /* True if the public address should be used */
    public boolean getIsPublicAddressEnabled();

    public void setIsPublicAddressEnabled( boolean newValue );

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public String getPublicAddress();

    /* Set the public address as a string */
    public void setPublicAddress( String newValue ) throws ParseException;

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public IPaddr getPublicIPaddr();

    public void setPublicIPaddr( IPaddr newValue );

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public int getPublicPort();

    public void setPublicPort( int newValue );

    /* Return true if the current settings have a public address */
    public boolean hasPublicAddress();


    /** --- HTTPs access configuration.  This shouldn't be here, --- **/
    /** --- rearchitect, networking is already far too large.    --- **/
    public boolean getIsOutsideAdministrationEnabled();

    public void setIsOutsideAdministrationEnabled( boolean newValue );

    public boolean getIsOutsideQuarantineEnabled();
    
    public void setIsOutsideQuarantineEnabled( boolean newValue );

    public boolean getIsOutsideReportingEnabled();
    
    public void setIsOutsideReportingEnabled( boolean newValue );
}
