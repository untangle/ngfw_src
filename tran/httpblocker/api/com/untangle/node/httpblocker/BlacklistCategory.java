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

package com.untangle.tran.httpblocker;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Settings for a Blacklist category.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_httpblk_blcat", schema="settings")
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
    private boolean logOnly = false;

    public BlacklistCategory() { }

    public BlacklistCategory(String name, String displayName,
                             String description)
    {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="category_id")
    @GeneratedValue
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
     */
    @Column(name="display_name")
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
     */
    @Column(name="block_domains", nullable=false)
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
     */
    @Column(name="block_urls", nullable=false)
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
     */
    @Column(name="block_expressions", nullable=false)
    public boolean getBlockExpressions()
    {
        return blockExpressions;
    }

    public void setBlockExpressions(boolean blockExpressions)
    {
        this.blockExpressions = blockExpressions;
    }

    /**
     * Should items be logged only.
     *
     * @return true of logged only
     */
    @Column(name="log_only", nullable=false)
    public boolean getLogOnly()
    {
        return logOnly;
    }

    public void setLogOnly(boolean logOnly)
    {
        this.logOnly = logOnly;
    }
}
