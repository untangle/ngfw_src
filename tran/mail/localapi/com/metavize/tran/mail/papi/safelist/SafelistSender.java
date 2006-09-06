/*
 * Copyright (c) 2005, 2006 Metavize Inc.
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

import com.metavize.tran.mail.papi.MessageInfo;

/**
 * Log e-mail message info.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_MAIL_SAFELS_SENDER"
 */
public class SafelistSender implements Serializable
{
    /* constants */

    /* columns */
    private Long id;
    private String addr;

    /* constructors */
    public SafelistSender() {}

    public SafelistSender(String addr)
    {
        this.addr = addr;
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    protected Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;

        return;
    }

    /**
     * The email address, in RFC822 format
     *
     * @return email address.
     * @hibernate.property
     * column="ADDR"
     * not-null="true"
     */
    public String getAddr()
    {
        return addr;
    }

    public void setAddr(String addr)
    {
        if (addr.length() > MessageInfo.DEFAULT_STRING_SIZE) {
            addr = addr.substring(0, MessageInfo.DEFAULT_STRING_SIZE);
        }
        this.addr = addr;

        return;
    }
}
