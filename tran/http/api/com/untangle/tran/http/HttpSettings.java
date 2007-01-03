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

package com.untangle.tran.http;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Http casing settings.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_http_settings", schema="settings")
public class HttpSettings implements Serializable
{
    public static final int MIN_HEADER_LENGTH = 1024;
    public static final int MAX_HEADER_LENGTH = 8192;
    public static final int MIN_URI_LENGTH = 1024;
    public static final int MAX_URI_LENGTH = 4096;

    private static final long serialVersionUID = -8901463578794639216L;

    private Long id;

    private boolean enabled = true;
    private boolean nonHttpBlocked = false;
    private int maxHeaderLength = MAX_HEADER_LENGTH;
    private boolean blockLongHeaders = false;
    private int maxUriLength = MAX_URI_LENGTH;
    private boolean blockLongUris = false;

    // constructors -----------------------------------------------------------

    public HttpSettings() { }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="settings_id")
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
     * Enabled status for casing.
     *
     * @return true when casing is enabled, false otherwise.
     */
    @Column(nullable=false)
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
     */
    @Column(name="non_http_blocked", nullable=false)
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
     */
    @Column(name="max_header_length", nullable=false)
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

    /**
     * Enable blocking of headers that exceed maxHeaderLength. If not
     * explicitly blocked the connection is treated as non-HTTP and
     * the behavior is determined by setNonHttpBlocked.
     *
     * @return true if connections containing long headers are blocked.
     */
    @Column(name="block_long_headers", nullable=false)
    public boolean getBlockLongHeaders()
    {
        return blockLongHeaders;
    }

    public void setBlockLongHeaders(boolean blockLongHeaders)
    {
        this.blockLongHeaders = blockLongHeaders;
    }

    /**
     * Maximum allowable URI length.
     *
     * @return maximum characters allowed in the request-line URI.
     */
    @Column(name="max_uri_length", nullable=false)
    public int getMaxUriLength()
    {
        return maxUriLength;
    }

    public void setMaxUriLength(int maxUriLength)
    {
        if (MIN_URI_LENGTH > maxUriLength
            || MAX_URI_LENGTH < maxUriLength) {
            throw new IllegalArgumentException("out of bounds: "
                                               + maxUriLength);
        }
        this.maxUriLength = maxUriLength;
    }

    /**
     * Enable blocking of URIs that exceed maxUriLength. If not
     * explicitly blocked the connection is treated as non-HTTP and
     * the behavior is determined by setNonHttpBlocked.
     *
     * @return true if connections containing long URIs are blocked.
     */
    @Column(name="block_long_uris", nullable=false)
    public boolean getBlockLongUris()
    {
        return blockLongUris;
    }

    public void setBlockLongUris(boolean blockLongUris)
    {
        this.blockLongUris = blockLongUris;
    }
}
