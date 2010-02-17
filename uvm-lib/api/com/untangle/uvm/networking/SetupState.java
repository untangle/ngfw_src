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

package com.untangle.uvm.networking;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.untangle.uvm.node.ParseException;

public final class SetupState implements Serializable
{
    /* Map from the unique identifier to a setup state */
    private static final Map<Integer,SetupState> TYPE_TO_SETUP = new HashMap<Integer,SetupState>();

    /* Map from the setup state name to a setup state */
    private static final Map<String,SetupState>  NAME_TO_SETUP = new HashMap<String,SetupState>();

    /* The settings have been configured through the wizard */
    public static final SetupState WIZARD  = new SetupState( 0, "wizard-unconfigured" );

    /* The settings have not been upgraded yet. */
    public static final SetupState NETWORK_SHARING  = new SetupState( 1, "deprecated" );
        
    /* The user hasn't selected basic or advanced. (unsupported state) */
    public static final SetupState UNCONFIGURED  = new SetupState( 2, "Unconfigured" );
        
    /* The user wants a simple interface */
    public static final SetupState BASIC = new SetupState( 3, "Basic" );
    
    /* The user is using advanced network settings. */
    public static final SetupState ADVANCED = new SetupState( 4, "Advanced" );
    
    /* mask used to indicate that the settings need to be restored
     * from the database */
    private static final int RESTORE_MASK = 0x40;

    /* Basic settings are in the database, but haven't been written to
     * the /etc/network interfaces yet */
    static final SetupState BASIC_RESTORE  =
        new SetupState( BASIC.type | RESTORE_MASK, "Basic Restore" );
    
    /* Advanced settings are in the database, but haven't been written
     * to the /etc/network interfaces yet */
    static final SetupState ADVANCED_RESTORE  =
        new SetupState( ADVANCED.type | RESTORE_MASK, "Advanced Restore" );

    /* All of the possible setup states. */
    private static final SetupState ENUMERATION[] =
    {
        WIZARD, NETWORK_SHARING, UNCONFIGURED, BASIC, ADVANCED, BASIC_RESTORE, ADVANCED_RESTORE
    };

    /* Unique identifier for this setup state */
    private final int type;

    /* User representation for this setup state */
    private final String name;

    private SetupState( int type, String name )
    {
        this.type  = type;
        this.name  = name;
    }

    /**
     * Retrieve the unique identifier of this setup state.
     *
     * @return The unique identifier of this setup state.
     */
    public int getType()
    {
        return this.type;
    }

    /**
     * Retrieve the user representation of this setup state.
     *
     * @return The user representation of this setup state.
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Return true if the current state is in the restore state.
     *
     * @return True iff the network settings must be restored.
     */
    boolean isRestore()
    {
        return ( this.type & RESTORE_MASK ) == RESTORE_MASK;
    }

    public String toString()
    {
        return this.name;
    }

    /**
     * Get all of the possible setup states.
     *
     * @return An array of all of the possible setup states.
     */
    public static SetupState[] getEnumeration()
    {
        return ENUMERATION;
    }

    /**
     * Get the default setup state.
     *
     * @return the default setup state.
     */
    public static SetupState getDefault()
    {
        return ENUMERATION[0];
    }

    /**
     * Retrieve a setup state based on its type.
     *
     * @param type The type to retrieve.
     * @return The <code>SetupState</code> corresponding to
     * <code>type</code>
     * @exception ParseException If <code>type</code> is not valid.
     */
    public static SetupState getInstance( int type ) throws ParseException
    {
        SetupState instance = TYPE_TO_SETUP.get( type );
        if ( instance == null ) throw new ParseException( "Invalid setup state["+ type + "]." );
        return instance;
    }

    @Override
    public boolean equals( Object o )
    {
        if (!(o instanceof SetupState )) return false;

        SetupState ss = (SetupState)o;
        return ( this.type == ss.type );
    }

    @Override
    public int hashCode()
    {
        return ( this.type * 103423 ) %  104659;
    }
    
    static
    {
        for ( SetupState em : ENUMERATION ) {
            TYPE_TO_SETUP.put( em.getType(), em );
            NAME_TO_SETUP.put( em.getName(), em );
        }
    }
}
