/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.networking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.untangle.jnetcap.InterfaceData;
import com.untangle.jnetcap.Netcap;
import com.untangle.mvvm.InterfaceAlias;
import com.untangle.mvvm.IntfConstants;
import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmState;
import com.untangle.mvvm.NetworkingConfiguration;
import com.untangle.mvvm.ArgonException;
import com.untangle.mvvm.networking.internal.RemoteInternalSettings;
import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.script.ScriptRunner;
import com.untangle.mvvm.tran.script.ScriptWriter;
import com.untangle.mvvm.util.StringUtil;
import org.apache.log4j.Logger;

import static com.untangle.mvvm.networking.NetworkManagerImpl.BUNNICULA_BASE;
import static com.untangle.mvvm.networking.NetworkManagerImpl.BUNNICULA_CONF;

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
    private static final String FLAGS_CFG_FILE    = BUNNICULA_CONF + "/networking.sh";

    private static final String SSH_ENABLE_SCRIPT  = BUNNICULA_BASE + "/ssh_enable.sh";
    private static final String SSH_DISABLE_SCRIPT = BUNNICULA_BASE + "/ssh_disable.sh";
    private static final String HOSTNAME_SCRIPT    = BUNNICULA_BASE + "/networking/save-hostname";
    private static final String DHCP_RENEW_SCRIPT  = BUNNICULA_BASE + "/networking/dhcp-renew";

    private static final String SSHD_PID_FILE     = "/var/run/sshd.pid";
    private static final String PROPERTY_FILE     = BUNNICULA_CONF + "/mvvm.networking.properties";
    private static final String DHCP_TEST_SCRIPT  = BUNNICULA_BASE + "/networking/dhcp-check";
    private static final int    DHCP_ENABLED_CODE = 1;

    private static final String FLAG_TCP_WIN      = "TCP_WINDOW_SCALING_EN";
    private static final String FLAG_HTTP_IN      = "MVVM_ALLOW_IN_HTTP";
    private static final String FLAG_HTTPS_OUT    = "MVVM_ALLOW_OUT_HTTPS";
    private static final String FLAG_HTTPS_RES    = "MVVM_ALLOW_OUT_RES";
    private static final String FLAG_OUT_NET      = "MVVM_ALLOW_OUT_NET";
    private static final String FLAG_OUT_MASK     = "MVVM_ALLOW_OUT_MASK";
    private static final String FLAG_EXCEPTION    = "MVVM_IS_EXCEPTION_REPORTING_EN";
    private static final String FLAG_POST_FUNC    = "MVVM_POST_CONF";
    private static final String FLAG_CUSTOM_RULES_FUNC = "MVVM_CUSTOM_RULES";
    private static final String POST_FUNC_NAME    = "postConfigurationScript";
    private static final String CUSTOM_RULES_NAME = "customRulesScript";
    
    /* Function declaration for the post configuration function */
    private static final String DECL_POST_CONF    = "function " + POST_FUNC_NAME + "() {";
    private static final String DECL_CUSTOM_RULES = "function " + CUSTOM_RULES_NAME + "() {";

    private static final String FLAG_IS_HOSTNAME_PUBLIC = "MVVM_IS_HOSTNAME_EN";
    private static final String FLAG_HOSTNAME          = "MVVM_HOSTNAME";
    private static final String FLAG_PUBLIC_ADDRESS_EN = "MVVM_PUBLIC_ADDRESS_EN";
    private static final String FLAG_PUBLIC_ADDRESS    = "MVVM_PUBLIC_ADDRESS";

    /* Property to determine the secondary https port */
    private static final String PROPERTY_HTTPS_PORT = "mvvm.https.port";
    private static final String PROPERTY_OUTSIDE_ADMINISTRATION = "mvvm.https.administration";
    private static final String PROPERTY_OUTSIDE_QUARANTINE     = "mvvm.https.quarantine";
    private static final String PROPERTY_OUTSIDE_REPORTING      = "mvvm.https.reporting";

    private static final String PROPERTY_COMMENT    = "Properties for the networking configuration";

    private final Logger logger = Logger.getLogger( this.getClass());

    private static final List<InterfaceData> EMPTY_INTF_DATA_LIST = Collections.emptyList();

    private boolean saveSettings = true;

    private NetworkConfigurationLoader()
    {
    }

    NetworkingConfiguration getNetworkingConfiguration()
        throws NetworkException
    {
        NetworkingConfiguration configuration = new NetworkingConfigurationImpl();

        loadBasicNetworkSettings( configuration );
        loadRemoteSettings( configuration );

        return configuration;
    }

    /* Load the current remote settings into a new remote settings object */
    RemoteSettings loadRemoteSettings()
    {
        RemoteSettings remote = new RemoteSettingsImpl();
        loadRemoteSettings( remote );
        return remote;
    }

    /* Fill in the remote settings for an existing remote settings object. */
    void loadRemoteSettings( RemoteSettings remote )
    {
        loadFlags( remote );
        loadHostname( remote );
        loadHttpsProperties( remote, remote.isOutsideAccessEnabled());
        loadSshFlag( remote );
    }

    void loadBasicNetworkSettings( BasicNetworkSettings basic ) throws NetworkException
    {
        loadDhcp( basic );
        loadDnsServers( basic );

        Netcap netcap = Netcap.getInstance();

        String external;

        external = MvvmContextFactory.context().localIntfManager().getExternal().getName();

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

    void disableSaveSettings()
    {
        this.saveSettings = false;
    }

    void enableSaveSettings()
    {
        this.saveSettings = true;
    }

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

    private void loadFlags( RemoteSettings remote )
    {
        String host = null;
        String mask = null;
        String publicAddress = null;

        /* Open up the interfaces file */
        try {
            BufferedReader in = new BufferedReader(new FileReader( FLAGS_CFG_FILE ));
            String str;
            while ((str = in.readLine()) != null) {
                str = str.trim();
                try {
                    /* Skip comments and empty lines */
                    if (( str.startsWith( "#" )) || ( str.length() == 0 )) {
                        continue;
                    } else if ( str.startsWith( FLAG_TCP_WIN )) {
                        remote.isTcpWindowScalingEnabled( parseBooleanFlag( str, FLAG_TCP_WIN ));
                    } else if ( str.startsWith( FLAG_HTTP_IN )) {
                        remote.isInsideInsecureEnabled( parseBooleanFlag( str, FLAG_HTTP_IN ));
                    } else if ( str.startsWith( FLAG_HTTPS_OUT )) {
                        remote.isOutsideAccessEnabled( parseBooleanFlag( str, FLAG_HTTPS_OUT ));
                    } else if ( str.startsWith( FLAG_HTTPS_RES )) {
                        remote.isOutsideAccessRestricted( parseBooleanFlag( str, FLAG_HTTPS_RES ));
                    } else if ( str.startsWith( FLAG_OUT_NET )) {
                        host = removeQuotes( str.substring( FLAG_OUT_NET.length() + 1 ));
                    } else if ( str.startsWith( FLAG_OUT_MASK )) {
                        mask = removeQuotes( str.substring( FLAG_OUT_MASK.length() + 1 ));
                    } else if ( str.startsWith( FLAG_EXCEPTION )) {
                        remote.isExceptionReportingEnabled( parseBooleanFlag( str, FLAG_EXCEPTION ));
                    } else if ( str.startsWith( FLAG_IS_HOSTNAME_PUBLIC )) {
                        remote.setIsHostnamePublic( parseBooleanFlag( str, FLAG_IS_HOSTNAME_PUBLIC ));
                    } else if ( str.startsWith( FLAG_PUBLIC_ADDRESS_EN )) {
                        remote.setIsPublicAddressEnabled( parseBooleanFlag( str, FLAG_PUBLIC_ADDRESS_EN ));
                    } else if ( str.startsWith( FLAG_PUBLIC_ADDRESS )) {
                        publicAddress = removeQuotes( str.substring( FLAG_PUBLIC_ADDRESS.length() + 1 ));
                    } else if ( str.startsWith( FLAG_POST_FUNC )) {
                        /* Nothing to do here, this is just here to indicate that a
                         * post configuration function exists */
                    } else if ( str.equals( DECL_POST_CONF )) {
                        remote.setPostConfigurationScript( parseScript( remote, in ));
                    } else if ( str.equals( DECL_CUSTOM_RULES )) {
                        remote.setCustomRules( parseScript( remote, in ));
                    } else {
                        logger.info( "Unknown line: '" + str + "'" );
                    }
                } catch ( Exception ex ) {
                    logger.warn( "Error while retrieving flags", ex );
                }
            }
            in.close();
        } catch ( FileNotFoundException ex ) {
            logger.warn( "Could not read '" + FLAGS_CFG_FILE + "' because it doesn't exist" );
        } catch ( Exception ex ) {
            logger.warn( "Error reading file: ", ex );
        }

        try {
            if ( host != null ) {
                remote.outsideNetwork( IPaddr.parse( host ));

                if ( mask != null ) remote.outsideNetmask( IPaddr.parse( mask ));
            }
        } catch ( Exception ex ) {
            logger.error( "Error parsing outside host or netmask", ex );
        }

        /* Handle the public address */
        if (( publicAddress != null ) && ( publicAddress.length() > 0 )) {
            /* Do not alter the value of the public address flag */
            try {
                remote.setPublicAddress( publicAddress );
            } catch ( ParseException e ) {
                logger.warn( "Unable to parse the public address: " + publicAddress );
                remote.setIsPublicAddressEnabled( false );
            }
        } else {
            remote.setIsPublicAddressEnabled( false );
        }
    }

    private void loadHostname( RemoteSettings remote )
    {
        HostName hostname = NetworkUtilPriv.getPrivInstance().loadHostname();

        if ( hostname != null && !hostname.isEmpty() && !NetworkUtil.DEFAULT_HOSTNAME.equals( hostname )) {
            remote.setHostname( hostname );
        } else {
            remote.setIsHostnamePublic( false );
            remote.setHostname( null );
        }
    }

    /* Parse the input stream until it reaches the end of the function */
    private String parseScript( RemoteSettings remote, BufferedReader in )
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

    private void loadHttpsProperties( RemoteSettings remote, boolean isOutsideAccessEnabled )
    {
        /* Try to read in the properties for the HTTPS port */
        remote.httpsPort( NetworkUtil.DEF_HTTPS_PORT );

        try {
            StringUtil su = StringUtil.getInstance();

            Properties properties = new Properties();
            File f = new File( PROPERTY_FILE );
            if ( f.exists()) {
                logger.debug( "Loading " + f );
                properties.load( new FileInputStream( f ));
                
                remote.httpsPort( su.parseInt( properties.getProperty( PROPERTY_HTTPS_PORT ), 
                                               NetworkUtil.DEF_HTTPS_PORT ));
                
                if ( properties.getProperty( PROPERTY_OUTSIDE_ADMINISTRATION ) == null ) {
                    logger.debug( "Outside administration is not set, using defaults." );
                    if ( isOutsideAccessEnabled ) {
                        remote.setIsOutsideAdministrationEnabled( true );
                        remote.setIsOutsideQuarantineEnabled( true );
                        remote.setIsOutsideReportingEnabled( true );
                    } else {
                        remote.setIsOutsideAdministrationEnabled( NetworkUtil.DEF_OUTSIDE_ADMINISTRATION );
                        remote.setIsOutsideQuarantineEnabled( NetworkUtil.DEF_OUTSIDE_QUARANTINE );
                        remote.setIsOutsideReportingEnabled( NetworkUtil.DEF_OUTSIDE_REPORTING );
                    }
                } else {
                    logger.debug( "Loading HTTP access settings." );
                    
                    boolean value = 
                        su.parseBoolean( properties.getProperty( PROPERTY_OUTSIDE_ADMINISTRATION ),
                                         NetworkUtil.DEF_OUTSIDE_ADMINISTRATION );
                    remote.setIsOutsideAdministrationEnabled( value );
                    
                    value = su.parseBoolean( properties.getProperty( PROPERTY_OUTSIDE_QUARANTINE ), 
                                             NetworkUtil.DEF_OUTSIDE_QUARANTINE );
                    remote.setIsOutsideQuarantineEnabled( value );
                    
                    value = su.parseBoolean( properties.getProperty( PROPERTY_OUTSIDE_REPORTING ), 
                                             NetworkUtil.DEF_OUTSIDE_REPORTING );
                    remote.setIsOutsideReportingEnabled( value );
                }

            }
        } catch ( Exception e ) {
            logger.warn( "Unable to load properties file: " + PROPERTY_FILE, e );
            remote.httpsPort( NetworkUtil.DEF_HTTPS_PORT );
            remote.setIsOutsideQuarantineEnabled( NetworkUtil.DEF_OUTSIDE_ADMINISTRATION );
            remote.setIsOutsideQuarantineEnabled( NetworkUtil.DEF_OUTSIDE_QUARANTINE );
            remote.setIsOutsideQuarantineEnabled( NetworkUtil.DEF_OUTSIDE_REPORTING );
        }
    }

    private void loadSshFlag( RemoteSettings remote )
    {
        /* SSH is enabled if and only if this file exists */
        File sshd = new File( SSHD_PID_FILE );

        remote.isSshEnabled( sshd.exists());
    }

    private boolean parseBooleanFlag( String nameValuePair, String name )
    {
        if ( nameValuePair.length() < name.length() + 1 ) return false;

        nameValuePair = removeQuotes( nameValuePair.substring( name.length() + 1 ));
        return Boolean.parseBoolean( nameValuePair );
    }

    private String removeQuotes( String value )
    {
        if ( value == null ) return "";
        return value.replace( '"', ' ' ).trim();
    }

    /* Save methods */

    void saveRemoteSettings( RemoteInternalSettings remote )
    {
        try {
            saveProperties( remote );
        } catch ( Exception e ) {
            logger.error( "Exception saving properties.", e );
        }

        try {
            saveHostname( remote );
        } catch ( Exception e ) {
            logger.error( "Exception saving hostname", e );
        }

        saveFlags( remote );
        saveSsh( remote );
    }

    private void saveFlags( RemoteInternalSettings remote ) {
        ScriptWriter sw = new ScriptWriter();

        sw.appendComment( "Set to true to enable\n" );
        sw.appendComment( "false or undefined is disabled.\n" );
        sw.appendVariable( FLAG_TCP_WIN, "" + remote.isTcpWindowScalingEnabled());
        sw.appendComment( "Allow inside HTTP true to enable" );
        sw.appendComment( "false or undefined is disabled." );
        sw.appendVariable( FLAG_HTTP_IN, "" + remote.isInsideInsecureEnabled());
        sw.appendComment( "Allow outside HTTPS true to enable" );
        sw.appendComment( "false or undefined to disable." );
        sw.appendVariable( FLAG_HTTPS_OUT, "" + remote.isOutsideAccessEnabled());
        sw.appendComment( "Restrict outside HTTPS access" );
        sw.appendComment( "True if restricted, undefined or false if unrestricted" );
        sw.appendVariable( FLAG_HTTPS_RES, "" + remote.isOutsideAccessRestricted());
        sw.appendComment( "Report exceptions\n" );
        sw.appendComment( "True to send out exception logs, undefined or false for not" );
        sw.appendVariable( FLAG_EXCEPTION, "" + remote.isExceptionReportingEnabled());

        if ( !remote.outsideNetwork().isEmpty()) {
            IPaddr network = remote.outsideNetwork();
            IPaddr netmask = remote.outsideNetmask();

            sw.appendComment( "If outside access is enabled and restricted, only allow access from" );
            sw.appendComment( "this network.\n" );

            sw.appendVariable( FLAG_OUT_NET, network.toString());

            if ( !netmask.isEmpty()) sw.appendVariable( FLAG_OUT_MASK, netmask.toString());

            sw.appendLine();
        }

        if ( remote.getPostConfigurationScript().length() > 0 ) {
            sw.appendComment( "Script to be executed after /etc/network/interfaces is executed\n" );
            sw.appendLine( DECL_POST_CONF );
            /* The post configuration script should be an object, allowing it to
             * be prevalidated */
            sw.appendLine( remote.getPostConfigurationScript().toString().trim());
            sw.appendLine( "}" );

            sw.appendComment( "Flag to indicate that there is a post configuuration script." );
            sw.appendVariable( FLAG_POST_FUNC, POST_FUNC_NAME );
        }

        if ( remote.getCustomRules().length() > 0 ) {
            sw.appendComment( "Script to be executed after the rule-generator is executed\n" );
            sw.appendLine( DECL_CUSTOM_RULES );
            /* The custom rules script should be an object, allowing it to be prevalidated */
            sw.appendLine( remote.getCustomRules().toString().trim());
            sw.appendLine( "}" );

            sw.appendComment( "Flag to indicate that there is is a custom rules script." );
            sw.appendVariable( FLAG_CUSTOM_RULES_FUNC, CUSTOM_RULES_NAME );
        }

        HostName hostname = remote.getHostname();
        /* The hostname itself is stored inside of /etc/hostname in saveHostname() */
        if ( hostname != null && !NetworkUtil.DEFAULT_HOSTNAME.equals( hostname )) {
            sw.appendVariable( FLAG_IS_HOSTNAME_PUBLIC, "" + remote.getIsHostnamePublic());
        } else {
            sw.appendVariable( FLAG_IS_HOSTNAME_PUBLIC, "" + false );
        }

        /* Append whether or not the public address is enabled */
        sw.appendVariable( FLAG_PUBLIC_ADDRESS_EN, "" + remote.getIsPublicAddressEnabled());

        if ( remote.getPublicAddress() != null ) {
            sw.appendVariable( FLAG_PUBLIC_ADDRESS, remote.getPublicAddress());
        }

        sw.writeFile( FLAGS_CFG_FILE );
    }

    private void saveProperties( RemoteInternalSettings remote ) throws Exception
    {

        /* rebind the https port */
        int httpsPort = remote.getPublicHttpsPort();
        try {
            MvvmContextFactory.context().appServerManager().rebindExternalHttpsPort( httpsPort );
        } catch ( Exception e ) {
            if ( !MvvmContextFactory.context().state().equals( MvvmState.RUNNING )) {
                /* This isn't a problem at startup, because the app manager uses the property also */
                /* this fails the first time because the tomcat manager isn't initialized yet */
                logger.info( "unable to rebind port at startup: " + e );
            } else {
                logger.warn( "unable to rebind https port", e );
            }
        }

        Properties properties = new Properties();
        // if ( configuration.httpsPort() != NetworkingConfigurationImpl.DEF_HTTPS_PORT ) {
            /* Make sure to write the file anyway, this guarantees that if the property
             * is already set, it gets overwritten with an empty value */
        // }

        /* Maybe only store this value if it has been changed */
        properties.setProperty( PROPERTY_HTTPS_PORT, String.valueOf( httpsPort ));
        properties.setProperty( PROPERTY_OUTSIDE_ADMINISTRATION, 
                                String.valueOf( remote.getIsOutsideAdministrationEnabled()));

        properties.setProperty( PROPERTY_OUTSIDE_QUARANTINE, 
                                String.valueOf( remote.getIsOutsideQuarantineEnabled()));

        properties.setProperty( PROPERTY_OUTSIDE_REPORTING, 
                                String.valueOf( remote.getIsOutsideReportingEnabled()));

        try {
            logger.debug( "Storing properties into: " + PROPERTY_FILE + "[" + httpsPort + "]" );
            properties.store( new FileOutputStream( new File( PROPERTY_FILE )), PROPERTY_COMMENT );
        } catch ( Exception e ) {
            logger.error( "Error saving HTTPS port" );
        }

        logger.debug( "Rebinding the HTTPS port" );
    }

    private void saveSsh( RemoteInternalSettings remote )
    {
        try {
            if ( remote.isSshEnabled()) {
                ScriptRunner.getInstance().exec( SSH_ENABLE_SCRIPT );
            } else {
                ScriptRunner.getInstance().exec( SSH_DISABLE_SCRIPT );
            }
        } catch ( Exception ex ) {
            logger.error( "Unable to configure ssh", ex );
        }
    }

    private void saveHostname( RemoteInternalSettings remote ) throws Exception
    {
        if ( !this.saveSettings ) {
            logger.warn( "not saving hostname as requested" );
            return;
        }

        ScriptRunner.getInstance().exec( HOSTNAME_SCRIPT, remote.getHostname().toString());
    }

    static NetworkConfigurationLoader getInstance()
    {
        return INSTANCE;
    }
}

