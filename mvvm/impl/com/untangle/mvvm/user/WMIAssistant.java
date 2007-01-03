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

package com.untangle.mvvm.user;

import java.net.InetAddress;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.wbem.cim.CIMDataType;
import javax.wbem.cim.CIMException;
import javax.wbem.cim.CIMNameSpace;
import javax.wbem.cim.CIMObjectPath;
import javax.wbem.cim.CIMProperty;
import javax.wbem.cim.CIMValue;
import javax.wbem.client.CIMClient;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.ValidateException;
import com.untangle.mvvm.tran.firewall.ip.IPMatcher;
import com.untangle.mvvm.tran.firewall.ip.IPMatcherFactory;
import com.untangle.mvvm.util.DataLoader;
import com.untangle.mvvm.util.DataSaver;
import com.untangle.mvvm.util.Worker;
import com.untangle.mvvm.util.WorkerRunner;
import com.untangle.tran.util.MVLogger;
import org.hibernate.Query;
import org.hibernate.Session;

import static com.untangle.mvvm.user.UserInfo.LookupState;

class WMIAssistant implements Assistant
{
    /* This is the maximum time to wait to connect to the server */
    private static final String PROPERTY_CONNECT_TIMEOUT = "javax.wbem.client.adapter.http.transport.timeout-connect";

    /* This is the maxiumum amount of time to wait for a read from the server */
    private static final String PROPERTY_READ_TIMEOUT = "javax.wbem.client.adapter.http.transport.timeout-read";

    /* default connection timeout to connect to server(millis) */
    private static final int DEFAULT_CONNECT_TIMEOUT = 4000;

    /* default connection timeout to read from the server  */
    private static final int DEFAULT_READ_TIMEOUT = 4000;

    /* this is the property for the success timeout */
    private static final String PROPERTY_LIFETIME = "com.untangle.mvvm.user.wmi.lifetime";

    /* this is the property for the failed timeout */
    private static final String PROPERTY_NEGATIVE_LIFETIME = "com.untangle.mvvm.user.wmi.negativelifetime";

    /* this is how long to assume a login is valid for (in millis) */
    private static final long DEFAULT_LIFETIME_MS = 5 * 60 * 1000 ;

    /* wait 10 minutes for a negative response */
    private static final long DEFAULT_NEGATIVE_LIFETIME_MS = 10 * 60 * 1000 ;

    /* Maximum delay to wait for a lookup event (1 minute, then cleanup) */
    private static final long POLL_TIMEOUT = 60000;

    private static final String DEFAULT_NAMESPACE = "/root/cimv2";
    private static final int DEFAULT_QUEUE_LENGTH = 128;

    private static final String WIN32_CLASS_NAME = "Win32_ComputerSystem";
    private static final String WIN32_KEY_NAME = "Name";
    private static final String WIN32_LOGIN_PROP = "UserName";

    private static final CIMDataType CIM_STRING_TYPE = new CIMDataType( CIMDataType.STRING );

    /* do this last, as it is the most expensive */
    private static final int PRIORITY = 1000;

    private final MVLogger logger = new MVLogger( getClass());

    /* matches machines that are currently on the private network, this is essentially
     * used to determine which machines can be queried. */
    /* this may have to change */
    private IPMatcher privateNetwork = IPMatcherFactory.getInstance().getInternalMatcher();

    /* cache of the results */
    private final Map<InetAddress,CIMData> cache = new ConcurrentHashMap<InetAddress,CIMData>();

    /* the worker thread that actually performs lookups */
    private final WMIWorker worker;

    /* the runner that polls for WMI lookup requests */
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
        if (( info.getUsernameState() != LookupState.UNITITIATED ) || !isOnPrivateNetwork( info )) return;

        /* settings are disabled, nothing to do */
        if ( !this.worker.settings.getIsEnabled()) return;

        /* attempt to grab the user info from the local cache */
        if ( updateFromCache( info )) return;

        /* if the connection is unitiated, then perform a full lookup */
         startLookup( info );
    }

    public int priority()
    {
        return PRIORITY;
    }

    /* ----------------- Package ----------------- */
    WMISettings getSettings()
    {
        return worker.getSettings();
    }

    /* set the WMI settings */
    void setSettings( WMISettings settings ) throws ValidateException
    {
        WMIInternal internal = WMIInternal.makeInternal( settings );

        saveSettings( settings );

        this.worker.setSettings( internal );

        /* clear all of the values from the cache, this gets rid of all of the negative lookups */
        cache.clear();
    }

    /* load the property values */
    void init()
    {
        this.worker.lifetimeMillis = Long.getLong( PROPERTY_LIFETIME, this.worker.lifetimeMillis );
        this.worker.negativeLifetimeMillis =
            Long.getLong( PROPERTY_LIFETIME, this.worker.negativeLifetimeMillis );

        /* set the system properties for connection and read timeouts on wbem connections */
        if ( null == System.getProperty( PROPERTY_CONNECT_TIMEOUT )) {
            System.setProperty( PROPERTY_CONNECT_TIMEOUT, String.valueOf( DEFAULT_CONNECT_TIMEOUT ));
        }

        if ( null == System.getProperty( PROPERTY_READ_TIMEOUT )) {
            System.setProperty( PROPERTY_READ_TIMEOUT, String.valueOf( DEFAULT_READ_TIMEOUT ));
        }

        logger.debug( "lifetime: ", this.worker.lifetimeMillis,
                      " negative-lifetime: ", this.worker.negativeLifetimeMillis );

        /* load the settings from the database */
        WMISettings settings = loadSettings();

        try {
            if ( settings == null ) {
                logger.debug( "No settings exists, attempting to save new settings" );
                settings = new WMISettings();
                settings.setIsEnabled( false );
                setSettings( settings );
            } else {
                /* configure the worker with the existing settings */
                this.worker.setSettings( WMIInternal.makeInternal( settings ));
            }
        } catch ( ValidateException e ) {
            logger.warn( e, "unable to load initial settings: ", settings );
        }
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
        return this.privateNetwork.isMatch( info.getAddress());
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
            logger.debug( "cached value [", address.getHostAddress(), "]: ", cimData, " expired" );
            return false;
        }

        logger.debug( "found valid cached value [", address.getHostAddress(), "]: ", cimData );

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
            logger.debug( "initiated lookup for: ", info );

        }
    }

    /* write the settings to the database */
    private void saveSettings( WMISettings settings )
    {
        DataSaver<WMISettings> saver = new WMISettingsDataSaver( MvvmContextFactory.context());
        saver.saveData( settings );
    }

    private WMISettings loadSettings()
    {
        DataLoader<WMISettings> loader =
            new DataLoader<WMISettings>( "WMISettings", MvvmContextFactory.context());

        return loader.loadData();
    }

    /* -------------- Inner Classes -------------- */

    /* WMIWorker:
     * independent thread that actually performs the WMI queries */
    private class WMIWorker implements Worker
    {
        /* queue to add more results */
        private final LinkedBlockingQueue<UserInfo> lookupQueue = new LinkedBlockingQueue<UserInfo>( DEFAULT_QUEUE_LENGTH );

        /* ordered list of the CIM data, used to expire requests */
        private final List<CIMData> expireList = new LinkedList<CIMData>();

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
            UserInfo info = this.lookupQueue.poll( POLL_TIMEOUT, TimeUnit.MILLISECONDS );

            expireValues();

            if ( info == null ) return;

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
                logger.info( e, "exception looking up user information" );
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

        void setSettings( WMIInternal internal ) throws ValidateException
        {
            this.settings = internal;
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

                logger.debug( "namespace: ", nameSpace, " uri: ", cs.getURI());

                clientConnection = new CIMClient( nameSpace, cs.getPrincipal(), cs.getCredentials());

                /* create a new CIM object path */
                CIMObjectPath objectPath = new CIMObjectPath( WIN32_CLASS_NAME );

                /* add a key to isolate just the machine */
                CIMProperty property =
                    new CIMProperty( WIN32_KEY_NAME, new CIMValue( machineName, CIM_STRING_TYPE ));

                objectPath.addKey( property );

                CIMValue value = clientConnection.getProperty( objectPath, WIN32_LOGIN_PROP );

                /* fail if the value is null */
                if ( value == null ) {
                    fail( info );
                } else {
                    logger.debug( "completed lookup for[", address.getHostAddress(), "]: ", value );
                    
                    /* should this append the domain? */
                    pass( info, value );
                }
            } catch ( WMIException e ) {
                logger.warn( e, "wmi settings are enabled, yet uri is invalid" );
                fail( info );
            } catch ( ParseException e ) {
                logger.info( e, "WMI returned an unparseable username." );
                fail( info );
            } finally {
                if ( clientConnection != null ) clientConnection.close();
            }
        }

        private void fail( UserInfo info )
        {
            HostName h = info.getHostname();
            InetAddress address = info.getAddress();
            long endTime = System.currentTimeMillis() + this.negativeLifetimeMillis;
            CIMData d = new FailedCIMData( endTime, address, h );

            cache.put( address, d );
            this.expireList.add( d );
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
                                       System.currentTimeMillis() + this.lifetimeMillis );

            cache.put( info.getAddress(), d );
            this.expireList.add( d );
            d.completeInfo( info );
        }

        /* no synchronization necessary since all values are added from this thread */
        private void expireValues()
        {

            /* optimization */
            if ( this.expireList.size() == 0 || !this.expireList.get( 0 ).isExpired() ) return;

            logger.debug( "checking for expired cim data." );

            for ( Iterator<CIMData> iter = this.expireList.iterator() ; iter.hasNext() ; ) {
                CIMData cimData = iter.next();

                if ( !cimData.isExpired()) break;

                iter.remove();

                InetAddress address = cimData.getAddress();

                cimData = cache.get( address );

                if ( cimData == null || !cimData.isExpired()) continue;

                /* value is expired, time to remove it */
                logger.debug( "cleaning expired value in cache: ", cimData );
                cache.remove( address );
            }
        }
    }

    /* CIMData: interface to keep track of the status of a WMI query. */
    private static abstract class  CIMData
    {
        private final long expirationDate;
        private final InetAddress address;

        CIMData( long expirationDate, InetAddress address )
        {
            this.expirationDate = expirationDate;
            this.address = address;
        }

        abstract void completeInfo( UserInfo info );

        boolean isExpired()
        {
            return ( System.currentTimeMillis() > expirationDate );
        }

        InetAddress getAddress()
        {
            return this.address;
        }
    }

    /* CIMData: representation of a failed CMI request, this is used to cache the fact
     * that the username should not be looked up again for a long time.  */
    private static class FailedCIMData extends CIMData
    {
        private final HostName hostname;

        FailedCIMData( long expirationDate, InetAddress address )
        {
            this( expirationDate, address, null );
        }

        FailedCIMData( long expirationDate, InetAddress address, HostName hostname )
        {
            super( expirationDate, address );
            this.hostname = hostname;
        }

        public String toString()
        {
            return "<FailedCIMData>";
        }

        void completeInfo( UserInfo info )
        {
            /* indicate that the lookup failed */
            info.setUsernameState( LookupState.FAILED );

            /* only indicate that the hostname lookup failed if it was pending */
            if ( info.getHostnameState() != LookupState.COMPLETED ) {
                if ( this.hostname != null ) info.setHostname( this.hostname );
                else info.setHostnameState( LookupState.FAILED );
            }
        }
    }

    /* CIMData: representation of a successful CMI request. */
    private static class SuccessfulCIMData extends CIMData
    {
        private final Username username;
        private final HostName hostname;

        /* in order to perform a CIM query, you have to lookup the
         * hostname, windows doesn't work on ip addresses. */
        SuccessfulCIMData( InetAddress address, Username username, HostName hostname, long expirationDate )
        {
            super( expirationDate, address );
            this.username = username;
            this.hostname = hostname;
        }

        public void completeInfo( UserInfo info )
        {
            if ( info.getHostnameState() != LookupState.COMPLETED ) info.setHostname( this.hostname );

            info.setUsername( this.username );
        }

        public String toString()
        {
            return "<SuccessfulCIMData: " + getAddress().getHostAddress() + "/" + username + ">";
        }
    }

    class WMISettingsDataSaver extends DataSaver<WMISettings>
    {
        public WMISettingsDataSaver( MvvmLocalContext local )
        {
            super( local );
        }

        protected void preSave( Session s )
        {
            Query q = s.createQuery( "from WMISettings" );
            for ( Iterator iter = q.iterate() ; iter.hasNext() ; ) {
                WMISettings settings = (WMISettings)iter.next();
                s.delete( settings );
            }
        }
    }

}