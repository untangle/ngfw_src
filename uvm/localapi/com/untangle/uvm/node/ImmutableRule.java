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

package com.untangle.uvm.node;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class ImmutableRule
{
    private final String name;
    private final String category;
    private final String description;

    private final boolean live;
    private final boolean alert;
    private final boolean log;

    // constructors -----------------------------------------------------------

    protected ImmutableRule( Rule rule )
    {
        this( rule.isLive(), rule.getName(), rule.getCategory(), 
              rule.getDescription(), rule.getAlert(), rule.getLog());
    }

    protected ImmutableRule( boolean live, String name, String category, String description,
                             boolean alert, boolean log )
    {
        this.live = live;
        this.name = name;
        this.category = category;
        this.description = description;
        this.alert = alert;
        this.log = log;
    }


    // accessors --------------------------------------------------------------

    /**
     * Get a name for display purposes.
     *
     * @return name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get a category for display purposes.
     *
     * @return category.
     */
    public String getCategory()
    {
        return category;
    }

    /**
     * Get a description for display purposes.
     *
     * @return human description;
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Will the rule be used for matching?
     *
     * @return true if this address is matched.
     */
    public boolean isLive()
    {
        return live;
    }

    /**
     * Should admin be alerted.
     *
     * @return true if alerts should be sent.
     */
    public boolean getAlert()
    {
        return alert;
    }

    /**
     * Should admin be logged.
     *
     * @return true if should be logged.
     */
    public boolean getLog()
    {
        return log;
    }

    /* ----------------- Protected ----------------- */
    /* This fills in Rule with all of the values from here */
    protected void toRule( Rule rule )
    {
        rule.setLive( isLive());
        rule.setName( getName());
        rule.setCategory( getCategory());
        rule.setDescription( getDescription());
        rule.setAlert( getAlert());
        rule.setLog( getLog());
    }
}
