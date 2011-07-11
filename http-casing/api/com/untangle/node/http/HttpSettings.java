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

package com.untangle.node.http;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Http casing settings.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_http_settings", schema="settings")
@SuppressWarnings("serial")
public class HttpSettings implements Serializable
{
    public static final int MIN_URI_LENGTH = 1024;
    public static final int MAX_URI_LENGTH = 4096;


    private Long id;

    private boolean enabled = true;
    private boolean nonHttpBlocked = false;
    private boolean blockLongHeaders = false;
    private int maxUriLength = MAX_URI_LENGTH;
    private boolean blockLongUris = false;

    // constructors -----------------------------------------------------------

    public HttpSettings() { }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="settings_id")
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
        return 8192;
    }

    public void setMaxHeaderLength(int maxHeaderLength)
    {
        return;
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
