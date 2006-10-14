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

import java.net.InetAddress;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

public class PhoneBookImpl implements PhoneBook
{
    /* Singleton */
    private static final PhoneBookImpl INSTANCE = new PhoneBookImpl();
    
    private final Logger logger = Logger.getLogger( getClass());

    /* This is a list of all of the assistants */
    private List<Assistant> assistantList = new LinkedList<Assistant>();
    private final Object assistantListLock = new Object();

    /* not sure if both lookup methods are necessary */
    /* ??? how exactly do we expire the entries in here ??? */
    private final Map<InetAddress,UserInfo> addressMap = new HashMap<InetAddress,UserInfo>();
    
    private final Map<Long,UserInfo> keyMap = new HashMap<Long,UserInfo>();   

    /* XXX not sure how to update this between startups, perhaps the first 32 bits are random */
    private long currentKey = 0;

    private PhoneBookImpl()
    {
    }

    /* ----------------- Public ----------------- */

    /* Lookup the corresponding user user information object user the address */
    public UserInfo lookup( InetAddress address )
    {
        UserInfo info = addressMap.get( address );

        if ( info == null ) info = executeLookup( address );

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
                if ( priority == assistant.priority() && assistant.equals( newAssistant )) {
                    logger.debug( "The assistant: " + assistant + " is already registered" );
                    return;
                }
                
                if ( priority < assistant.priority()) break;
                c++;
            }
            
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

                if ( priority == a.priority() && a.equals( assistant )) iter.remove();
                
                if ( priority < assistant.priority()) break;
            }
            
            /* Only true if an object hasn't been removed */
            if ( this.assistantList.size() == newList.size()) return;
        
            /* replace the entire list, anything iterating will be just use the old list */
            this.assistantList = Collections.unmodifiableList( newList );        
        }
    }
    
    /* -------------- Public Static -------------- */
    public static PhoneBookImpl getInstance()
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
        synchronized ( this ) {
            /* Attempt to lookup again now that this has the lock */
            info = addressMap.get( address );

            if ( info != null ) return info;

            /* Create a new user info object */
            info = UserInfo.makeInstance( getNextKey(), address );
            addressMap.put( address, info );
            keyMap.put( info.getUserLookupKey(), info );
        }

        List<Assistant> currentAssistantList = this.assistantList;
        for ( Assistant assistant : currentAssistantList ) assistant.lookup( info );

        return info;
    }

    private synchronized long getNextKey()
    {
        return ++currentKey;
    }
}