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

import com.metavize.mvvm.tran.Rule;

/**
 * A description of an interface.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="mvvm_network_intf"
 */
public class Interface extends Rule
{
    private byte argonIntf;
    private String intfName; /* non-hibernate */
    private NetworkSpace networkSpace;
    private EthernetMedia ethernetMedia = EthernetMedia.AUTO_NEGOTIATE;
    /* This is the current status of the ethernet port */
    private String currentMedia = "Link ok";
    private boolean isPingable = true;

    public Interface()
    {
    }

    public Interface( byte argonIntf, EthernetMedia ethernetMedia, boolean isPingable )
    {
        this.argonIntf     = argonIntf;
        this.ethernetMedia = ethernetMedia;
        this.isPingable    = isPingable;
    }

    /**
     * @return The argon interface id for this interface.
     * @hibernate.property
     * column="argon_intf"
     */
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
     * @hibernate.many-to-one
     * cascade="all"
     * class="com.metavize.mvvm.networking.NetworkSpace"
     * column="network_space"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.networking.EthernetMediaUserType"
     * @hibernate.column
     * name="media"
     */
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
     * @hibernate.property
     * column="pingable"
     */
    public boolean getIsPingable()
    {
        return this.isPingable;
    }

    public void setIsPingable( boolean isPingable )
    {
        this.isPingable = isPingable;
    }

    /** The following are not stored in the database ***/
    public String getCurrentMedia()
    {
        return this.currentMedia;
    }

    void setCurrentMedia( String newValue )
    {
        this.currentMedia = newValue;
    }

    public String getIntfName()
    {
        return this.intfName;
    }

    /* Should only be settable locally */
    void setIntfName( String newValue )
    {
        this.intfName = newValue;
    }
}
