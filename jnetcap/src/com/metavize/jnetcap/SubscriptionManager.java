/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: SubscriptionManager.java,v 1.2 2005/01/31 00:53:03 rbscott Exp $
 */

package com.metavize.jnetcap;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;


public class SubscriptionManager
{
    private final List<Subscription> subscriptionList = new LinkedList<Subscription>();
    
    public SubscriptionManager()
    {
        /* Not much to do here */
    }

    /**
     * Add a subscription from the subscription manager, returns true
     * if the subscription was added (Always true) 
     */
    public synchronized boolean add( Subscription subscription )
    {
        return subscriptionList.add( subscription );
    }

    /**
     * Remove a subscription from the subscription manager, returns true
     * if the subscription was removed 
     */
    public synchronized boolean remove( Subscription subscription )
    {
        return subscriptionList.remove( subscription );
    }

    public synchronized void unsubscribeAll()
    {
        /* Iterate through all of the subscriptions unsubscribing */
        for ( Iterator<Subscription> iter = subscriptionList.iterator(); iter.hasNext() ; ) {
            Subscription subscription = iter.next();
            try {
                subscription.unsubscribe();
            } catch ( Exception e ) {
                Netcap.logError( "Unable to unsubscribe: " + subscription + e );
            }

            /* Remove this item */
            iter.remove();
        }
    }
}

