/*
 * Copyright (c) 2003, 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking.internal;

import com.metavize.mvvm.networking.PPPoEConnectionRule;

import com.metavize.mvvm.tran.ImmutableRule;
import com.metavize.mvvm.tran.ValidateException;

/**
 * Immutable PPPoE Connection settings values.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 */
public class PPPoEConnectionInternal extends ImmutableRule
{
    /** Set to true in order to enable the PPPoE connection */
    private final String username;
    private final String password;

    /* Index of the argon interface to run PPPoE on */
    private final byte argonIntf;
    
    /** This is most likely going to not be used.  Set to true to
     * automatically redial every minute, even if there is no traffic.
     * This should be the default and I can't imagine anyone every
     * turning it off. */
    private final boolean keepalive;

    private PPPoEConnectionInternal( PPPoEConnectionRule rule )
    {
        super( rule );
        this.username    = rule.getUsername();
        this.password    = rule.getPassword();
        this.keepalive   = rule.getKeepalive();
        this.argonIntf   = rule.getArgonIntf();
    }
    
    public String getUsername()
    {
        return this.username;
    }

    public String getPassword()
    {
        return this.password;
    }

    public byte getArgonIntf()
    {
        return this.argonIntf;
    }

    public boolean getKeepalive()
    {
        return this.keepalive;
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
        return rule;
    }
    
    public static PPPoEConnectionInternal makeInstance( PPPoEConnectionRule rule )
        throws ValidateException
    {
        rule.validate();

        return new PPPoEConnectionInternal( rule );
    }
}