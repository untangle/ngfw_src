/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.argon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.untangle.jnetcap.JNetcapException;
import com.untangle.jnetcap.Netcap;
import com.untangle.uvm.ArgonException;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.IntfEnum;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.policy.LocalPolicyManager;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import org.apache.log4j.Logger;

/* Manager for controlling argon -> netcap interface matching */
class LocalIntfManagerImpl implements LocalIntfManager
{
    /* File where the custom interface properties are stored */
    private static final String CUSTOM_INTF_FILE =
        System.getProperty( "bunnicula.conf.dir" ) + "/argon.properties";

    /* Name of the property used for the custom interfaces */
    private static final String PROPERTY_CUSTOM_INTF = "argon.userintf";

    /* Comment for the custom interfaces */
    private static final String PROPERTY_COMMENT = "Custom interfaces (eg VPN)";

    /* Separator that goes in between each custom interface */
    private static final String CUSTOM_INTF_SEPARATOR = "_";

    /* Separator that goes in between each interface name and its device index */
    private static final String CUSTOM_INTF_VAL_SEPARATOR = ",";


    private ArgonInterfaceConverter intfConverter = null;

    private final Logger logger = Logger.getLogger( this.getClass());

    /* Converter from all of the interface indexes to their display name(eg. external) */
    private IntfEnum intfEnum;

    /**
     * Convert an interface using the argon standard (0 = outside, 1 = inside, 2 = DMZ 1, etc)
     * to an interface that uses that netcap unique identifiers
     */
    public byte toNetcap( byte argonIntf )
    {
        switch ( argonIntf ) {
        case IntfConstants.ARGON_ERROR:
            throw new IllegalArgumentException( "Invalid argon interface[" + argonIntf + "]" );
        case IntfConstants.ARGON_UNKNOWN:  return IntfConstants.NETCAP_UNKNOWN;
        case IntfConstants.ARGON_LOOPBACK: return IntfConstants.NETCAP_LOOPBACK;
        }

        /* May actually want to check if interfaces exists */
        if ( argonIntf < IntfConstants.ARGON_MIN  || argonIntf > IntfConstants.ARGON_MAX ) {
            throw new IllegalArgumentException( "Invalid argon interface[" + argonIntf + "]" );
        }

        return (byte)(argonIntf + 1);
    }


    /**
     * Convert an interface from a netcap interface to the argon standard
     */
    public byte toArgon( byte netcapIntf )
    {
        switch ( netcapIntf ) {
        case IntfConstants.NETCAP_ERROR:
            throw new IllegalArgumentException( "Invalid netcap interface[" + netcapIntf + "]" );
        case IntfConstants.NETCAP_UNKNOWN:  return IntfConstants.ARGON_UNKNOWN;
        case IntfConstants.NETCAP_LOOPBACK: return IntfConstants.ARGON_LOOPBACK;
        }

        /* May actually want to check if interfaces exists */
        if ( netcapIntf < IntfConstants.NETCAP_MIN  || netcapIntf > IntfConstants.NETCAP_MAX ) {
            throw new IllegalArgumentException( "Invalid netcap interface[" + netcapIntf + "]" );
        }

        return (byte)(netcapIntf - 1);
    }

    /* Convert from an argon interface to the physical name of the interface */
    public String argonIntfToString( byte argonIntf ) throws ArgonException
    {
        return this.intfConverter.getIntfByArgon( argonIntf ).getName();
    }

    /* Retrieve the interface that corresponds to a specific argon interface */
    public ArgonInterface getIntfByArgon( byte argonIntf ) throws ArgonException
    {
        return this.intfConverter.getIntfByArgon( argonIntf );
    }

    /* Retrieve the interface that corresponds to a specific netcap interface */
    public ArgonInterface getIntfByNetcap( byte netcapIntf ) throws ArgonException
    {
        return this.intfConverter.getIntfByNetcap( netcapIntf );
    }

    /* Retrieve the interface that corresponds to the name */
    public ArgonInterface getIntfByName( String name ) throws ArgonException
    {
        return this.intfConverter.getIntfByName( name );
    }

    /* Get the External interface */
    public ArgonInterface getExternal()
    {
        return this.intfConverter.getExternal();
    }

    /* Get the Internal interface */
    public ArgonInterface getInternal()
    {
        return this.intfConverter.getInternal();
    }

    /* This maybe null */
    public ArgonInterface getDmz()
    {
        return this.intfConverter.getDmz();
    }

    public List<ArgonInterface> getIntfList()
    {
        return this.intfConverter.getIntfList();
    }

    /* This is a list of non-physical interfaces (everything except for internal, external and dmz ).
     * This list would contain interfaces like VPN. */
    public List<ArgonInterface> getCustomIntfList()
    {
        return this.intfConverter.getCustomIntfList();
    }

    /* Return an array of the argon interfaces */
    public byte[] getArgonIntfArray()
    {
        return this.intfConverter.getArgonIntfArray();
    }

    /* Register a custom interface.  EG. VPN */
    public synchronized void registerIntf( String name, byte argon ) throws ArgonException
    {
        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;

        /* Unable to replace internal and DMZ, those can only be replaced as secondary interfaces */
        if ( argon == IntfConstants.EXTERNAL_INTF || argon == IntfConstants.INTERNAL_INTF ) {
            throw new ArgonException( "Unable to re-register the internal or external interfaces: " + name );
        }

        logger.debug( "Registering the interface: [" + argon + ":" + name +"]" );
        this.intfConverter = this.intfConverter.registerIntf( new ArgonInterface( name, argon ));

        notifyDependents( prevIntfConverter );
    }

    /* Register a secondary interface, this is an interface that replaces another interface,
     * EG. if ETH0 -> PPP0, PPP0 is the secondary interface and ETH0 is the primary interface */
    public synchronized void registerSecondaryIntf( String name, byte argon ) throws ArgonException
    {
        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;

        logger.debug( "Registering the secondary interface: [" + argon + ":" + name +"]" );
        ArgonInterface intf = this.intfConverter.getIntfByArgon( argon );
        this.intfConverter = this.intfConverter.registerIntf( intf.makeNewSecondaryIntf( name ));
        notifyDependents( prevIntfConverter );
    }

    /* Unregister a custom interface or DMZ. */
    public synchronized void unregisterIntf( byte argon ) throws ArgonException
    {
        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;
        this.intfConverter = this.intfConverter.unregisterIntf( argon );

        notifyDependents( prevIntfConverter );
    }

    /* Unregister an individual secondary interface */
    public synchronized void unregisterSecondaryIntf( byte argon ) throws ArgonException
    {
        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;

        logger.debug( "Unregistering the secondary interface: [" + argon + "]" );
        ArgonInterface intf = this.intfConverter.getIntfByArgon( argon );
        this.intfConverter = this.intfConverter.registerIntf( intf.makeNewSecondaryIntf( null ));
        notifyDependents( prevIntfConverter );
    }

    /* This resets all of the secondary interfaces to their physical interfaces */
    public synchronized void resetSecondaryIntfs()
    {
        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;

        logger.debug( "Unregistering all secondary interfaces." );
        try {
            this.intfConverter = this.intfConverter.resetSecondaryIntfs();
            notifyDependents( prevIntfConverter );
        } catch ( ArgonException e ) {
            logger.error( "Error while resetting the secondary interfaces continuing.", e );
        }
    }

    /* Retrieve the current interface enumeration */
    public IntfEnum getIntfEnum()
    {
        return this.intfEnum;
    }

    /* ----------------- Package ----------------- */
    LocalIntfManagerImpl()
    {
    }

    /* Initialize the interface converter */
    void initializeIntfArray( String internal, String external, String dmz ) throws ArgonException
    {
        logger.debug( "Initializing array with: '" + internal + "','" + external + "','" + dmz );

        /* Create a new list for the interfaces */
        List<ArgonInterface> intfList = toInterfaceList( internal, external, dmz );

        /* Load all of the custom interfaces from the file */
        loadCustomIntfs( intfList );

        /* Initialize the interface converter */
        this.intfConverter = ArgonInterfaceConverter.makeInstance( intfList );

        notifyDependents( null );
    }

    /* ----------------- Private ----------------- */
    /* Notify everything that needs to be aware of changes to the interface array */
    private void notifyDependents( ArgonInterfaceConverter prevIntfConverter ) throws ArgonException
    {
        /* Save the state of the custom interfaces to a file */
        saveCustomIntfs();

        /* Notify netcap that there has been a change to the interfaces */
        try {
            Netcap.getInstance().configureInterfaceArray( this.intfConverter.getNetcapIntfArray(),
                                                          this.intfConverter.getNameArray());
        } catch ( JNetcapException e ) {
            logger.warn( "Error updating interface array", e );
            throw new ArgonException( "Unable to configure interface array", e );
        }

        /* Update the interface enumeration */
        updateIntfEnum();

        /* If there are new interfaces, then notify the policy manager */
        if ( !this.intfConverter.hasMatchingInterfaces( prevIntfConverter )) {
            UvmContextFactory.context().policyManager().reconfigure( getArgonIntfArray());
        }

        /* Update the interface matcher factory, this should be a listener */
        IntfMatcherFactory.getInstance().updateEnumeration( this.intfEnum );
    }

    /* Write the list of custom node interfaces to a file */
    private void saveCustomIntfs() throws ArgonException
    {
        /* List, _ seperated, of interfaces and their corresponding device */
        /* Write out the list of user interfaces */
        List<ArgonInterface> cil = this.intfConverter.getCustomIntfList();

        String list = "";
        for ( ArgonInterface ci : cil ) {
            if ( list.length() > 0 ) list += CUSTOM_INTF_SEPARATOR;
            list += ci.getName() + CUSTOM_INTF_VAL_SEPARATOR + ci.getArgon();
        }

        Properties properties = new Properties();
        properties.setProperty( PROPERTY_CUSTOM_INTF, list );

        try {
            logger.debug( "Storing properties into: " + CUSTOM_INTF_FILE );
            properties.store( new FileOutputStream( new File( CUSTOM_INTF_FILE )), PROPERTY_COMMENT );
        } catch ( Exception e ) {
            logger.error( "Unable to write node interface properties:" + CUSTOM_INTF_FILE, e );
        }
    }

    /* Load the list of node interfaces from a file, and append them to the interface array */
    private void loadCustomIntfs( List<ArgonInterface> list ) throws ArgonException
    {
        String customIntfs = "";


        try {
            Properties properties = new Properties();
            File f = new File( CUSTOM_INTF_FILE );
            String temp;

            if ( f.exists()) {
                properties.load( new FileInputStream( f ));
                if (( temp = properties.getProperty( PROPERTY_CUSTOM_INTF )) != null ) {
                    customIntfs = temp;
                } else {
                    customIntfs = "";
                }
            }
        } catch ( Exception e ) {
            logger.warn( "Error loading node interface file, defaulting to no custom interfaces.", e );
            customIntfs = "";
        }

        for ( String customIntf : customIntfs.split( CUSTOM_INTF_SEPARATOR )) {
            customIntf = customIntf.trim();

            /* Ignore the empty string */
            if ( customIntf.length() == 0 ) continue;

            String[] values = customIntf.split( CUSTOM_INTF_VAL_SEPARATOR );
            if ( values.length != 2 ) {
                logger.warn( "Invalid custom intf: '" + customIntf + "'" );
                continue;
            }

            String deviceName = values[0];
            byte   argonIntf;
            try {
                argonIntf = Byte.parseByte( values[1] );
            } catch ( NumberFormatException e ) {
                logger.error( "Invalid argon interface index: '" + values[1] + "'" );
                continue;
            }

            /* Check if the interface has a valid index */
            if (( 0 > argonIntf ) || ( argonIntf > IntfConstants.MAX_INTF ) ||
                ( argonIntf <= IntfConstants.DMZ_INTF )) {
                logger.error( "Invalid argon intf: " + argonIntf + " '" + deviceName + "', continuing" );
                continue;
            }

            list.add( new ArgonInterface( deviceName, argonIntf ));
        }
    }

    /* Update the interface enumeration */
    private void updateIntfEnum()
    {
        byte[] argonIntfArray = getArgonIntfArray();

        Arrays.sort( argonIntfArray );

        String[] intfNameArray = new String[argonIntfArray.length];

        for ( int c = 0; c < argonIntfArray.length ; c++ ) {
            String name = "unknown";
            byte intf = argonIntfArray[c];
            switch ( intf ) {
            case IntfConstants.EXTERNAL_INTF: name = IntfConstants.EXTERNAL; break;
            case IntfConstants.INTERNAL_INTF: name = IntfConstants.INTERNAL; break;
            case IntfConstants.DMZ_INTF:      name = IntfConstants.DMZ;      break;
            case IntfConstants.VPN_INTF:      name = IntfConstants.VPN;      break;
            default:
                logger.error( "Unknown interface: " + intf + " using unknown" );
                continue;
            }

            intfNameArray[c] = name;
        }

        this.intfEnum = new IntfEnum( argonIntfArray, intfNameArray );
    }

    /* Convert the arguments into a list of ArgonInterfaces */
    private List<ArgonInterface> toInterfaceList( String internal, String external, String dmz )
    {
        List<ArgonInterface> list = new LinkedList<ArgonInterface>();

        list.add( new ArgonInterface( internal.trim(), IntfConstants.INTERNAL_INTF ));
        list.add( new ArgonInterface( external.trim(), IntfConstants.EXTERNAL_INTF ));
        if ( null != dmz ) list.add( new ArgonInterface( dmz.trim(), IntfConstants.DMZ_INTF ));

        return list;
    }
}
