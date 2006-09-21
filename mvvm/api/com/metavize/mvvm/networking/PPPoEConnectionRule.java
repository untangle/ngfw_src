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

package com.metavize.mvvm.networking;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.metavize.mvvm.IntfConstants;
import com.metavize.mvvm.tran.Rule;

import com.metavize.mvvm.tran.Validatable;
import com.metavize.mvvm.tran.ValidateException;


/**
 * Settings used for a single PPPoE connection.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_pppoe_connection", schema="settings")
public class PPPoEConnectionRule extends Rule implements Serializable, Validatable
{
    private String username   = "pppoe";
    private String password   = "eoppp";

    /* Index of the argon interface to run PPPoE on */
    private byte argonIntf = IntfConstants.ARGON_UNKNOWN;

    /** This is most likely going to not be used.  Set to true to
     * automatically redial every minute, even if there is no traffic.
     * This should be the default and I can't imagine anyone every
     * turning it off. */
    private boolean keepalive = true;

    public PPPoEConnectionRule()
    {
    }
        
    @Column(name="username")
    public String getUsername()
    {
        return this.username;
    }
    
    public void setUsername( String newValue )
    {
        newValue = ( null == newValue ) ? "" : newValue;
        this.username = newValue.trim();
    }

    @Column(name="password")
    public String getPassword()
    {
        return this.password;
    }

    public void setPassword( String newValue )
    {
        newValue = ( null == newValue ) ? "" : newValue;
        this.password = newValue.trim();
    }

    @Column(name="intf")
    public byte getArgonIntf()
    {
        return this.argonIntf;
    }

    public void setArgonIntf( byte newValue )
    {
        this.argonIntf = newValue;
    }

    @Column(name="keepalive")
    public boolean getKeepalive()
    {
        return this.keepalive;
    }

    public void setKeepalive( boolean newValue )
    {
        this.keepalive = newValue;
    }

    public String toString()
    {
        return "[" + this.argonIntf + "," + "," + this.keepalive + "," + this.username + "]";
    }

    public void validate() throws ValidateException
    {
        /* Nothing to validate if this connection is disabled. */
        if ( !this.isLive()) return;

        if ( null == this.username || ( 0 == this.username.length())) {
            throw new ValidateException( "Empty username for PPPoE" );
        }

        if ( null == this.password || ( 0 == this.password.length())) {
            throw new ValidateException( "Empty password for PPPoE" );
        }
        
        if ( this.argonIntf < IntfConstants.ARGON_MIN || this.argonIntf > IntfConstants.ARGON_MAX ) {
            throw new ValidateException( "Invalid argon interface." );
        }
    }
}
