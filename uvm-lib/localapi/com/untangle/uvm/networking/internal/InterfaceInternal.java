/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.networking.internal;

import com.untangle.uvm.ArgonException;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.networking.EthernetMedia;
import com.untangle.uvm.networking.Interface;
import com.untangle.uvm.node.ValidateException;

public class InterfaceInternal
{
    /* This is the user representation of the interface name (eg. Internal/External) */
    private final String name;
    /* The system name of the interface (eg eth0) */
    private final String systemName;

    private final ArgonInterface argonIntf;
    private final NetworkSpaceInternal networkSpace;
    private final EthernetMedia ethernetMedia;
    private final boolean isPingable;
    private final boolean isPhysicalInterface;

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
        this.isPhysicalInterface = intf.isPhysicalInterface();

        this.name = intf.getName();
        this.systemName = intf.getSystemName();
    }
    
    public ArgonInterface getArgonIntf()
    {
        return this.argonIntf;
    }
    
    public String getName()
    {
        return this.name;
    }

    public String getSystemName()
    {
        return this.systemName;
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

    /**
     * Get whether or not this is a physical interface
     */
    public boolean isPhysicalInterface()
    {
        return this.isPhysicalInterface;
    }

    /* Returns a new interface object pre-filled with all of the data from this object,
     * careful using this method, this should only be used by NetworkUtilPriv since the space
     * must be set seperately.
     */
    public Interface toInterface()
    {
        Interface i = new Interface( this.argonIntf.getArgon(), this.ethernetMedia, this.isPingable, 
                                     this.isPhysicalInterface );
        i.setName( getName());
        i.setSystemName( getSystemName());
        i.setConnectionState( getConnectionState());
        i.setCurrentMedia( getCurrentMedia());

        return i;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "argon intf:  " ).append( getArgonIntf());
        sb.append( "\nname:        " ).append( getName());
        sb.append( "\nsystem-name: " ).append( getSystemName());
        sb.append( "\nspace-name: " ).append( getNetworkSpace().getName());
        sb.append( "\neth-media:   " ).append( getEthernetMedia());
        sb.append( "\nstatus:      " ).append( getConnectionState() + "/" + getCurrentMedia());
        sb.append( "\npingable:    " ).append( isPingable());
        sb.append( "\nis-physical:    " ).append( isPhysicalInterface());
        return sb.toString();
    }
    
    public static InterfaceInternal 
        makeInterfaceInternal( Interface intf, NetworkSpaceInternal networkSpace )
        throws ValidateException
    {
        ArgonInterface argonIntf = null;

        try {
            LocalIntfManager lim = LocalUvmContextFactory.context().localIntfManager();
            argonIntf = lim.getIntfByArgon( intf.getArgonIntf());
        } catch ( ArgonException e ) {
            throw new ValidateException( "Invalid argon interface: " + argonIntf, e );
        }

        return new InterfaceInternal( intf, argonIntf, networkSpace );
    }
    
}
