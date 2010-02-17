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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.SessionMatcherFactory;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.ip.IPSimpleMatcher;
import com.untangle.uvm.node.firewall.ip.IPSubnetMatcher;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.Worker;
import com.untangle.uvm.util.WorkerRunner;



class SingleNicManager
{
    private static final String SINGLE_NIC_FLAG = "8e1f48a294372f872b74fedec79696a8";

    private static long POLL_TIMEOUT_MS = 2500;

    /* How often to tell the arp eater about a host. */
    private static long UPDATE_TIMEOUT_NS = 5l * 1000l * 1000l * 1000l;

    /* How often to clean off any expired nodes. */
    private static final long CLEANUP_INTERVAL_NS = 600l * 1000l * 1000l * 1000l;

    private static final int STATUS_SUCCESS = 104;    

    private static final String RESPONSE_STATUS = "status";
    private static final String RESPONSE_MESSAGE = "message";

    private final Logger logger = Logger.getLogger(getClass());

    /* List of the networks that should be updated. */
    private IPMatcher networkMatcher;

    private final BlockingQueue<InetAddress> queue = new LinkedBlockingQueue<InetAddress>( 32 );

    private boolean isEnabled;

    private final NetworkSettingsListener listener = new SettingsListener();

    private final WorkerRunner registrationHandler = new WorkerRunner( new AddressRegistrationHandler(), LocalUvmContextFactory.context());

    SingleNicManager()
    {
    }

    void registerAddress( InetAddress address )
    {
        if ( this.isEnabled == false ) return;

        if ( address == null ) return;

        /* Check if this is a local address */
        if ( !networkMatcher.isMatch( address )) {
            if ( logger.isDebugEnabled()) {
                logger.debug( "Address <"  + address.getHostAddress() + "> not on the internal network." );
            }
            
            return;
        }

        if ( !this.queue.offer( address )) {
            if ( logger.isInfoEnabled()) {
                logger.info( "Queue is full skipping address: " + address.getHostAddress());
            }
            return;
        }

        if ( logger.isDebugEnabled()) logger.debug( "Registered the host: " + address.getHostAddress());
    }

    void setIsEnabled( String flag )
    {
        if ( flag == null ) flag = "";

        boolean newValue = false;
        if ( SINGLE_NIC_FLAG.equals( flag )) newValue = true;

        logger.debug( "setSingleNicMode(" + newValue + ")" );

        if ( newValue != this.isEnabled ) {
            logger.info( "Changing the state of single NIC mode[" + newValue + "], killall all sessions." );
            
            ArgonManager argonManager = LocalUvmContextFactory.context().argonManager();
            argonManager.shutdownMatches(SessionMatcherFactory.getAllInstance());
        }
        
        this.isEnabled = newValue;
    }
    
    boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    NetworkSettingsListener getListener()
    {
        return this.listener;
    }

    void start()
    {
        this.registrationHandler.start();
    }


    void stop()
    {
        this.registrationHandler.stop();
    }

    /**
     * Update the internal network with a list of networks.
     * 
     * @param networkList The list of networks that are on the local network.
     */    
    private void setNetworks( List<IPNetwork> networkList )
    {
        switch ( networkList.size()) {
        case 0:
            this.networkMatcher = IPSimpleMatcher.getNilMatcher();
            break;

        case 1: {
            IPNetwork network = networkList.get( 0 );
            InetAddress ip = network.getNetwork().getAddr();
            InetAddress netmask = network.getNetmask().getAddr();
            
            if (( ip == null ) || ( netmask == null )) {
                this.networkMatcher = IPSimpleMatcher.getNilMatcher();
            } else {
                this.networkMatcher = IPSubnetMatcher.makeInstance( ip, netmask );
            }
            break;
        }
            
        default:
            /* There presently isn't a generic way of matching or'ing several matchers
             * together, right now this is hacked together with a simple matcher that
             * just iterates a list */
            final List<IPMatcher> matcherList = new LinkedList<IPMatcher>();
            for ( IPNetwork network : networkList ) {
                matcherList.add( IPSubnetMatcher.makeInstance( network.getNetwork(), network.getNetmask()));
            }
            
            this.networkMatcher = new IPMatcher() {
                    public boolean isMatch( InetAddress address ) {
                        /* iterate all of the matchers and check if any of them match */
                        for ( IPMatcher matcher : matcherList ) {
                            if ( matcher.isMatch( address )) return true;
                        }
                        /* otherwise return false */
                        return false;
                    }

                    public String toDatabaseString() {
                        return "never will i ever";
                    }
                };
        }
    }

    private class SettingsListener implements NetworkSettingsListener
    {
        public void event( NetworkSpacesInternalSettings settings )
        {
            NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();

            List<IPNetwork> networkList = new LinkedList<IPNetwork>();

            for ( NetworkSpaceInternal space : settings.getNetworkSpaceList()) {
                for ( IPNetwork network : space.getNetworkList()) {
                    if ( nup.isBogus( network.getNetwork())) continue;
                    networkList.add( network );
                }
            }
            
            setNetworks( networkList );
        }
    }

    private class AddressRegistrationHandler implements Worker
    {
        /* Map between the address and the next time it should be updated. */
        private final Map<InetAddress,Long> addressMap = new HashMap<InetAddress,Long>();

        private final List<String> readyList = new LinkedList<String>();

        private long nextTransmit = System.nanoTime() + UPDATE_TIMEOUT_NS;
        private long nextCleanup = 0;
        
        public void work() throws InterruptedException
        {
            boolean sendNow = handleQueue();

            long now = System.nanoTime();
            
            if ( sendNow || ( now > this.nextTransmit )) {
                try {
                    send();
                } catch ( Exception e ) {
                    if ( logger.isInfoEnabled()) logger.info( "Unable to send message", e );
                } finally {
                    this.readyList.clear();

                    this.nextTransmit = System.nanoTime() + UPDATE_TIMEOUT_NS;
                }
            }

            /* clock has gone weird, time to reset it */
            if (( this.nextTransmit - now ) > ( UPDATE_TIMEOUT_NS * 10 )) {
                logger.warn( "serious clock skew : " + this.nextTransmit + " - " + now );
                for ( InetAddress address : this.addressMap.keySet()) this.addressMap.put( address, 0L );
                this.nextTransmit = now + UPDATE_TIMEOUT_NS;
            } else if (( now > this.nextCleanup ) || 
                       (( this.nextCleanup - now ) > ( 10 * CLEANUP_INTERVAL_NS ))) {
                for ( Iterator<Map.Entry<InetAddress,Long>> iter = this.addressMap.entrySet().iterator() ; 
                      iter.hasNext() ;  ) {
                    Map.Entry<InetAddress,Long> entry = iter.next();
                    InetAddress address = entry.getKey();
                    Long expiration = entry.getValue();
                    /* this should never happen */
                    if ( expiration == null ) continue;
                    if ( now > expiration ) {
                        if ( logger.isDebugEnabled()) {
                            logger.debug( "host <" + address.getHostAddress() + "> expired, removing it" );
                        }

                        iter.remove();
                    }
                }

                nextCleanup = System.nanoTime() + CLEANUP_INTERVAL_NS;
            }
        }

        public boolean handleQueue() throws InterruptedException
        {
            InetAddress address = queue.poll( POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS );
            
            if ( address == null ) return false;
            
            Long expiration = this.addressMap.get( address );

            /* This is a fresh entry, it needs to be sent immediately. */
            if ( expiration == null ) {
                if ( logger.isDebugEnabled()) logger.debug( "new host <" + address.getHostAddress() + ">" );
                this.addressMap.put( address, this.nextTransmit + UPDATE_TIMEOUT_NS );
                this.readyList.add( address.getHostAddress());
                return true;
            } else {
                long now = System.nanoTime();
                /* host hasn't expired yet, nothing to do. */
                if ( now < expiration ) return false;
                
                if ( logger.isDebugEnabled()) logger.debug( "host <" + address.getHostAddress() + "> is ready" );
                this.readyList.add( address.getHostAddress());
                this.addressMap.put( address, this.nextTransmit + UPDATE_TIMEOUT_NS );
            }

            return false;
        }

        public void start()
        {
        }
        
        public void stop()
        {
        }

        private void send() throws JsonClient.ConnectionException, JSONException
        {
            logger.debug( "Adding " + this.readyList.size() + " activeHosts." );

            if ( !isEnabled || this.readyList.isEmpty()) return;

            JSONObject request = new JSONObject();
            request.put( "function", "add_active_hosts" );
            JSONArray hosts = new JSONArray( this.readyList );
            request.put( "hosts", hosts );
            
            String url = System.getProperty( "uvm.arp-eater.url", "http://localhost:3002" );
                
            JSONObject response = JsonClient.getInstance().call( url, request );
            
            if ( logger.isDebugEnabled()) logger.debug( "Server returned:\n" + response.toString() + "\n" );
            int status = response.getInt( RESPONSE_STATUS );
            String message = response.getString( RESPONSE_MESSAGE );
            
            if ( status != STATUS_SUCCESS ) {
                logger.warn( "There was an error[" + status +"] retrieving the logs: '" + message + "'" );
            }
        }
    }
}
