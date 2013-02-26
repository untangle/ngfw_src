/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * the configuration for a vpn client.
 */
@SuppressWarnings("serial")
public class VpnClient
{
    private static final Pattern NAME_PATTERN;

    private static final int MAX_NAME_LENGTH = 60;

    private InetAddress address;            // may be null.

    // The name of the address group to pull this client address
    private String groupName;

    private String certificateStatus = "Unknown";

    /* A 96-bit random string to be emailed out.  When the user goes
     * to the page and uses this key, they are allowed to download the
     * configuration for one session */
    private String distributionKey;

    /* Most likely unused for early versions but a nice possiblity for
     * the future */
    private String distributionPassword;

    /* Not stored in settings */
    /* Set to true to tell the node to distribute the configuration files
     * for this client */
    private boolean distributeClient = false;

    /* Email addresss where to send the client */
    private String distributionEmail = null;

    private String name = "";
    private boolean live = true;
    
    // constructors -----------------------------------------------------------

    public VpnClient() { }

    // accessors --------------------------------------------------------------

    /**
     * The address group that this client belongs to.
     */
    public String getGroupName() { return this.groupName; }
    public void setGroupName( String newValue ) { this.groupName = newValue; }

    /**
     * Static address for this client, this cannot be set, it is assigned.
     *
     * @return static address of the machine.
     */
    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress address ) { this.address = address; }

    /**
     * the key required to download the client.
     */
    public String getDistributionKey() { return this.distributionKey; }
    public void setDistributionKey( String distributionKey ) { this.distributionKey = distributionKey; }

    /**
     * @return the key password to download the client.(unused)
     */
    public String getDistributionPassword() { return this.distributionPassword; }
    public void setDistributionPassword( String distributionPassword ) { this.distributionPassword = distributionPassword; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean getLive() { return live; }
    public void setLive(boolean live) { this.live = live; }

    /* Indicates whether or not the server should distribute a config for this client */
    public boolean getDistributeClient() { return this.distributeClient; }
    public void setDistributeClient( boolean distributeClient ) { this.distributeClient = distributeClient; }

    public String getDistributionEmail() { return this.distributionEmail; }
    public void setDistributionEmail( String email ) { this.distributionEmail = email; }

    public boolean trans_hasDistributionEmail()
    {
        return this.distributionEmail != null;
    }

    /* This is the name that is used as the common name in the certificate */
    public String trans_getInternalName()
    {
        return getName().trim().toLowerCase().replace( " ", "_" );
    }

    public void validate() throws Exception
    {
        String name = getName();

        validateName( name );
    }

    public String trans_getCertificateStatus()
    {
        return this.certificateStatus;
    }

    public void trans_setCertificateStatusUnknown( )
    {
        this.certificateStatus = "unknown";
    }

    public void trans_setCertificateStatusValid( )
    {
        this.certificateStatus = "valid";
    }

    public void trans_setCertificateStatusRevoked( )
    {
        this.certificateStatus = "revoked";
    }

    public boolean trans_isUntanglePlatform()
    {
        /* A single client can never be an edgeguard */
        return false;
    }

    /* If the client is alive and the group it is in is enabled, and it has been\
     * assigned an address. */
    public boolean trans_isEnabled()
    {
        return getLive() && ( this.address != null );
    }

    static void validateName( String name ) throws Exception
    {
        if ( name == null ) throw new Exception( "Name cannot be null" );

        if ( name.length() == 0 ) {
            throw new Exception( "A client cannot have an empty name" );
        }

        if ( name.length() > MAX_NAME_LENGTH ) {
            throw new Exception( "A client's name is limited to " + MAX_NAME_LENGTH + " characters." );
        }

        if ( !NAME_PATTERN.matcher( name ).matches()) {
            throw new Exception( "A client name should only contains numbers, letters, " + "dashes and periods.  Spaces are not allowed. " + name );
        }
    }

    static
    {
        Pattern p;

        try {
            /* Limited to prevent funny shell hacks(the name goes to the shell) */
            p = Pattern.compile( "^[A-Za-z0-9]([-_.0-9A-Za-z]*[0-9A-Za-z])?$" );
        } catch ( PatternSyntaxException e ) {
            System.err.println( "Unable to intialize the host label pattern" );
            p = null;
        }

        NAME_PATTERN = p;
    }
}
