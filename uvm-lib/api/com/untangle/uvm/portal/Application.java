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

package com.untangle.uvm.portal;

import java.io.Serializable;

/**
 * Class for portal applications
 */
public final class Application
    implements Comparable<Application>, Serializable
{
    private static final long serialVersionUID = 3175632248906996934L;

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
        String getDestinationHost(Object bookmark);

        int getDestinationPort(Object bookmark);
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
