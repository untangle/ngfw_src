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

package com.untangle.node.openvpn;

import com.untangle.uvm.security.Tid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.LinkedHashMap;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.net.UnknownHostException;


import org.apache.log4j.Logger;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;

import com.untangle.uvm.networking.IPNetwork;
import com.untangle.uvm.networking.LocalNetworkManager;
import com.untangle.uvm.networking.NetworkUtil;

import com.untangle.uvm.networking.internal.NetworkSpaceInternal;


import com.untangle.uvm.node.AddressRange;
import com.untangle.uvm.node.AddressValidator;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.ValidateException;

import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.node.script.ScriptException;




/* XXX Probably want to make this an abstract class and make this a little more generic */
class Sandbox
{
    private final Logger logger = Logger.getLogger( getClass());
    
    private static final int DEFAULT_MAX_CLIENTS = 500;
    private static final boolean DEFAULT_KEEP_ALIVE  = true;
    private static final boolean DEFAULT_EXPOSE_CLIENTS = true;

    private static final String DOWNLOAD_SCRIPT = Constants.SCRIPT_DIR + "/get-client";
    private static final String LS_CLIENTS_SCRIPT = Constants.SCRIPT_DIR + "/list-usb-configs";
    private static final String CLIENT_LIST_FILE = Constants.MISC_DIR + "/client-list";

    private static final String OPENVPN_CLIENT_FILE = OpenVpnManager.OPENVPN_CONF_DIR + "/client.conf";

    /* Trying a pretty strange collection, hopefully there is a match. */
    private static final String[] AUTO_ADDRESS_POOLS_STRING = {
        "172.16.0.0/24",   "172.16.1.0/24",   "172.16.2.0/24",   "172.16.3.0/24",
        "172.16.4.0/24",   "172.16.5.0/24",   "172.16.6.0/24",   "172.16.7.0/24",
        "192.168.16.0/24", "192.168.17.0/24", "192.168.18.0/24", "192.168.19.0/24",
        "192.168.20.0/24", "192.168.21.0/24", "192.168.22.0/24", "192.168.23.0/24",
        "10.254.16.0/24",  "10.254.17.0/24",  "10.254.18.0/24",  "10.254.19.0/24",
        "10.254.20.0/24",  "10.254.21.0/24",  "10.254.22.0/24",  "10.254.23.0/24"

    };
    private static final Map<IPNetwork,AddressRange> AUTO_ADDRESS_POOLS;

    private HostAddress vpnServerAddress;

    private CertificateParameters certificateParameters;
    private GroupList  groupList;
    private ExportList exportList;
    private ClientList clientList;
    private SiteList   siteList;
    private final VpnNode.ConfigState configState;

    private final Map<String,VpnGroup> resolveGroupMap = new HashMap<String,VpnGroup>();
    
    Sandbox( VpnNode.ConfigState configState )
    {
        this.configState = configState;
        if(configState==VpnNode.ConfigState.SERVER_ROUTE) {
            //XXX: this is an workaround to add a default group.
            List<VpnGroup> groupList=new ArrayList<VpnGroup>();
            VpnGroup vpnGroup= new VpnGroup();
            vpnGroup.setName("[no name]");
            vpnGroup.setLive(true);
            try {
                vpnGroup.setAddress(IPaddr.parse("172.16.16.0"));
                vpnGroup.setNetmask(IPaddr.parse("255.255.255.0"));
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            groupList.add(vpnGroup);
            this.groupList= new GroupList(groupList);
            this.clientList=new ClientList();
            this.siteList=new SiteList();
        }
    }

    List<String> getAvailableUsbList() throws NodeException
    {
        BufferedReader in = null;
        
        try {
            /* Run the script to generate a list of the clients that are available */
            ScriptRunner.getInstance().exec( LS_CLIENTS_SCRIPT );

            /* Get the List */
            in = new BufferedReader( new FileReader( CLIENT_LIST_FILE ));
            List<String> availableList = new LinkedList<String>();

            /* Add each line to the available list */
            String str;
            while (( str = in.readLine()) != null ) availableList.add( str.trim());
            
            return availableList;
        } catch ( ScriptException e ) {
            switch ( e.getCode()) {                
            case Constants.USB_ERROR_CODE:
                throw new UsbUnavailableException( "Unable to read the USB device." );

            default:
                logger.warn( "Unable to read USB files", e );
                throw new NodeException( "Unable to read the USB device." );
            }
        } catch ( FileNotFoundException e ) {
            /* It is a problem, but no need to let the client know this */
            logger.warn( "File " + CLIENT_LIST_FILE + " does not exist" );
        } catch ( Exception e ) {
            logger.warn( "Unable to read USB files", e );
            throw new NodeException( "Unable to read USB files." );
        } finally {
            try {
                if ( in != null ) in.close();
            } catch ( Exception e ) {
                /* who cares, it is attempting to close the file */
            }
        }

        /* This is for any exceptions that are logged, but not errors */
        return new LinkedList<String>();
    }

    void downloadConfig( HostAddress address, int port, String key ) throws Exception
    {
        /* The key must be a valid hexadecimal number, this is 
         * an easy check to detect spaces, and anyother weird characters
         */
        try {
            Long.parseLong( key, 16 );
        } catch ( NumberFormatException e ) { 
            throw new ValidateException( "A key must only contain numbers and the letters A-F: " + key );
        }
        String serverSocketAddress = address.toString();
        
        if ( port != 443 && port != 0 ) serverSocketAddress += ":" + port;

        logger.debug( "Downloading key from: " + serverSocketAddress );

        execDownloadScript( false, serverSocketAddress, key );
    }

    void downloadConfigUsb( String name ) throws Exception
    {
        VpnClientBase.validateName( name );
        execDownloadScript( true, name, "" );
    }

    private void execDownloadScript( boolean isUsb, String arg1, String arg2 ) throws Exception
    {
        try {
            ScriptRunner.getInstance().exec( DOWNLOAD_SCRIPT, String.valueOf( isUsb ), arg1, arg2 );
        } catch ( ScriptException e ) {
            switch ( e.getCode()) {
            case Constants.DOWNLOAD_ERROR_CODE: 
                throw new DownloadException( "Unable to download client." );
                
            case Constants.START_ERROR:
                throw new StartException( "Test connection with OpenVPN server failed." );

            case Constants.USB_ERROR_CODE:
                throw new UsbUnavailableException( "Unable to read the USB device." );

            default:
                logger.warn( "Unable to install client configuration", e );
                throw new NodeException( "Unable to install client configuration" );
            }
        } catch ( Exception e ) {
            logger.warn( "Unable to install client configuration", e );
            throw new NodeException( "Unable to install client configuration." );
        }

        /* Parse out the client configuration address */
        vpnServerAddress = new HostAddress( new IPaddr( null ));
        BufferedReader in = null;
        try { 
            in = new BufferedReader( new FileReader( OPENVPN_CLIENT_FILE ));
            String line;
            while(( line = in.readLine()) != null ) {
                line = line.trim();
                if ( line.startsWith( OpenVpnManager.FLAG_REMOTE )) {
                    String valueArray[] = line.split( " " );

                    if ( valueArray.length != 3 ) {
                        logger.warn( "Invalid client configuration" );
                        break;
                    }
                    
                    vpnServerAddress = HostAddress.parse( valueArray[1] );
                    break;
                }
            }
        } catch ( Exception e ) {
            logger.warn( "Error reading client configuration file", e );
            vpnServerAddress = new HostAddress( new IPaddr( null ));
        } finally {
            if ( in != null ) try { in.close(); } catch ( Exception e ) {};
        }
    }
    
    void generateCertificate( CertificateParameters parameters ) throws Exception
    {
        parameters.validate();

        this.certificateParameters = parameters;
    }

    GroupList getGroupList() throws Exception {
        if ( this.groupList == null ) throw new ValidateException( "Groups haven't been created yet" );
        return this.groupList;
    }
    
    void setGroupList( GroupList parameters ) throws ValidateException
    {
        parameters.validate();
        
        this.groupList = parameters;

        /* Update the resolve group map */
        resolveGroupMap.clear();

        for ( VpnGroup group : this.groupList.getGroupList()) {
            if ( resolveGroupMap.put( group.getName(), group ) != null ) {
                /* This shouldn't happen because this is validated in parameters.validate */
                throw new ValidateException( "Group name must be unique: '" + group.getName() + "'" );
            }
        }        
    }

    /* This will automatically pick a valid address group based on the
     * network settings. */
    void autoDetectAddressPool() throws ValidateException
    {
        /* Load the list of networks. */
        LocalNetworkManager lnm = LocalUvmContextFactory.context().networkManager();
        
        List<AddressRange> currentNetwork = new LinkedList<AddressRange>();
        for ( NetworkSpaceInternal space : lnm.getNetworkInternalSettings().getNetworkSpaceList()) {
            for ( IPNetwork network : space.getNetworkList()) {
                AddressRange range = AddressRange.makeNetwork( network.getNetwork().getAddr(), 
                                                               network.getNetmask().getAddr());
                currentNetwork.add( range );
            }
        }
        
        IPNetwork network = null;
        
        for ( Map.Entry<IPNetwork,AddressRange> e : AUTO_ADDRESS_POOLS.entrySet()) {
            network = e.getKey();
            for ( AddressRange range : currentNetwork ) {
                if ( range.overlaps( e.getValue())) {
                    network = null;
                    break;
                }
            }

            if ( network != null ) break;
        }
        
        if ( network == null ) {
            logger.warn( "Unable to auto detect a network for VPN." );
            return;
        }
        
        VpnGroup group = new VpnGroup();
        group.setLive( true );
        group.setUseDNS( false );
        group.setAddress( network.getNetwork());
        group.setNetmask( network.getNetmask());
        /* i18n. */
        group.setName( "[no name]" );
        GroupList gl = new GroupList();
        List<VpnGroup> list = new LinkedList<VpnGroup>();
        list.add( group );
        gl.setGroupList( list );
        setGroupList( gl );
    }

    ExportList getExportList()
    {
        return this.exportList;
    }
    
    void setExportList( ExportList parameters ) throws ValidateException
    {
        parameters.validate();

        this.exportList = parameters;
    }

    void autoDetectExportList() throws ValidateException
    {
        /* Load the list of networks. */
        LocalNetworkManager lnm = LocalUvmContextFactory.context().networkManager();
        
        byte intf = IntfConstants.INTERNAL_INTF;
        if ( lnm.isSingleNicModeEnabled()) intf = IntfConstants.EXTERNAL_INTF;
        
        NetworkSpaceInternal space = lnm.getNetworkInternalSettings().getNetworkSpace( intf );

        if ( space == null ) {
            logger.warn( "Unable to find the network space for the internal interface." );
            return;
        }
        
        List<ServerSiteNetwork> networkList = new LinkedList<ServerSiteNetwork>();
        LinkedList<AddressRange> rangeList = new LinkedList<AddressRange>();
        
        AddressValidator av = AddressValidator.getInstance();
        
        for ( IPNetwork network : space.getNetworkList()) {
            if ( NetworkUtil.getInstance().isBogus( network.getNetwork())) continue;

            rangeList.addFirst( AddressRange.makeNetwork( network.getNetwork().getAddr(), 
                                                          network.getNetmask().getAddr()));
            
            if ( !av.validate( rangeList ).isValid()) {
                rangeList.removeFirst();
                continue;
            }

            ServerSiteNetwork ssn = new ServerSiteNetwork();
            ssn.setNetwork( network.getNetwork());
            ssn.setNetmask( network.getNetmask());
            ssn.setLive( true );
            ssn.setName( "[no name]" );
            networkList.add( ssn );
        }

        
        setExportList( new ExportList( networkList ));
    }

    void setClientList( ClientList parameters ) throws ValidateException
    {
        parameters.validate();
        
        fixGroups( parameters.getClientList());
        this.clientList = parameters;
    }

    void setSiteList( SiteList parameters ) throws ValidateException
    {
        /* Validate the site list against the client list */
        parameters.validate( clientList );

        fixGroups( parameters.getSiteList());
        this.siteList = parameters;
    }

    private void fixGroups( List newClientList )
        throws ValidateException
    {
        for ( VpnClientBase client : (List<VpnClientBase>)newClientList ) {
            String name = client.getGroup().getName();
            VpnGroup newGroup = resolveGroupMap.get( name );
            if ( newGroup == null ) {
                throw new ValidateException( "The group '" + name + "' is not in the group list" );
            }
            client.setGroup( newGroup );
        }
    }

    VpnSettings completeConfig( Tid tid ) throws Exception
    {
        /* Create new settings */
        VpnSettings settings = new VpnSettings( tid );

        switch ( configState ) {
        case SERVER_BRIDGE:
            throw new ValidateException( "Bridge mode is presently unsupported" );
            // settings.setBridgeMode( true );
            // break;
        case SERVER_ROUTE:
            settings.setBridgeMode( false );
            settings.setUntanglePlatformClient( false );
            break;

        case CLIENT:
            settings.setUntanglePlatformClient( true );
            settings.setBridgeMode( false ); /* This would come from the other box */
            settings.setServerAddress( vpnServerAddress );

            /* Nothing left to do */
            return settings;
            
        default:
            throw new ValidateException( "Invalid state for sandbox: " + this.configState );
        }

        /* Certificate parameters */
        settings.setOrganization( this.certificateParameters.getOrganization());
        settings.setDomain( this.certificateParameters.getDomain());
        settings.setCountry( this.certificateParameters.getCountry());
        settings.setProvince( this.certificateParameters.getState());
        settings.setLocality( this.certificateParameters.getLocality());
        settings.setCaKeyOnUsb( this.certificateParameters.getStoreCaUsb());

        /* Presently the site name is the organization name */
        settings.setSiteName( this.certificateParameters.getOrganization());

        /* Group list */
        settings.setGroupList( this.groupList.getGroupList());

        /* Client list */
        settings.setClientList( this.clientList.getClientList());
        
        settings.setSiteList( this.siteList.getSiteList());

        settings.setExportedAddressList( this.exportList.getExportList());

        /* Hiding these from the user right now */
        settings.setMaxClients( DEFAULT_MAX_CLIENTS );
        settings.setKeepAlive( DEFAULT_KEEP_ALIVE );
        settings.setExposeClients( DEFAULT_EXPOSE_CLIENTS );
        
        settings.validate();
        
        return settings;
    }
    
    static
    {
        Map<IPNetwork,AddressRange> map = new LinkedHashMap<IPNetwork,AddressRange>();

        for ( String s : AUTO_ADDRESS_POOLS_STRING ) {
            try {
                IPNetwork network = IPNetwork.parse( s );
                AddressRange range = AddressRange.makeNetwork( network.getNetwork().getAddr(),
                                                               network.getNetmask().getAddr());
                map.put( network, range );
            } catch ( ParseException e ) {
                System.err.println( "Unable to parse: " + s );
            }
        }

        AUTO_ADDRESS_POOLS = Collections.unmodifiableMap( map );
    }
    
}
