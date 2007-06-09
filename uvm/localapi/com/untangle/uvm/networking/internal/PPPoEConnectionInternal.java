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

package com.untangle.mvvm.networking.internal;

import com.untangle.mvvm.networking.PPPoEConnectionRule;

import com.untangle.mvvm.tran.ImmutableRule;
import com.untangle.mvvm.tran.ValidateException;

/**
 * Immutable PPPoE Connection settings values.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class PPPoEConnectionInternal extends ImmutableRule
{
    /** Set to true in order to enable the PPPoE connection */
    private final String username;
    private final String password;

    /* additional (unanticipated) parameters for the PPP options file */
    private final String secretField;

    /** Index of the argon interface to run PPPoE on */
    private final byte argonIntf;
    
    /** This is the name of the ppp device (eg ppp0 or ppp1) */
    private final String deviceName;
    
    /** This is most likely going to not be used.  Set to true to
     * automatically redial every minute, even if there is no traffic.
     * This should be the default and I can't imagine anyone every
     * turning it off. */
    private final boolean keepalive;

    private PPPoEConnectionInternal( PPPoEConnectionRule rule, String deviceName )
    {
        super( rule );
        this.username    = rule.getUsername();
        this.password    = rule.getPassword();
        this.keepalive   = rule.getKeepalive();
        this.argonIntf   = rule.getArgonIntf();
        this.secretField = rule.getSecretField();
        this.deviceName  = deviceName;
    }
    
    public String getUsername()
    {
        return this.username;
    }

    public String getPassword()
    {
        return this.password;
    }

    public String getSecretField()
    {
        return this.secretField;
    }

    public byte getArgonIntf()
    {
        return this.argonIntf;
    }

    public String getDeviceName()
    {
        return this.deviceName;
    }

    public boolean getKeepalive()
    {
        return this.keepalive;
    }

    public String toString()
    {
        return "[" + this.argonIntf + "," + this.deviceName + "," + this.keepalive + 
            "," + this.username + "]";
    }
    
    /* Return a new rule objects prepopulated with these values */
    public PPPoEConnectionRule toRule()
    {
        PPPoEConnectionRule rule = new PPPoEConnectionRule();
        super.toRule( rule );
        rule.setUsername( getUsername());
        rule.setPassword( getPassword());
        rule.setKeepalive( getKeepalive());
        rule.setArgonIntf( getArgonIntf());
        rule.setSecretField( getSecretField());
        return rule;
    }
    
    static PPPoEConnectionInternal makeInstance( PPPoEConnectionRule rule, String deviceName )
        throws ValidateException
    {
        rule.validate();
        
        deviceName = ( null == deviceName ) ? "" : deviceName.trim();
        
        if ( rule.isLive() && ( deviceName.length() == 0 )) {
            throw new ValidateException( "null device name for enabled pppoe rule" + rule );
        }

        return new PPPoEConnectionInternal( rule, deviceName );
    }
}