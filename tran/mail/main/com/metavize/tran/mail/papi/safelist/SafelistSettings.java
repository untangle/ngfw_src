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

package com.metavize.tran.mail.papi.safelist;

import java.io.Serializable;
//import org.apache.log4j.Logger;

/**
 * Setting for safelist (recipient and sender pair).
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_MAIL_SAFELS_SETTINGS"
 */
public class SafelistSettings implements Serializable
{
    //private final Logger logger = Logger.getLogger(SafelistSettings.class);

    private static final long serialVersionUID = -7466793822226799781L;

    private Long id;

    private SafelistRecipient recipient;
    private SafelistSender sender;

    // constructors -----------------------------------------------------------

    public SafelistSettings() {}

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="SAFELS_ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;

        return;
    }

    /**
     * @return the recipient of this safelist
     * @hibernate.many-to-one
     * column="RECIPIENT"
     * cascade="all"
     * not-null="true"
     */
    public SafelistRecipient getRecipient()
    {
        return recipient;
    }

    public void setRecipient(SafelistRecipient recipient)
    {
        this.recipient = recipient;

        return;
    }

    /**
     * @return the sender of this safelist
     * @hibernate.many-to-one
     * column="SENDER"
     * cascade="all"
     * not-null="true"
     */
    public SafelistSender getSender()
    {
        return sender;
    }

    public void setSender(SafelistSender sender)
    {
        this.sender = sender;

        return;
    }

    // Object methods
    public boolean equals(Object o)
    {
        if (!(o instanceof SafelistSettings)) {
            return false;
        }

        SafelistSettings sls = (SafelistSettings)o;
        return recipient.equals(sls.recipient) && sender.equals(sls.sender);
    }

    public int hashCode()
    {
        return recipient.hashCode() + sender.hashCode();
    }
}
