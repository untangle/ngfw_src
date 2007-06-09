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

package com.untangle.node.openvpn;

import java.util.Date;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmLocalContext;

import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.script.ScriptRunner;

class OpenVpnCaretaker implements Runnable
{
    private static final String KEEP_ALIVE = Constants.SCRIPT_DIR + "/keepalive";

    /* Delay a second while the thread is joining */
    private static final long   THREAD_JOIN_TIME_MSEC = 1000;

    /* Poll every 30 seconds */
    private static final long   SLEEP_TIME_MSEC = 30 * 1000;

    private final Logger logger = Logger.getLogger( this.getClass());

    /* Local context */
    private final UvmLocalContext localContext;

    /* Status of the monitor */
    private volatile boolean isAlive = false;

    /* The thread the monitor is running on */
    private Thread thread = null;

    OpenVpnCaretaker()
    {
        this.localContext = UvmContextFactory.context();
    }

    public void run()
    {
        logger.debug( "Starting" );
        
        if ( !this.isAlive ) {
            logger.error( "died before starting" );
            return;
        }

        Date nextEvent = new Date( System.currentTimeMillis() + SLEEP_TIME_MSEC );

        while ( true ) {
            /* Check if the node is still running */
            if ( !this.isAlive ) break;

            try {
                /* Give it an extra half second bit of room to ensure
                 * that the end time is after nextEvent */
                Thread.sleep( SLEEP_TIME_MSEC + 500 );
            } catch ( InterruptedException e ) {
                logger.info( "OpenVPN caretaker was interrupted" );
            }

            /* Check if the node is still running */
            if ( !this.isAlive ) break;

            /* Loop if the next iteration hasn't occured yet */
            if ( nextEvent.after( new Date())) continue;
            
            try {
                ScriptRunner.getInstance().exec( KEEP_ALIVE );
            } catch ( NodeException e ) {
                logger.warn( "Error executing script: " + KEEP_ALIVE, e );
            }
            
            /* Update the next time to check */
            nextEvent = new Date( System.currentTimeMillis() + SLEEP_TIME_MSEC );
        }
    }

    synchronized void start()
    {
        isAlive = true;

        logger.debug( "Starting OpenVPN Caretaker" );

        /* If thread is not-null, there is a running thread that thinks it is alive */
        if ( thread != null ) {
            logger.debug( "OpenVPN caretaker is already running" );
            return;
        }

        thread = this.localContext.newThread( this );
        thread.start();
    }

    synchronized void stop()
    {
        if ( thread != null ) {
            logger.debug( "Stopping OpenVPN caretaker" );
            
            isAlive = false;
            try {
                thread.interrupt();
                thread.join( THREAD_JOIN_TIME_MSEC );
            } catch ( SecurityException e ) {
                logger.error( "security exception, impossible", e );
            } catch ( InterruptedException e ) {
                logger.error( "interrupted while stopping", e );
            }
            thread = null;
        } else {
            logger.debug( "OpenVPN caretaker already stopped." );
        }
    }
}