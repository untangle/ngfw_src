/**
 * $Id$
 */
package com.untangle.uvm.networking;

import java.io.Serializable;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.HostAddress;

/**
 * These are settings related to the hostname and the adddress that is
 * used to connect to box.
 */
@SuppressWarnings("serial")
public class AddressSettings implements Serializable, Validatable
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String PUBLIC_ADDRESS_EXCEPTION = "A public address is an ip address, optionally followed by a port.  (e.g. 1.2.3.4:445 or 1.2.3.4)";

    private Long id;
    
    /* An additional HTTPS port that the web server will bind to */
    private int httpsPort;

    /* True if the hostname is resolvable on the internet */
    private boolean isHostnamePublic;

    /* Settings related to having an external router address */
    /* True if this untangle uses a public address */
    private boolean isPublicAddressEnabled;
    
    /* This is the address of an external router that has a redirect
     * to this untangle.  Used when we are in bridge mode behind
     * another box. */
    private IPAddress publicIPAddress;
    
    /**
     * This is the port on the external router that is redirect to the
     * untangle's httpsPort. <code>publicPort</code> is used in
     * conjunction with <code>publicIPAddress</code>
     */
    private int publicPort;

    public AddressSettings()
    {
    }

    public AddressSettings( AddressSettings settings )
    {
        this.httpsPort = settings.httpsPort;
        this.isHostnamePublic = settings.isHostnamePublic;
        this.isPublicAddressEnabled = settings.isPublicAddressEnabled;
        this.publicIPAddress = settings.publicIPAddress;
    }
        
    public Long getId() { return id; }
    public void setId(  Long id  ) { this.id = id; }

    /**
     * Get the port to run HTTPs on in addition to port 443.
     */
    public int getHttpsPort() { return this.httpsPort; }
    public void setHttpsPort(  int newValue  ) { this.httpsPort = newValue; }

    /**
     * Returns if the hostname for this box is publicly resolvable to
     * this box
     */
    public boolean getIsHostNamePublic() { return this.isHostnamePublic; }
    public void setIsHostNamePublic(  boolean newValue  ) { this.isHostnamePublic = newValue; }

    /**
     * True if the public address should be used
     */
    public boolean getIsPublicAddressEnabled() { return this.isPublicAddressEnabled; }
    public void setIsPublicAddressEnabled(  boolean newValue  ) { this.isPublicAddressEnabled = newValue; }

    /**
     * Retrieve the public address for the box.
     */
    public String getPublicAddress()
    {
        if ( this.publicIPAddress == null || this.publicIPAddress.isEmpty()) return "";

        if ( this.publicPort == NetworkUtil.DEF_HTTPS_PORT ) return this.publicIPAddress.toString();

        return this.publicIPAddress.toString() + ":" + this.publicPort;
    }

    /**
     * Set the public address as a string, this is a convenience
     * method for the GUI, it sets the public ip address and port.
     * 
     * @param newValue The hostname and port in a string for the
     * public address and port of the box.  If the port is left off,
     * this used the default https port.
     */
    public void setPublicAddress( String newValue ) throws ParseException
    {
        /* This is the to fix fabsorb which serializes everything, even methods marked transient */
        if ( newValue == null ) return;
        if ( newValue.length() == 0 ) return;

        try {
            IPAddress address;
            String valueArray[] = newValue.split( ":" );
            switch ( valueArray.length ) {
            case 1:
                address = IPAddress.parse( valueArray[0] );
                setPublicIPAddress( address );
                setPublicPort( NetworkUtil.DEF_HTTPS_PORT );
                break;

            case 2:
                address = IPAddress.parse( valueArray[0] );
                int port = Integer.parseInt( valueArray[1] );
                setPublicIPAddress( address );
                setPublicPort( port );
                break;

            default:
                /* just throw an exception to get out of dodge */
                throw new Exception();
            }
        } catch ( Exception e ) {
            throw new ParseException( PUBLIC_ADDRESS_EXCEPTION );
        }
    }

    /**
     * Retrieve the address portion of the public address.
     */
    public IPAddress getPublicIPAddress() { return this.publicIPAddress; }
    public void setPublicIPAddress(  IPAddress newValue  ) { this.publicIPAddress = newValue; }

    /**
     * Retrieve the port component of the public address.
     */
    public int getPublicPort()
    {
        if (( this.publicPort <= 0 ) || ( this.publicPort >= 0xFFFF )) {
            this.publicPort = NetworkUtil.DEF_HTTPS_PORT;
        }

        return this.publicPort;
    }

    /**
     * Set the port component of the public address.
     *
     * @param newValue the new port for the public address.
     */
    public void setPublicPort( int newValue )
    {
        if (( newValue <= 0 ) || ( newValue >= 0xFFFF )) newValue = NetworkUtil.DEF_HTTPS_PORT;

        this.publicPort = newValue;
    }

    /**
     * Return true if the current settings use a public address
     */
    public boolean hasPublicAddress()
    {
        return (( this.publicIPAddress != null ) &&  !this.publicIPAddress.isEmpty());
    }

    /**
     * The current hostname (calculated)
     */
    public HostAddress getCurrentPublicAddress()
    {
        HostAddress address = null;

        /* if using the public address then, get the address from the settings. */
        String publicAddress = this.getPublicAddress();

        IPAddress primaryAddress = com.untangle.uvm.UvmContextFactory.context().networkManager().getPrimaryAddress();
        
        /* has public address trumps over all other settings */
        if ( this.getIsPublicAddressEnabled() && ( publicAddress != null ) && ( publicAddress.trim().length() > 0 )) {
            /* has public address is set and a public address is available */
            address = new HostAddress( this.getPublicIPAddress());
            
        } else if ( this.getIsHostNamePublic() )  {
            /* no public address, use the primary address, and the hostname */
            String name = com.untangle.uvm.UvmContextFactory.context().networkManager().getHostname();
            
            /* If a hostname is available, and qualified, then use the hostname */
            if (( name != null ) && !name.equals("")) {
                address = new HostAddress( primaryAddress, name );
            }
        }

        /* If none of the other condititions have been met, just use the primary address */
        if ( address == null ) {
            address = new HostAddress( primaryAddress );
        }

        return address;
    }

    /**
     * @return the public url for the box, this is the address (may be hostname or ip address)
     */
    public String getCurrentURL()
    {
        HostAddress currentPublicAddress = this.getCurrentPublicAddress();
        int currentPublicPort = this.getCurrentPublicPort();
        String url = currentPublicAddress.toString();
        
        if (( NetworkUtil.DEF_HTTPS_PORT != currentPublicPort ) && ( currentPublicPort > 0 ) && ( currentPublicPort < 0xFFFF )) {
            url += ":" + currentPublicPort;
        }

        return url;
    }

    /**
     * The current port (calculated)
     */
    public int getCurrentPublicPort()
    {
        int port = this.getHttpsPort();

        if ( this.getIsPublicAddressEnabled() ) {
            port = this.getPublicPort();
        }

        if ( port <= 0 || port >= 0xFFFF ) {
            logger.warn( "port is an invalid value, using default: " + port );
            port = NetworkUtil.DEF_HTTPS_PORT;
        }

        return port;
    }

    /**
     * Validate that the settings are free of errors.
     */
    public void validate() throws ValidateException
    {
        /* nothing appears to be necessary here for now */
    }
}

