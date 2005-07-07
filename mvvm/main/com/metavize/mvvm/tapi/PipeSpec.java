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

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.metavize.mvvm.tran.Transform;

/**
 * Describes the fittings for this pipe.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class PipeSpec
{
    private final String name;
    private final Transform transform;

    private transient Set<Subscription> subscriptions;

    // constructors -----------------------------------------------------------

    /**
     * Creates a new PipeSpec, with a subscription for all traffic.
     *
     * @param name display name for this MPipe.
     */
    protected PipeSpec(String name, Transform transform)
    {
        this.name = name;
        this.transform = transform;

        this.subscriptions = new CopyOnWriteArraySet<Subscription>();
        this.subscriptions.add(new Subscription(Protocol.TCP));
        this.subscriptions.add(new Subscription(Protocol.UDP));
    }

    /**
     * Creates a new PipeSpec.
     *
     * @param name display name of the PipeSpec.
     * @param subscriptions set of Subscriptions.
     */
    protected PipeSpec(String name, Transform transform, Set subscriptions)
    {
        this.name = name;
        this.transform = transform;

        this.subscriptions = null == subscriptions
            ? new CopyOnWriteArraySet<Subscription>()
            : new CopyOnWriteArraySet<Subscription>(subscriptions);
    }

    /**
     * Creates a new PipeSpec.
     *
     * @param name display name of the PipeSpec.
     * @param subscription the Subscription.
     */
    protected PipeSpec(String name, Transform transform,
                       Subscription subscription)
    {
        this.name = name;
        this.transform = transform;

        this.subscriptions = new CopyOnWriteArraySet<Subscription>();
        if (null != subscription) {
            this.subscriptions.add(subscription);
        }
    }

    // public abstract methods ------------------------------------------------

    public abstract void connectMPipe();
    public abstract void disconnectMPipe();
    public abstract void dumpSessions();
    public abstract IPSessionDesc[] liveSessionDescs();

    // public methods ---------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public Transform getTransform()
    {
        return transform;
    }

    public boolean matches(com.metavize.mvvm.argon.IPSessionDesc sessionDesc)
    {
        Set s = subscriptions;

        for (Iterator i = s.iterator(); i.hasNext(); ) {
            Subscription subscription = (Subscription)i.next();
            if (subscription.matches(sessionDesc)) {
                return true;
            }
        }

        return false;
    }

    public void setSubscriptions(Set<Subscription> subscriptions)
    {
        this.subscriptions = new CopyOnWriteArraySet<Subscription>(subscriptions);
    }

    public void addSubscription(Subscription subscription)
    {
        subscriptions.add(subscription);
    }

    public void removeSubscription(Subscription subscription)
    {
        subscriptions.remove(subscription);
    }
}
