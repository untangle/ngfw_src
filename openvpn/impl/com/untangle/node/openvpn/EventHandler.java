/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import org.apache.log4j.Logger;

class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger( EventHandler.class );

    /* Is this a OPENVPN client, a OPENVPN client passes all traffic */
    private boolean isUntanglePlatformClient = false;

    /* Any client can connect to any exported address and vice versa */
    private List <IPMatcher> clientAddressList = new LinkedList<IPMatcher>();
    private List <IPMatcher> exportedAddressList = new LinkedList<IPMatcher>();

    private VpnSettings settings;
    
    /* Firewall Node */
    private final VpnNodeImpl node;

    EventHandler( VpnNodeImpl node )
    {
        super(node);

        this.node = node;
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequestEvent event )
        
    {
        handleNewSessionRequest( event.sessionRequest());
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequestEvent event )
        
    {
        handleNewSessionRequest( event.sessionRequest());
    }

    private void handleNewSessionRequest( IPNewSessionRequest request )
    {
        if ( logger.isDebugEnabled()) logger.debug( "New session: [" + request.id() + "]" );

        if ( request.getClientIntf() != IntfConstants.OPENVPN_INTF &&
             request.getServerIntf() != IntfConstants.OPENVPN_INTF ) {
            /* Nothing to do */
            request.release();
            return;
        }
        else if ( request.getClientIntf() == IntfConstants.OPENVPN_INTF &&
                  request.getServerIntf() == IntfConstants.OPENVPN_INTF ) {
            /* XXXXXXXXXX sort this out */
            request.release();
            return;
        }
        /* XXXX Handle bridging */
        else if ( request.getClientIntf() == IntfConstants.OPENVPN_INTF ) {
            /* OPENVPN client going to another interface */
            checkAddress( request, request.getClientAddr(), request.getServerAddr());
        }
        else {
            /* Local user trying to reach a OPENVPN client */
            checkAddress( request, request.getServerAddr(), request.getClientAddr());
        }
    }

    /**
     * Check whether vpn is allowed to talk to local
     */
    private void checkAddress( IPNewSessionRequest request, InetAddress vpn, InetAddress local )
    {
        boolean isValid = false;

        /* Clients pass all traffic */
        if ( this.isUntanglePlatformClient ) {
            node.incrementPassCount();
            request.release();
            return;
        }

        for ( IPMatcher matcher : this.clientAddressList ) {
            if ( logger.isDebugEnabled()) {
                logger.debug( "Testing vpn " + vpn.getHostAddress() + " against " + matcher );
            }
            if ( matcher.isMatch( vpn )) {
                isValid = true;
                break;
            }
        }
        if ( !isValid ) {
            reject( request );
            return;
        }

        /* The local address must be one of the exported addresses if proxy isn't enabled */
        isValid = false;
        for ( IPMatcher matcher : this.exportedAddressList ) {
            if ( logger.isDebugEnabled()) {
                logger.debug( "Testing local " + local.getHostAddress() + " against " + matcher );
            }
            if ( matcher.isMatch( local )) {
                isValid = true;
                break;
            }
        }

        if ( !isValid ) {
            reject( request );
            return;
        }

        /* Accept the request */
        if ( logger.isDebugEnabled()) {
            logger.debug( "Accepted OPENVPN session: [" + request.id() + "]" );
        }
        node.incrementPassCount();
        request.release();

        /* XXX Probably want to create an event */
        // node.statisticManager.incrRequest( protocol, request, reject );
    }

    private void reject( IPNewSessionRequest request )
    {
        /* XXX Should this always reject silently */
        request.rejectSilently();

        node.incrementBlockCount();

        /* XXX Probably want to create an event */
        if ( logger.isDebugEnabled()) {
            logger.debug( "Blocked OPENVPN session: [" + request.id() + "]" );
        }

        // node.statisticManager.incrRequest( protocol, request, reject );
    }

    void configure( VpnSettings settings )
    {
        logger.debug( "Configuring handler" );

        this.settings = settings;
        
        if ( settings.isUntanglePlatformClient()) {
            isUntanglePlatformClient = settings.isUntanglePlatformClient();
            return;
        }

        /* Create temporary lists */
        List <IPMatcher> clientAddressList = new LinkedList<IPMatcher>();
        List <IPMatcher> exportedAddressList = new LinkedList<IPMatcher>();

        for ( VpnGroup group : settings.getGroupList()) {
            /* Don't insert inactive groups */
            if ( !group.getLive()) continue;
            IPMatcher matcher = IPMatcher.makeSubnetMatcher( group.getAddress(), group.getNetmask());

            clientAddressList.add( matcher );
            if (logger.isDebugEnabled()) {
                logger.debug( "clientAddressList: [" + matcher + "]" );
            }
        }
        
        Map<String,VpnGroup> groupMap = OpenVpnManager.buildGroupMap(settings);

        for ( VpnSite site : settings.getSiteList()) {
            VpnGroup group = groupMap.get(site.getGroupName());
            /* Continue if the client isn't live or the group the client is in isn't live */
            if ( !site.trans_isEnabled() || ( group == null ) || !group.getLive()) {
                continue;
            }

            for ( SiteNetwork siteNetwork : site.getExportedAddressList()) {
                if ( !siteNetwork.getLive()) continue;
                IPMatcher matcher =
                    IPMatcher.makeSubnetMatcher( siteNetwork.getNetwork(), siteNetwork.getNetmask());

                clientAddressList.add( matcher );
                if (logger.isDebugEnabled()) {
                    logger.debug( "clientAddressList: [" + matcher + "]" );
                }
            }
        }

        for ( SiteNetwork siteNetwork : settings.getExportedAddressList()) {
            if ( !siteNetwork.getLive()) continue;
            IPMatcher matcher =
                IPMatcher.makeSubnetMatcher( siteNetwork.getNetwork(), siteNetwork.getNetmask());

            exportedAddressList.add( matcher );
            if (logger.isDebugEnabled()) {
                logger.debug( "exportedAddressList: [" + matcher + "]" );
            }
        }

        this.clientAddressList   = clientAddressList;
        this.exportedAddressList = exportedAddressList;

        logger.debug( "" );
    }
}
