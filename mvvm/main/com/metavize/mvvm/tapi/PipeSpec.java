/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tapi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.metavize.mvvm.argon.IPSessionDesc;


/**
 * Describes the fittings for this pipe.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class PipeSpec
{
    private final String name;
    private final Set subscriptions;

    // constructors -----------------------------------------------------------

    protected PipeSpec(String name)
    {
        this.name = name;
        this.subscriptions = new HashSet();
    }

    protected PipeSpec(String name, Set subscriptions)
    {
        this.name = name;
        this.subscriptions = null == subscriptions ? new HashSet()
            :  subscriptions;
    }

    protected PipeSpec(String name, Subscription subscription)
    {
        this.name = name;

        this.subscriptions = new HashSet();

        if (null != subscription) {
            subscriptions.add(subscription);
        }
    }

    // business methods -------------------------------------------------------

    public void setSubscriptions(Set subscriptions)
    {
        this.subscriptions.clear();
        this.subscriptions.addAll(subscriptions);
    }

    public boolean addSubscription(Subscription sub)
    {
        return subscriptions.add(sub);
    }

    public boolean matches(IPSessionDesc sessionDesc)
    {
        for (Iterator i = subscriptions.iterator(); i.hasNext(); ) {
            Subscription subscription = (Subscription)i.next();
            if (subscription.matches(sessionDesc)) {
                return true;
            }
        }

        System.out.println("PipeSpec not matches");
        return false;
    }

    // accessors --------------------------------------------------------------

    public String getName()
    {
        return name;
    }
}
