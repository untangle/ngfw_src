/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Rule.java,v 1.10 2005/03/12 01:54:31 amread Exp $
 */

package com.metavize.mvvm.tran;

import java.io.Serializable;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="RULE"
 */
public class Rule implements Serializable
{
    private static final long serialVersionUID = -7861114769604834397L;

    public static final String EMPTY_NAME        = "[no name]";
    public static final String EMPTY_DESCRIPTION = "[no description]";
    public static final String EMPTY_CATEGORY    = "[no category]";

    private Long id;
    private String name = EMPTY_NAME;
    private String category = EMPTY_CATEGORY;
    private String description = EMPTY_DESCRIPTION;
    private boolean live = true;
    private boolean alert = false;
    private boolean log = false;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public Rule() { }

    public Rule(String name, String description, boolean live)
    {
        this.name = name;
        this.description = description;
        this.live = live;
    }

    public Rule(String name, String description)
    {
        this.name = name;
        this.description = description;
    }

    public Rule(boolean live)
    {
        this.live = live;
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="RULE_ID"
     * generator-class="native"
     */
    protected Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Get a name for display purposes.
     *
     * @return name.
     * @hibernate.property
     * column="NAME"
     */
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get a category for display purposes.
     *
     * @return category.
     * @hibernate.property
     * column="CATEGORY"
     */
    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }


    /**
     * Get a description for display purposes.
     *
     * @return human description;
     * @hibernate.property
     * column="DESCRIPTION"
     */
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * Will the rule be used for matching?
     *
     * @return true if this address is matched.
     * @hibernate.property
     * column="LIVE"
     */
    public boolean isLive()
    {
        return live;
    }

    public void setLive(boolean live)
    {
        this.live = live;
    }

    /**
     * Should admin be alerted.
     *
     * @return true if alerts should be sent.
     * @hibernate.property
     * column="ALERT"
     */
    public boolean getAlert()
    {
        return alert;
    }

    public void setAlert(boolean alert)
    {
        this.alert = alert;
    }

    /**
     * Should admin be logged.
     *
     * @return true if should be logged.
     * @hibernate.property
     * column="LOG"
     */
    public boolean getLog()
    {
        return log;
    }

    public void setLog(boolean log)
    {
        this.log = log;
    }

}
