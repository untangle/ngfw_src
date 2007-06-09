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

/**
 * Class for portal applications
 */
public final class Application
    implements Comparable<Application>, Serializable
{

    public interface Validator
    {
        boolean isValid(String target);
        // String whyNotValid(String target);
    }


    /**
     * For host services, the destinator is used to determine the
     * destination given the bookmark
     */
    public interface Destinator
    {
        String getDestinationHost(Bookmark bm);

        int getDestinationPort(Bookmark bm);
    }

    private final String name;
    private final String description;
    private final String longDescription;
    private final Destinator destinator;
    private final Validator validator;
    private final int sortPosition;
    private final String appJsUrl;

    public Application(String name, String description, String longDescription,
                       Destinator destinator, Validator validator,
                       int sortPosition, String appJsUrl)
    {
        this.name = name;
        this.description = description;
        this.longDescription = longDescription;
        this.destinator = destinator;
        this.validator = validator;
        this.sortPosition = sortPosition;
        this.appJsUrl = appJsUrl;
    }

    /**
     * Name of the application.  This is also its key.  Example: "SSH".
     *
     * @return a <code>String</code> giving the name of the application.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Short description of the application.  Example "Secure Shell".
     *
     * @return a <code>String</code> giving a short description of the
     * application.
     */
    public String getDescription()
    {
        return description;
    }


    /**
     * Long description of the application.  Example "Secure Shell".
     *
     * @return a <code>String</code> giving a long description of the
     * application.
     */
    public String getLongDescription()
    {
        return longDescription;
    }

    /**
     * True if the application is a host-based service.  For example,
     * CIFS and Email are not host-based services, SSH and HTTP are.
     *
     * @return a <code>boolean</code> true if the application is a
     * host-based service
     */
    public boolean isHostService()
    {
        return (destinator != null);
    }

    public Destinator getDestinator()
    {
        return destinator;
    }


    public String getAppJsUrl()
    {
        return appJsUrl;
    }

    /**
     * Validates the given target, returning true if the target is
     * syntactically value for the application.
     *
     * @param target a <code>String</code> value.
     * @return a <code>boolean</code> true if the target is valid.
     */
    public boolean isValid(String target)
    {
        return validator.isValid(target);
    }

    // Comparable methods -----------------------------------------------------

    public int compareTo(Application app)
    {
        if (sortPosition == app.sortPosition) {
            return name.compareTo(app.name);
        } else {
            return sortPosition < app.sortPosition ? -1 : 1;
        }
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof Application)) {
            return false;
        }

        Application a = (Application)o;
        return name.equals(a.name) && sortPosition == a.sortPosition;
    }

    public int hashCode()
    {
        return name.hashCode();
    }
}
