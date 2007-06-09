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

package com.untangle.uvm.networking.internal;

import com.untangle.uvm.node.ValidateException;

import com.untangle.uvm.ArgonException;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.localapi.LocalIntfManager;

import com.untangle.uvm.networking.Interface;
import com.untangle.uvm.networking.EthernetMedia;

public class InterfaceInternal
{
    /* This is the user representation of the interface name (eg. Internal/External) */
    private final String name;
    private final ArgonInterface argonIntf;
    private final NetworkSpaceInternal networkSpace;
    private final EthernetMedia ethernetMedia;
    private final boolean isPingable;

    private String connectionState ="";
    private String currentMedia = "";

    /* Done this way so validation can occur */
    private InterfaceInternal( Interface intf, ArgonInterface argonIntf, NetworkSpaceInternal networkSpace )
    {
        /* Set the network space, this can't be retrieved from the interface because 
         * the interface deals in NetworkSpace objects which are modifiable */
        this.networkSpace = networkSpace;

        this.argonIntf = argonIntf;
        this.ethernetMedia = intf.getEthernetMedia();
        this.isPingable = intf.getIsPingable();

        this.connectionState = intf.getConnectionState();
        this.currentMedia = intf.getCurrentMedia();

        this.name = intf.getName();
    }
    
    public ArgonInterface getArgonIntf()
    {
        return this.argonIntf;
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
        Interface i = new Interface( this.argonIntf.getArgon(), this.ethernetMedia, this.isPingable );
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
        ArgonInterface argonIntf = null;

        try {
            LocalIntfManager lim = UvmContextFactory.context().localIntfManager();
            argonIntf = lim.getIntfByArgon( intf.getArgonIntf());
        } catch ( ArgonException e ) {
            throw new ValidateException( "Invalid argon interface: " + argonIntf, e );
        }

        return new InterfaceInternal( intf, argonIntf, networkSpace );
    }
    
}
