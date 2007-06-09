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

package com.untangle.uvm.portal;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Bookmark for portal use.  Points to a target for a given application
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@Entity
@Table(name="portal_bookmark", schema="settings")
public class Bookmark implements Serializable
{
    private static final long serialVersionUID = -7681114679064384937L;

    private Long id;
    private String name = "";
    private String applicationName = "HTTP";
    private String target = "";

    // constructors -----------------------------------------------------------

    public Bookmark() { }

    /**
     * Constructor does not check that the applicationName is valid.
     *
     * @param name a <code>String</code> value
     * @param applicationName a <code>String</code> value
     * @param target a <code>String</code> value
     */
    public Bookmark(String name, String applicationName, String target)
    {
        this.name = name;
        this.applicationName = applicationName;
        this.target = target;
    }

    public Bookmark(String name, Application application, String target)
    {
        this.name = name;
        this.applicationName = application.getName();
        this.target = target;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="id")
    @GeneratedValue
    public Long getId()
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
     * Get the target of the bookmark.  This is application dependent, but is often
     * a hostname or IP address.
     *
     * @return target.
     */
    public String getTarget()
    {
        return target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    /**
     * Gets the application name of the bookmark.
     *
     * @return application name;
     */
    @Column(name="application_name")
    public String getApplicationName()
    {
        return applicationName;
    }

    public void setApplicationName(String applicationName)
    {
        this.applicationName = applicationName;
    }

    // Object methods ----------------------------------------------------------
    public int hashCode()
    {
        if ( name == null || applicationName == null || target == null )
            // shouldn't happen
            return 0;

        return name.hashCode() * 37 + applicationName.hashCode() * 7 + target.hashCode();
    }

    public boolean equals( Object o )
    {
        if (!(o instanceof Bookmark ))
            return false;

        Bookmark other = (Bookmark)o;
        if (name.equals(other.name) &&
            applicationName.equals(other.applicationName) &&
            target.equals(other.target))
            // idle time and group aren't important.
            return true;

        return false;
    }
}
