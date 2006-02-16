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

package com.metavize.mvvm.networking.internal;

import com.metavize.mvvm.tran.ValidateException;

import com.metavize.mvvm.networking.Interface;
import com.metavize.mvvm.networking.EthernetMedia;

public class InterfaceInternal
{
    private final byte argonIntf;
    private final String intfName;
    private final NetworkSpaceInternal networkSpace;
    private final EthernetMedia ethernetMedia;
    private final boolean isPingable;

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
    }
    
    public byte getArgonIntf()
    {
        return this.argonIntf;
    }

    public String getIntfName()
    {
        return this.intfName;
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

    /* Returns a new interface object pre-filled with all of the data from this object,
     * careful using this method, this should only be used by NetworkUtilPriv since the space
     * must be set seperately.
     */
    public Interface toInterface()
    {
        return new Interface( this.argonIntf, this.ethernetMedia,
                              this.isPingable );
    }

    public String toString()
    {
        return 
            "argon intf:  "   + getArgonIntf() +
            "\nname:        " + getIntfName() +
            "\nspace-index: " + getNetworkSpace().getIndex() +
            "\neth-media:   " + getEthernetMedia() +
            "\npingable:    " + isPingable();
    }
    
    public static InterfaceInternal makeInterfaceInternal( Interface intf,
                                                           NetworkSpaceInternal networkSpace ) 
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
