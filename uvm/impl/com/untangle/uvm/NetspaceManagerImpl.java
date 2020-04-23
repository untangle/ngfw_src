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
import org.apache.log4j.Logger;
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
     * getAvailableAddressSpace should be used to get an unregistered address space based on a random subnet, IP6 generation will use the Unique Unicast range
     * 
     * @param version
     *        The IP Version to generate a space for (IP4 or IP6)
     * @param hostIdentifier
     *        The host ID
     * @param CIDRSpace
     *        The CIDRSpace
     * @return
     */
    public IPMaskedAddress getAvailableAddressSpace(IPVersion version, int hostIdentifier, int CIDRSpace) {
        return getAvailableAddressSpace(version, hostIdentifier, CIDRSpace, 15);
    }

    /**
     * getAvailableAddressSpace should be used to get an unregistered address space based on a random subnet, IP6 generation will use the Unique Unicast range
     * 
     * @param version
     *        The IP Version to generate a space for (IP4 or IP6)
     * @param hostIdentifier
     *        The host ID
     * @param CIDRSpace
     *        The CIDRSpace
     * @param generationAttempts
     *        The number of generations we should test before giving up with a warning
     * @return IPMaskedAddress - A CIDR address that is not conflicting with other address spaces on the appliance
     */
    public IPMaskedAddress getAvailableAddressSpace(IPVersion version, int hostIdentifier, int CIDRSpace, int generationAttempts) {
        IPMaskedAddress randAddress = null;
        boolean uniqueAddress = true;
        List<IPMaskedAddress> attemptedAddresses = new ArrayList<>();

        // Gen a random address
        Random rand = new Random();

        //Validate the host ID
        if(hostIdentifier > 255 || hostIdentifier < 0) {
            logger.warn("Host ID passed into getAvailableAddressSpace is invalid: " + hostIdentifier + " defaulting to 0.");
            hostIdentifier = 0;
        }

        //Validate the CIDR Space
        if(CIDRSpace > 32 || CIDRSpace < 0) {
            logger.warn("CIDRSpace passed into getAvailableAddressSpace is invalid: " + CIDRSpace + " defaulting to 24.");
            CIDRSpace = 24;
        }

        // If the address intersects another address, gen another one until we have one that is not matching
        do {

            //If we are exceeding the generationAttempts, then return with a warning.
            if(attemptedAddresses.size() > generationAttempts) {
                logger.warn("getAvailableAddressSpace has failed after "+ generationAttempts + " and is giving up now.");
                return new IPMaskedAddress("0.0.0." + hostIdentifier + "/" + CIDRSpace);
            }

            if(version == IPVersion.IPv6) {
                randAddress = getRandomLocalIp6Address(rand, CIDRSpace);
            } else {
                randAddress = getRandomLocalIp4Address(rand, hostIdentifier, CIDRSpace);
            }

            // If we've already tested this address then don't even try it against the registry
            if(attemptedAddresses.contains(randAddress)) {
                uniqueAddress = false;
                break;
            }

            // Verify any intersections in the registry
            for (NetworkSpace netSpace : networkRegistry) {
                logger.debug("Comparing " + randAddress + " against: " + netSpace.maskedAddress);
                if(netSpace.maskedAddress.isIntersecting(randAddress)) {
                    logger.debug(randAddress + " is not unique, generating another.");
                    uniqueAddress = false;
                    attemptedAddresses.add(randAddress);
                    break;
                }
            }
        } while (!uniqueAddress);
        
        logger.debug("Unique address found: " + randAddress);

        return randAddress;
    }

    /**
     * getRandomLocalIp4Address is a helper function that uses the current Random class to generate a random IP6 local address
     * 
     * @param rand - An instance of the random class in use (Increases "randomness" by reusing the instance)
     * @param CIDRSpace - The CIDR Space
     * @return IPMaskedAddress - A random INet 6 address with given parameters
     */
    private IPMaskedAddress getRandomLocalIp6Address(Random rand, int CIDRSpace) {
        //Get local prefixes
        String prefix = "fd";

        // Generating random 40 bit Global ID
        byte[]gBytes = new byte[5];
        rand.nextBytes(gBytes);
        String globalId = Hex.encodeHexString(gBytes);

        // Generating random 16 bit subnet ID
        byte[]sBytes = new byte[2];
        rand.nextBytes(sBytes);
        String subnet = Hex.encodeHexString(sBytes);

        //Combine and add : delimiter
        String combinedAddr = (prefix + globalId + subnet).replaceAll("(.{4})", "$1:") + ":";

        return new IPMaskedAddress(combinedAddr, CIDRSpace);
    }

    /**
     * getRandomLocalIp4Address is a helper function that uses the current Random class, host, and CIDR space to generate a random IP4 address
     * 
     * @param rand - An instance of the random class in use (Increases "randomness" by reusing the instance)
     * @param hostIdentifier - host ID
     * @param CIDRSpace - The CIDR Space
     * @return IPMaskedAddress - A random INet 4 address with given parameters
     */
    private IPMaskedAddress getRandomLocalIp4Address(Random rand, int hostIdentifier, int CIDRSpace) {
        
        // Pull random net from private spaces
        int leadingNet = new int[]{10, 172, 192}[rand.nextInt(3)];
        int nextNet = 0;

        //Limit the network depending on the private space chosen
        switch(leadingNet) {
            case 192:
                //192 must be in 192.168 space
                nextNet = 168;
                break;
            case 172:
                //Generate a random 0-16, and add 16 to prevent it from falling into 172.0 - 172.15 space, but limiting to the 172.16 - 172.31 spaces
                nextNet = rand.nextInt(16) + 16;
                break;
            case 10:
                // Everything in 10.X is valid
                nextNet = rand.nextInt(256);
                break;
        }

        //Combine the above and form IP with random subnet, host address, and CIDR from params
        return new IPMaskedAddress( leadingNet + "." + nextNet + "." + rand.nextInt(256) + "." + hostIdentifier, CIDRSpace);
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
