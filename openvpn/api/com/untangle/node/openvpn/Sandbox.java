/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.util.I18nUtil;

public class Sandbox
{
    private final Logger logger = Logger.getLogger( getClass());

    private static final int DEFAULT_MAX_CLIENTS = 500;
    private static final boolean DEFAULT_KEEP_ALIVE  = true;
    private static final boolean DEFAULT_EXPOSE_CLIENTS = true;

    private static final String INSTALL_SCRIPT = Constants.SCRIPT_DIR + "/install-client";

    private static final String OPENVPN_CLIENT_FILE = OpenVpnManager.OPENVPN_CONF_DIR + "/client.conf";

    private InetAddress vpnServerAddress;

    private CertificateParameters certificateParameters;
    private GroupList  groupList;
    private ExportList exportList;
    private ClientList clientList;
    private SiteList   siteList;
    private final VpnNode.ConfigState configState;

    private final Map<String,VpnGroup> resolveGroupMap = new HashMap<String,VpnGroup>();

    private I18nUtil i18nUtil = null;

    Sandbox( VpnNode.ConfigState configState )
    {
        this.configState = configState;
        if( configState==VpnNode.ConfigState.SERVER ) {
            this.groupList= new GroupList();
            this.clientList=new ClientList();
            this.siteList=new SiteList();
        }
        i18nUtil = new I18nUtil(UvmContextFactory.context().languageManager().getTranslations("untangle-node-openvpn"));
    }

    void installClientConfig( String path ) throws Exception
    {

        logger.debug( "Installing from : " + path );

        execInstallScript( path );
    }

    private void execInstallScript( String path ) throws Exception
    {
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( INSTALL_SCRIPT + " \""  + path + "\"");

        switch ( result.getResult()) {
        case 0:
            break;

        case Constants.START_ERROR:
            throw new StartException( "Test connection with OpenVPN server failed." );

        case Constants.INVALID_FILE_ERROR:
            throw new InvalidFileException( "Invalid OpenVPN Client configuration" );

        default:
            logger.warn( "Unable to install client configuration. returned :" + result.getResult() + " output: " + result.getOutput() );
            throw new Exception( "Unable to install client configuration" );
        }

        /* Parse out the client configuration address */
        vpnServerAddress = null;
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

                    vpnServerAddress = InetAddress.getByName( valueArray[1] );
                    break;
                }
            }
        } catch ( Exception e ) {
            logger.warn( "Error reading client configuration file", e );
            vpnServerAddress = null;
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
        if ( this.groupList == null ) throw new Exception( "Groups haven't been created yet" );
        return this.groupList;
    }

    void setGroupList( GroupList parameters ) throws Exception
    {
        this.groupList = parameters;

        /* Update the resolve group map */
        resolveGroupMap.clear();

        for ( VpnGroup group : this.groupList.getGroupList()) {
            if ( resolveGroupMap.put( group.getName(), group ) != null ) {
                /* This shouldn't happen because this is validated in parameters.validate */
                throw new Exception( "Group name must be unique: '" + group.getName() + "'" );
            }
        }
    }

    /* This will automatically pick a valid address group based on the
     * network settings. */
    void autoDetectAddressPool() throws Exception
    {
        /* Load the list of networks. */
        List<AddressRange> currentNetwork = new LinkedList<AddressRange>();
        for (InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces()) {
            IPMaskedAddress net;
            AddressRange range;

            /* add the primary */
            InetAddress address = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Address();
            InetAddress netmask = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Netmask();
            if ( address != null && netmask != null ) {
                range = AddressRange.makeNetwork( address, netmask );
                currentNetwork.add(range);
            }

            // FIXME aliases
            // if (intf.getAliases() != null) {
            //     for ( IPMaskedAddress alias : intf.getAliases() ) {
            //         range = AddressRange.makeNetwork( alias.getNetwork().getAddr(), alias.getNetmask().getAddr() );
            //         currentNetwork.add(range);
            //     }
            // }
        }

        VpnGroup group = new VpnGroup();
        group.setLive( true );
        group.setUseDNS( false );

        //FIXME new better suggestion
        group.setAddress( InetAddress.getByName("172.16.2.0") );
        group.setNetmask( InetAddress.getByName("255.255.255.0") );
        
        group.setName( i18nUtil.tr("default") );
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

    void setExportList( ExportList parameters ) throws Exception
    {
        parameters.validate();

        this.exportList = parameters;
    }

    void autoDetectExportList() throws Exception
    {
        /* Load the list of networks. */
        List<SiteNetwork> networkList = new LinkedList<SiteNetwork>();

        for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if (! intf.getIsWan() ) {
                InetAddress address = intf.getV4StaticAddress();
                InetAddress netmask = intf.getV4StaticNetmask();

                if (address == null || netmask == null)
                    continue;

                SiteNetwork ssn = new SiteNetwork();
                ssn.setNetwork( address );
                ssn.setNetmask( netmask );
                ssn.setLive( true );
                ssn.setName( intf.getName() );
                networkList.add( ssn );
            }
        }

        setExportList( new ExportList( networkList ));
    }

    void setClientList( ClientList parameters ) throws Exception
    {
        parameters.validate();

        fixGroups( parameters.getClientList());
        this.clientList = parameters;
    }

    void setSiteList( SiteList parameters ) throws Exception
    {
        fixGroups( parameters.getSiteList());
        this.siteList = parameters;
    }

    @SuppressWarnings("unchecked")
    private void fixGroups( List newClientList )
        throws Exception
    {
        for ( VpnClient client : (List<VpnClient>)newClientList ) {
            String name = client.getGroupName();
            VpnGroup newGroup = resolveGroupMap.get( name );
            if ( newGroup == null ) {
                throw new Exception( "The group '" + name + "' is not in the group list" );
            }
        }
    }

    VpnSettings completeConfig( NodeSettings tid ) throws Exception
    {
        /* Create new settings */
        VpnSettings settings = new VpnSettings();

        switch ( configState ) {
        case SERVER:
            settings.setUntanglePlatformClient( false );
            break;

        case CLIENT:
            settings.setUntanglePlatformClient( true );
            settings.setServerAddress( vpnServerAddress );

            /* Nothing left to do */
            return settings;

        default:
            throw new Exception( "Invalid state for sandbox: " + this.configState );
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

        return settings;
    }
}
