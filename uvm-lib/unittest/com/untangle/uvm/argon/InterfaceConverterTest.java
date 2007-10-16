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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.ArgonException;

import com.untangle.uvm.localapi.ArgonInterface;

public class InterfaceConverterTest
{
    private static final ArgonInterface EXTERNAL     = new ArgonInterface( "eth0", (byte)0, (byte)1 );
    private static final ArgonInterface EXTERNAL_PPP = new ArgonInterface( "eth0", "ppp0", (byte)0, (byte)1 );
    private static final ArgonInterface INTERNAL     = new ArgonInterface( "eth1", (byte)1, (byte)2 );
    private static final ArgonInterface DMZ          = new ArgonInterface( "eth2", (byte)2, (byte)3 );
    private static final ArgonInterface VPN          = new ArgonInterface( "tun0", (byte)3, (byte)4 );
    private static final ArgonInterface NODE    = new ArgonInterface( "dummy0", (byte)4, (byte)5 );

    private static final ArgonInterface SAME_ARGON   = new ArgonInterface( "foobar", (byte)7, (byte)1 );
    private static final ArgonInterface SAME_NETCAP  = new ArgonInterface( "foobar", (byte)1, (byte)7 );
    private static final ArgonInterface SAME_NAME    = new ArgonInterface( "eth1", (byte)8, (byte)7 );

    private static final List<ArgonInterface> BASIC = 
        Arrays.asList( new ArgonInterface[] { EXTERNAL, INTERNAL, DMZ } );

    private static final List<ArgonInterface> NO_DMZ = 
        Arrays.asList( new ArgonInterface[] { EXTERNAL, INTERNAL } );

    private static final List<ArgonInterface> NO_DMZ_VPN = 
        Arrays.asList( new ArgonInterface[] { EXTERNAL, INTERNAL, VPN } );

    private static final List<ArgonInterface> FULL = 
        Arrays.asList( new ArgonInterface[] { EXTERNAL, INTERNAL, DMZ, VPN, NODE } );
    
    private static final List<ArgonInterface> FULL_CUSTOM =
        Arrays.asList( new ArgonInterface[] { VPN, NODE } );

    public InterfaceConverterTest()
    {
    }

    @Test public void basic() throws ArgonException
    {
        /* Make sure all of the functions work */
        testBasics( BASIC, ArgonInterfaceConverter.makeInstance( BASIC ));
        testBasics( FULL, ArgonInterfaceConverter.makeInstance( FULL ));
        testBasics( NO_DMZ, ArgonInterfaceConverter.makeInstance( NO_DMZ ));
        testBasics( NO_DMZ_VPN, ArgonInterfaceConverter.makeInstance( NO_DMZ_VPN ));

    }

    @Test public void register() throws ArgonException
    {
        ArgonInterfaceConverter instance = ArgonInterfaceConverter.makeInstance( BASIC );

        /* Insert the VPN interface */
        ArgonInterfaceConverter newInstance = instance.registerIntf( VPN );

        /* Verify the new changes */
        List<ArgonInterface> newList = new LinkedList<ArgonInterface>( BASIC );
        newList.add( VPN );
        testBasics( newList, newInstance );
        /* Verify that hasMatchingInterfaces returns false */
        Assert.assertFalse( newInstance.hasMatchingInterfaces( instance ));
        instance = newInstance;
        
        /* Register again, and verify it doesn't change */
        newInstance = instance.registerIntf( VPN );
        testBasics( newList, newInstance );
        /* Verify that hasMatchingInterfaces returns true */
        Assert.assertTrue( newInstance.hasMatchingInterfaces( instance ));
        instance = newInstance;        

        /* Register the node interface and verify that it changes */
        newInstance = instance.registerIntf( NODE );
        newList.add( NODE );
        testBasics( newList, newInstance );
        /* Verify that it doesn't have matching interfaces */
        Assert.assertFalse( newInstance.hasMatchingInterfaces( instance ));
        instance = newInstance;

        /* Change the external interface, and verify it changes */
        newInstance = instance.registerIntf( EXTERNAL_PPP );
        newList.remove( EXTERNAL );
        newList.add( EXTERNAL_PPP );
        /* Verify that it has matching interfaces */
        Assert.assertTrue( newInstance.hasMatchingInterfaces( instance ));
        testBasics( newList, newInstance );
    }

    @Test public void unregister() throws ArgonException
    {
        ArgonInterfaceConverter instance = ArgonInterfaceConverter.makeInstance( FULL );
        
        ArgonInterfaceConverter newInstance = instance.unregisterIntf( IntfConstants.VPN_INTF );
        /* Verify that the interface was removed */
        List<ArgonInterface> newList = new LinkedList<ArgonInterface>( FULL );
        newList.remove( VPN );
        testBasics( newList, newInstance );
        /* Verify that hasMatchingInterfaces returns false */
        Assert.assertFalse( newInstance.hasMatchingInterfaces( instance ));
        instance = newInstance;
        
        /* Unregister again, and verify nothing changes */
        newInstance = instance.unregisterIntf( IntfConstants.VPN_INTF );
        testBasics( newList, newInstance );
        /* Verify that hasMatchingInterfaces returns false */
        Assert.assertTrue( newInstance.hasMatchingInterfaces( instance ));
        instance = newInstance;        
    }

    @Test public void registerSecondaryIntf() throws ArgonException
    {
        
    }

    /* exception testing, to verify that exceptions are thrown at the appropriate times */
    @Test(expected=ArgonException.class) public void unregisterInternal() throws ArgonException
    {
        ArgonInterfaceConverter.makeInstance( BASIC ).unregisterIntf( IntfConstants.INTERNAL_INTF );
    }

    @Test(expected=ArgonException.class) public void unregisterExternal() throws ArgonException
    {
        ArgonInterfaceConverter.makeInstance( BASIC ).unregisterIntf( IntfConstants.EXTERNAL_INTF );
    }

    @Test(expected=ArgonException.class) public void duplicateArgonInterface() throws ArgonException
    {
        List list = new LinkedList( BASIC );
        list.add( SAME_ARGON );
        ArgonInterfaceConverter.makeInstance( list );
    }

    @Test(expected=ArgonException.class) public void duplicateNetcapInterface() throws ArgonException
    {
        List list = new LinkedList( BASIC );
        list.add( SAME_NETCAP );
        ArgonInterfaceConverter.makeInstance( list );
    }

    @Test(expected=ArgonException.class) public void duplicateNameInterface() throws ArgonException
    {
        List list = new LinkedList( BASIC );
        list.add( SAME_NAME );
        ArgonInterfaceConverter.makeInstance( list );
    }

    private void testBasics( List<ArgonInterface> input, ArgonInterfaceConverter instance ) 
        throws ArgonException
    {
        /* Make sure that it is possible to lookup all of the interfaces */
        for ( ArgonInterface intf : input ) {
            Assert.assertEquals( instance.getIntfByArgon( intf.getArgon()), intf );
            Assert.assertEquals( instance.getIntfByNetcap( intf.getNetcap()), intf );
            Assert.assertEquals( instance.getIntfByName( intf.getName()), intf );
            
            /* Verify that internal, external, dmz and vpn work properly */
            switch ( intf.getArgon()) {
            case IntfConstants.EXTERNAL_INTF:
                Assert.assertEquals( instance.getExternal(), intf );
                break;

            case IntfConstants.INTERNAL_INTF:
                Assert.assertEquals( instance.getInternal(), intf );
                break;

            case IntfConstants.DMZ_INTF:
                Assert.assertEquals( instance.getDmz(), intf );
                break;
                
            default:
                /* No helper functions to check */
            }
        }
        
        /* Verify that the lists are the same */
        Set<ArgonInterface> set1 = new HashSet<ArgonInterface>( input );
        Set<ArgonInterface> set2 = new HashSet<ArgonInterface>( instance.getIntfList());
        Assert.assertEquals( set1, set2 );
    }    
}
