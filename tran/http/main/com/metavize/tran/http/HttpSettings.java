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

package com.metavize.tran.http;

import java.io.Serializable;

import com.metavize.mvvm.security.Tid;

/**
 * Http casing settings.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_HTTP_SETTINGS"
 */
public class HttpSettings implements Serializable
{
    public static final int MIN_HEADER_LENGTH = 1024;
    public static final int MAX_HEADER_LENGTH = 4096;

    private static final long serialVersionUID = -8901463578794639216L;

    private Long id;
    private Tid tid;

    private boolean enabled = true;
    private boolean nonHttpBlocked = false;
    private int maxHeaderLength = MAX_HEADER_LENGTH;

    // constructors -----------------------------------------------------------

    public HttpSettings() { }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
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
     * Transform id for these settings.
     *
     * @return tid for these settings.
     * @hibernate.many-to-one
     * column="TID"
     * unique="true"
     * not-null="true"
     */
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Enabled status for casing.
     *
     * @return true when casing is enabled, false otherwise.
     * @hibernate.property
     * column="ENABLED"
     * not-null="true"
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Enables non-http traffic on port 80.
     *
     * @return a <code>boolean</code> value
     * @hibernate.property
     * column="NON_HTTP_BLOCKED"
     * not-null="true"
     */
    public boolean isNonHttpBlocked()
    {
        return nonHttpBlocked;
    }

    public void setNonHttpBlocked(boolean nonHttpBlocked)
    {
        this.nonHttpBlocked = nonHttpBlocked;
    }

    /**
     * Maximum allowable header length.
     *
     * @return maximum characters allowed in a HTTP header.
     * @hibernate.property
     * column="MAX_HEADER_LENGTH"
     * not-null="true"
     */
    public int getMaxHeaderLength()
    {
        return maxHeaderLength;
    }

    public void setMaxHeaderLength(int maxHeaderLength)
    {
        if (MIN_HEADER_LENGTH > maxHeaderLength
            || MAX_HEADER_LENGTH < maxHeaderLength) {
            throw new IllegalArgumentException("out of bounds: "
                                               + maxHeaderLength);
        }
        this.maxHeaderLength = maxHeaderLength;
    }
}
