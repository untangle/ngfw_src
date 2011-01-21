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

package com.untangle.uvm.localapi;

import com.untangle.uvm.IntfConstants;

/** ArgonInterface:
 * Contains information about a physical interfaces. */
public final class ArgonInterface
{
    /* The physical name of the interface, eg eth0 */
    private final String physicalName;

    /* Secondary name of an interface.  This is for virtual devices
     * like ppp which can replace the physical interface. */
    private final String secondaryName;

    private final String userName;

    /* Index for the interface */
    private final int interfaceId;

    /* The string value */
    private final String string;

    /* True if this is a WAN interface */
    private final boolean isWanInterface;

    public ArgonInterface( String physicalName, int interfaceId, String userName )
    {
        this( physicalName, null, interfaceId, userName );
    }

    /**
     * In order to avoid the pain of typecasting everywhere, netcap and argon are
     * should be bytes, but are typecast inside of the constructor
     */
    public ArgonInterface( String physicalName, String secondaryName, int interfaceId, String userName )
    {
        this( physicalName, secondaryName, interfaceId, userName, false );
    }

    public ArgonInterface( String physicalName, String secondaryName, int interfaceId, String userName, boolean isWanInterface )
    {
        this.physicalName = physicalName.trim();
        if ( secondaryName == null ) secondaryName = "";
        this.secondaryName = secondaryName.trim();
        this.interfaceId = interfaceId;
        this.string =  "'" + this.physicalName + "," +  this.secondaryName + "' " + this.interfaceId;
        this.userName = userName;
        this.isWanInterface = isWanInterface;
    }
                               
    /** Get the name of the interface where traffic should go be routed. */
    public String getName()
    {
        if ( this.secondaryName != null && this.secondaryName.length() > 0 ) return this.secondaryName;

        return this.physicalName;
    }

    /** Get the linux/physical name of the interface (eg. eth0) */
    public String getPhysicalName()
    {
        return this.physicalName;
    }

    /** Get the name of the secondary interface, either (eg. null or ppp0) */
    public String getSecondaryName()
    {
        return this.secondaryName;
    }

    public String getUserName()
    {
        return this.userName;
    }

    /** Determine if there is a secondary name */
    public boolean hasSecondaryName()
    {
        return (( this.secondaryName != null ) && ( this.secondaryName.length() > 0 ));
    }

    /** Get the index that netcap uses to reference the interface */
    public int getInterfaceId()
    {
        return this.interfaceId;
    }

    public boolean isWanInterface()
    {
        return this.isWanInterface;
    }

    public String toString()
    {
        return this.string;
    }

    public boolean equals(Object o)
    {
        ArgonInterface ai = (ArgonInterface)o;
        return ( this.physicalName == null ? ai.physicalName == null :
                 this.physicalName.equals( ai.physicalName )) &&
            ( this.secondaryName == null ? ai.secondaryName == null :
              this.secondaryName.equals( ai.secondaryName )) &&
            ( this.interfaceId == ai.interfaceId );
    }

    public int hashCode()
    {
        int result = 17;
        if ( this.physicalName != null ) result = ( 37 * result ) + this.physicalName.hashCode();
        if ( this.secondaryName != null ) result = ( 37 * result ) + this.secondaryName.hashCode();
        result = ( 37 * result ) + ( 263 + interfaceId );
        return result;
    }
}
