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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.untangle.jnetcap.JNetcapException;
import com.untangle.jnetcap.Netcap;
import com.untangle.uvm.ArgonException;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.IntfEnum;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.node.InterfaceComparator;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import com.untangle.uvm.policy.LocalPolicyManager;
import org.apache.log4j.Logger;

/* Manager for controlling argon -> netcap interface matching */
class LocalIntfManagerImpl implements LocalIntfManager
{
    private static final File INTF_ORDER_FILE
        = new File("/etc/untangle-net-alpaca/interface.properties");

    private static final String DEFAULT_INTERFACE_ORDER =
        "External:eth0:1,DMZ:eth2:3,VPN:tun0:8,Internal:eth1:2";

    private ArgonInterfaceConverter intfConverter = null;

    private final Logger logger = Logger.getLogger(this.getClass());

    /* Converter from all of the interface indexes to their display name(eg. external) */
    private IntfEnum intfEnum;

    private InterfaceComparator interfaceComparator;

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

    /* This is a list of non-physical interfaces (everything except for internal, external and dmz).
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

    /* Register a secondary interface, this is an interface that replaces another interface,
     * EG. if ETH0 -> PPP0, PPP0 is the secondary interface and ETH0 is the primary interface */
    public synchronized void registerSecondaryIntf(String name, byte argon) throws ArgonException
    {
        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;

        logger.debug("Registering the secondary interface: [" + argon + ":" + name +"]");
        ArgonInterface intf = this.intfConverter.getIntfByArgon(argon);
        this.intfConverter = this.intfConverter.registerIntf(intf.makeNewSecondaryIntf(name));
        notifyDependents(prevIntfConverter);
    }

    /* Unregister a custom interface or DMZ. */
    public synchronized void unregisterIntf(byte argon) throws ArgonException
    {
        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;
        this.intfConverter = this.intfConverter.unregisterIntf(argon);

        notifyDependents(prevIntfConverter);
    }

    /* Unregister an individual secondary interface */
    public synchronized void unregisterSecondaryIntf(byte argon) throws ArgonException
    {
        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;

        logger.debug("Unregistering the secondary interface: [" + argon + "]");
        ArgonInterface intf = this.intfConverter.getIntfByArgon(argon);
        this.intfConverter = this.intfConverter.registerIntf(intf.makeNewSecondaryIntf(null));
        notifyDependents(prevIntfConverter);
    }

    /* This resets all of the secondary interfaces to their physical interfaces */
    public synchronized void resetSecondaryIntfs()
    {
        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;

        logger.debug("Unregistering all secondary interfaces.");
        try {
            this.intfConverter = this.intfConverter.resetSecondaryIntfs();
            notifyDependents(prevIntfConverter);
        } catch (ArgonException e) {
            logger.error("Error while resetting the secondary interfaces continuing.", e);
        }
    }

    /* Retrieve the current interface enumeration */
    public IntfEnum getIntfEnum()
    {
        return this.intfEnum;
    }

    public InterfaceComparator getInterfaceComparator()
    {
        return interfaceComparator;
    }

    public void loadInterfaceConfig() throws ArgonException
    {
        /* First check to see if the property file exists */
        FileInputStream fis = null;
        String interfaceOrder = null;
        try {
            Properties p = new Properties();
            fis = new FileInputStream(INTF_ORDER_FILE);
            p.load(fis);
            interfaceOrder = p.getProperty("com.untangle.interface-order");
        } catch (IOException exn) {
            logger.warn("could not close: " + INTF_ORDER_FILE, exn);
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException exn) {
                    logger.warn("could not close: " + INTF_ORDER_FILE, exn);
                }
            }
        }

        if (interfaceOrder == null || interfaceOrder.trim().length() == 0) {
            interfaceOrder = DEFAULT_INTERFACE_ORDER;
        }
        interfaceOrder = interfaceOrder.trim();

        logger.debug("Loading the interface order: " + interfaceOrder);

        String[] ifds = interfaceOrder.split(",");
        List<Byte> l = new ArrayList<Byte>(ifds.length);
        List<ArgonInterface> argonInterfaceList = new LinkedList<ArgonInterface>();

        for (String ifd : ifds) {
            String[] d = ifd.split(":");
            if (3 < d.length) {
                logger.warn("skiping bad interface description: "
                            + ifd);
            } else {


                try {
                    byte netcap = Byte.parseByte(d[2]);
                    byte argon = (byte)(netcap - 1);

                    String userString = d[0];
                    String osName = d[1];
                    l.add(argon);

                    argonInterfaceList.add(new ArgonInterface(osName, argon, netcap, userString));
                } catch (NumberFormatException exn) {
                    logger.warn("skiping bad interface description: "
                                + ifd);
                }
            }
        }

        interfaceComparator = new InterfaceComparator(l);
        logger.info("using interface order: "
                    + interfaceComparator.toString());

        ArgonInterfaceConverter prevIntfConverter = this.intfConverter;
        this.intfConverter = ArgonInterfaceConverter.makeInstance(argonInterfaceList);
        notifyDependents(prevIntfConverter);
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
            Netcap.getInstance().configureInterfaceArray(this.intfConverter.getNetcapIntfArray(),
                                                          this.intfConverter.getNameArray());
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

        this.intfEnum = new IntfEnum(argonIntfArray, intfNameArray,
                                     intfUserNameArray);
    }
}
