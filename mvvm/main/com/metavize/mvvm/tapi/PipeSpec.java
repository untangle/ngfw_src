/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: PipeSpec.java,v 1.1 2005/01/30 09:20:31 amread Exp $
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
public class PipeSpec
{
    private String name;
    private Fitting input;
    private Fitting output;
    private Affinity affinity;
    private Set subscriptions;

    // constructors -----------------------------------------------------------

    /**
     * Make a spec for a pipe that does not change the stream's type
     * using a set of subscriptions.
     *
     * @param name name for debugging.
     * @param type type of input and output.
     * @param subscriptions multiple subscriptions.
     * @param affinity where in pipeline.
     */
    public PipeSpec(String name, Fitting type, Set subscriptions,
                    Affinity affinity)
    {
        this.name = name;
        this.input = this.output = type;
        this.subscriptions = subscriptions;
        this.affinity = affinity;
    }

    /**
     * Make a spec for a pipe that does not change the stream's type
     * using a single subscription.
     *
     * @param name name for debugging.
     * @param type type of input and output.
     * @param subscription a single subscription.
     * @param affinity where in pipeline.
     */
    public PipeSpec(String name, Fitting type, Subscription subscription,
                    Affinity affinity)
    {
        this.name = name;
        this.input = this.output = type;
        this.subscriptions = new HashSet();
        this.subscriptions.add(subscription);
        this.affinity = affinity;
    }

    /**
     * Make a spec for a pipe that changes the stream's type using a
     * set of subscriptions.
     *
     * @param name name for debugging.
     * @param input type of input.
     * @param output type of output.
     * @param subscriptions multiple subscriptions.
     */
    public PipeSpec(String name, Fitting input, Fitting output,
                    Set subscriptions)
    {
        this.name = name;
        this.input = input;
        this.output = output;
        this.subscriptions = subscriptions;
    }

    /**
     * Make a spec for a pipe that changes the stream's type using a
     * single subscription.
     *
     * @param name name for debugging.
     * @param input type of input.
     * @param output type of output.
     * @param subscription the subscription.
     */
    public PipeSpec(String name, Fitting input, Fitting output,
                    Subscription subscription)
    {
        this.name = name;
        this.input = input;
        this.output = output;
        this.subscriptions = new HashSet();
        this.subscriptions.add(subscription);
    }

    /**
     * Make a spec for a pipe that changes the stream's type using a
     * single subscription.
     *
     * @param name name for debugging.
     * @param input type of input.
     * @param output type of output.
     */
    public PipeSpec(String name, Fitting type, Affinity affinity)
    {
        this.name = name;
        this.input = this.output = type;
        this.affinity = affinity;
        this.subscriptions = new HashSet();
    }

    // business methods -------------------------------------------------------

    public boolean addSubscription(Subscription sub)
    {
        if (null == subscriptions) {
            subscriptions = new HashSet();
        }

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

        return false;
    }

    // accessors --------------------------------------------------------------

    public Set getSubscriptions()
    {
        return subscriptions;
    }

    public void setSubscriptions(Set subscriptions)
    {
        this.subscriptions = subscriptions;
    }

    public Fitting getOutput()
    {
        return output;
    }

    public Fitting getInput()
    {
        return input;
    }

    public Affinity getAffinity()
    {
        return affinity;
    }

    public String getName()
    {
        return name;
    }
}
