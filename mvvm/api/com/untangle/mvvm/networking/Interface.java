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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.tran.Rule;
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
    private byte argonIntf;
    private String intfName; /* non-hibernate */
    private NetworkSpace networkSpace;
    private EthernetMedia ethernetMedia = EthernetMedia.AUTO_NEGOTIATE;
    /* This is the current status of the ethernet port */
    private String currentMedia = "";
    private boolean isPingable = true;
    private String connectionState = "";

    public Interface() { }

    public Interface( byte argonIntf, EthernetMedia ethernetMedia, boolean isPingable )
    {
        this.argonIntf     = argonIntf;
        this.ethernetMedia = ethernetMedia;
        this.isPingable    = isPingable;
    }

    /**
     * @return The argon interface id for this interface.
     */
    @Column(name="argon_intf", nullable=false)
    public byte getArgonIntf()
    {
        return this.argonIntf;
    }

    public void setArgonIntf( byte argonIntf )
    {
        this.argonIntf = argonIntf;
    }

    /**
     * @return The network space this interface belongs to
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="network_space")
    public NetworkSpace getNetworkSpace()
    {
        return this.networkSpace;
    }

    public void setNetworkSpace( NetworkSpace networkSpace )
    {
        this.networkSpace = networkSpace;
    }

    /**
     * The media for type for this interface.
     * @return The media for type for this interface.
     */
    @Column(name="media")
    @Type(type="com.untangle.mvvm.networking.EthernetMediaUserType")
    public EthernetMedia getEthernetMedia()
    {
        if ( this.ethernetMedia == null ) this.ethernetMedia = EthernetMedia.AUTO_NEGOTIATE;
        return this.ethernetMedia;
    }

    public void setEthernetMedia( EthernetMedia ethernetMedia )
    {
        if ( ethernetMedia == null ) ethernetMedia = EthernetMedia.AUTO_NEGOTIATE;
        this.ethernetMedia = ethernetMedia;
    }

    /**
     * @return Whether or not this interface should respond to pings, this may be
     *         more appropriate at a property of the space.
     */
    @Column(name="pingable", nullable=false)
    public boolean getIsPingable()
    {
        return this.isPingable;
    }

    public void setIsPingable( boolean isPingable )
    {
        this.isPingable = isPingable;
    }

    @Transient
    public String getConnectionState()
    {
        return this.connectionState;
    }

    public void setConnectionState( String connectionState )
    {
        this.connectionState = connectionState;
    }

    /** The following are not stored in the database ***/
    @Transient
    public String getCurrentMedia()
    {
        return this.currentMedia;
    }

    public void setCurrentMedia( String newValue )
    {
        this.currentMedia = newValue;
    }
}
