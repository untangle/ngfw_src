/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.webfilter;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Settings for a Blacklist category.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_webfilter_blcat", schema="settings")
@SuppressWarnings("serial")
public class BlacklistCategory implements Serializable
{
    public static final String UNCATEGORIZED = "Uncategorized";


    private Long id;
    private String name;
    private String displayName;
    private String description;
    private boolean block = false;
    private boolean log = false;

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
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
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
     * Block rules are used when block is true.
     *
     * @return true if domain block rules are used.
     */
    @Column(name="block", nullable=false)
    public boolean getBlock()
    {
        return block;
    }

    public void setBlock(boolean block)
    {
        this.block = block;
    }

    /**
     * Should items be logged.
     *
     * @return true of logged
     */
    @Column(name="log", nullable=false)
    public boolean getLog()
    {
        return log;
    }

    public void setLog(boolean log)
    {
        this.log = log;
    }

    public void update(BlacklistCategory blacklistCategory)
    {
        this.name = blacklistCategory.name;
        this.displayName = blacklistCategory.displayName;
        this.description = blacklistCategory.description;
        this.block = blacklistCategory.block;
        this.log = blacklistCategory.log;
    }
}
