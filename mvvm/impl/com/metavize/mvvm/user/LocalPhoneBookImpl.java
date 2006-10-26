/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.user;

import java.net.InetAddress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.ValidateException;
import com.metavize.tran.util.MVLogger;

import static com.metavize.mvvm.user.UserInfo.LookupState;

public class LocalPhoneBookImpl implements LocalPhoneBook
{
    /* properties */
    private static final String PROPERTY_LIFETIME = "com.metavize.mvvm.user.phonebook.lifetime";
    
    /* Singleton */
    private static final LocalPhoneBookImpl INSTANCE = new LocalPhoneBookImpl();
    
    private final MVLogger logger = new MVLogger( getClass());

    private long lifetimeMillis = UserInfo.DEFAULT_LIFETIME_MILLIS;

    /* This is a list of all of the assistants */
    private List<Assistant> assistantList = new LinkedList<Assistant>();
    private final Object assistantListLock = new Object();

    /* not sure if both lookup methods are necessary */
    /* ??? how exactly do we expire the entries in here ??? */
    private final Map<InetAddress,UserInfo> addressMap = new ConcurrentHashMap<InetAddress,UserInfo>();

    /* XXX not sure how to update this between startups, perhaps the first 32 bits are random */
    private long currentKey = 0;

    private final WMIAssistant wmiAssistant;

    private LocalPhoneBookImpl()
    {
        /* have to build the WMI assistant */
        this.wmiAssistant = new WMIAssistant();
    }

    /* ----------------- Public ----------------- */

    /* retrieve the WMI settings */
    public WMISettings getWMISettings()
    {
        return this.wmiAssistant.getSettings();
    }

    /* set the WMI settings */
    public void setWMISettings( WMISettings settings ) throws ValidateException
    {
        this.wmiAssistant.setSettings( settings );
    }

    /* Lookup the corresponding user user information object user the address */
    public UserInfo lookup( InetAddress address )
    {
        UserInfo info = addressMap.get( address );

        if ( info == null || info.isExpired()) info = executeLookup( address );
        else  logger.debug( "cached info: ", info, ";" );

        return info;
    }

    public void updateEntry( UserInfo info )
    {
        throw new IllegalStateException( "unimplemented" );
    }

    /* Register a phone book assistant which is used to help with addres lookups */
    public void registerAssistant( Assistant newAssistant )
    {
        synchronized ( assistantListLock ) {
            /* Wasteful, but this doesn't happen very often */
            List<Assistant> newList = new LinkedList<Assistant>( this.assistantList );
            
            int priority = newAssistant.priority();
            
            /* This is a silly method, but it is simple and it works */
            int c = 0;
            
            for ( Assistant assistant : newList ) {
                /* This checks if the assistant is already registered */
                if ( priority == assistant.priority()) {
                    if ( assistant.equals( newAssistant )) {
                        logger.debug( "The assistant: ", assistant, " is already registered" );
                        return;
                    }
                    
                    if (( assistant instanceof TransformAssistant ) && 
                        ((TransformAssistant)assistant).getAssistant().equals( newAssistant )) {
                        logger.debug( "The trnasform assistant: ", assistant, " is already registered" );
                        return;
                    }
                }
                
                if ( priority < assistant.priority()) break;
                c++;
            }
            
            /* if necessary convert to a transform assistant */
            newAssistant = TransformAssistant.fixTransformContext( newAssistant );

            logger.debug( "registering the new assistant: ", newAssistant );
            newList.add( c, newAssistant );
            
            /* replace the entire list, anything iterating will be just use the old list */
            this.assistantList = Collections.unmodifiableList( newList );
        }
    }

    /* Unregister a phone book assistant */
    public void unregisterAssistant( Assistant assistant )
    {
        synchronized ( assistantListLock ) {
            List<Assistant> newList = new LinkedList<Assistant>( this.assistantList );
            
            int priority = assistant.priority();
            
            /* This is a silly method, but it is simple and it works */
            for ( Iterator<Assistant> iter = newList.iterator() ; iter.hasNext() ; ) {
                /* This checks if the assistant is already registered */
                Assistant a = iter.next();

                if ( priority == a.priority()) {
                    if ( a.equals( assistant )) iter.remove();
                    else if (( assistant instanceof TransformAssistant ) && 
                             ((TransformAssistant)a).getAssistant().equals( assistant )) {
                        iter.remove();
                    }
                }
                
                if ( priority < assistant.priority()) break;
            }
            
            /* Only true if an object hasn't been removed */
            if ( this.assistantList.size() == newList.size()) return;
        
            /* replace the entire list, anything iterating will be just use the old list */
            this.assistantList = Collections.unmodifiableList( newList );        
        }
    }
    
    public void init()
    {
        /* Check for a property overriding the timeout. */
        this.lifetimeMillis = Long.getLong( PROPERTY_LIFETIME, this.lifetimeMillis );

        logger.debug( "lifetime: ", this.lifetimeMillis );
        
        /* initialize the wmi assistant */
        this.wmiAssistant.init();

        /* register the WMI assistant */
        registerAssistant( this.wmiAssistant );

        /* Start the wmi asssitant lookup thread */
        this.wmiAssistant.start();
    }

    public void destroy()
    {
        /* Start the wmi asssitant lookup thread */
        this.wmiAssistant.stop();
    }

    /* XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX testing code */
    private static final String DEFAULT_SCHEME = "http";
    private static final int DEFAULT_PORT = 5988;
    
    public void wmi( String args[] ) throws Exception
    {
        String username = null;
        String password = null;
        String host = null;
        String scheme = DEFAULT_SCHEME;
        int port = DEFAULT_PORT;

        logger.debug( "current settings: ", getWMISettings());

        int length = args.length;
        if (( length & 1 ) != 1 ) {
            throw new IllegalArgumentException( "requires an even number of arguments" );
        }

        int c = 1; 

        for ( ; c < length ; c++ ) {
            String flag = args[c];
            if ( !flag.startsWith( "-" ) || flag.length() != 2 ) {
                throw new IllegalArgumentException( "no dash" );
            }
            
            switch ( flag.charAt( 1 )) {
            case 'f':
                Properties properties = new Properties();
                File f = new File( args[++c] );
                try {
                    properties.load( new FileInputStream( f ));
                } catch ( FileNotFoundException e ) {
                    throw new IllegalArgumentException( "Property file: '" + args[c] + "' does not exist" );
                } catch ( IOException e ) {
                    throw new IllegalArgumentException( "Unable to parse: '" + args[c] + "'." );
                }
                username = parseProperty( properties, "com.metavize.mvvm.wmiassistant.username", username );
                password = parseProperty( properties, "com.metavize.mvvm.wmiassistant.password", password );
                host     = parseProperty( properties, "com.metavize.mvvm.wmiassistant.hostname", host );
                scheme   = parseProperty( properties, "com.metavize.mvvm.wmiassistant.scheme", scheme );
                port     = parseProperty( properties, "com.metavize.mvvm.wmiassistant.port", port );
                break;

            case 'u':
                username = args[++c].trim();
                break;

            case 'p':
                password = args[++c].trim();
                break;

            case 'h':
                host = args[++c].trim();
                break;

            case 's':
                scheme = args[++c].trim().toLowerCase();
                break;
                
            case 'r':
                port = Integer.parseInt( args[++c].trim());
                break;
                
            default:
                throw new IllegalArgumentException( "Invalid flag: '" + args[c] + "'" );
            }
        }

        WMISettings  settings = new WMISettings();
        settings.setIsEnabled( true );
        settings.setUsername( username );
        settings.setPassword( password );
        settings.setScheme( scheme );
        settings.setAddress( IPaddr.parse( host ));
        settings.setPort( port );
        
        setWMISettings( settings );
        
        logger.debug( "new settings: ", getWMISettings());
        // return new GetUserInfo( username, password, client, host, scheme, port );
    }

    private int parseProperty( Properties p, String name, int currentValue )
    {
        String v = parseProperty( p, name, String.valueOf( currentValue ));
        return Integer.parseInt( v );
    }

    private String parseProperty( Properties p, String name, String currentValue )
    {
        String value = p.getProperty( name );
        
        if ( value == null ) return currentValue;

        value = value.trim();

        if ( value.length() == 0 ) return currentValue;

        return value;
    }
    /* XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX testing code */

    
    /* -------------- Public Static -------------- */
    public static LocalPhoneBookImpl getInstance()
    {
        return INSTANCE;
    }

    /* ----------------- Package ----------------- */
    /* mainly used for testing and shutdown */
    void clearAssistants()
    {
        synchronized ( assistantListLock ) {
            this.assistantList = Collections.unmodifiableList( new LinkedList<Assistant>());
        }
    }

    /* ----------------- Private ----------------- */
    private UserInfo executeLookup( InetAddress address )
    {
        UserInfo info = null;
        /* Designed this way so if multiple lookups are pending on an
         * address, only the first one will initiate a complete
         * lookup */
        synchronized ( address.getHostAddress().intern()) {
            logger.debug( "checking cache again for ", address.getHostAddress(), "." );

            /* Attempt to lookup again now that this has the lock */
            info = addressMap.get( address );

            if ( info != null ) {
                if ( !info.isExpired()) return info;
                
                logger.debug( "expiring the key: ", info );
                addressMap.remove( address );
                /* xxxxx log the info */
            } 

            /* Create a new user info object */
            info = UserInfo.makeInstance( getNextKey(), address, this.lifetimeMillis );
            logger.debug( "making a new instance for: ", info, "." );
            addressMap.put( address, info );
        }

        List<Assistant> currentAssistantList = this.assistantList;

        /* new cached info, time for a full lookup. */
        for ( Assistant assistant : currentAssistantList ) {
            try {
                assistant.lookup( info );
            } catch ( Exception e ){
                logger.warn( "the assistant " + assistant + "threw an exception.", e );
            }

            /* stop once something has initiated a lookup for the hostname and the username */
            if ( info.getHostnameState() != LookupState.UNITITIATED && 
                 info.getUsernameState() != LookupState.UNITITIATED ) {
                break;
            }
        }

        return info;
    }

    private synchronized long getNextKey()
    {
        return ++currentKey;
    }
}