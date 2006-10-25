/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.user;

import org.apache.log4j.Logger;

import java.net.InetAddress;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.wbem.cim.CIMDataType;
import javax.wbem.cim.CIMException;
import javax.wbem.cim.CIMNameSpace;
import javax.wbem.cim.CIMObjectPath;
import javax.wbem.cim.CIMProperty;
import javax.wbem.cim.CIMValue;

import javax.wbem.client.CIMClient;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.ValidateException;

import com.metavize.mvvm.util.WorkerRunner;
import com.metavize.mvvm.util.Worker;

import com.metavize.mvvm.tran.firewall.ip.IPMatcher;
import com.metavize.mvvm.tran.firewall.ip.IPMatcherFactory;

import static com.metavize.mvvm.user.UserInfo.LookupState;

class WMIAssistant implements Assistant
{
    /* this is how long to assume a login is valid for (in millis) */
    private static final long DEFAULT_LIFETIME_MS = 5 * 60 * 1000 ;
    
    /* wait 10 minutes for a negative response */
    private static final long DEFAULT_NEGATIVE_LIFETIME_MS = 10 * 60 * 1000 ;
    
    private static final String DEFAULT_NAMESPACE = "/root/cimv2";
    private static final int DEFAULT_QUEUE_LENGTH = 128;
    
    private static final String WIN32_CLASS_NAME = "Win32_ComputerSystem";
    private static final String WIN32_KEY_NAME = "Name";
    private static final String WIN32_LOGIN_PROP = "UserName";

    private static final CIMDataType CIM_STRING_TYPE = new CIMDataType( CIMDataType.STRING );

    /* do this last, as it is the most expensive */
    private static final int PRIORITY = 1000;

    private final Logger logger = Logger.getLogger( getClass());
    
    /* matches machines that are currently on the private network, this is essentially
     * used to determine which machines can be queried. */

    /* this will have to change */
    private IPMatcher privateNetwork = IPMatcherFactory.getInstance().getInternalMatcher();

    /* cache of the results */
    private final Map<InetAddress,CIMData> cache = new ConcurrentHashMap<InetAddress,CIMData>();

    /* the worker thread that actually performs lookups */
    private final WMIWorker worker;

    private final WorkerRunner runner;


    /* xxx consider keeping the connection open */
    WMIAssistant()
    {
        this( DEFAULT_LIFETIME_MS, DEFAULT_NEGATIVE_LIFETIME_MS );
    }

    WMIAssistant( long lifetimeMillis, long negativeLiftetimeMillis )
    {
        this.worker = new WMIWorker( lifetimeMillis, negativeLiftetimeMillis );
        this.runner = new WorkerRunner( this.worker, MvvmContextFactory.context());
    }

    /* ----------------- Public ----------------- */

    /* the api for lookup guarantees that multiple simultaneous requests will not be made for the same ip */
    public void lookup( UserInfo info )
    {
        /* if someone else is looking up the request, or it is not on the private network, ignore it */
        if (( info.getUsernameState() != LookupState.UNITITIATED ) || !isOnPrivateNetwork( info )) {
            return;
        }

        /* attempt to grab the user info from the local cache */
        if ( updateFromCache( info )) return;

        /* if the connection is unitiated, then perform a full lookup */
         startLookup( info );
    }

    public int priority()
    {
        return PRIORITY;
    }

    /* ---------------- Protected ---------------- */
    WMISettings getSettings()
    {
        return worker.getSettings();
    }

    /* set the WMI settings */
    void setSettings( WMISettings settings ) throws ValidateException
    {
        this.worker.setSettings( settings );

        /* xxx have to write this to a database or something */
        /* clear all of the values from the cache, this gets rid of all of the negative lookups */
        cache.clear();
    }

    void start()
    {
        this.runner.start();
    }

    void stop()
    {
        this.runner.stop();
    }

    /* ----------------- Private ----------------- */

    /* true if the address is able to be queried */
    private boolean isOnPrivateNetwork( UserInfo info )
    {
        return true;
        // XXX return this.privateNetwork.isMatch( info.getAddress());
    }
    
    /* attempt to lookup the response from the cache */
    private boolean updateFromCache( UserInfo info )
    {
        InetAddress address = info.getAddress();
        
        CIMData cimData = cache.get( address );
        if ( cimData == null ) return false;

        /* if the data has already expired, then remove it from the
         * cache and perform a full query */
        if ( cimData.isExpired()) {
            cache.remove( address );
            if ( logger.isDebugEnabled()) {
                logger.debug( "cached value[" +address.getHostAddress() + "]: " + cimData + " expired" );
            }
            return false;
        }
        
        if ( logger.isDebugEnabled()) {
            logger.debug( "cached value[" +address.getHostAddress() + "]: " + cimData + " expired" );
        }
        
        cimData.completeInfo( info );

        return true;
    }

    /* put the lookup onto the queue */
    private void startLookup( UserInfo info )
    {
        InetAddress address = info.getAddress();
        boolean hasHostname = ( info.getHostnameState() == LookupState.UNITITIATED );
        
        info.setUsernameState( LookupState.IN_PROGRESS );
        if ( !hasHostname ) info.setHostnameState( LookupState.IN_PROGRESS );

        if ( !worker.offer( info )) {
            logger.warn( "lookup queue is full, ignoring request" );
            info.setUsernameState( LookupState.FAILED );
            if ( !hasHostname ) info.setHostnameState( LookupState.FAILED );
        } else {
            if ( logger.isDebugEnabled()) logger.debug( "initiated lookup for: " + info );
                
        }
    }

    /* -------------- Inner Classes -------------- */

    /* WMIWorker:
     * independent thread that actually performs the WMI queries */
    private class WMIWorker implements Worker
    {
        /* queue to add more results */
        private final LinkedBlockingQueue<UserInfo> lookupQueue = new LinkedBlockingQueue<UserInfo>( DEFAULT_QUEUE_LENGTH );

        /* positive response timeout */
        private long lifetimeMillis;

        /* negative response length */
        private long negativeLifetimeMillis;

        /* parameters for the wmi server */
        private boolean isValidated = false;
        private WMIInternal settings = null;

        WMIWorker( long lifetimeMillis, long negativeLifetimeMillis )
        {
            this.lifetimeMillis = lifetimeMillis;
            this.negativeLifetimeMillis = negativeLifetimeMillis;
        }
        
        /* Execute one iteration */
        public void work() throws InterruptedException
        {
            /* remove one element */
            UserInfo info = this.lookupQueue.take();
            
            /* first check the queue */
            if ( updateFromCache( info )) return;
            
            if ( !getHostname( info )) {
                /* unable to lookup the hostname */
                fail( info );
                return;
            }
            
            try {
                lookup( info );
            } catch ( CIMException e ) {
                /* this can actually happen for a number of reasons */
                logger.info( "exception looking up user information", e );
                fail( info );
            }
        }

        boolean offer( UserInfo info )
        {
            return lookupQueue.offer( info );
        }

        WMISettings getSettings()
        {
            if ( settings == null ) return new WMISettings();
            return settings.toSettings();
        }

        void setSettings( WMISettings settings ) throws ValidateException
        {
            this.settings = WMIInternal.makeInternal( settings );
        }

               
        private boolean getHostname( UserInfo info )
        {
            InetAddress address = info.getAddress();
            HostName hostname = info.getHostname();
            
            if ( hostname == null ) {
                /* perform a reverse lookup */
                String h = address.getHostName();
                /* if it returned the ip textual version, there is nothing to be done */
                if ( h.equals( address.getHostAddress())) return false;
                
                try {                          
                    hostname = HostName.parse( h );
                } catch ( ParseException e ) {
                    logger.warn( "unable to parse the hostname: '" + h + "'" );
                    return false;
                }
                
                /* only update the info if it has changed */
                info.setHostname( hostname );
            }
            
            return true;
        }
        
        private void lookup( UserInfo info ) throws CIMException
        {
            /* no need for locking */
            WMIInternal cs = this.settings;

            /* no server or the server is not validated, then fail */
            if (( cs == null ) || ( !cs.getIsEnabled())) {
                fail( info );
                return;
            }

            CIMClient clientConnection = null;

            try {                
                InetAddress address = info.getAddress();
                
                /* assuming the hostname is valid and is not an textual ip address */
                HostName hostname = info.getHostname();
                
                String machineName = hostname.unqualified().toString();
                
                String ns = DEFAULT_NAMESPACE;
                
                IPaddr wmiServer = cs.getAddress();
            
                /* If the machine is not the WMI server, than have to prepend the machine to the name space */
                if ( !wmiServer.toString().equals( address.getHostAddress())) ns = "/" + machineName + ns;
                
                CIMNameSpace nameSpace = new CIMNameSpace( cs.getURI(), ns );
                
                if ( logger.isDebugEnabled()) {
                    logger.debug( "namespace: " + nameSpace + " uri: " + cs.getURI());
                }
                
                clientConnection = new CIMClient( nameSpace, cs.getPrincipal(), cs.getCredentials());
                
                /* create a new CIM object path */
                CIMObjectPath objectPath = new CIMObjectPath( WIN32_CLASS_NAME );
            
                /* add a key to isolate just the machine */
                CIMProperty property = 
                    new CIMProperty( WIN32_KEY_NAME, new CIMValue( machineName, CIM_STRING_TYPE ));
                                     
                objectPath.addKey( property );
                
                CIMValue value = clientConnection.getProperty( objectPath, WIN32_LOGIN_PROP );

                if ( logger.isDebugEnabled()) {
                    logger.debug( "completed lookup for[" + address.getHostAddress() + "]: " + value );
                }

                /* should this append the domain? */
                pass( info, value );
            } catch ( WMIException e ) {
                logger.warn( "wmi settings are enabled, yet uri is invalid", e );
                fail( info );
            } catch ( ParseException e ) {
                logger.info( "WMI returned an unparseable username.", e );
                fail( info );
            } finally {
                if ( clientConnection != null ) clientConnection.close();
            }
        }
        
        private void fail( UserInfo info )
        {
            CIMData d = 
                new FailedCIMData( new Date( System.currentTimeMillis() + this.negativeLifetimeMillis ));

            cache.put( info.getAddress(), d );
            
            d.completeInfo( info );
        }
        
        private void pass( UserInfo info, CIMValue value ) throws ParseException
        {
            /* not sure why it needs 4 per one \ but it does? */
            String[] components = value.toString().replaceAll( "\"", "" ).split( "\\\\\\\\" );

            Username username = Username.parse(( components.length == 1 ) ? components[0] : components[1] );

            InetAddress address = info.getAddress();
            
            CIMData d = 
                new SuccessfulCIMData( address, username, info.getHostname(), 
                                       new Date( System.currentTimeMillis() + this.lifetimeMillis ));

            cache.put( info.getAddress(), d );
            
            d.completeInfo( info );
        }
    }

    /* CIMData: interface to keep track of the status of a WMI query. */
    private static interface CIMData
    {
        public void completeInfo( UserInfo info );
        
        public boolean isExpired();
    }
    
    /* CIMData: representation of a failed CMI request, this is used to cache the fact
     * that the username should not be looked up again for a long time.  */
    private static class FailedCIMData implements CIMData
    {
        private final Date expirationDate;

        FailedCIMData( Date expirationDate )
        {
            this.expirationDate = expirationDate;
        }

        public boolean isExpired()
        {
            return this.expirationDate.after( new Date());
        }

        public void completeInfo( UserInfo info )
        {
            /* indicate that the lookup failed */
            info.setUsernameState( LookupState.FAILED );

            /* only indicate that the hostname lookup failed if it was pending */
            if ( info.getHostnameState() != LookupState.COMPLETED ) {
                info.setHostnameState( LookupState.FAILED );
            }
        }
    }

    /* CIMData: representation of a successful CMI request. */
    private static class SuccessfulCIMData implements CIMData
    {
        private final InetAddress address;
        private final Username username;
        private final HostName hostname;
        private final Date expirationDate;

        /* in order to perform a CIM query, you have to lookup the
         * hostname, windows doesn't work on ip addresses. */
        SuccessfulCIMData( InetAddress address, Username username, HostName hostname, Date expirationDate )
        {
            this.address = address;
            this.username = username;
            this.hostname = hostname;
            this.expirationDate = expirationDate;
        }

        public void completeInfo( UserInfo info )
        {
            if ( info.getHostnameState() != LookupState.COMPLETED ) info.setHostname( this.hostname );
            
            info.setUsername( this.username );
        }

        public boolean isExpired()
        {
            return this.expirationDate.after( new Date());
        }
    }      
}