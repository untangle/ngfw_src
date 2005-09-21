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

package com.metavize.tran.httpblocker;

import java.io.Serializable;

/**
 * Settings for a Blacklist category.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_HTTPBLK_BLCAT"
 */
public class BlacklistCategory implements Serializable
{
    private static final long serialVersionUID = 445403437262316857L;

    private Long id;
    private String name;
    private String displayName;
    private String description;
    private boolean blockDomains = false;
    private boolean blockUrls = false;
    private boolean blockExpressions = false;

    /**
     * Hibernate constructor.
     */
    public BlacklistCategory() { }

    public BlacklistCategory(String name, String displayName,
                             String description)
    {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="CATEGORY_ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * The internal category name.
     *
     * @return the category name.
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
     * Name for UI.
     *
     * @return this display name.
     * @hibernate.property
     * column="DISPLAY_NAME"
     */
    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * Description of the category.
     *
     * @return the description.
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
     * Domain block rules are used when blockDomains is true.
     *
     * @return true if domain block rules are used.
     * @hibernate.property
     * column="BLOCK_DOMAINS"
     */
    public boolean getBlockDomains()
    {
        return blockDomains;
    }

    public void setBlockDomains(boolean blockDomains)
    {
        this.blockDomains = blockDomains;
    }

    /**
     * URL block rules are used when blockUrls is true.
     *
     * @return true when URL rules are used.
     * @hibernate.property
     * column="BLOCK_URLS"
     */
    public boolean getBlockUrls()
    {
        return blockUrls;
    }

    public void setBlockUrls(boolean blockUrls)
    {
        this.blockUrls = blockUrls;
    }

    /**
     * Expressions are used for blocking when blockExpressions is true.
     *
     * @return a <code>boolean</code> value
     * @hibernate.property
     * column="BLOCK_EXPRESSIONS"
     */
    public boolean getBlockExpressions()
    {
        return blockExpressions;
    }

    public void setBlockExpressions(boolean blockExpressions)
    {
        this.blockExpressions = blockExpressions;
    }
}
