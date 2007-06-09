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

package com.untangle.mvvm.tran;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="mimetype_rule", schema="settings")
public class MimeTypeRule extends Rule
{
    private MimeType mimeType;

    // constructors -----------------------------------------------------------

    public MimeTypeRule() { }

    // XXX inconstant constuctor
    public MimeTypeRule(MimeType mimeType)
    {
        this.mimeType = mimeType;
    }

    // XXX inconstant constuctor
    public MimeTypeRule(MimeType mimeType, boolean live)
    {
        super(live);
        this.mimeType = mimeType;
    }

    // XXX inconstant constuctor
    public MimeTypeRule(MimeType mimeType, String name, String category,
                        String description, boolean live)
    {
        super(name, category, description, live);
        this.mimeType = mimeType;
    }

    public MimeTypeRule(MimeType mimeType, String name, String category,
                        boolean live)
    {
        super(name, category, live);
        this.mimeType = mimeType;
    }

    // accessors --------------------------------------------------------------

    /**
     * The MimeType.
     *
     * @return the mime-type.
     */
    @Column(name="mime_type")
    @Type(type="com.untangle.mvvm.type.MimeTypeUserType")
    public MimeType getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(MimeType mimeType)
    {
        this.mimeType = mimeType;
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof MimeTypeRule)) {
            return false;
        }

        MimeTypeRule mtr = (MimeTypeRule)o;
        return mimeType.equals(mtr.mimeType);
    }

    public int hashCode()
    {
        return mimeType.hashCode();
    }
}
