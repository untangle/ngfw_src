/**
 * $Id: NetspaceManagerImpl.java 42115 2016-01-15 17:56:36Z mahotz $
 */

package com.untangle.uvm;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Iterator;
import org.apache.log4j.Logger;

import com.untangle.uvm.NetspaceManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.IPMaskedAddress;

/**
 * The NetspaceManager provides a centralized registry for network address
 * blocks that are in use by applications in the system.
 */
public class NetspaceManagerImpl implements NetspaceManager
{
    private final Logger logger = Logger.getLogger(getClass());
    private final LinkedList<NetworkSpace> networkRegistry = new LinkedList<NetworkSpace>();

    /**
     * Constructor
     */
    protected NetspaceManagerImpl()
    {
    }

    /**
     * Called to register a network address block in use by an application
     * 
     * @param ownerName
     *        The name of the owner
     * @param ownerPurpose
     *        What the network block is being used for
     * @param networkAddress
     *        The address of the network block
     * @param networkSize
     *        The size of the network block
     */
    public void registerNetworkBlock(String ownerName, String ownerPurpose, InetAddress networkAddress, Integer networkSize)
    {
        NetworkSpace space = new NetworkSpace();
        space.ownerName = ownerName;
        space.ownerPurpose = ownerPurpose;
        space.maskedAddress = new IPMaskedAddress(networkAddress, networkSize);
        networkRegistry.add(space);
        logger.debug("Added Netspace " + space.toString());
    }

    /**
     * Called to register a network address block in use by an application
     * 
     * @param ownerName
     *        The name of the owner
     * @param ownerPurpose
     *        What the network block is being used for
     * @param networkText
     *        The network
     */
    public void registerNetworkBlock(String ownerName, String ownerPurpose, String networkText)
    {
        NetworkSpace space = new NetworkSpace();
        space.ownerName = ownerName;
        space.ownerPurpose = ownerPurpose;
        space.maskedAddress = new IPMaskedAddress(networkText);
        networkRegistry.add(space);
        logger.debug("Added Netspace " + space.toString());
    }

    /**
     * Called to register a network address block in use by an application
     * 
     * @param ownerName
     *        The name of the owner
     * @param ownerPurpose
     *        What the network block is being used for
     * @param networkInfo
     *        The network
     */
    public void registerNetworkBlock(String ownerName, String ownerPurpose, IPMaskedAddress networkInfo)
    {
        NetworkSpace space = new NetworkSpace();
        space.ownerName = ownerName;
        space.ownerPurpose = ownerPurpose;
        space.maskedAddress = networkInfo;
        networkRegistry.add(space);
        logger.debug("Added Netspace " + space.toString());
    }

    /**
     * Called to remove all registrations for an owner
     *
     * @param ownerName
     *        The owner
     */
    public void clearOwnerRegistrationAll(String ownerName)
    {
        Iterator<NetworkSpace> nsi = networkRegistry.iterator();
        NetworkSpace space;

        while (nsi.hasNext()) {
            space = nsi.next();
            if (!ownerName.equals(space.ownerName)) continue;
            nsi.remove();
            logger.debug("Removed Netspace " + space.toString());
        }
    }

    /**
     * Called to remove all registrations for an owner
     * 
     * @param ownerName
     *        The owner name
     * @param ownerPurpose
     *        The owner purpose
     */
    public void clearOwnerRegistrationPurpose(String ownerName, String ownerPurpose)
    {
        Iterator<NetworkSpace> nsi = networkRegistry.iterator();
        NetworkSpace space;

        while (nsi.hasNext()) {
            space = nsi.next();
            if (!ownerName.equals(space.ownerName)) continue;
            if (!ownerPurpose.contentEquals(space.ownerPurpose)) continue;
            nsi.remove();
            logger.debug("Removed Netspace " + space.toString());
        }
    }

    /**
     * Called to determine if the passed network conflicts with any existing
     * network registrations.
     * 
     * @param ownerName
     *        The name of the calling application
     * @param networkAddress
     *        The network address
     * @param networkSize
     *        The network size
     * @return true if the network block is available for use or false if it
     *         conflicts with an existing registration
     */
    public NetworkSpace isNetworkAvailable(String ownerName, InetAddress networkAddress, Integer networkSize)
    {
        IPMaskedAddress tester = new IPMaskedAddress(networkAddress, networkSize);
        return isNetworkAvailable(ownerName, tester);
    }

    /**
     * Called to determine if the passed network conflicts with any existing
     * network registrations
     * 
     * @param ownerName
     *        The name of the calling application
     * @param networkText
     *        The network address
     * @return true if the network block is available for use or false if it
     *         conflicts with an existing registration
     */
    public NetworkSpace isNetworkAvailable(String ownerName, String networkText)
    {
        IPMaskedAddress tester = new IPMaskedAddress(networkText);
        return isNetworkAvailable(ownerName, tester);
    }

    /**
     * Called to determine if the passed network conflicts with any existing
     * network registrations
     * 
     * @param ownerName
     *        The name of the calling application
     * @param tester
     *        The network to test
     * @return true if the network block is available for use or false if it
     *         conflicts with an existing registration
     */

    public NetworkSpace isNetworkAvailable(String ownerName, IPMaskedAddress tester)
    {
        Iterator<NetworkSpace> nsi = networkRegistry.iterator();
        NetworkSpace space;

        while (nsi.hasNext()) {
            space = nsi.next();
            /*
             * Ignore reservations for the calling owner since we expect they
             * will be removed and replaced with new reservations on save
             */
            if ((ownerName != null) && (ownerName.equals(space.ownerName))) continue;

            // if the test address intersects a reservation return the conflicting space
            if (tester.isIntersecting(space.maskedAddress)) return space;
        }

        // no conflicts found so return null
        return null;
    }

    /**
     * Called to get the first usable address in an address space
     *
     * @param networkAddress
     *        The network address
     * @param networkSize
     *        The network size
     * @return The first usable IP address
     */
    public InetAddress getFirstUsableAddress(InetAddress networkAddress, Integer networkSize)
    {
        IPMaskedAddress tester = new IPMaskedAddress(networkAddress, networkSize);
        return tester.getFirstMaskedAddress();
    }

    /**
     * Called to get the first usable address in an address space
     *
     * @param networkText
     *        The network in CIDR format
     * @return The first usable IP address
     */
    public InetAddress getFirstUsableAddress(String networkText)
    {
        IPMaskedAddress tester = new IPMaskedAddress(networkText);
        return tester.getFirstMaskedAddress();
    }
}
