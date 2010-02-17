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

package com.untangle.node.spam;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * Hibernate mappings for this class are in the UVM resource
 * directory.
 */
@MappedSuperclass
public abstract class SpamProtoConfig implements Serializable
{
    public static final int DEFAULT_MESSAGE_SIZE_LIMIT = 1 << 18;
    public static final int DEFAULT_STRENGTH = 43;
    public static final boolean DEFAULT_ADD_SPAM_HEADERS = false;
    public static final boolean DEFAULT_SCAN = false;
    public static final String DEFAULT_HEADER_NAME = "X-Spam-Flag";
    private Long id;

    /* settings */
    private boolean bScan = this.DEFAULT_SCAN;
    private int strength = this.DEFAULT_STRENGTH;
    private boolean addSpamHeaders = this.DEFAULT_ADD_SPAM_HEADERS;
    private int msgSizeLimit = this.DEFAULT_MESSAGE_SIZE_LIMIT;
    private String headerName = this.DEFAULT_HEADER_NAME;

    // constructors -----------------------------------------------------------

    protected SpamProtoConfig() { }

    protected SpamProtoConfig(boolean bScan,
                              int strength,
                              boolean addSpamHeaders,
                              String headerName) {
        this.bScan = bScan;
        this.strength = strength;
        this.addSpamHeaders = addSpamHeaders;
        this.headerName = headerName;

    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="config_id")
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
        return;
    }

    /**
     * Get the name of the header (e.g. "X-SPAM") used to indicate the
     * SPAM/HAM value of this email
     */
    @Transient
    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }


    /**
     * scan: a boolean specifying whether or not to scan a message for
     * spam (defaults to true)
     *
     * @return whether or not to scan message for spam
     */
    @Column(nullable=false)
    public boolean getScan()
    {
        return bScan;
    }

    public void setScan(boolean bScan)
    {
        this.bScan = bScan;
        return;
    }

    /**
     * strength: an integer giving scan strength.  Divide by 10 to get
     * SpamAssassin strength.  Thus range should be something like: 30
     * to 100
     *
     * @return an <code>int</code> giving the spam strength * 10
     */
    @Column(nullable=false)
    public int getStrength()
    {
        return strength;
    }

    public void setStrength(int strength)
    {
        this.strength = strength;
    }

    @Column(name="add_spam_headers", nullable=false)
    public boolean getAddSpamHeaders()
    {
        return addSpamHeaders;
    }

    public void setAddSpamHeaders(boolean addSpamHeaders)
    {
        this.addSpamHeaders = addSpamHeaders;
    }

    /**
     * msgSizeLimit: an integer giving scan message size limit.  Files
     * over this size are presumed not to be spam, and not scanned for
     * performance reasons.
     *
     * @return an <code>int</code> giving the spam message size limit
     * (cutoff) in bytes.
     */
    @Column(name="msg_size_limit", nullable=false)
    public int getMsgSizeLimit()
    {
        return msgSizeLimit;
    }

    public void setMsgSizeLimit(int msgSizeLimit)
    {
        this.msgSizeLimit = msgSizeLimit;
    }

    // Help for the UI follows.
    public static final int LOW_STRENGTH = 50;
    public static final int MEDIUM_STRENGTH = 43;
    public static final int HIGH_STRENGTH = 35;
    public static final int VERY_HIGH_STRENGTH = 33;
    public static final int EXTREME_STRENGTH = 30;
}
