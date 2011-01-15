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
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.JNetcapException;
import com.untangle.jnetcap.Netcap;
import com.untangle.uvm.ArgonException;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.IntfEnum;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.node.firewall.intf.IntfDBMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import com.untangle.uvm.networking.NetworkConfiguration;
import com.untangle.uvm.networking.InterfaceConfiguration;

/* Manager for controlling argon -> netcap interface matching */
class LocalIntfManagerImpl implements LocalIntfManager
{
    private ArgonInterfaceConverter intfConverter = null;

    private final Logger logger = Logger.getLogger(this.getClass());

    /* Converter from all of the interface indexes to their display name(eg. external) */
    private IntfEnum intfEnum;

    /**
     * Convert an interface using the argon standard (0 = outside, 1 = inside, 2 = DMZ 1, etc)
     * to an interface that uses that netcap unique identifiers
     */
    public byte toNetcap(byte argonIntf)
    {
        switch (argonIntf) {
        case IntfConstants.ARGON_ERROR:
            throw new IllegalArgumentException("Invalid argon interface[" + argonIntf + "]");
        case IntfConstants.ARGON_UNKNOWN:  return IntfConstants.NETCAP_UNKNOWN;
        case IntfConstants.ARGON_LOOPBACK: return IntfConstants.NETCAP_LOOPBACK;
        }

        /* May actually want to check if interfaces exists */
        if (argonIntf < IntfConstants.ARGON_MIN  || argonIntf > IntfConstants.ARGON_MAX) {
            throw new IllegalArgumentException("Invalid argon interface[" + argonIntf + "]");
        }

        return (byte)(argonIntf + 1);
    }

    /**
     * Convert an interface from a netcap interface to the argon standard
     */
    public byte toArgon(byte netcapIntf)
    {
        switch (netcapIntf) {
        case IntfConstants.NETCAP_ERROR:
            throw new IllegalArgumentException("Invalid netcap interface[" + netcapIntf + "]");
        case IntfConstants.NETCAP_UNKNOWN:  return IntfConstants.ARGON_UNKNOWN;
        case IntfConstants.NETCAP_LOOPBACK: return IntfConstants.ARGON_LOOPBACK;
        }

        /* May actually want to check if interfaces exists */
        if (netcapIntf < IntfConstants.NETCAP_MIN  || netcapIntf > IntfConstants.NETCAP_MAX) {
            throw new IllegalArgumentException("Invalid netcap interface[" + netcapIntf + "]");
        }

        return (byte)(netcapIntf - 1);
    }

    /* Convert from an argon interface to the physical name of the interface */
    public String argonIntfToString(byte argonIntf) throws ArgonException
    {
        return this.intfConverter.getIntfByArgon(argonIntf).getName();
    }

    /* Retrieve the interface that corresponds to a specific argon interface */
    public ArgonInterface getIntfByArgon(byte argonIntf) throws ArgonException
    {
        return this.intfConverter.getIntfByArgon(argonIntf);
    }

    /* Retrieve the interface that corresponds to a specific netcap interface */
    public ArgonInterface getIntfByNetcap(byte netcapIntf) throws ArgonException
    {
        return this.intfConverter.getIntfByNetcap(netcapIntf);
    }

    /* Retrieve the interface that corresponds to the name */
    public ArgonInterface getIntfByName(String name) throws ArgonException
    {
        return this.intfConverter.getIntfByName(name);
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

    /* Return an array of the argon interfaces */
    public byte[] getArgonIntfArray()
    {
        return this.intfConverter.getArgonIntfArray();
    }

    /* Unregister a custom interface or DMZ. */
    public synchronized void unregisterIntf(byte argon) throws ArgonException
    {
        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;
        this.intfConverter = this.intfConverter.unregisterIntf(argon);

        notifyDependents(prevIntfConverter);
    }

    @SuppressWarnings("cast")
    public void loadInterfaceConfig() throws ArgonException
    {
        NetworkConfiguration netConf = LocalUvmContextFactory.context().networkManager().getNetworkConfiguration();
        List<ArgonInterface> argonInterfaceList = new LinkedList<ArgonInterface>();

        if (netConf == null) {
            logger.warn("netConfg is null");
            return;
        }
        
        for (InterfaceConfiguration intfConf : netConf.getInterfaceList()) {
            try {
                byte netcap = (byte)intfConf.getInterfaceId().byteValue(); // XXX cast Integer -> byte
                boolean isWanInterface = intfConf.isWAN();
                String userString = intfConf.getName();
                String osName = intfConf.getSystemName();
                byte argon = (byte)(netcap - 1);

                argonInterfaceList.add(new ArgonInterface(osName, null, argon, netcap, userString, isWanInterface));
            } catch (Exception exn) {
                logger.warn("Bad interface description: ",exn);
            }
        }

        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;
        this.intfConverter = ArgonInterfaceConverter.makeInstance(argonInterfaceList);
        notifyDependents(prevIntfConverter);
    }

    public IntfDBMatcher[] getIntfMatcherEnumeration()
    {
        return IntfMatcherFactory.getInstance().getEnumeration();
    }

    /* ----------------- Package ----------------- */
    LocalIntfManagerImpl() throws ArgonException
    {
        loadInterfaceConfig();
    }

    /* Initialize the interface converter */
    void initializeIntfArray() throws ArgonException
    {
        loadInterfaceConfig();
    }

    /* ----------------- Private ----------------- */
    /* Notify everything that needs to be aware of changes to the interface array */
    private void notifyDependents(ArgonInterfaceConverter prevIntfConverter) throws ArgonException
    {
        /* Notify netcap that there has been a change to the interfaces */
        try {
            Netcap.getInstance().configureInterfaceArray(this.intfConverter.getNetcapIntfArray(), this.intfConverter.getNameArray());
        } catch (JNetcapException e) {
            logger.warn("Error updating interface array", e);
            throw new ArgonException("Unable to configure interface array", e);
        }

        /* Update the interface enumeration */
        updateIntfEnum();

        /* Update the interface matcher factory, this should be a listener */
        IntfMatcherFactory.getInstance().updateEnumeration(this.intfEnum);
    }

    /* Update the interface enumeration */
    private void updateIntfEnum()
    {
        List<ArgonInterface> ail = this.intfConverter.getIntfList();
        byte[] argonIntfArray = new byte[ail.size()];
        String[] intfNameArray = new String[ail.size()];
        String[] intfUserNameArray = new String[ail.size()];

        int i = 0;
        for (ArgonInterface ai : ail) {
            argonIntfArray[i] = ai.getArgon();
            intfNameArray[i] = ai.getName();
            intfUserNameArray[i] = ai.getUserName();
            i++;
        }

        this.intfEnum = new IntfEnum(argonIntfArray, intfNameArray, intfUserNameArray);
    }
}
