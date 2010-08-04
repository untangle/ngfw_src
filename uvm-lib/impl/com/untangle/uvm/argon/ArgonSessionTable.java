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

package com.untangle.uvm.argon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.jvector.Vector;
import com.untangle.uvm.localapi.SessionMatcher;
import com.untangle.uvm.localapi.SessionMatcherFactory;

/**
 * This table stores a global list of all currently active sessions being vectored
 */
class ArgonSessionTable
{
    /* Debugging */
    private final Logger logger = Logger.getLogger(getClass());
    private static final ArgonSessionTable INSTANCE = new ArgonSessionTable();

    private final Map<Vector,SessionGlobalState> activeSessions = new HashMap<Vector,SessionGlobalState>();

    /* Singleton */
    private ArgonSessionTable() {}

    /**
     * Add a vectron to the hash set.
     * @param  vectron - The vectron to add.
     * @return - True if the item did not already exist
     */
    synchronized boolean put( Vector vectron, SessionGlobalState session )
    {
        return ( activeSessions.put( vectron, session ) == null ) ? true : false;
    }

    /**
     * Remove a vectron from the hash set.
     * @param  vectron - The vectron to remove.
     * @return - True if the item was removed, false if it wasn't in the set.
     */
    synchronized boolean remove( Vector vectron )
    {
        return ( activeSessions.remove( vectron ) == null ) ? false : true;
    }

    /**
     * Get the number of vectrons remaining
     */
    synchronized int count()
    {
        return activeSessions.size();
    }

    /**
     * This kills all active vectors, since this is synchronized, it pauses the creation
     * of new vectoring machines, but it doesn't prevent the creating of new vectoring
     * machines
     * @return - Returns false if there are no active sessions. */
    synchronized boolean shutdownActive()
    {
        if ( activeSessions.isEmpty()) return false;

        for ( Iterator<Vector> iter = activeSessions.keySet().iterator();
              iter.hasNext() ; ) {
            Vector vectron = iter.next();
            vectron.shutdown();
            /* Don't actually remove the item, it is removed when the session exits */
        }

        return true;
    }

    synchronized void shutdownMatches( SessionMatcher matcher )
    {
        boolean isDebugEnabled = logger.isDebugEnabled();

        logger.debug( "Shutting down matching sessions with: matcher " + matcher );

        if ( activeSessions.isEmpty()) return;

        /* This matcher doesn't match anything */
        if ( matcher == SessionMatcherFactory.getNullInstance()) {
            logger.debug( "NULL Session matcher" );
            return;
        } else if ( matcher == SessionMatcherFactory.getAllInstance()) {
            logger.debug( "ALL Session matcher" );

            /* Just clear all, without checking for matches */
            for ( Iterator<Vector> iter = activeSessions.keySet().iterator() ;
                  iter.hasNext() ; ) {
                Vector vectron = iter.next();
                vectron.shutdown();
            }
            return;
        }

        /* XXXX THIS IS INCREDIBLY INEFFICIENT AND LOCKS THE CREATION OF NEW SESSIONS */
        for ( Iterator<Map.Entry<Vector,SessionGlobalState>> iter = activeSessions.entrySet().iterator() ; iter.hasNext() ; ) {
            Map.Entry<Vector,SessionGlobalState> e = iter.next();
            boolean isMatch;

            SessionGlobalState session = e.getValue();
            Vector vectron  = e.getKey();

            ArgonHook argonHook = session.argonHook();

            isMatch = matcher.isMatch( argonHook.policy, argonHook.clientSide, argonHook.serverSide );

            if ( isDebugEnabled )
                logger.debug( "Tested session: " + session + " id: " + session.id() +
                              " matched: " + isMatch );

            if ( isMatch )
                vectron.shutdown();
        }
    }

    static ArgonSessionTable getInstance()
    {
        return INSTANCE;
    }
}
