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

package com.untangle.uvm.user;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.networking.NetworkUtil;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;
import org.hibernate.annotations.Type;


@Entity
@Table(name="mvvm_wmi_settings", schema="settings")
public class WMISettings implements Serializable, Validatable
{
    // private static final long serialVersionUID = 172494253701617361L;

    private static final int DEFAULT_WMI_PORT = 5989;
    private static final String DEFAULT_WMI_SCHEME = "https";

    private Long id;

    /* XXX This should be separated out into a rule, this way you can have
     * a different WMI server for various networks.  the default
     * windows firewall rejects WMI requests that are not on your
     * local network. */
    private boolean isEnabled = false;

    private String username = "";
    private String password = "";
    private String scheme = DEFAULT_WMI_SCHEME;

    private IPaddr address;

    private int port = DEFAULT_WMI_PORT;

    /* this is the url where the file should be accessed from (not saved in the database) */
    private String url;

    public WMISettings()
    {
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    protected Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }

    @Column(name="live")
    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    public void setIsEnabled( boolean newValue )
    {
        this.isEnabled = newValue;
    }


    /**
     * Address of the WMI server.
     *
     * @return Address of the WMI server.
     */
    @Column(name="address")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getAddress()
    {
        if ( this.address == null ) this.address = NetworkUtil.EMPTY_IPADDR;
        return this.address;
    }

    public void setAddress( IPaddr newValue )
    {
        if ( newValue == null || newValue.isEmpty()) newValue = NetworkUtil.EMPTY_IPADDR;
        this.address = newValue;
    }

    /**
     * Username to log into the WMI server with.
     *
     * @return Username to log into the WMI server with.
     */
    @Column(name="username")
    public String getUsername()
    {
        if ( this.username == null ) this.username = "";
        return this.username;
    }

    public void setUsername( String newValue )
    {
        newValue = ( null == newValue ) ? "" : newValue;
        this.username = newValue.trim();
    }

    /**
     * Password to log into the WMI server with.
     *
     * @return Password to log into the WMI server with.
     */
    @Column(name="password")
    public String getPassword()
    {
        if ( this.password == null ) this.password = "";
        return this.password;
    }

    public void setPassword( String newValue )
    {
        newValue = ( null == newValue ) ? "" : newValue;
        this.password = newValue.trim();
    }

    /**
     * Port to connect to the server on.
     *
     * @return Port to connect to the server on.
     */
    @Column(name="port")
    public int getPort()
    {
        if ( this.port <= 0 || this.port >= 0xFFFF ) this.port = DEFAULT_WMI_PORT;
        return this.port;
    }

    public void setPort( int newValue )
    {
        if ( newValue <= 0 || newValue >= 0xFFFF ) newValue = DEFAULT_WMI_PORT;
        this.port = newValue;
    }

    /**
     * Scheme (http|https) to connect to the WMI server with.
     *
     * @return Scheme (http|https) to connect to the WMI server with.
     */
    @Column(name="scheme")
    public String getScheme()
    {
        if ( this.scheme == null ) this.scheme = DEFAULT_WMI_SCHEME;

        return this.scheme;
    }

    public void setScheme( String newValue )
    {
        newValue = ( null == newValue ) ? DEFAULT_WMI_SCHEME : newValue.trim();
        this.scheme = newValue;
    }

    /**
     * URL Where you can access the file from.
     *
     * @return URL Where you can access the file from.
     */
    @Transient
    public String getUrl()
    {
        if ( this.url == null ) this.url = "";
        return this.url;
    }

    void setUrl( String newValue )
    {
        newValue = ( null == newValue ) ? "" : newValue;
        this.url = newValue.trim();
    }


    @Transient
    public void validate() throws ValidateException
    {
        if ( !"https".equals( this.scheme ) && !"http".equals( this.scheme )) {
            this.scheme = "https";
            /* no user configurable value for this */
            // throw new ValidateException( "invalid scheme: " + this.scheme );
        }

        if ( this.port <= 0 || this.port >= 0xFFFF ) {
            this.port = DEFAULT_WMI_PORT;

            /* no user configurable value for this */
            // throw new ValidateException( "invalid port: " + this. );
        }

        if ( !this.isEnabled ) return;

        if ( this.username == null || this.username.trim().length() == 0 ) {
            throw new ValidateException( "A username must be specified for the WMI server." );
        }

        if ( this.password == null || this.password.trim().length() == 0 ) {
            throw new ValidateException( "A password must be specified for the WMI server." );
        }

        if ( this.address == null || this.address.isEmpty()) {
            throw new ValidateException( "An address must be specified for the WMI server." );
        }
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "<live: " + this.isEnabled );
        sb.append( " user: " + this.username );
        sb.append( " scheme: " + this.scheme );
        sb.append( " addr: " + this.address );
        sb.append( " port: " + this.port + ">" );
        return sb.toString();
    }
}