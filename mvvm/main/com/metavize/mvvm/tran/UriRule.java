/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UriRule.java,v 1.3 2005/02/10 22:28:12 jdi Exp $
 */

package com.metavize.mvvm.tran;

import java.net.URI;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="URI_RULE"
 */
public class UriRule extends Rule
{
    private URI uri;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public UriRule() { }

    /**
     * USE THIS ONE to create a UriRule with an initial URI value.
     */
    public UriRule(URI uri) {
        this.uri = uri;
    }

    // accessors --------------------------------------------------------------

    /**
     * The URI.
     *
     * @return the URI.
     * @hibernate.property
     * column="URI"
     * type="com.metavize.mvvm.type.UriUserType"
     */
    public URI getUri()
    {
        return uri;
    }

    public void setUri(URI uri)
    {
        this.uri = uri;
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof UriRule)) {
            return false;
        }

        UriRule ur = (UriRule)o;
        return uri.equals(ur.uri);
    }

    public int hashCode()
    {
        return uri.hashCode();
    }
}
