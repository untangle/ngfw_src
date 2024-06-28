/**
 * $Id: NetspaceManagerImpl.java 42115 2016-01-15 17:56:36Z mahotz $
 */

package com.untangle.uvm;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Random;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.codec.binary.Hex;

import com.untangle.uvm.NetspaceManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.IPMaskedAddress;

/**
 * The NetspaceManager provides a centralized registry for network address
 * blocks that are in use by applications in the system.
 */
public class NetspaceManagerImpl implements NetspaceManager
{
    private final Logger logger = LogManager.getLogger(getClass());
    private final LinkedList<NetworkSpace> networkRegistry = new LinkedList<NetworkSpace>();

    // this is the total number of private class C networks in the RFC1918 reserved blocks
    private final int PRIVATE_ATTEMPT_LIMIT = 4096 + 256 + 65536;

    // this is number of times we'll try to generate a unique IPv6 ULA network
    private final int RANDOM_ATTEMPT_LIMIT = 4096;

    /**
     * we use these to keep track of the last private IPv4 network returned from
     * the getAvailableAddressSpace function. This allows subsequent calls to
     * generate unique results. This is necessary so that multiple calls to the
     * function prior to the returned values actually being registered won't
     * just return the same unused space repeatedly. An example is the ipsec app
     * which calls the function twice in a row for L2TP and Xauth during
     * initialization.
     */
    private int nextAval = 172;
    private int nextBval = 16;
    private int nextCval = 0;

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
     * getAvailableAddressSpace should be used to get an unregistered address
     * space. IPv4 will find the first unused private network. IPv6 generation
     * will use the Unique Unicast range
     *
     * @param version
     *        The IP version to generate a space for (IPv4 or IPv6)
     * @param hostId
     *        The host identifier
     * @return An unused IP address space
     */
    public IPMaskedAddress getAvailableAddressSpace(IPVersion version, int hostId)
    {
        IPMaskedAddress tester;
        NetworkSpace space;

        // validate the host identifier
        if (hostId > 255 || hostId < 0) {
            logger.warn("Invalid hostId: " + hostId + " - defaulting to 0");
            hostId = 0;
        }

        // for IPv4 we return an unused private network we find in the reserved
        // blocks defined in RFC1918.
        if (version == IPVersion.IPv4) {
            for (int x = 0; x < PRIVATE_ATTEMPT_LIMIT; x++) {
                // get the next private network from the helper function
                tester = new IPMaskedAddress(getNextPrivateNetwork(hostId), 24);

                // see if the network is available
                space = isNetworkAvailable(null, tester);

                // null return means no conflict so return the private network
                if (space == null) {
                    return tester;
                }
            }

            // if we did't find a available block there are no good options
            // left so just return TEST-NET-2 which is valid and usable and
            // shouldn't conflict with anything we know about
            return new IPMaskedAddress("198.51.100." + hostId, 24);
        }

        // for IPv6 we generate random networks in the unique local address
        // space defined in RFC4193
        Random rand = new Random();

        for (int count = 0; count < RANDOM_ATTEMPT_LIMIT; count++) {
            // generate a random network
            tester = getRandomLocalIp6Address(rand, hostId);

            // see if the network is available
            space = isNetworkAvailable(null, tester);

            // null return means no conflict so return the random network
            if (space == null) {
                return (tester);
            }
        }

        // nothing found so return a generic /64 that is valid and usable
        return new IPMaskedAddress("fdfd:fcfc:fbfb:fafa::" + hostId, 64);
    }

    /**
     * getRandomLocalIp6Address is a helper function that uses the current
     * Random class to generate a random IP6 local address
     * 
     * @param rand
     *        - An instance of the random class in use (Increases "randomness"
     *        by reusing the instance)
     * @param hostId
     *        - The host identifier
     * @return IPMaskedAddress - A random INet 6 address with given parameters
     */
    private IPMaskedAddress getRandomLocalIp6Address(Random rand, int hostId)
    {
        //Get local prefixes
        String prefix = "fd";

        // Generating random 40 bit Global ID
        byte[] gBytes = new byte[5];
        rand.nextBytes(gBytes);
        String globalId = Hex.encodeHexString(gBytes);

        // Generating random 16 bit subnet ID
        byte[] sBytes = new byte[2];
        rand.nextBytes(sBytes);
        String subnet = Hex.encodeHexString(sBytes);

        //Combine and add : delimiter
        String combinedAddr = (prefix + globalId + subnet).replaceAll("(.{4})", "$1:") + ":";

        return new IPMaskedAddress(combinedAddr, 64);
    }

    /**
     * getNextPrivateNetwork is a function that will enumerate every class C
     * network in RFC1918 private address space, returning the next sequential
     * network for each subsequent call. The logic below contemplates networks
     * as a combination of aaa.bbb.ccc.ddd and we smartly increment the first
     * three components as we work through each of the reserved blocks so we
     * will effectively enumerate the space over and over.
     * 
     * @param hostId
     *        - The host identifier
     * @return - The next sequential private class C network
     */
    private String getNextPrivateNetwork(int hostId)
    {
        // the result will always aaa.bbb.ccc.hostId so we set it now and will
        // return it later as we work through the increment and wrap logic
        String result = (nextAval + "." + nextBval + "." + nextCval + "." + hostId);

        // we always increment the ccc value and if it did not wrap our work
        // is done and we simply return the result
        nextCval += 1;
        if (nextCval < 256) {
            return (result);
        }

        // if ccc hits 256 we set back to zero, increment bbb, and check more
        nextCval = 0;
        nextBval += 1;

        // if aaa is in the 172 space and bbb hits 32 we advance to the
        // 192.168 space and return our result
        if ((nextAval == 172) && (nextBval > 31)) {
            nextAval = 192;
            nextBval = 168;
            return (result);
        }

        // if aaa is in the 192 space we know bbb can only be 168 which means
        // we advance to the 10.0.0.0 space and return our result
        if (nextAval == 192) {
            nextAval = 10;
            nextBval = 0;
            return (result);
        }

        // if aaa is in the 10 space and bbb has hit 256 we've tried everything
        // in 10/8 so we reset back to the beginning of the 172 space
        if ((nextAval == 10) && (nextBval > 255)) {
            nextAval = 172;
            nextBval = 16;
            return (result);
        }

        // the bbb increment didn't cause a wrap so we return our result
        return result;
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
