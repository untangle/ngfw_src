/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MimeTypeRule.java,v 1.7 2005/03/12 01:54:31 amread Exp $
 */

package com.metavize.mvvm.tran;


/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="MIMETYPE_RULE"
 */
public class MimeTypeRule extends Rule
{
    private MimeType mimeType;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public MimeTypeRule() { }

    public MimeTypeRule(MimeType mimeType)
    {
        this.mimeType = mimeType;
    }

    public MimeTypeRule(MimeType mimeType, boolean live)
    {
        super(live);
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
     * @hibernate.property
     * column="MIME_TYPE"
     * type="com.metavize.mvvm.type.MimeTypeUserType"
     */
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
