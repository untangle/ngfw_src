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
package com.untangle.node.mail.papi.safelist;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.untangle.node.mail.papi.MessageInfo;

/**
 * Log e-mail message info.
 *
 * @author <a href="mailto:cng@untangle.com">C Ng</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_mail_safels_recipient", schema="settings")
public class SafelistRecipient implements Serializable
{
    /* constants */

    /* columns */
    private Long id;
    private String addr;

    /* constructors */
    public SafelistRecipient() {}

    public SafelistRecipient(String addr)
    {
        this.addr = addr;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="id")
    @GeneratedValue
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
     */
    @Column(nullable=false)
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
