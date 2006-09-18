/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: IntfConverter.java 7128 2006-09-06 17:32:14Z rbscott $
 */

package com.metavize.mvvm.argon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.metavize.mvvm.ArgonException;
import com.metavize.mvvm.IntfConstants;

final class ArgonInterfaceConverter
{
    private Logger logger = Logger.getLogger( this.getClass());

    static final byte  OUTSIDE        = IntfConstants.EXTERNAL_INTF;
    static final byte  NETCAP_OUTSIDE = OUTSIDE + 1;
    static final byte  INSIDE         = IntfConstants.INTERNAL_INTF;
    static final byte  NETCAP_INSIDE  = INSIDE + 1;
    static final byte  DMZ            = IntfConstants.DMZ_INTF;
    static final byte  NETCAP_DMZ     = DMZ + 1;
    static final byte  VPN            = IntfConstants.VPN_INTF;
    static final byte  NETCAP_VPN     = VPN + 1;
        
    private final ArgonInterface external;
    private final ArgonInterface internal;
    private final ArgonInterface dmz;
    private final ArgonInterface vpn;
    
    private final Map<Byte,ArgonInterface> argonToInterfaceMap;
    private final Map<Byte,ArgonInterface> netcapToInterfaceMap;
    private final Map<String,ArgonInterface> nameToInterfaceMap;

    private final List<ArgonInterface> intfList;

    /* Build an interface converter from a list of the argon interfaces */
    private ArgonInterfaceConverter( ArgonInterface external, ArgonInterface internal,
                                     ArgonInterface dmz,  ArgonInterface vpn, 
                                     Map<Byte,ArgonInterface> argon,
                                     Map<Byte,ArgonInterface> netcap,
                                     Map<String,ArgonInterface> name,
                                     List<ArgonInterface> intfList )
    {
        /* Setup all of the variables for fast access */
        this.external = external;
        this.internal = internal;
        this.dmz = dmz;
        this.vpn = vpn;
        
        /* Create unmodifiable copies of all of the maps and lists */
        this.argonToInterfaceMap = Collections.unmodifiableMap( new HashMap<Byte,ArgonInterface>( argon ));
        this.netcapToInterfaceMap = Collections.unmodifiableMap( new HashMap<Byte,ArgonInterface>( netcap ));
        this.nameToInterfaceMap = Collections.unmodifiableMap( new HashMap<String,ArgonInterface>( name ));
        this.intfList = Collections.unmodifiableList( new ArrayList<ArgonInterface>( intfList ));
    }

    /* Retrieve the interface that corresponds to a specific argon interface */
    ArgonInterface getIntfByArgon( byte argonIntf ) throws ArgonException
    {
        ArgonInterface intf = argonToInterfaceMap.get( argonIntf );
        if ( null == intf ) throw new ArgonException( "Invalid interface: " + argonIntf );
        return intf;
    }

    /* Retrieve the interface that corresponds to a specific netcap interface */
    ArgonInterface getIntfByNetcap( byte netcapIntf ) throws ArgonException
    {
        ArgonInterface intf = netcapToInterfaceMap.get( netcapIntf );
        if ( null == intf ) throw new ArgonException( "Invalid interface: " + netcapIntf );
        return intf;
    }

    /* Retrieve the interface that corresponds to the name */
    ArgonInterface getIntfByName( String name ) throws ArgonException
    {
        ArgonInterface intf = nameToInterfaceMap.get( name );
        if ( null == intf ) throw new ArgonException( "Invalid interface: '" + name + "'" );
        return intf;        
    }

    /* Get the External interface */
    ArgonInterface getExternal()
    {
        return this.external;
    }

    /* Get the Internal interface */
    ArgonInterface getInternal()
    {
        return this.internal;
    }

    /* This maybe null */
    ArgonInterface getDmz()
    {
        return this.dmz;
    }

    /* This may be null */
    ArgonInterface getVpn()
    {
        return this.vpn;
    }

    List<ArgonInterface> getIntfList()
    {
        return this.intfList;
    }

    /* Returns true if both converters have the same set of argon interfaces */
    boolean hasMatchingInterfaces( ArgonInterfaceConverter o ) 
    {
        return o.argonToInterfaceMap.keySet().equals( this.argonToInterfaceMap.keySet());
    }

    /* Register a new interface an interface from the interface list */
    ArgonInterfaceConverter registerIntf( ArgonInterface newIntf ) throws ArgonException
    {
        List<ArgonInterface> newIntfList = new LinkedList<ArgonInterface>( this.intfList );

        byte argonIntf = newIntf.getArgon();
        ArgonInterface intf = this.argonToInterfaceMap.get( argonIntf );

        /* If necessary remove the item from the interface list */
        if ( intf != null ) {
            logger.info( "Re-registering the interface [" + argonIntf + "]" );
            if ( !newIntfList.remove( intf )) {
                logger.warn( "Error re-registering the interface [" + argonIntf + "] continuing" );
            }
        }

        /* Add the new item to the list */
        newIntfList.add( newIntf );
        
        return makeInstance( newIntfList );
    }

    /* Remove an interface from the interface list */
    ArgonInterfaceConverter deregisterIntf( byte argonIntf ) throws ArgonException
    {
        if (( 0 > argonIntf ) || ( argonIntf > IntfConstants.MAX_INTF ) || 
            ( argonIntf < IntfConstants.DMZ_INTF )) {
            throw new ArgonException( "Unable to deregister the argon interface: " + argonIntf );
        }

        ArgonInterface intf = this.argonToInterfaceMap.get( argonIntf );

        if ( intf == null ) {
            logger.info( "Attempt to deregister an unregistered interface [" + argonIntf + "]" );
            return this;
        }
        
        List<ArgonInterface> newIntfList = new LinkedList<ArgonInterface>( this.intfList );
        if ( !newIntfList.remove( intf )) {
            logger.warn( "Error de-registering the interface [" + argonIntf + "] continuing" );
        }
        
        return makeInstance( newIntfList );
    }

    /* Make a new interface converter from a list of interfaces */
    static ArgonInterfaceConverter makeInstance( List<ArgonInterface> intfList )
        throws ArgonException
    {
        Map<Byte,ArgonInterface> argon  = new HashMap<Byte,ArgonInterface>();
        Map<Byte,ArgonInterface> netcap = new HashMap<Byte,ArgonInterface>();
        Map<String,ArgonInterface> name = new HashMap<String,ArgonInterface>();

        for ( ArgonInterface intf : intfList ) {
            if ( argon.put( intf.getArgon(), intf ) != null ) {
                throw new ArgonException( "The argon interface: " + intf.getArgon() + " is used twice" );
            }

            if ( netcap.put( intf.getNetcap(), intf ) != null ) {
                throw new ArgonException( "The netcap interface: " + intf.getNetcap() + " is used twice" );
            }

            if ( name.put( intf.getName(), intf ) != null ) {
                throw new ArgonException( "The name: " + intf.getName() + " is used twice" );
            }
        }

        ArgonInterface external = argon.get( IntfConstants.EXTERNAL_INTF );
        ArgonInterface internal = argon.get( IntfConstants.INTERNAL_INTF );
        ArgonInterface dmz      = argon.get( IntfConstants.DMZ_INTF );
        ArgonInterface vpn      = argon.get( IntfConstants.VPN_INTF );

        if ( internal == null ) throw new ArgonException( "The internal interface is not set" );
        if ( external == null ) throw new ArgonException( "The external interface is not set" );
        
        return new ArgonInterfaceConverter( external, internal, dmz, vpn, argon, netcap, name, intfList );
    }
}