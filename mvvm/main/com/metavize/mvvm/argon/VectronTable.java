/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VectronTable.java,v 1.3 2005/02/10 00:44:54 rbscott Exp $
 */

package com.metavize.mvvm.argon;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.metavize.jvector.Vector;

public class VectronTable
{
    /* Debugging */
    private static final Logger logger = Logger.getLogger( VectronTable.class );
    private static final VectronTable INSTANCE = new VectronTable();

    private final Map<Vector,IPSessionDesc> activeVectrons = new HashMap<Vector,IPSessionDesc>();
    private boolean isAlive = true;

    /* Singleton */
    private VectronTable()
    {
    }

    /** 
     * Add a vectron to the hash set.
     * @param  vectron - The vectron to add.
     * @return - True if the item did not already exist 
     */
    public synchronized boolean put( Vector vectron, IPSessionDesc session )
    {
        return ( activeVectrons.put( vectron, session ) == null ) ? true : false;
    }

    /**
     * Remove a vectron from the hash set.
     * @param  vectron - The vectron to remove.
     * @return - True if the item was removed, false if it wasn't in the set.
     */
    public synchronized boolean remove( Vector vectron )
    {
        return ( activeVectrons.remove( vectron ) == null ) ? false : true;
    }

    /**
     * Get the number of vectrons remaining
     */
    public synchronized int count()
    {
        return activeVectrons.size();
    }

    /**
     * This kills all active vectors, since this is synchronized, it pauses the creation
     * of new vectoring machines, but it doesn't prevent the creating of new vectoring
     * machines 
     * @return - Returns false if there are no active sessions. */
    public synchronized boolean shutdownActive()
    {
        if ( activeVectrons.isEmpty()) return false;

        for ( Iterator<Vector> iter = (Iterator<Vector>)activeVectrons.keySet().iterator(); 
              iter.hasNext() ; ) {
            Vector vectron = iter.next();            
            vectron.shutdown();
            /* Don't actually remove the item, it is removed when the session exits */
        }

        return true;
    }    

    public synchronized void shutdownMatches( SessionMatcher matcher )
    {
        boolean isDebugEnabled = logger.isDebugEnabled();

        logger.debug( "Shutting down matching sessions with: matcher " + matcher );
        
        if ( activeVectrons.isEmpty()) return;

        /* This matcher doesn't match anything */
        if ( matcher == SessionMatcherFactory.NULL_MATCHER ) {
            logger.debug( "NULL Session matcher" );
            return;
        } else if ( matcher == SessionMatcherFactory.ALL_MATCHER ) {
            logger.debug( "ALL Session matcher" );
            
            /* Just clear all, without checking for matches */
            for ( Iterator<Vector> iter = (Iterator<Vector>)activeVectrons.keySet().iterator() ; 
                  iter.hasNext() ; ) {
                Vector vectron = iter.next();
                vectron.shutdown();
            }
            return;
        }
        
        /* XXXX THIS IS INCREDIBLY INEFFICIENT AND LOCKS THE CREATION OF NEW SESSIONS */
        for ( Iterator iter = activeVectrons.entrySet().iterator() ; iter.hasNext() ; ) {
            Map.Entry<Vector,IPSessionDesc> e = (Map.Entry<Vector,IPSessionDesc>)iter.next();
            boolean isMatch;

            IPSessionDesc session = e.getValue();
            Vector vectron  = e.getKey();

            isMatch = matcher.isMatch( session );

            if ( isDebugEnabled )
                logger.debug( "Tested session: " + session + " id: " + session.id() + 
                              " matched: " + isMatch );

            if ( isMatch )
                vectron.shutdown();
        }
    }

    public static VectronTable getInstance()
    {
        return INSTANCE;
    }
}
