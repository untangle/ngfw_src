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

package com.untangle.uvm.networking;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.Rule;
import org.hibernate.annotations.Type;

/**
 * A description of an interface.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_network_intf", schema="settings")
public class Interface extends Rule
{
    private static final long serialVersionUID = 7720361692356024775L;

    /* The argon interface identifier for this interface */
    private byte argonIntf;
    
    /* The network space this interface belongs to */
    private NetworkSpace networkSpace;

    /* The current media for this interface */
    private EthernetMedia ethernetMedia = EthernetMedia.AUTO_NEGOTIATE;

    /* This is the current status of the ethernet port */
    private String currentMedia = "";

    /* True when the untangle should answer to ping on this interface */
    private boolean isPingable = true;

    /* The current state of the connection */
    private String connectionState = "";

    public Interface() { }

    public Interface( byte argonIntf, EthernetMedia ethernetMedia, boolean isPingable )
    {
        this.argonIntf     = argonIntf;
        this.ethernetMedia = ethernetMedia;
        this.isPingable    = isPingable;
    }

    /**
     * Retrieve the argon interface id for this interface.
     *
     * @return The argon interface id for this interface.
     */
    @Column(name="argon_intf", nullable=false)
    public byte getArgonIntf()
    {
        return this.argonIntf;
    }

    /**
     * Set the argon interface id for this interface.
     *
     * @param newValue The new argon interface id for this interface.
     */
    public void setArgonIntf( byte newValue )
    {
        this.argonIntf = newValue;
    }

    /**
     * Retrieve the network space this interface belongs to.
     *
     * @return The network space this interface belongs to.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="network_space")
    public NetworkSpace getNetworkSpace()
    {
        return this.networkSpace;
    }

    /**
     * Set the network space this interface belongs to.
     *
     * @param newValue The network space this interface belongs to.
     */
    public void setNetworkSpace( NetworkSpace newValue )
    {
        this.networkSpace = newValue;
    }

    /**
     * Retrieve the media for this interface.
     * 
     * @return The media for this interface.
     */
    @Column(name="media")
    @Type(type="com.untangle.uvm.networking.EthernetMediaUserType")
    public EthernetMedia getEthernetMedia()
    {
        if ( this.ethernetMedia == null ) this.ethernetMedia = EthernetMedia.AUTO_NEGOTIATE;
        return this.ethernetMedia;
    }

    /**
     * Set the media for this interface.
     * 
     * @param newValue The media for this interface.
     */
    public void setEthernetMedia( EthernetMedia newValue )
    {
        if ( newValue == null ) newValue = EthernetMedia.AUTO_NEGOTIATE;
        this.ethernetMedia = newValue;
    }

    /**
     * Whether or not this interface should respond to ping.  This may
     * be more appropriate as a property of the space.
     *
     * @return True iff this interface responds to ping.
     */
    @Column(name="pingable", nullable=false)
    public boolean getIsPingable()
    {
        return this.isPingable;
    }

    /**
     * Set Whether or not this interface should respond to ping.
     *
     * @param newValue True iff this interface responds to ping.
     */
    public void setIsPingable( boolean newValue )
    {
        this.isPingable = isPingable;
    }

    /** The following are not stored in the database ***/
    
    /**
     * A string representation of whether or not the interface is
     * connected.
     *
     * @return Whether or not the interface is connected.
     */
    @Transient
    public String getConnectionState()
    {
        return this.connectionState;
    }

    /**
     * Set the string representing whether or not the interface is
     * connected.
     *
     * @param newValue Whether or not the interface is
     * connected.
     */
    public void setConnectionState( String newValue )
    {
        this.connectionState = newValue;
    }

    /**
     * A user string describing the current media of this interface.
     *
     * @return The current interface media.
     */
    @Transient
    public String getCurrentMedia()
    {
        return this.currentMedia;
    }

    /**
     * Set the user representation the current media of this
     * interface.
     *
     * @param newValue The current interface media.
     */
    public void setCurrentMedia( String newValue )
    {
        this.currentMedia = newValue;
    }
}
