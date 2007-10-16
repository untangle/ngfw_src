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

package com.untangle.uvm.networking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.untangle.jnetcap.InterfaceData;
import com.untangle.jnetcap.Netcap;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.util.StringUtil;
import org.apache.log4j.Logger;

import static com.untangle.uvm.networking.NetworkManagerImpl.BUNNICULA_BASE;
import static com.untangle.uvm.networking.NetworkManagerImpl.BUNNICULA_CONF;

import static com.untangle.uvm.networking.ShellFlags.FILE_RULE_CFG;
import static com.untangle.uvm.networking.ShellFlags.FILE_PROPERTIES;

import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTP_IN;
import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTPS_OUT;
import static com.untangle.uvm.networking.ShellFlags.FLAG_HTTPS_RES;
import static com.untangle.uvm.networking.ShellFlags.FLAG_OUT_NET;
import static com.untangle.uvm.networking.ShellFlags.FLAG_OUT_MASK;

import static com.untangle.uvm.networking.ShellFlags.FLAG_TCP_WIN;
import static com.untangle.uvm.networking.ShellFlags.FLAG_POST_FUNC;
import static com.untangle.uvm.networking.ShellFlags.POST_FUNC_NAME;
import static com.untangle.uvm.networking.ShellFlags.DECL_POST_CONF;

import static com.untangle.uvm.networking.ShellFlags.FLAG_CUSTOM_RULES;
import static com.untangle.uvm.networking.ShellFlags.CUSTOM_RULES_NAME;
import static com.untangle.uvm.networking.ShellFlags.DECL_CUSTOM_RULES;

import static com.untangle.uvm.networking.ShellFlags.FLAG_IS_HOSTNAME_PUBLIC;
import static com.untangle.uvm.networking.ShellFlags.FLAG_HOSTNAME;
import static com.untangle.uvm.networking.ShellFlags.FLAG_PUBLIC_ADDRESS_EN;
import static com.untangle.uvm.networking.ShellFlags.FLAG_PUBLIC_ADDRESS;

import static com.untangle.uvm.networking.ShellFlags.PROPERTY_HTTPS_PORT;

/**
 * NetworkingConfigurationLoader is used to load a network configuration from the system.
 * It should only be used at two times:
 * 1. No parameters in the database.
 * 2. Load the RemoteSettings which are not stored inside of the database
 */

class NetworkConfigurationLoader
{
    /* Singleton */
    private static final NetworkConfigurationLoader INSTANCE = new NetworkConfigurationLoader();
    private static final String IP_CFG_FILE       = "/etc/network/interfaces";
    private static final String NS_CFG_FILE       = "/etc/resolv.conf";

    private static final String DHCP_RENEW_SCRIPT  = BUNNICULA_BASE + "/networking/dhcp-renew";

    private static final String SSHD_PID_FILE     = "/var/run/sshd.pid";
    private static final String DHCP_TEST_SCRIPT  = BUNNICULA_BASE + "/networking/dhcp-check";
    private static final int    DHCP_ENABLED_CODE = 1;

    private static final String FLAG_EXCEPTION    = "UVM_IS_EXCEPTION_REPORTING_EN";
    
    /* Property to determine the secondary https port */
    private static final String PROPERTY_OUTSIDE_ADMINISTRATION = "uvm.https.administration";
    private static final String PROPERTY_OUTSIDE_QUARANTINE     = "uvm.https.quarantine";
    private static final String PROPERTY_OUTSIDE_REPORTING      = "uvm.https.reporting";

    private final Logger logger = Logger.getLogger( this.getClass());

    private static final List<InterfaceData> EMPTY_INTF_DATA_LIST = Collections.emptyList();

    private NetworkConfigurationLoader()
    {
    }

    AccessSettings loadAccessSettings()
    {
        AccessSettings settings = new AccessSettings();

        /* Load the defaults */
        settings.setIsInsideInsecureEnabled( true );
        settings.setIsOutsideAccessEnabled( true );
        settings.setIsOutsideAdministrationEnabled( false );
        settings.setIsOutsideQuarantineEnabled( true );
        settings.setIsOutsideReportingEnabled( false );

        /* try to retrieve the settings from the configuration files */
        loadAccessSettings( settings );
        return settings;
    }

    void loadAccessSettings( AccessSettings settings )
    {
        loadFlags( settings );
        /* have to load properties after loading flags */
        loadProperties( settings );
        loadSshFlag( settings );
        settings.isClean( false );
    }

    AddressSettings loadAddressSettings()
    {
        AddressSettings settings = new AddressSettings();
        /* load reasonable defaults */
        settings.setHttpsPort( NetworkUtil.DEF_HTTPS_PORT );
        settings.setIsHostNamePublic( false );
        settings.setIsPublicAddressEnabled( false );
        /* try to retrieve the settings from the configuration files */
        loadAddressSettings( settings );
        return settings;
    }

    void loadAddressSettings( AddressSettings settings )
    {
        loadFlags( settings );
        loadProperties( settings );
        loadHostName( settings );
        settings.isClean( false );
    }

    MiscSettings loadMiscSettings()
    {
        MiscSettings settings = new MiscSettings();

        /* Load the defaults */
        settings.setIsExceptionReportingEnabled( false );
        settings.setIsTcpWindowScalingEnabled( false );
        settings.setPostConfigurationScript( "" );
        settings.setCustomRules( "" );
        /* try to retrieve the settings from the configuration files */
        loadMiscSettings( settings );
        return settings;
    }

    void loadMiscSettings( MiscSettings settings )
    {
        loadFlags( settings );
        settings.isClean( false );
    }


    BasicNetworkSettings loadBasicNetworkSettings() throws NetworkException
    {
        BasicNetworkSettings basic = new BasicNetworkSettings();
        loadBasicNetworkSettings( basic );
        return basic;
    }

    void loadBasicNetworkSettings( BasicNetworkSettings basic ) throws NetworkException
    {
        loadDhcp( basic );
        loadDnsServers( basic );

        Netcap netcap = Netcap.getInstance();

        String external;

        external = LocalUvmContextFactory.context().localIntfManager().getExternal().getName();

        netcap.updateAddress();

        List<InterfaceData> externalIntfDataList;

        try {
            externalIntfDataList = netcap.getInterfaceData( external );
        } catch ( Exception e ) {
            logger.warn( "Exception retrieving external interface data, setting to an empty list" );
            externalIntfDataList = EMPTY_INTF_DATA_LIST;
        }

        if ( externalIntfDataList == null ) externalIntfDataList = EMPTY_INTF_DATA_LIST;

        basic.host( NetworkUtil.EMPTY_IPADDR );
        basic.netmask( NetworkUtil.EMPTY_IPADDR );

        List<InterfaceAlias> interfaceAliasList = new LinkedList<InterfaceAlias>();

        boolean isFirst = true;
        for ( InterfaceData data : externalIntfDataList ) {
            if ( isFirst ) {
                basic.host( new IPaddr((Inet4Address)data.getAddress()));
                basic.netmask( new IPaddr((Inet4Address)data.getNetmask()));
            } else {
                /* XXX Broadcast address is presently not used */
                interfaceAliasList.add( new InterfaceAlias( data.getAddress(), data.getNetmask(), null ));
            }

            isFirst = false;
        }

        basic.setAliasList( interfaceAliasList );

        Inet4Address gateway = (Inet4Address)Netcap.getGateway();
        if ( gateway == null ) basic.gateway( NetworkUtil.EMPTY_IPADDR );
        else                   basic.gateway( new IPaddr( gateway ));
    }

    static NetworkConfigurationLoader getInstance()
    {
        return INSTANCE;
    }
    
    /************************ PRIVATE **********************/
    private void loadDhcp( BasicNetworkSettings basic )
    {
        boolean isDhcpEnabled;
        try {
            int code = 0;
            Process p = Runtime.getRuntime().exec( "sh " + DHCP_TEST_SCRIPT  );
            code = p.waitFor();

            isDhcpEnabled = ( code == DHCP_ENABLED_CODE );
        } catch ( Exception e ) {
            logger.warn( "Error testing DHCP address, continuing with false.", e );
            isDhcpEnabled = false;
        }

        basic.isDhcpEnabled( isDhcpEnabled );
    }

    private void loadDnsServers( BasicNetworkSettings basic )
    {
        List<IPaddr> dnsServers = NetworkUtilPriv.getPrivInstance().getDnsServers();

        if ( dnsServers.size() >= 2 ) {
            basic.dns1( dnsServers.get( 0 ));
            basic.dns2( dnsServers.get( 1 ));
        } else if ( dnsServers.size() == 1 ) {
            basic.dns1( dnsServers.get( 0 ));
        } else {
            logger.warn( "There are presently no DNS servers" );
        }
    }

    private void loadHostName( AddressSettings address )
    {
        HostName hostname = NetworkUtilPriv.getPrivInstance().loadHostname();

        if ( hostname != null && !hostname.isEmpty() && !NetworkUtil.DEFAULT_HOSTNAME.equals( hostname )) {
            address.setHostName( hostname );
        } else {
            address.setIsHostNamePublic( false );
            address.setHostName( null );
        }
    }

    private void loadProperties( AccessSettings access )
    {
        /* Used to calculate the defaults */
        boolean isOutsideAccessEnabled = access.getIsOutsideAccessEnabled();
        try {
            StringUtil su = StringUtil.getInstance();
            
            Properties properties = new Properties();
            File f = new File( FILE_PROPERTIES );
            if ( f.exists()) {
                logger.debug( "Loading " + f );
                properties.load( new FileInputStream( f ));
                
                if ( properties.getProperty( PROPERTY_OUTSIDE_ADMINISTRATION ) == null ) {
                    logger.debug( "Outside administration is not set, using defaults." );
                    if ( isOutsideAccessEnabled ) {
                        access.setIsOutsideAdministrationEnabled( true );
                        access.setIsOutsideQuarantineEnabled( true );
                        access.setIsOutsideReportingEnabled( true );
                    } else {
                        access.setIsOutsideAdministrationEnabled( NetworkUtil.DEF_OUTSIDE_ADMINISTRATION );
                        access.setIsOutsideQuarantineEnabled( NetworkUtil.DEF_OUTSIDE_QUARANTINE );
                        access.setIsOutsideReportingEnabled( NetworkUtil.DEF_OUTSIDE_REPORTING );
                    }
                } else {
                    logger.debug( "Loading HTTP access settings." );
                    
                    boolean value = 
                        su.parseBoolean( properties.getProperty( PROPERTY_OUTSIDE_ADMINISTRATION ),
                                         NetworkUtil.DEF_OUTSIDE_ADMINISTRATION );
                    access.setIsOutsideAdministrationEnabled( value );
                    
                    value = su.parseBoolean( properties.getProperty( PROPERTY_OUTSIDE_QUARANTINE ), 
                                             NetworkUtil.DEF_OUTSIDE_QUARANTINE );
                    access.setIsOutsideQuarantineEnabled( value );
                    
                    value = su.parseBoolean( properties.getProperty( PROPERTY_OUTSIDE_REPORTING ), 
                                             NetworkUtil.DEF_OUTSIDE_REPORTING );
                    access.setIsOutsideReportingEnabled( value );
                }
            }
        } catch ( Exception e ) {
            logger.warn( "Unable to load access properties from file: " + FILE_PROPERTIES, e );
            access.setIsOutsideAdministrationEnabled( NetworkUtil.DEF_OUTSIDE_ADMINISTRATION );
            access.setIsOutsideQuarantineEnabled( NetworkUtil.DEF_OUTSIDE_QUARANTINE );
            access.setIsOutsideReportingEnabled( NetworkUtil.DEF_OUTSIDE_REPORTING );
        }
    }

    private void loadProperties( AddressSettings address )
    {
        address.setHttpsPort( NetworkUtil.DEF_HTTPS_PORT );

        try {
            StringUtil su = StringUtil.getInstance();
            
            Properties properties = new Properties();
            File f = new File( FILE_PROPERTIES );
            if ( f.exists()) {
                logger.debug( "Loading " + f );
                properties.load( new FileInputStream( f ));
                
                address.setHttpsPort( su.parseInt( properties.getProperty( PROPERTY_HTTPS_PORT ),
                                                   NetworkUtil.DEF_HTTPS_PORT ));
            }
        } catch ( Exception e ) {
            logger.warn( "Unable to load access properties from file: " + FILE_PROPERTIES, e );
            address.setHttpsPort( NetworkUtil.DEF_HTTPS_PORT );
        }
    }
    
    /** The following functions are for reading settings from
     * networking.sh, they are only used on upgrade. */
    private void loadFlags( AccessSettings access )
    {
        String host = null;
        String mask = null;

        /* Open up the interfaces file */
        try {
            BufferedReader in = new BufferedReader( new FileReader( FILE_RULE_CFG ));
            String str;
            while ((str = in.readLine()) != null) {
                str = str.trim();
                try {
                    /* Skip comments and empty lines */
                    if (( str.startsWith( "#" )) || ( str.length() == 0 )) {
                        continue;
                    } else if ( str.startsWith( FLAG_HTTP_IN )) {
                        access.setIsInsideInsecureEnabled( parseBooleanFlag( str, FLAG_HTTP_IN ));
                    } else if ( str.startsWith( FLAG_HTTPS_OUT )) {
                        access.setIsOutsideAccessEnabled( parseBooleanFlag( str, FLAG_HTTPS_OUT ));
                    } else if ( str.startsWith( FLAG_HTTPS_RES )) {
                        access.setIsOutsideAccessRestricted( parseBooleanFlag( str, FLAG_HTTPS_RES ));
                    } else if ( str.startsWith( FLAG_OUT_NET )) {
                        host =  removeQuotes( str.substring( FLAG_OUT_NET.length() + 1 ));
                    } else if ( str.startsWith( FLAG_OUT_MASK )) {
                        mask = removeQuotes( str.substring( FLAG_OUT_MASK.length() + 1 ));
                    }  else {
                        logger.info( "Unknown line: '" + str + "'" );
                    }
                } catch ( Exception ex ) {
                    logger.warn( "Error while retrieving flags", ex );
                }
            }
            in.close();
        } catch ( FileNotFoundException ex ) {
            logger.warn( "Could not read '" + FILE_RULE_CFG + "' because it doesn't exist" );
        } catch ( Exception ex ) {
            logger.warn( "Error reading file: ", ex );
        }

        try {
            if ( host != null ) {
                access.setOutsideNetwork( IPaddr.parse( host ));

                if ( mask != null ) access.setOutsideNetmask( IPaddr.parse( mask ));
            }
        } catch ( Exception ex ) {
            logger.error( "Error parsing outside host or netmask", ex );
        }        
    }

    private void loadFlags( AddressSettings address )
    {
        String publicAddress = "";

        /* Open up the configuration file */
        try {
            BufferedReader in = new BufferedReader(new FileReader( FILE_RULE_CFG ));
            String str;
            while ((str = in.readLine()) != null) {
                str = str.trim();
                try {
                    /* Skip comments and empty lines */
                    if (( str.startsWith( "#" )) || ( str.length() == 0 )) {
                        continue;
                    } if ( str.startsWith( FLAG_PUBLIC_ADDRESS_EN )) {
                        address.setIsPublicAddressEnabled( parseBooleanFlag( str, FLAG_PUBLIC_ADDRESS_EN ));
                    } else if ( str.startsWith( FLAG_PUBLIC_ADDRESS )) {
                        publicAddress = removeQuotes( str.substring( FLAG_PUBLIC_ADDRESS.length() + 1 ));
                    } else if ( str.startsWith( FLAG_IS_HOSTNAME_PUBLIC )) {
                        address.setIsHostNamePublic( parseBooleanFlag( str, FLAG_IS_HOSTNAME_PUBLIC ));
                    } else {
                        logger.info( "Unknown line: '" + str + "'" );
                    }
                } catch ( Exception ex ) {
                    logger.warn( "Error while retrieving flags", ex );
                }
            }
            in.close();
        } catch ( FileNotFoundException ex ) {
            logger.warn( "Could not read '" + FILE_RULE_CFG + "' because it doesn't exist" );
        } catch ( Exception ex ) {
            logger.warn( "Error reading file: ", ex );
        }
        
        /* Handle the public address */
        if (( publicAddress != null ) && ( publicAddress.length() > 0 )) {
            /* Do not alter the value of the public address flag */
            try {
                address.setPublicAddress( publicAddress );
            } catch ( ParseException e ) {
                logger.warn( "Unable to parse the public address: " + publicAddress );
                /* disable the public address, if unable to determine what it is. */
                address.setIsPublicAddressEnabled( false );
            }
        } else {
            /* otherwise, disable the public address field if there isn't one. */
            address.setIsPublicAddressEnabled( false );
        }
    }

    /** The following functions are for reading settings from
     * networking.sh, they are only used on upgrade. */
    private void loadFlags( MiscSettings misc )
    {
        /* Open up the configuration file */
        try {
            BufferedReader in = new BufferedReader(new FileReader( FILE_RULE_CFG ));
            String str;
            while ((str = in.readLine()) != null) {
                str = str.trim();
                try {
                    /* Skip comments and empty lines */
                    if (( str.startsWith( "#" )) || ( str.length() == 0 )) {
                        continue;
                    } else if ( str.startsWith( FLAG_TCP_WIN )) {
                        misc.setIsTcpWindowScalingEnabled( parseBooleanFlag( str, FLAG_TCP_WIN ));
                    } else if ( str.startsWith( FLAG_EXCEPTION )) {
                        misc.setIsExceptionReportingEnabled( parseBooleanFlag( str, FLAG_EXCEPTION ));
                    } else if ( str.startsWith( FLAG_POST_FUNC )) {
                        /* Nothing to do here, this is just here to indicate that a
                         * post configuration function exists */
                    } else if ( str.equals( DECL_POST_CONF )) {
                        misc.setPostConfigurationScript( parseScript( in ));
                    } else if ( str.startsWith( FLAG_CUSTOM_RULES )) {
                        /* Nothing to do here, this is just here to indicate that a
                         * post configuration function exists */
                    } else if ( str.equals( DECL_CUSTOM_RULES )) {
                        misc.setCustomRules( parseScript( in ));
                    } else {
                        logger.info( "Unknown line: '" + str + "'" );
                    }
                } catch ( Exception ex ) {
                    logger.warn( "Error while retrieving flags", ex );
                }
            }
            in.close();
        } catch ( FileNotFoundException ex ) {
            logger.warn( "Could not read '" + FILE_RULE_CFG + "' because it doesn't exist" );
        } catch ( Exception ex ) {
            logger.warn( "Error reading file: ", ex );
        }
    }

    private void loadSshFlag( AccessSettings access )
    {
        /* SSH is enabled if and only if this file exists */
        File sshd = new File( SSHD_PID_FILE );

        access.setIsSupportEnabled( sshd.exists());
    }

    private boolean parseBooleanFlag( String nameValuePair, String name )
    {
        if ( nameValuePair.length() < name.length() + 1 ) return false;

        nameValuePair = removeQuotes( nameValuePair.substring( name.length() + 1 ));
        return Boolean.parseBoolean( nameValuePair );
    }

    /* Parse the input stream until it reaches the end of the function */
    private String parseScript( BufferedReader in )
        throws IOException
    {
        String command;
        StringBuilder sb = new StringBuilder();

        boolean isComplete = false;

        while (( command = in.readLine()) != null ) {
            command = command.trim();

            if ( command.equals( "}" )) {
                isComplete = true;
                break;
            }

            sb.append( command + "\n" );
        }

        if ( isComplete ) return sb.toString().trim();
        
        logger.warn( "Invalid post configuration script: " + sb.toString());
        return "";
    }

    private String removeQuotes( String value )
    {
        if ( value == null ) return "";
        return value.replace( '"', ' ' ).trim();
    }
}

