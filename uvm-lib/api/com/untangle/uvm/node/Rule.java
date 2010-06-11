/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Abstract class for rules.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@MappedSuperclass
public abstract class Rule implements Serializable
{

    public static final String EMPTY_NAME        = "[no name]";
    public static final String EMPTY_DESCRIPTION = "[no description]";
    public static final String EMPTY_CATEGORY    = "[no category]";

    private Long id;
    private String name = EMPTY_NAME;
    private String category = EMPTY_CATEGORY;
    private String description = EMPTY_DESCRIPTION;
    // XXX we need to set hibernate & SQL NOT NULL on these
    private boolean live = true;
    private boolean alert = false;
    private boolean log = false;

    // constructors -----------------------------------------------------------

	public Rule() { }

    public Rule(boolean live)
    {
        this.live = live;
    }

    public Rule(boolean live, String name)
    {
        this.live = live;
        this.name = name;
    }

    public Rule(String name, String category, String description, boolean live)
    {
        this.live = live;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public Rule(String name, String category, String description)
    {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public Rule(String name, String category)
    {
        this.name = name;
        this.category = category;
    }

    public Rule(String name, String category, boolean live)
    {
        this.name = name;
        this.category = category;
        this.live = live;
    }

    
    // accessors --------------------------------------------------------------

    @Id
    @Column(name="rule_id")
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
     * Get a name for display purposes.
     *
     * @return name.
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
     */
    public boolean getLog()
    {
        return log;
    }

    public void setLog(boolean log)
    {
        this.log = log;
    }

    
    public void update(Rule rule) {
		this.name = rule.name;
		this.category = rule.category;
		this.description = rule.description;
		this.live = rule.live;
		this.alert = rule.alert;
		this.log = rule.log;
	}

}
