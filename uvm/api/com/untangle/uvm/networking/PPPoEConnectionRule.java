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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.untangle.mvvm.IntfConstants;
import com.untangle.mvvm.tran.Rule;

import com.untangle.mvvm.tran.Validatable;
import com.untangle.mvvm.tran.ValidateException;


/**
 * Settings used for a single PPPoE connection.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_pppoe_connection", schema="settings")
public class PPPoEConnectionRule extends Rule implements Serializable, Validatable
{
    /* the username */
    private String username   = "pppoe";

    /* the password */
    private String password   = "eoppp";

    /* The value for the secret field, this is a string that is
     * appended verbatim to the file connection-pppoe in
     * /etc/ppp/peers. */
    private String secretField = "";

    /* Index of the argon interface to run PPPoE on */
    private byte argonIntf = IntfConstants.ARGON_UNKNOWN;

    /**
     * Presently unused.  Set to true to automatically redial every
     * minute, even if there is no traffic.  This should be the
     * default and I can't imagine anyone every turning it off. */
    private boolean keepalive = true;

    public PPPoEConnectionRule()
    {
    }
    
    /**
     * Get the username.
     *
     * @return The username.
     */
    @Column(name="username")
    public String getUsername()
    {
        if ( this.username == null ) this.username = "";
        return this.username;
    }
    
    /**
     * Set the username.
     *
     * @param newValue The username.
     */
    public void setUsername( String newValue )
    {
        newValue = ( null == newValue ) ? "" : newValue;
        this.username = newValue.trim();
    }

    /**
     * Get the password.
     *
     * @return The password.
     */
    @Column(name="password")
    public String getPassword()
    {
        if ( this.password == null ) this.password = "";
        return this.password;
    }

    /**
     * Set the password.
     *
     * @param newValue The password.
     */
    public void setPassword( String newValue )
    {
        newValue = ( null == newValue ) ? "" : newValue;
        this.password = newValue.trim();
    }

    /**
     * Get the secret field.
     *
     * @return The secret field for this connection.
     */
    @Column(name="secret_field")
    public String getSecretField()
    {
        if ( this.secretField == null ) this.secretField = "";
        return this.secretField;
    }

    /**
     * Set the secret field.
     *
     * @param newValue The secret field for this connection.
     */
    public void setSecretField( String newValue )
    {
        newValue = ( null == newValue ) ? "" : newValue;
        this.secretField = newValue.trim();
    }

    /**
     * The argon index of the interface to run PPPoE on.
     *
     * @return The argon index of the interface to run PPPoE on.
     */
    @Column(name="intf")
    public byte getArgonIntf()
    {
        return this.argonIntf;
    }

    /**
     * Set the argon index of the interface to run PPPoE on.
     *
     * @param newValue The argon index of the interface to run PPPoE
     * on.
     */
    public void setArgonIntf( byte newValue )
    {
        this.argonIntf = newValue;
    }

    /**
     * Unused parameter.  Originally used to automatically redial.
     * PPPoE appears to do this automatically anyway.     
     */
    @Column(name="keepalive")
    public boolean getKeepalive()
    {
        return this.keepalive;
    }

    /**
     * Unused parameter.  Originally used to automatically redial.
     * PPPoE appears to do this automatically anyway.     
     */
    public void setKeepalive( boolean newValue )
    {
        this.keepalive = newValue;
    }

    public String toString()
    {
        return "[" + this.argonIntf + "," + "," + this.keepalive + "," + this.username + "]";
    }

    /**
     * Validate these PPPoE setting are free of errors.
     *
     * @exception ValidationException Occurs if there is an error in
     * these settings.
     */
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
