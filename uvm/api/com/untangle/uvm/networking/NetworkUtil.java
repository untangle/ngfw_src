/* $HeadURL$ */
package com.untangle.uvm.networking;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.ValidationException;

import com.untangle.uvm.node.AddressRange;
import com.untangle.uvm.node.AddressValidator;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.IPMatcher;

/**
 * A number of utilities for working with IP addresses and network
 * settings.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class NetworkUtil
{
    private static final NetworkUtil INSTANCE;

    /** An empty IP address, 0.0.0.0 */
    public static final IPAddress  EMPTY_IPADDR;

    /* Default address for the outside restricted outside network */
    public static final IPAddress  DEF_OUTSIDE_NETWORK;

    /* Default address for the outside restricted outside netmask */
    public static final IPAddress  DEF_OUTSIDE_NETMASK;

    /* The address to use when a DHCP request fails */
    public static final IPAddress  BOGUS_DHCP_ADDRESS;

    /* The netmask to use if a DHCP request fails */
    public static final IPAddress  BOGUS_DHCP_NETMASK;

    /* Default start of the DHCP range */
    public static final IPAddress DEFAULT_DHCP_START;

    /* Default end of the DHCP range. */
    public static final IPAddress DEFAULT_DHCP_END;

    /* Default NAT address */
    public static final IPAddress DEFAULT_NAT_ADDRESS;

    /* Default NAT netmask */
    public static final IPAddress DEFAULT_NAT_NETMASK;

    /* This is the address to use during setup */
    public static final IPAddress SETUP_ADDRESS;

    public static final IPAddress BOGUS_ADDRESS;

    /* Default lease time for the DHCP server */
    public static int DEFAULT_LEASE_TIME_SEC = 4 * 60 * 60;

    /* The default name for the primary space */
    public static final String DEFAULT_SPACE_NAME_PRIMARY = "public";

    /* The default name for the NAT space. */
    public static final String DEFAULT_SPACE_NAME_NAT     = "private";

    /* Default HTTPS port */
    public static final int    DEF_HTTPS_PORT = 443;

    /* Default setting for allowing internal access via http */
    public static final boolean DEF_IS_INSIDE_INSECURE_EN = true;

    /* Default setting for allowing external access via https */  
    public static final boolean DEF_IS_OUTSIDE_EN         = true;

    /* Default setting for whether or not external access is restricted */
    public static final boolean DEF_IS_OUTSIDE_RESTRICTED = false;

    /* Default setting for remote support */
    public static final boolean DEF_IS_SSH_EN             = false;

    /* Default setting for sending exception reports */
    public static final boolean DEF_IS_EXCEPTION_REPORTING_EN = false;

    /* Default setting for TCP Window scaling */
    public static final boolean DEF_IS_TCP_WIN_EN         = false;

    /* Default setting for whether public administration is allowed */
    public static final boolean DEF_OUTSIDE_ADMINISTRATION = false;

    /* Default setting for whether users can retrieve their quarantine
     * from the internet. */
    public static final boolean DEF_OUTSIDE_QUARANTINE = true;
    
    /* Default setting for whether administrators can access reports from the internet. */
    public static final boolean DEF_OUTSIDE_REPORTING = false;

    /* The list of private networks, this is defined by RFC 1918 */
    private static final String PRIVATE_NETWORK_STRINGS[] =
    {
        "192.168.0.0/16", "10.0.0.0/8", "172.16.0.0/12"
    };

    /* ports 9500 -> 9650 are redirect ports, 10000 -> 60000 are reserved for NAT */
    public static final int INTERNAL_OPEN_HTTPS_PORT = 64157;

    /* XXX This should be final, but there is a bug in javac that won't let it be. */
    
    /* A list of matchers that can be uesd to determine if an address
     * is in the private network. */
    private List<IPMatcher> privateNetworkList;

    NetworkUtil()
    {
        List<IPMatcher> matchers = new LinkedList<IPMatcher>();

        for ( String matcherString : PRIVATE_NETWORK_STRINGS ) {
            try {
                matchers.add( new IPMatcher( matcherString ));
            } catch ( Exception e ) {
                System.err.println( "Unable to parse: " + matcherString );
            }
        }

        this.privateNetworkList = Collections.unmodifiableList( matchers );
    }

    /**
     * Return true if <code>address/netmask</code> is in one of the
     * private ranges from RFC 1918.
     *
     * @param address The network to test.
     * @param netmaks The netmask of the network.
     * @return True iff <code>address/netmask</code> is considered a
     * private address.
     */
    public boolean isPrivateNetwork( IPAddress address, IPAddress netmask )
    {
        if (( address == null ) || ( netmask == null )) return false;

        IPAddress base = address.and( netmask );

        for ( IPMatcher matcher : this.privateNetworkList ) {
            if ( matcher.isMatch( base.getAddr())) return true;
        }

        return false;
    }

    /**
     * Validate that a network doesn't have any errors.
     *
     * @param network The network to validate.
     * @exception ValidationException Occurs if there is an error in
     * <code>route</code>.
     */
    public void validate( IPNetwork network ) throws ValidateException
    {
        /* implement me, test if the netmask is okay, etc. */
    }

    /**
     * Test if the IPNetwork has a unicast address.
     * this is a bogus function that should be removed.
     *
     * @param network The network to check.
     * @return True iff <code>network</code> is a unicast address.
     */
    public boolean isUnicast( IPNetwork network )
    {
        byte[] address = network.getNetwork().getAddr().getAddress();

        /* Magic numbers, -127, because of unsigned bytes */
        return (( address[3] != 0 ) && ( address[3] != -127 ));
    }

    /**
     * Convert network to a string that is parseable by the 'ip' command.
     * 
     * @param network The network to convert.
     * @return The string representation of <code>network</code>.
     */
    public String toRouteString( IPNetwork network ) throws Exception
    {
        /* XXX This is kind of hokey and should be precalculated at creation time */
        IPAddress netmask = network.getNetmask();

        try {
            int cidr = netmask.toCidr();

            IPAddress networkAddress = network.getNetwork().and( netmask );
            /* Very important, the ip command barfs on spaces. */
            return networkAddress.toString() + "/" + cidr;
        } catch ( ParseException e ) {
            throw new Exception( "Unable to convert the netmask " + netmask + " into a cidr suffix" );
        }
    }

    /**
     * Create a string for <code>publicAddress:publicPort</code>.
     * This will automatically remove port for 443, and stuff like that.
     * 
     * @param publicAddress The address part of the public address.
     * @param publicPort The port part of the public address.
     * @return The public address string.
     */
    public String generatePublicAddress( IPAddress publicAddress, int publicPort )
    {
        if ( publicAddress == null || publicAddress.isEmpty()) return "";

        if ( publicPort == DEF_HTTPS_PORT ) return publicAddress.toString();

        return publicAddress.toString() + ":" + publicPort;
    }

    public boolean isBogus( IPAddress address )
    {
        if ( BOGUS_ADDRESS == null ) return false;

        return BOGUS_ADDRESS.equals( address );
    }

    /**
     * Determine if a hostname is most likely resolvable by an
     * external DNS server.
     *
     * @param hostName The hostname to test.
     * @return True if <code>hostName</code> is most likely a
     * publically resolvable address.
     */
    public static boolean isHostnameLikelyPublic(String hostName)
    {
        /* This is a pretty weak test, and should be updated to test
         * for .com, etc */
        if (!hostName.contains("."))
            return false;
        if (hostName.endsWith(".domain"))
            return false;
        return true;
    }

    public static NetworkUtil getInstance()
    {
        return INSTANCE;
    }

    static
    {
        IPAddress emptyAddr        = null;
        IPAddress outsideNetwork   = null;
        IPAddress outsideNetmask   = null;
        IPAddress bogusDHCPAddress = null;
        IPAddress bogusDHCPNetmask = null;
        IPAddress dhcpStart        = null;
        IPAddress dhcpEnd          = null;
        IPAddress bogusAddress     = null;

        IPAddress natAddress       = null;
        IPAddress natNetmask       = null;

        IPAddress setupAddress     = null;

        try {
            emptyAddr        = IPAddress.parse( "0.0.0.0" );
            outsideNetwork   = IPAddress.parse( "1.2.3.4" );
            outsideNetmask   = IPAddress.parse( "255.255.255.0" );
            bogusDHCPAddress = IPAddress.parse( "169.254.210.50" );
            bogusDHCPNetmask = IPAddress.parse( "255.255.255.0" );
            bogusAddress     = IPAddress.parse( "192.0.2.1" );

            dhcpStart      = IPAddress.parse( "192.168.2.100" );
            dhcpEnd        = IPAddress.parse( "192.168.2.200" );

            natAddress = IPAddress.parse( "192.168.2.254" );
            natNetmask = IPAddress.parse( "255.255.255.0" );

            setupAddress = IPAddress.parse( "192.168.1.1" );
        } catch( Exception e ) {
            System.err.println( "this should never happen: " + e );
            emptyAddr = null;
            dhcpStart = dhcpEnd = null;
            bogusDHCPAddress = bogusDHCPNetmask = null;
            natAddress = natNetmask = null;
            /* THIS SHOULD NEVER HAPPEN */
        }

        EMPTY_IPADDR        = emptyAddr;
        DEF_OUTSIDE_NETWORK = outsideNetwork;
        DEF_OUTSIDE_NETMASK = outsideNetmask;

        DEFAULT_DHCP_START  = dhcpStart;
        DEFAULT_DHCP_END    = dhcpEnd;

        BOGUS_DHCP_ADDRESS  = bogusDHCPAddress;
        BOGUS_DHCP_NETMASK  = bogusDHCPNetmask;

        BOGUS_ADDRESS = bogusAddress;

        DEFAULT_NAT_ADDRESS = natAddress;
        DEFAULT_NAT_NETMASK = natNetmask;

        SETUP_ADDRESS = setupAddress;

        INSTANCE = new NetworkUtil();
    }
}
