/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking.internal;

import com.metavize.mvvm.tran.ValidateException;

import com.metavize.mvvm.networking.Interface;
import com.metavize.mvvm.networking.EthernetMedia;

public class InterfaceInternal
{
    private final byte argonIntf;
    /* This is the user representation of the interface name (eg. Internal/External) */
    private final String name;
    private final String intfName;
    private final NetworkSpaceInternal networkSpace;
    private final EthernetMedia ethernetMedia;
    private final boolean isPingable;

    private String connectionState ="";
    private String currentMedia = "";

    /* Done this way so validation can occur */
    private InterfaceInternal( Interface intf, String intfName, NetworkSpaceInternal networkSpace )
    {
        /* Set the network space, this can't be retrieved from the interface because 
         * the interface deals in NetworkSpace objects which are modifiable */
        this.networkSpace = networkSpace;

        this.argonIntf = intf.getArgonIntf();
        this.intfName  = intfName;
        this.ethernetMedia = intf.getEthernetMedia();
        this.isPingable = intf.getIsPingable();

        this.connectionState = intf.getConnectionState();
        this.currentMedia = intf.getCurrentMedia();

        this.name = intf.getName();
    }
    
    public byte getArgonIntf()
    {
        return this.argonIntf;
    }

    public String getIntfName()
    {
        return this.intfName;
    }
    
    public String getName()
    {
        return this.name;
    }

    public NetworkSpaceInternal getNetworkSpace()
    {
        return this.networkSpace;
    }
        
    public EthernetMedia getEthernetMedia()
    {
        return this.ethernetMedia;
    }

    public boolean isPingable()
    {
        return this.isPingable;
    }

    /** The following are read/write attributes, they reflect the state of the interfacse
     * that shouldn't be saved to the database */
    public String getConnectionState()
    {
        return this.connectionState;
    }

    public void setConnectionState( String newValue )
    {
        this.connectionState = newValue;
    }

    public String getCurrentMedia()
    {
        return this.currentMedia;
    }

    public void setCurrentMedia( String newValue )
    {
        this.currentMedia = newValue;
    }


    /* Returns a new interface object pre-filled with all of the data from this object,
     * careful using this method, this should only be used by NetworkUtilPriv since the space
     * must be set seperately.
     */
    public Interface toInterface()
    {
        Interface i = new Interface( this.argonIntf, this.ethernetMedia, this.isPingable );
        i.setName( getName());
        i.setConnectionState( getConnectionState());
        i.setCurrentMedia( getCurrentMedia());

        return i;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "argon intf:  " ).append( getArgonIntf());
        sb.append( "\nname:        " ).append( getName());
        sb.append( "\nphy-name:    " ).append( getIntfName());
        sb.append( "\nspace-index: " ).append( getNetworkSpace().getIndex());
        sb.append( "\neth-media:   " ).append( getEthernetMedia());
        sb.append( "\nstatus:      " ).append( getConnectionState() + "/" + getCurrentMedia());
        sb.append( "\npingable:    " ).append( isPingable());
        return sb.toString();
    }
    
    public static InterfaceInternal 
        makeInterfaceInternal( Interface intf, NetworkSpaceInternal networkSpace )
        throws ValidateException
    {
        /* unable to pass this in since the internal interfaces are
         * generated from a network space */
        String intfName = intf.getIntfName();
        if ( intfName == null || intfName.length() == 0 ) {
            throw new ValidateException( "Interface[" + intf.getArgonIntf() + "] must be assigned a name" );
        }

        return new InterfaceInternal( intf, intfName, networkSpace );
    }
    
}
